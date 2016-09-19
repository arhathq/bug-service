package bugapp.repository

import java.time.{LocalDate, OffsetDateTime}

import scala.concurrent.Future

trait BugRepository {
  def getBugs(fromDate: LocalDate): Future[Seq[Bug]]
}

case class Bug(id: Int,
               severity: String,
               priority: String,
               status: String,
               resolution: String,
               reporter: String,
               opened: OffsetDateTime,
               assignee: String,
               changed: OffsetDateTime,
               product: String,
               component: String,
               environment: String,
               summary: String,
               hardware: String)
object Bug {

}

case class BugsError(message: String) extends Exception(message)