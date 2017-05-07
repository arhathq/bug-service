package bugapp.repository

import java.time.OffsetDateTime

import bugapp.bugzilla.Metrics
import bugapp.bugzilla.Metrics._

import scala.concurrent.Future

/**
  * Domain model
  */

trait BugRepository {
  def getBugs: Future[Seq[Bug]]

  def getBugs(fromDate: OffsetDateTime): Future[Seq[Bug]]
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
               events: Seq[BugEvent]
              ) {

  def actualDate: OffsetDateTime = actualStatus match {
    case Metrics.OpenStatus => opened
    case Metrics.FixedStatus => resolvedTime.get
    case Metrics.InvalidStatus => resolvedTime.get
  }

  val actualStatus: String = {
    if (resolvedStatuses.contains(status)) {
      if (invalidResolutions.contains(resolution)) InvalidStatus else FixedStatus
    } else OpenStatus
  }

  val isNotResolved: Boolean = resolvedStatuses.contains(status)

  val resolvedTime: Option[OffsetDateTime] = actualStatus match {
    case FixedStatus | InvalidStatus => events.filter { event =>
      event.isInstanceOf[BugResolvedEvent]
    }.lastOption match {
      case Some(BugResolvedEvent(_, _, date, _)) => Some(date)
      case _ => None
    }
    case _ => None
  }

  def daysOpen: Int = {
    events.filter(event => event.isInstanceOf[BugReopenedEvent]).lastOption match {
      case Some(event) => durationInBusinessDays(event.date, resolvedTime)
      case _ => durationInBusinessDays(opened, resolvedTime)
    }
  }

  val reopenedCount: Int = events.count(event => event.isInstanceOf[BugReopenedEvent])

  val resolvedPeriod: String = daysOpen match {
    case value if value < 3 => ResolvedIn2Days
    case value if value < 7 => ResolvedIn6Days
    case value if value < 31 => ResolvedIn30Days
    case value if value < 91 => ResolvedIn90Days
    case value if value < 365 => ResolvedIn365Days
    case _ => ResolvedInMoreThan365Days
  }

  val passSla: Boolean = priority match {
      case P1Priority if daysOpen < 3  => true
      case P2Priority if daysOpen < 7  => true
      case P3Priority if daysOpen < 31 => true
      case _ => false
  }

  val openMonth: String = weeksStrFormat.format(opened)
}

case class BugsError(message: String) extends Exception(message)

sealed trait BugEvent {
  val date: OffsetDateTime
}
case class BugCreatedEvent(eventId: Int, bugId: Int, date: OffsetDateTime, reporter: String) extends BugEvent
case class BugPriorityChangedEvent(eventId: Int, bugId: Int, date: OffsetDateTime, priority: String) extends BugEvent
case class BugSubscriberAddedEvent(eventId: Int, bugId: Int, date: OffsetDateTime, subscriber: String) extends BugEvent
case class BugAssignedEvent(eventId: Int, bugId: Int, date: OffsetDateTime, from: String, to: String) extends BugEvent
case class BugInProgressEvent(eventId: Int, bugId: Int, date: OffsetDateTime, progressBy: String) extends BugEvent
case class BugCommentedEvent(eventId: Int, bugId: Int, date: OffsetDateTime, commentator: String, comment: String) extends BugEvent
case class BugReopenedEvent(eventId: Int, bugId: Int, date: OffsetDateTime, reopenedBy: String) extends BugEvent
case class BugResolvedEvent(eventId: Int, bugId: Int, date: OffsetDateTime, resolvedBy: String) extends BugEvent
case class BugResolutionChangedEvent(eventId: Int, bugId: Int, date: OffsetDateTime, resolution: String) extends BugEvent
case class BugSeverityChangedEvent(eventId: Int, bugId: Int, date: OffsetDateTime, severity: String) extends BugEvent
case class BugClosedEvent(eventId: Int, bugId: Int, date: OffsetDateTime, closedBy: String) extends BugEvent
case class BugEscalatedEvent(eventId: Int, bugId: Int, date: OffsetDateTime, escalatedBy: String) extends BugEvent
case class BugBlockedEvent(eventId: Int, bugId: Int, date: OffsetDateTime, blockedBy: String) extends BugEvent
case class BugVerifiedEvent(eventId: Int, bugId: Int, date: OffsetDateTime, verifiedBy: String) extends BugEvent
case class BugComponentChangedEvent(eventId: Int, bugId: Int, date: OffsetDateTime, oldComponent: String, newComponent: String) extends BugEvent
case class BugMarkedAsProductionEvent(eventId: Int, bugId: Int, date: OffsetDateTime, markedBy: String) extends BugEvent