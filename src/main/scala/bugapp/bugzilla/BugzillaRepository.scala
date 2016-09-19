package bugapp.bugzilla

import java.io.File
import java.time.LocalDate
import java.time.temporal.IsoFields

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import bugapp._
import bugapp.Implicits._
import bugapp.bugzilla.BugzillaActor.GetData
import bugapp.repository.{Bug, BugRepository}
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.streaming._
import io.iteratee.scalaz.task._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration._
import scalaz.{-\/, \/-}
import scalaz.concurrent.Task

class BugzillaRepository(bugzillaActor: ActorRef)(implicit val s: ActorSystem, implicit val m: ActorMaterializer, implicit val ec: ExecutionContext) extends BugRepository with BugzillaConfig {

  protected val log: LoggingAdapter = Logging(s, getClass)

  QuartzSchedulerExtension(s).schedule("bugzillaActor", bugzillaActor, GetData())

  def getBugs(fromDate: LocalDate): Future[Seq[Bug]] = {

    loadDataIfNeeded().flatMap { path =>
      val p: Promise[Seq[Bug]] = Promise()
      readBytes(new File(path)).
        through(byteParser).
        through(decoder[Task, BugzillaBug]).
        through(filter(_.creation_time.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) >= fromDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))).
        map(BugzillaRepository.asBug).
        toVector.unsafePerformAsync {
        case -\/(ex) => p.failure(ex)
        case \/-(bugs) => p.success(bugs)
      }
      p.future
    }

  }

  def loadDataIfNeeded(): Future[String] = {
    val path = UtilsIO.bugzillaDataPath(repositoryPath, LocalDate.now)

    val dataPath = s"$path/$repositoryFile"

    if (UtilsIO.ifFileExists(dataPath)) Future.successful(dataPath)
    else {
      implicit val timeout = Timeout(fetchTimeout seconds)
      val f = ask(bugzillaActor, GetData()).mapTo[String]
      f
    }
  }

  //query1
  val openStatuses = List("UNCOFIRMED", "NEW", "ASSIGNED", "IN_PROGRESS", "BLOCKED", "PROBLEM_DETERMINED", "REOPENED")
  val openPriorities = List("P1", "P2")
  val environment = "Production"
  val excludedProducts = List("CRF Hot Deploy - Prod DB", "Ecomm Deploy - Prod DB")
  val excludedComponents = List("Dataload Failed", "New Files Arrived", "Data Consistency")

}

object BugzillaRepository {

  def asBug(bugzillaBug: BugzillaBug): Bug = {
    Bug(
      bugzillaBug.id,
      bugzillaBug.severity,
      bugzillaBug.priority,
      bugzillaBug.status,
      bugzillaBug.resolution.getOrElse(""),
      bugzillaBug.creator,
      bugzillaBug.creation_time,
      bugzillaBug.assigned_to.getOrElse(""),
      bugzillaBug.last_change_time.getOrElse(bugzillaBug.creation_time),
      bugzillaBug.product.getOrElse(""),
      bugzillaBug.component.getOrElse(""),
      "",
      bugzillaBug.summary,
      ""
    )
  }
}

