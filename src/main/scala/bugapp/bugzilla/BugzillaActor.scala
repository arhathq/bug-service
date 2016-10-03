package bugapp.bugzilla

import java.nio.file.Paths
import java.time.LocalDate
import java.time.temporal.IsoFields

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.model.{HttpMethods, Uri}
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.util.ByteString
import bugapp.{BugzillaConfig, UtilsIO}
import bugapp.http.HttpClient
import bugapp.Implicits._
import bugapp.repository.{Bug, BugHistory, HistoryItem, HistoryItemChange}
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

  val senders = mutable.Set.empty[ActorRef]

  val batchSize = 200
  val parallel = 8

  val bugsFilter: (BugzillaBug) => Boolean = bug =>
    !excludedProducts.contains(bug.product) && !excludedComponents.contains(bug.component)

  override def receive: Receive = process()

  def process(): Receive = {
    case GetData() =>

      if (!senders.contains(sender())) senders.add(sender)

      val date = LocalDate.now
      val currentWeek = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
      log.debug(s"Scheduled for data with params [date=$date; currentWeek=$currentWeek; periodInWeeks=$fetchPeriod]")

      loadData(date.minusWeeks(fetchPeriod)).map { response =>
        val dataPath = UtilsIO.bugzillaDataPath(rootPath, date)
        val rawPath = s"$dataPath/$repositoryFile"
        UtilsIO.createDirectoryIfNotExists(dataPath)
        UtilsIO.write(rawPath, response)
        log.debug(s"File $rawPath created")

        val output = s"$dataPath/bugs_origin.json"


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
  def loadData(startDate: LocalDate): Future[String] = {
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

  def transformBugs(parallel: Int)(implicit ec: ExecutionContext) = {
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

  def fileSource(filename: String): Source[ByteString, Future[IOResult]] = FileIO.fromPath(Paths.get(filename))
  def fileSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String].map(s => ByteString(s)).toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

}

object BugzillaActor {
  case class GetData()
  case class DataReady(path: String)

  def props(httpClient: HttpClient) = Props(classOf[BugzillaActor], httpClient)

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

  val createBug: (BugzillaBug, BugzillaHistory) => Bug = (bug, history) => {
    Bug(bug.id, bug.severity, bug.priority, bug.status, bug.resolution.getOrElse(""),
      bug.creator, bug.creation_time, bug.assigned_to.getOrElse(""),
      bug.last_change_time.getOrElse(bug.creation_time),
      bug.product, bug.component, "", bug.summary, "", Some(createHistory(history)))
  }
}