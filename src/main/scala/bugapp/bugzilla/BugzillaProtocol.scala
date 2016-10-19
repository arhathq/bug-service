package bugapp.bugzilla

import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoField, Temporal}
import java.time.{DayOfWeek, OffsetDateTime}

import bugapp.Implicits._

case class BugzillaBug(id: Int,
                       priority: String,
                       severity: String,
                       product: String,
                       component: String,
                       platform: String,
                       creator: String,
                       creation_time: OffsetDateTime,
                       status: String,
                       is_open: Boolean,
                       assigned_to: String,
                       last_change_time: Option[OffsetDateTime],
                       summary: String,
                       cf_project_team: Option[String],
                       is_cc_accessible: Boolean,
                       url: String,
                       groups: Seq[String],
                       whiteboard: Option[String],
                       qa_contact: Option[String],
                       cf_v1_reference: Option[String],
                       estimated_time: Int,
                       remaining_time: Int,
                       resolution: String,
                       classification: Option[String],
                       cf_versiononestate: Option[String],
                       cf_databasestoupdate: Option[String],
                       cf_hotdeploy_approved: Option[String],
                       flags: Seq[String],
                       cf_production: Option[String],
                       cf_customer_perspective: Option[String],
                       version: String,
                       cf_target_milestone: Option[String],
                       deadline: Option[String],
                       actual_time: Int,
                       is_creator_accessible: Boolean,
                       is_confirmed: Boolean,
                       target_milestone: Option[String]
                      )

case class BugzillaHistory(id: Int, alias: Option[String], history: List[BugzillaHistoryItem])
case class BugzillaHistoryItem(when: OffsetDateTime, who: String, changes: List[BugzillaHistoryChange])
case class BugzillaHistoryChange(removed: String, added: String, field_name: String)
case class BugzillaParams(Bugzilla_login: String,
                          Bugzilla_password: String,
                          creation_time: Option[OffsetDateTime] = None,
                          ids: Option[Seq[Int]] = None,
                          cf_production: Option[String] = Some("Production")) {
  import io.circe.syntax._

  def toJsonString: String = List(this).asJson.pretty(implicitly)
}
object BugzillaParams {
  def apply(username: String, password: String, startDate: OffsetDateTime) = new BugzillaParams(username, password, creation_time = Some(startDate))
  def apply(username: String, password: String, ids: Seq[Int]) = new BugzillaParams(username, password, ids = Some(ids))
}

case class BugzillaRequest(method: String, params: BugzillaParams)
case class BugzillaResponse[T](error: Option[BugzillaError], id: String, result: Option[T])
case class BugzillaError(message: String, code: Int)
case class BugzillaResult(bugs: List[BugzillaBug])
case class BugzillaHistoryResult(bugs: List[BugzillaHistory])

object Metrics {

  private val weeksStrFormat = DateTimeFormatter.ofPattern("uuuu-ww")

  val InvalidStatus = "Invalid"
  val FixedStatus = "Fixed"
  val OpenStatus = "Open"

  val ResolvedIn2Days = " < 2 days"
  val ResolvedIn6Days = "2-6 days"
  val ResolvedIn30Days = "6-30 days"
  val ResolvedIn90Days = "30-90 days"
  val ResolvedInMoreThan90Days = " > 90 days"

  val resolvedStatuses = List("RESOLVED", "VERIFIED", "CLOSED")
  val resolvedResolutions = List("INVALID", "WORKSFORME", "DUPLICATE", "WONTFIX")

  def getStatus(status: String, resolution: String): String = {
    if (resolvedStatuses.contains(status)) {
      if (resolvedResolutions.contains(resolution)) return InvalidStatus else return FixedStatus
    }
    OpenStatus
  }

  def isWeekend(date: OffsetDateTime): Boolean = date.getDayOfWeek match {
    case DayOfWeek.SUNDAY => true
    case DayOfWeek.SATURDAY => true
    case _ => false
  }

  def weekFormat(date: Temporal): String = weeksStrFormat.format(date)

  def durationInBusinessDays(fromInc: Temporal, toExc: Option[Temporal]): Int = {
    val start = OffsetDateTime.from(fromInc)
    val end = OffsetDateTime.from(toExc.getOrElse(OffsetDateTime.now))
    if (start.isAfter(end)) return 0
    calculateDuration(start, end, 0)
  }

  private def calculateDuration(start: OffsetDateTime, end: OffsetDateTime, acc: Int): Int = {
    if (start.isBefore(end)) {
      if (start.getDayOfWeek == DayOfWeek.SUNDAY || start.getDayOfWeek == DayOfWeek.SATURDAY)
        calculateDuration(start.plusDays(1), end, acc)
      else
        calculateDuration(start.plusDays(1), end, acc + 1)
    }
    else acc
  }

  def age(priority: String, from: Temporal, to: Option[Temporal]): (Int, String, Boolean) = {
    val daysOpen = durationInBusinessDays(from, to)
    var passSla = false
    priority match {
      case "P1" if daysOpen < 3  => passSla = true
      case "P2" if daysOpen < 7  => passSla = true
      case "P3" if daysOpen < 31 => passSla = true
      case _ => passSla = false
    }

    var resolvedPeriod = ResolvedInMoreThan90Days
    if (daysOpen < 3)  resolvedPeriod = ResolvedIn2Days
    else if (daysOpen < 7)  resolvedPeriod = ResolvedIn6Days
    else if (daysOpen < 31) resolvedPeriod = ResolvedIn30Days
    else if (daysOpen < 91) resolvedPeriod = ResolvedIn90Days

    (daysOpen, resolvedPeriod, passSla)
  }

  def resolvedInfo(date: OffsetDateTime, bugHistory: BugzillaHistory): (OffsetDateTime, Option[OffsetDateTime], Int) = {
    var openedTime = date
    var resolvedTime: Option[OffsetDateTime] = None
    var reopenedCount = 0
    for (history <- bugHistory.history) {
      for (change <- history.changes) {
        if (change.field_name == "status" && change.added == "REOPENED") {
          reopenedCount = reopenedCount + 1
          openedTime = history.when
        }
        if (change.field_name == "status" && change.added == "RESOLVED") {
          resolvedTime = Some(history.when)
        }
      }
    }
    (openedTime, resolvedTime, reopenedCount)
  }

  val marks: (OffsetDateTime, Int) => Seq[String] = (date, weeks) => {
    val week = date.get(ChronoField.ALIGNED_WEEK_OF_YEAR)
    (week to (week - weeks + 1) by -1).map(w => weeksStrFormat.format(date.minusWeeks(week - w))).reverse
  }
}
