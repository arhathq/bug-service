package bugapp.report

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import akka.actor.{Actor, ActorLogging, Props}
import bugapp.BugApp
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.repository.Bug

import scala.xml.{Elem, Group, Null, TopScope}

/**
  *
  */
class SlaReportActor extends Actor with ActorLogging {
  import bugapp.report.SlaReportActor._

  private val dateFormat = DateTimeFormatter.ISO_ZONED_DATE_TIME

  private val weekPeriod = 4
  private val toDate = OffsetDateTime.now

  override def receive: Receive = {
    case ReportDataRequest(reportId, bugs) =>
      val startDate = BugApp.fromDate(toDate, weekPeriod)
      val marks = Metrics.marks(toDate, weekPeriod)
      val bugs4weeks = bugs.filter(bugsForPeriod(_, startDate))
      val outSlaBugs = bugsOutSla(bugs4weeks)
      val p1OutSla = outSlaBugs.getOrElse("P1", Seq())
      val p2OutSla = outSlaBugs.getOrElse("P2", Seq())

      log.debug(s"Start date $startDate (${Metrics.weeksStrFormat.format(startDate)})")
      log.debug(s"End date $toDate (${Metrics.weeksStrFormat.format(toDate)})")
      log.debug(s"Week Period $weekPeriod")
      log.debug(s"Marks: $marks")
      log.debug(s"Filtered Bugs: $bugs4weeks")
      log.debug(s"Bugs Out SLA: $outSlaBugs")

      val data =
        <sla>
          {bugsOutSlaElem(p1OutSla, p2OutSla)}
          <p1-sla>
            {sla("P1", marks, p1OutSla, bugs4weeks.filter(_.priority == "P1"))}
            <image>
              <contype-type></contype-type>
              <contype-value></contype-value>
            </image>
          </p1-sla>
          <p2-sla>
            {sla("P2", marks, p2OutSla, bugs4weeks.filter(_.priority == "P2"))}
            <image>
              <contype-type></contype-type>
              <contype-value></contype-value>
            </image>
          </p2-sla>
        </sla>

      context.parent ! ReportDataResponse(reportId, data)
  }


  def bugsOutSlaElem(p1: Seq[Bug], p2: Seq[Bug]): Elem = {
    val numOfP1 = p1.length
    val numOfP2 = p2.length

    val idsP1 = p1.collect({case b: Bug => b.id}).mkString(",")
    val idsP2 = p2.collect({case b: Bug => b.id}).mkString(",")

    <out-sla-bugs>
      <p1-bugs>{numOfP1}</p1-bugs>
      <p2-bugs>{numOfP2}</p2-bugs>
      <list>
        {bugListElem(p1)}
        {bugListElem(p2)}
      </list>
      <image>
        <contype-type></contype-type>
        <contype-value></contype-value>
      </image>
    </out-sla-bugs>
  }

  def bugListElem(bugs: Seq[Bug]): Seq[Elem] = {
    bugs.map {bug =>
      <bug>
        <id>{bug.id}</id>
        <priority>{bug.priority}</priority>
        <opened>{dateFormat.format(bug.opened)}</opened>
        <resolved>{
           bug.stats.resolvedTime match {
             case Some(time) => dateFormat.format(time)
             case None => ""
            }
          }</resolved>
        <daysOpen>{bug.stats.daysOpen}</daysOpen>
        <reopenedCount>{bug.stats.reopenCount}</reopenedCount>
        <summary>{bug.summary}</summary>
      </bug>
    }
  }

  def chart(bugs: Seq[Bug]): Array[Byte] = ???

  def sla(priority: String, marks:Seq[String], bugsOutSla: Seq[Bug], allBugs: Seq[Bug]): Elem = {
    val slaGrouped = bugsOutSla.groupBy(bug => bug.stats.openMonth)
    val allGrouped = allBugs.groupBy(bug => bug.stats.openMonth)
    val res = marks.map(mark => slaGrouped.get(mark) match {
      case Some(v) =>
        <week-period>
          <priority>{priority}</priority>
          <week>{mark}</week>
          <count>{v.length}</count>
          <totalCount>{allGrouped.getOrElse(mark, Seq()).length}</totalCount>
        </week-period>
      case None =>
        <week-period>
          <priority>{priority}</priority>
          <week>{mark}</week>
          <count>0</count>
          <totalCount>{allGrouped.getOrElse(mark, Seq()).length}</totalCount>
        </week-period>
    })
    val total =
      <week-period>
        <priority>{priority}</priority>
        <week>Grand Total</week>
        <count>{slaGrouped.map(v => v._2.length).sum}</count>
        <totalCount>{allBugs.length}</totalCount>
      </week-period>
    Elem.apply(null, s"sla-achievement", Null, TopScope, true, Group(res), total)
  }

}

object SlaReportActor {
  def props() = Props(classOf[SlaReportActor])

  val bugsForPeriod: (Bug, OffsetDateTime) => Boolean = (bug, startDate) => {
    bug.priority match {
      case "P1" if bug.opened.isAfter(startDate) => true
      case "P2" if bug.opened.isAfter(startDate) => true
      case _ => false
    }
  }

  val slaBugs: (Seq[Bug]) => Map[String, Seq[Bug]] = bugs => {
    bugs.groupBy(bug => bug.stats.resolvedPeriod)
  }

  val bugsOutSla: (Seq[Bug]) => Map[String, Seq[Bug]] = bugs => {
    bugs.groupBy(bug => bug.stats match {
      case stats if !stats.passSla && bug.priority == "P1" => "P1"
      case stats if !stats.passSla && bug.priority == "P2" => "P2"
      case _ => "sla"
    })
  }
}