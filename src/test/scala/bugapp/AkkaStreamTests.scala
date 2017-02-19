package bugapp

import akka.NotUsed
import akka.actor.ActorSystem
import akka.util.ByteString

import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths
import java.time.OffsetDateTime

import akka.stream._
import akka.stream.scaladsl._
import bugapp.bugzilla._
import bugapp.Implicits._
import bugapp.stream.CirceStreamSupport
import org.scalatest.FunSuite

/**
  *
  */
class AkkaStreamTests extends FunSuite with AkkaConfig with CirceStreamSupport {

  test("Simple Stream") {
    implicit val system = ActorSystem()
    implicit val ec = system.dispatcher
    implicit val materializer = ActorMaterializer()

    val source = Source(1 to 1000)
    val sink = Sink.seq[Int]

    val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

    val result: Future[IOResult] =
      factorials
        .map(num => ByteString(s"$num\n"))
        .runWith(FileIO.toPath(Paths.get("factorials.txt")))

  }

  test("Stream sequence") {
    implicit val system = ActorSystem()
    implicit val ec = system.dispatcher
    implicit val materializer = ActorMaterializer()

    val source = Source(1 to 10)
    val sink = Sink.seq[Int]

    source.filter { value =>
      println(s"Filter value $value")
      value % 2 == 0
    }. map { value =>
      println(s"Map value $value")
      throw new RuntimeException("re")
      value * 2
    }.reduce { (v1, v2) =>
      println(s"Reduce value $v2")
      v1 + v2
    }.runWith(sink).
      recover {
      case t: RuntimeException =>
        println(s"Error $t")
        List(1, 2)
    }.andThen {
      case v: Any =>
        println(s"Result $v")
    }


  }
/*
  test("Custom source") {
    implicit val system = ActorSystem()
    implicit val ec = system.dispatcher
    implicit val materializer = ActorMaterializer()

    val limit = 2200

    // A GraphStage is a proper Graph, just like what GraphDSL.create would return
    val sourceGraph: Graph[SourceShape[ByteString], NotUsed] = new BugzillaSource(Some(OffsetDateTime.now.minusYears(2)), limit)

    // Create a Source from the Graph to access the DSL
    val source: Source[ByteString, NotUsed] = Source.fromGraph(sourceGraph)

    val future1: Future[String] =
      source.via(decode[BugzillaResponse[BugzillaResult]]).
        map(response => response.result.get.bugs).
        takeWhile(bugs => bugs.nonEmpty).
        mapConcat(identity).
        filter(bug =>
          bug.product != "CRF Hot Deploy - Prod DB" &&
          bug.product != "Ecomm Deploy - Prod DB"
        ).
        runFold("")(_ + _ + "\n")

    val result1 = Await.result(future1, 80.seconds)
    println(result1)
  }
*/
/*
  test("File to File streaming") {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher

    val from = "d:/tmp/bugs/2016-09-30/bugs_all.json"
    val to = "bugs_history.json"

    val topPriority = List("P1", "P2")
    val excludedProducts = List("")
    val excludedComponents = List("Dataload Failed", "New Files Arrived", "Data Consistency")

    val batchSize = 200

    val source = fileSource(from)
    val sink = fileSink(to)

    val importantBugs: (BugzillaBug) => Boolean = bug => {
      topPriority.contains(bug.priority) && !excludedProducts.contains(bug.product) && !excludedComponents.contains(bug.component)
    }

    val histories: (Seq[Int]) => Seq[BugzillaHistory] = ids => {
      ids.map((id) => BugzillaHistory(id, Some(s"alias $id"), List()))
    }

    val createItemChange: (BugzillaHistoryChange) => HistoryItemChange = item => {
      HistoryItemChange(item.removed, item.added, item.field_name)
    }

    val createHistoryItem: (BugzillaHistoryItem) => HistoryItem = item => {
      HistoryItem(item.when, item.who, item.changes.map(createItemChange))
    }

    val createHistory: (BugzillaHistory) => BugHistory = history => {
      BugHistory(history.id, history.alias, history.history.map(createHistoryItem))
    }

    val createBugStats: (BugzillaBug, BugzillaHistory) => BugStats = (bug, history) => {
      val (daysOpen, resolvedPeriod, passSla) = Metrics.age(bug.priority, bug.creation_time, bug.last_change_time)
      val weekStr = Metrics.weekFormat(bug.creation_time)
      val status = Metrics.getStatus(bug.status, bug.resolution)
      BugStats(status, bug.creation_time, bug.last_change_time, bug.actual_time, 0, resolvedPeriod, passSla, weekStr)
    }

    val createBug: (BugzillaBug, BugzillaHistory) => Bug = (bug, history) => {
      Bug(bug.id, bug.severity, bug.priority, bug.status, bug.resolution,
        bug.creator, bug.creation_time, bug.assigned_to,
        bug.last_change_time.getOrElse(bug.creation_time),
        bug.product, bug.component, "", bug.summary, "", createBugStats(bug, history))
    }

    val batchUpdate: (Seq[BugzillaBug]) => List[Bug] = bugs => {
      val ids = bugs.map(_.id)
      val result = for {
        bug <- bugs
        history <- histories(ids)
        if bug.id == history.id
      } yield createBug(bug, history)
      result.toList
    }

    val collectBugs: PartialFunction[List[Bug], Seq[Bug]] = {
      case bugs: List[Bug] => bugs
    }

    val futureBugs = source.via(CirceStreamSupport.decode[BugzillaResponse[BugzillaResult]]).via(Flow[BugzillaResponse[BugzillaResult]].map(_.result.get.bugs)).
      mapConcat(identity).filter(importantBugs).grouped(batchSize).
      map(batchUpdate).collect(collectBugs).via(CirceStreamSupport.encode[Seq[Bug]]).runWith(sink)
    val result1 = Await.result(futureBugs, 5.seconds)

    val futureHistories = fileSource("d:/tmp/bugs/2016-10-03/bugs_history.json").
      via(CirceStreamSupport.decode[BugzillaResponse[BugzillaHistoryResult]]).
      via(Flow[BugzillaResponse[BugzillaHistoryResult]].map(_.result.get.bugs)).
      toMat(Sink.seq)(Keep.right)
    val result2 = Await.result(futureHistories.run().map(_.flatten), 5.seconds)

    println(result2)
  }
*/
/*
  test("Streaming processed data") {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val from = "d:/tmp/bugs/2016-10-03/bugs.json"

    val source = fileSource(from)
    val future = source.via(CirceStreamSupport.decode[List[Bug]]).mapConcat(identity).runForeach(println)
    val result = Await.result(future, 5.seconds)
  }
*/
  def fileSource(filename: String): Source[ByteString, Future[IOResult]] = FileIO.fromPath(Paths.get(filename))
  def fileSink(filename: String): Sink[String, Future[IOResult]] = Flow[String].map(s => ByteString(s)).toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

}
