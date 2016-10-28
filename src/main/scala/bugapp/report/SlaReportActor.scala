package bugapp.report

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import akka.actor.{Actor, ActorLogging, Props}
import bugapp.BugApp
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.repository.Bug
import org.jfree.data.category.DefaultCategoryDataset

import scala.xml.{Elem, Group, Null, TopScope}

/**
  *
  */
class SlaReportActor extends Actor with ActorLogging {
  import bugapp.report.SlaReportActor._

  private val dateFormat = DateTimeFormatter.ISO_ZONED_DATE_TIME

  private val weekPeriod = 4
  private val toDate = BugApp.toDate

  override def receive: Receive = {
    case ReportDataRequest(reportId, bugs) =>
      val startDate = BugApp.fromDate(toDate, weekPeriod)
      val marks = Metrics.marks(toDate, weekPeriod)
      val actualBugs = bugs.filter(bugsForPeriod(_, startDate))
      val outSlaBugs = bugsOutSla(actualBugs)
      val p1OutSla = outSlaBugs.getOrElse("P1", Seq())
      val p2OutSla = outSlaBugs.getOrElse("P2", Seq())

      log.debug(s"Start date $startDate (${Metrics.weeksStrFormat.format(startDate)})")
      log.debug(s"End date $toDate (${Metrics.weeksStrFormat.format(toDate)})")
      log.debug(s"Week Period $weekPeriodElem")
      log.debug(s"Marks: $marks")
      log.debug(s"Filtered Bugs: $actualBugs")
      log.debug(s"Bugs Out SLA: $outSlaBugs")

      val data =
        <sla>
          {bugsOutSlaElem(outSlaBugs, marks)}
          <p1-sla>
            {sla("P1", marks, p1OutSla, actualBugs.filter(_.priority == "P1"))}
            <image>
              <content-type>image/jpeg</content-type>
              <content-value></content-value>
            </image>
          </p1-sla>
          <p2-sla>
            {sla("P2", marks, p2OutSla, actualBugs.filter(_.priority == "P2"))}
            <image>
              <content-type>image/jpeg</content-type>
              <content-value></content-value>
            </image>
          </p2-sla>
        </sla>

      context.parent ! ReportDataResponse(reportId, data)
  }


  def bugsOutSlaElem(bugs: Map[String, Seq[Bug]], marks: Seq[String]): Elem = {
    val p1 = bugs.getOrElse("P1", Seq())
    val p2 = bugs.getOrElse("P2", Seq())

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
        <content-type>image/jpeg</content-type>
        <content-value>{outSlaChart(marks, p1 ++ p2)}</content-value>
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

  def outSlaChartData(priority: String, marks:Seq[String], bugs: Seq[Bug]): Seq[(Int, String, String)] = {
    val grouped = bugs.groupBy(bug => bug.stats.openMonth)
    marks.map { mark =>
      grouped.get(mark) match {
        case Some(v) => (v.length, priority, mark)
        case None => (0, priority, mark)
      }
    }
  }

  def outSlaChart(marks:Seq[String], bugs: Seq[Bug]): String = {
    val dataSet = new DefaultCategoryDataset()
    outSlaChartData("P1", marks, bugs.filter(_.priority == "P1")).foreach { v => dataSet.addValue(v._1, v._2, v._3) }
    outSlaChartData("P2", marks, bugs.filter(_.priority == "P2")).foreach { v => dataSet.addValue(v._1, v._2, v._3) }
    ChartGenerator.generateBase64OutSlaBugs(dataSet)
  }


  def sla(priority: String, marks:Seq[String], bugsOutSla: Seq[Bug], allBugs: Seq[Bug]): Elem = {
    val outSlaGrouped = bugsOutSla.groupBy(bug => bug.stats.openMonth)
    val allGrouped = allBugs.groupBy(bug => bug.stats.openMonth)
    val res = marks.map(mark => outSlaGrouped.get(mark) match {
      case Some(v) =>
        weekPeriodElem(priority, mark, v.length, allGrouped.getOrElse(mark, Seq()).length)
      case None =>
        weekPeriodElem(priority, mark, 0, allGrouped.getOrElse(mark, Seq()).length)

    })
    val total =
      weekPeriodElem(priority, "Grand Total", outSlaGrouped.map(v => v._2.length).sum, allBugs.length)
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

  val weekPeriodElem: (String, String, Int, Int) => Elem = (priority, mark, outSlaCount, totalCount) => {
    <week-period>
      <priority>{priority}</priority>
      <week>{mark}</week>
      <slaPercentage>{((totalCount - outSlaCount) * 100 / totalCount).toDouble}</slaPercentage>
      <slaCount>{totalCount - outSlaCount}</slaCount>
      <outSlaCount>{outSlaCount}</outSlaCount>
      <totalCount>{totalCount}</totalCount>
    </week-period>
  }
}