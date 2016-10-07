package bugapp

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.util.ByteString

import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

import akka.stream._
import akka.stream.scaladsl._
import bugapp.bugzilla._
import bugapp.repository.{Bug, BugHistory, HistoryItem, HistoryItemChange}
import bugapp.Implicits._
import de.knutwalker.akka.stream.support.CirceStreamSupport
import org.scalatest.FunSuite

/**
  *
  */
class AkkaStreamTests extends FunSuite {

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

    val createBug: (BugzillaBug, BugzillaHistory) => Bug = (bug, history) => {
      Bug(bug.id, bug.severity, bug.priority, bug.status, bug.resolution,
        bug.creator, bug.creation_time, bug.assigned_to,
        bug.last_change_time.getOrElse(bug.creation_time),
        bug.product, bug.component, "", bug.summary, "", None)
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

  test("Streaming processed data") {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val from = "d:/tmp/bugs/2016-10-03/bugs.json"

    val source = fileSource(from)
    val future = source.via(CirceStreamSupport.decode[List[Bug]]).mapConcat(identity).runForeach(println)
    val result = Await.result(future, 5.seconds)
  }

  def fileSource(filename: String): Source[ByteString, Future[IOResult]] = FileIO.fromPath(Paths.get(filename))
  def fileSink(filename: String): Sink[String, Future[IOResult]] = Flow[String].map(s => ByteString(s)).toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

}
