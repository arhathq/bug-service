package bugapp.bugzilla

import java.io.File
import java.time.LocalDate
import java.time.temporal.IsoFields

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.model.{HttpMethods, Uri}
import bugapp.{BugzillaConfig, UtilsIO}
import bugapp.bugzilla.BugzillaActor.{DataReady, GetData}
import bugapp.http.HttpClient
import bugapp.Implicits._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.streaming._
import io.iteratee.scalaz.task._

import scala.collection.mutable
import scala.concurrent.Future
import scalaz.{-\/, \/-}
import scalaz.concurrent.Task


/**
  *
  */
class BugzillaActor(httpClient: HttpClient) extends Actor with ActorLogging with BugzillaConfig {

  implicit val ec = context.dispatcher

  val regex = "\"result\":\\{\"bugs\":"
  val optimize = true

  val senders = mutable.Set.empty[ActorRef]

  override def receive: Receive = process()

  def process(): Receive = {
    case GetData() =>

      if (!senders.contains(sender())) senders.add(sender)

      val date = LocalDate.now
      val currentWeek = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

      log.debug(s"Scheduled for data with params [date=$date; currentWeek=$currentWeek; periodInWeeks=$fetchPeriod]")

      loadData(date.minusWeeks(fetchPeriod)).map { response =>

        val dataPath = UtilsIO.bugzillaDataPath(rootPath, date)

        val output = s"$dataPath/$repositoryFile"

        UtilsIO.createDirectoryIfNotExists(dataPath)

        val normalized = normalize(response)
        log.debug("Normalized ending: " + normalized.substring(normalized.length - 10, normalized.length))
        UtilsIO.write(output, normalized)

        log.debug(s"File $output created")

        if (optimize) {
          val filtered = s"$dataPath/bugs_weekly.json"
          findAndStoreRecentBugs(output, filtered, currentWeek)
        }

        senders.foreach(_ ! DataReady(output))
        senders.clear
      }
  }

  def normalize(response: String): String = {
    val responseParts = response.split(regex)
    if (responseParts.length < 2)
      response
    else
      responseParts(1).substring(0, responseParts(1).length - 2)
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

  def findAndStoreRecentBugs(input: String, output: String, week: Int) = {

    readBytes(new File(input)).
      through(byteParser).
      through(decoder[Task, BugzillaBug]).
      through(filter(_.creation_time.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) == week)).
      toVector.unsafePerformAsync {
        case -\/(ex) =>
          log.error("Error while streaming bugs data", ex)

        case \/-(bugs) =>
          UtilsIO.write(output, bugs.asJson.noSpaces)
          log.debug(s"File $output created")
          val ids = bugs.map(_.id)
          loadAndStoreBugsHistory(ids)
    }
  }

  def loadAndStoreBugsHistory(ids: Seq[Int]) = {
    loadHistory(ids).map { response =>
      val path = UtilsIO.bugzillaDataPath(rootPath, LocalDate.now)
      val historyPath = s"$path/bugs_weekly_history.json"
      UtilsIO.write(historyPath, normalize(response))
      log.debug(s"File $historyPath created")
    }
  }

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

}