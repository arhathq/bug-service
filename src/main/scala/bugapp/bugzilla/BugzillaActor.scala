package bugapp.bugzilla

import java.nio.file.Paths
import java.time.OffsetDateTime
import java.time.temporal.IsoFields

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.model.HttpMethods
import akka.stream._
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.util.ByteString
import bugapp.{BugApp, BugzillaConfig, UtilsIO}
import bugapp.http.HttpClient
import bugapp.Implicits._
import bugapp.stream.CirceStreamSupport
import bugapp.repository._

import scala.collection.mutable
import scala.concurrent._

/**
  *
  */
class BugzillaActor(httpClient: HttpClient, repositoryEventBus: RepositoryEventBus) extends Actor with ActorLogging with BugzillaConfig with CirceStreamSupport {
  import bugapp.bugzilla.BugzillaActor._

  private val senders = mutable.Set.empty[ActorRef]

  val decider: Supervision.Decider = {
    ex =>
      log.error(ex, "Stream failed")
      senders.clear()
      context.become(waitDataload())
      Supervision.Stop
  }
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(context.system).withSupervisionStrategy(decider))
  implicit val ec = context.dispatcher

  val batchSize = 200
  val parallel = 8

  val bugsFilter: (BugzillaBug) => Boolean = { bug =>
    !excludedProducts.contains(bug.product) && !excludedComponents.contains(bug.component)
  }

  override def receive: Receive = waitDataload()

  def waitDataload(): Receive = {
    case GetData =>
      context.become(dataload())

      senders.add(sender)

      log.debug(s"Scheduled for data")

      val rawFile = rootPath + "/bugzilla.json.tmp"

      loadHistoryStream(rawFile, bugsFilter).map { result =>
        if (result.wasSuccessful) {
          log.debug(s"File $rawFile created")
          val repoFile = rootPath + "/" + repositoryFile + ".tmp"
          transformationStream(rawFile, repoFile).map { transformationResult =>
            context.become(applyDataload())
            repositoryEventBus.publish(RepositoryEventBus.UpdateRequiredEvent())
            if (transformationResult.wasSuccessful) {
              log.debug(s"File $repoFile created")
              senders.foreach(sender => sender ! DataReady(repoFile))
            } else {
              log.debug(s"File $repoFile was not created")
              context.become(waitDataload())
            }
          }
        } else {
          log.debug(s"File $rawFile was not created")
          context.become(waitDataload())
        }
      }.recover {
        case t: Throwable =>
          log.error(t, s"Error during data load stream. State switches back to wait the new data load")
          context.become(waitDataload())
      }
  }

  def dataload(): Receive = {
    case GetData => senders.add(sender)
  }

  def applyDataload(): Receive = {
    case GetData => senders.add(sender)

    case RepositoryEventBus.UpdateGranted =>
      log.debug("Access to update data files was granted!")
      val rawFile = rootPath + "/bugzilla.json.tmp"
      val appliedRawFile = rootPath + "/bugzilla.json"
      if (!UtilsIO.move(rawFile, appliedRawFile)) {
        log.warning(s"File $rawFile wasn't applied")
      } else {
        log.debug(s"File $appliedRawFile was loaded")
      }

      val repoFile = rootPath + "/" + repositoryFile + ".tmp"
      val appliedRepoFile = rootPath + "/" + repositoryFile
      if (!UtilsIO.move(repoFile, appliedRepoFile)) {
        log.warning(s"File $repoFile wasn't applied")
      } else {
        log.debug(s"File $appliedRepoFile was loaded")
      }

      context.become(waitDataload())
      repositoryEventBus.publish(RepositoryEventBus.UpdateCompletedEvent())
  }

  def loadHistoryStream(destination: String, f: (BugzillaBug) => Boolean, batchSize: Int = 200, parallelism: Int = 8): Future[IOResult] = {
      Source.fromGraph(new BugzillaSource(limit = bugLimit)(context.system)).
      via(decode[BugzillaResponse[BugzillaResult]]).
      map(response => response.result.get.bugs).
      takeWhile(bugs => bugs.nonEmpty).
      mapConcat(identity).
      filter(f).
      grouped(batchSize).
      via(loadHistoryAndTransformFlow(parallelism, (bug, history) => bug.copy(history = Some(history)))).
      via(encode[Seq[BugzillaBug]]).
      runWith(fileSink(destination))
  }

  def loadHistoryAndTransformFlow[T](parallelism: Int, transform: (BugzillaBug, BugzillaHistory) => T) =
    Flow[Seq[BugzillaBug]].mapAsync(parallelism) { bugs =>
      loadHistoriesStream(bugs.map(bug => bug.id)).map { histories =>
        for {
          bug <- bugs
          history <- histories
          if bug.id == history.id
        } yield transform(bug, history)
      }
    } collect { case bugs: Seq[T] => bugs }

  def loadHistoriesStream: (Seq[Int]) => Future[Seq[BugzillaHistory]] = { ids =>
    Source.fromFuture(loadHistory(ids)).map(ByteString(_)).
      via(decode[BugzillaResponse[BugzillaHistoryResult]]).
      map(response => response.result.get.bugs).
      toMat(Sink.seq)(Keep.right).
      run().map(_.flatten)
  }

  def transformationStream(source: String, destination: String, batchSize: Int = 200, parallelism: Int = 8) = {
    fileSource(source).
      via(decode[Seq[BugzillaBug]]).
      map(bugs => bugs.toList).
      mapConcat(identity).
      grouped(batchSize).
      via(Flow[Seq[BugzillaBug]].mapAsync(parallelism) { bugs =>
        Future.successful(bugs.map(bug => createBug(bug, bug.history.get)))
      }. collect { case bugs: Seq[Bug] => bugs }).
      via(encode[Seq[Bug]]).
      runWith(fileSink(destination))
  }

  /**
    * method "Bug.search"
    * params   [{"Bugzilla_login":"user","Bugzilla_password":"password","status":["RESOLVED"],"cf_target_milestone":["2016"],"cf_production":["Production"]}]
    * {"error":{"message":"When using JSON-RPC over GET, you must specify a 'method' parameter. See the documentation at docs/en/html/api/Bugzilla/WebService/Server/JSONRPC.html","code":32000},"id":"http://192.168.0.2","result":null}
    */
  def loadData(startDate: OffsetDateTime): Future[String] = {
    val params = BugzillaParams(bugzillaUsername, bugzillaPassword, startDate)
    val request = BugzillaRequest("Bug.search", params)
    httpClient.execute[String](BugzillaRequest.jsonrpc(bugzillaUrl, request), HttpMethods.GET)
  }

  /**
    * method "Bug.history"
    */
  def loadHistory(ids: Seq[Int]): Future[String] = {
    val params = BugzillaParams(bugzillaUsername, bugzillaPassword, ids = Some(ids))
    val request = BugzillaRequest("Bug.history", params)
    httpClient.execute[String](BugzillaRequest.jsonrpc(bugzillaUrl, request), HttpMethods.GET)
  }

}

object BugzillaActor {
  case object GetData
  case class DataReady(path: String)

  def props(httpClient: HttpClient, repositoryEventBus: RepositoryEventBus): Props = Props(classOf[BugzillaActor], httpClient, repositoryEventBus)

  private def fileSource(filename: String): Source[ByteString, Future[IOResult]] = FileIO.fromPath(Paths.get(filename))

  private def fileSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String].map(s => ByteString(s)).toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

  val createItemChange: (BugzillaHistoryChange) => HistoryItemChange = item => {
    HistoryItemChange(item.removed, item.added, item.field_name)
  }

  val createHistoryItem: (BugzillaHistoryItem) => HistoryItem = item => {
    HistoryItem(item.when, item.who, item.changes.map(createItemChange))
  }

  val createHistory: (BugzillaHistory) => BugHistory = history => {
    BugHistory(history.id, history.alias, history.history.map(createHistoryItem))
  }

  val createBugStats: (BugzillaBug, BugzillaHistory) => BugStats = (bug, bugHistory) => {
    var openedTime = bug.creation_time
    var resolvedTime: Option[OffsetDateTime] = None
    var reopenedCount = 0
    for (history <- bugHistory.history) {
      for (change <- history.changes) {
        if (change.field_name == "status" && change.added == "REOPENED") {
          reopenedCount = reopenedCount + 1
//          openedTime = history.when
        }
        if (change.field_name == "status" && change.added == "RESOLVED") {
          resolvedTime = Some(history.when)
        }
      }
    }
    val (daysOpen, resolvedPeriod, passSla) = Metrics.age(bug.priority, openedTime, resolvedTime)
    val weekStr = Metrics.weekFormat(bug.creation_time)
    val status = Metrics.getStatus(bug.status, bug.resolution)
    BugStats(status, openedTime, resolvedTime, daysOpen, reopenedCount, resolvedPeriod, passSla, weekStr)
  }

  val priority: (String) => String = priority => if (priority == "not prioritized") "NP" else priority

  val createBug: (BugzillaBug, BugzillaHistory) => Bug = (bug, history) => {
    Bug(bug.id, bug.severity, priority(bug.priority), bug.status, bug.resolution,
      bug.creator, bug.creation_time, bug.assigned_to,
      bug.last_change_time.getOrElse(bug.creation_time),
      bug.product, bug.component, bug.cf_production.getOrElse(""), bug.summary, bug.platform,
      Some(createHistory(history)), createBugStats(bug, history))
  }
}