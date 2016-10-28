package bugapp.bugzilla

import java.nio.file.Paths
import java.time.OffsetDateTime
import java.time.temporal.IsoFields

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.model.{HttpMethods, Uri}
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.util.ByteString
import bugapp.{BugApp, BugzillaConfig, UtilsIO}
import bugapp.http.HttpClient
import bugapp.Implicits._
import bugapp.repository._
import de.knutwalker.akka.stream.support.CirceStreamSupport

import scala.collection.mutable
import scala.concurrent._

/**
  *
  */
class BugzillaActor(httpClient: HttpClient) extends Actor with ActorLogging with BugzillaConfig with CirceStreamSupport {
  import bugapp.bugzilla.BugzillaActor._

  implicit val ec = context.dispatcher
  implicit val materializer = ActorMaterializer()

  private val senders = mutable.Set.empty[ActorRef]

  val batchSize = 200
  val parallel = 8

  val bugsFilter: (BugzillaBug) => Boolean = bug =>
    !excludedProducts.contains(bug.product) && !excludedComponents.contains(bug.component)

  override def receive: Receive = process()

  def process(): Receive = {
    case GetData =>

      if (!senders.contains(sender())) senders.add(sender)

      val endDate = BugApp.toDate
      val startDate = BugApp.fromDate(endDate, fetchPeriod)
      val currentWeek = endDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
      log.debug(s"Scheduled for data with params [startDate=$startDate; endDate=$endDate; currentWeek=$currentWeek; periodInWeeks=$fetchPeriod]")

      loadData(startDate).map { response =>
        val dataPath = UtilsIO.bugzillaDataPath(rootPath, endDate)
        val rawPath = s"$dataPath/bugs_origin.json"
        UtilsIO.createDirectoryIfNotExists(dataPath)
        UtilsIO.write(rawPath, response)
        log.debug(s"File $rawPath created")

        val output = s"$dataPath/$repositoryFile"
        val future = transform(rawPath, output, batchSize)
        future.map { res =>
          if (res.wasSuccessful) log.debug(s"File $output created")
          senders.foreach(_ ! DataReady(output))
          senders.clear
        }
      }
  }

  def transform(from: String, to: String, batchSize: Int): Future[IOResult] = {

    val source = fileSource(from)
    val sink = fileSink(to)

    val future = source.via(decode[BugzillaResponse[BugzillaResult]]).
      via(Flow[BugzillaResponse[BugzillaResult]].map(_.result.get.bugs)).
      mapConcat(identity).filter(bugsFilter).grouped(batchSize).
      via(transformBugs(parallel)).via(encode[Seq[Bug]]).runWith(sink)

    future
  }

  /**
    * method "Bug.search"
    * params   [{"Bugzilla_login":"user","Bugzilla_password":"password","status":["RESOLVED"],"cf_target_milestone":["2016"],"cf_production":["Production"]}]
    * {"error":{"message":"When using JSON-RPC over GET, you must specify a 'method' parameter. See the documentation at docs/en/html/api/Bugzilla/WebService/Server/JSONRPC.html","code":32000},"id":"http://192.168.0.2","result":null}
    */
  def loadData(startDate: OffsetDateTime): Future[String] = {
    val params = BugzillaParams(bugzillaUsername, bugzillaPassword, startDate)
    val request = BugzillaRequest("Bug.search", params)
    httpClient.execute[String](BugzillaActor.uri(bugzillaUrl, request), HttpMethods.GET)
  }

  /**
    * method "Bug.history"
    */
  def loadHistory(ids: Seq[Int]): Future[String] = {
    val params = BugzillaParams(bugzillaUsername, bugzillaPassword, ids = Some(ids))
    val request = BugzillaRequest("Bug.history", params)
    httpClient.execute[String](BugzillaActor.uri(bugzillaUrl, request), HttpMethods.GET)
  }

  val loadHistories: (Seq[Int]) => Future[Seq[BugzillaHistory]] = ids => {
    val stream = Source.fromFuture(loadHistory(ids)).map(ByteString(_)).
      via(decode[BugzillaResponse[BugzillaHistoryResult]]).
      via(Flow[BugzillaResponse[BugzillaHistoryResult]].map(_.result.get.bugs)).
      toMat(Sink.seq)(Keep.right)
    stream.run().map(_.flatten)
  }

  private def transformBugs(parallel: Int)(implicit ec: ExecutionContext) = {
    Flow[Seq[BugzillaBug]].mapAsync(parallel) {bugs =>
      val ids = bugs.map(_.id)
      val future = loadHistories(ids)
      future.map { histories =>
        val result = for {
          bug <- bugs
          history <- histories
          if bug.id == history.id
        } yield createBug(bug, history)
        result.toList
      }
    }.collect { case bugs: List[Bug] => bugs }
  }
}

object BugzillaActor {
  case object GetData
  case class DataReady(path: String)

  def props(httpClient: HttpClient) = Props(classOf[BugzillaActor], httpClient)

  private def fileSource(filename: String): Source[ByteString, Future[IOResult]] = FileIO.fromPath(Paths.get(filename))

  private def fileSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String].map(s => ByteString(s)).toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

  private def uri(bugzillaUrl: String, request: BugzillaRequest): Uri = {
    Uri(bugzillaUrl).
      withPath(Uri.Path("/jsonrpc.cgi")).
      withQuery(Uri.Query(
        Map(
          "method" -> request.method,
          "params" -> request.params.toJsonString
        )
      ))
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
      bug.product, bug.component, "", bug.summary, "",
      createBugStats(bug, history))
  }
}