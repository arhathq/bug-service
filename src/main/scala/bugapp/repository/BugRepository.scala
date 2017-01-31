package bugapp.repository

import java.time.OffsetDateTime

import bugapp.bugzilla.Metrics

import scala.concurrent.Future

trait BugRepository {
  def getBugs: Future[Seq[Bug]]

  def getBugs(fromDate: OffsetDateTime): Future[Seq[Bug]]

  def getBugHistory(bugId: List[Int]): Seq[BugHistory]
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
               hardware: String,
               history: Option[BugHistory],
               stats: BugStats
              ) {

  def actualDate: OffsetDateTime = stats.status match {
    case Metrics.OpenStatus => opened
    case Metrics.FixedStatus => stats.resolvedTime.get
    case Metrics.InvalidStatus => stats.resolvedTime.get
  }

}
case class BugHistory(id: Int, alias: Option[String], items: Seq[HistoryItem])
case class HistoryItem(when: OffsetDateTime, who: String, changes: Seq[HistoryItemChange])
case class HistoryItemChange(removed: String, added: String, field: String)
case class BugStats(status: String,
                    openTime: OffsetDateTime,
                    resolvedTime: Option[OffsetDateTime],
                    daysOpen: Int,
                    reopenCount: Int,
                    resolvedPeriod: String,
                    passSla: Boolean,
                    openMonth: String)

case class BugsError(message: String) extends Exception(message)