package bugapp.bugzilla

import java.nio.file.Paths
import java.time.OffsetDateTime

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Sink}
import akka.util.Timeout
import bugapp._
import bugapp.Implicits._
import bugapp.bugzilla.BugzillaActor.{DataReady, GetData}
import bugapp.repository.{Bug, BugHistory, BugRepository}
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import de.knutwalker.akka.stream.support.CirceStreamSupport

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class BugzillaRepository(bugzillaActor: ActorRef)(implicit val s: ActorSystem, implicit val m: ActorMaterializer, implicit val ec: ExecutionContext) extends BugRepository with BugzillaConfig {

  protected val log: LoggingAdapter = Logging(s, getClass)

  QuartzSchedulerExtension(s).schedule("bugzillaActor", bugzillaActor, GetData)

  def getBugs(fromDate: OffsetDateTime): Future[Seq[Bug]] = {
    getBugs((b: Bug) => {
      if (b.opened.isAfter(fromDate)) true else false
    })
  }

  def getBugHistory(bugId: List[Int]): Seq[BugHistory] = {
    bugId.map(BugHistory(_, None, Seq()))
  }

  def getBugs(f: (Bug) => Boolean): Future[Seq[Bug]] = {
    loadDataIfNeeded().flatMap { path =>
      FileIO.fromPath(Paths.get(path)).
        via(CirceStreamSupport.decode[List[Bug]]).
        mapConcat(identity).
        filter(f).
        runWith(Sink.seq).
        flatMap(b => Future.successful(b))
    }
  }

  def loadDataIfNeeded(): Future[String] = {
    val dataPath = UtilsIO.bugzillaDataPath(rootPath)
    val repositoryPath = s"$dataPath/$repositoryFile"
    if (UtilsIO.ifFileExists(repositoryPath)) Future.successful(repositoryPath)
    else {
      implicit val timeout = Timeout(fetchTimeout seconds)
      ask(bugzillaActor, GetData).mapTo[DataReady].map(_.path)
    }
  }
}