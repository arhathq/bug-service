package bugapp.report

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import akka.actor.{Actor, ActorLogging, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.repository.Bug

import scala.xml.Elem

/**
  *
  */
class SlaReportActor extends Actor with ActorLogging {
  import bugapp.report.SlaReportActor._

  private val dateFormat = DateTimeFormatter.ISO_ZONED_DATE_TIME

  override def receive: Receive = {
    case ReportDataRequest(reportId, bugs) =>
      val bugs4weeks = bugs.filter(bugsFor4Weeks)
      val outSlaBugs = bugsOutSla(bugs4weeks)
      val p1 = outSlaBugs.getOrElse("P1", Seq())
      val p2 = outSlaBugs.getOrElse("P2", Seq())

      val data =
        <sla>
          {bugsOutSlaElem(p1, p2)}
          <p1-sla>
            <image>
              <contype-type></contype-type>
              <contype-value></contype-value>
            </image>
          </p1-sla>
          <p2-sla>
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
        <daysOpen>{bug.stats.get.daysOpen}</daysOpen>
        <reopenedCount>{bug.stats.get.reopenCount}</reopenedCount>
        <summary>{bug.summary}</summary>
      </bug>
    }
  }

  def chart(bugs: Seq[Bug]): Array[Byte] = ???

  def sla(p1: Seq[Bug], p2: Seq[Bug]): Elem = {
      <node/>
  }

}

object SlaReportActor {
  def props() = Props(classOf[SlaReportActor])

  val bugsFor4Weeks: (Bug) => Boolean = bug => {
    val from = OffsetDateTime.now.minusWeeks(4)
    bug.priority match {
      case "P1" if bug.opened.isAfter(from) => true
      case "P2" if bug.opened.isAfter(from) => true
      case _ => false
    }
  }

  val slaBugs: (Seq[Bug]) => Map[String, Seq[Bug]] = bugs => {
    bugs.groupBy(bug => bug.stats match {
      case Some(stats) if stats.resolvedPeriod == Metrics.ResolvedIn2Days => Metrics.ResolvedIn2Days
      case Some(stats) if stats.resolvedPeriod == Metrics.ResolvedIn6Days => Metrics.ResolvedIn6Days
      case Some(stats) if stats.resolvedPeriod == Metrics.ResolvedIn30Days => Metrics.ResolvedIn30Days
      case Some(stats) if stats.resolvedPeriod == Metrics.ResolvedIn90Days => Metrics.ResolvedIn90Days
    })
  }

  val bugsOutSla: (Seq[Bug]) => Map[String, Seq[Bug]] = bugs => {
    bugs.groupBy(bug => bug.stats match {
      case Some(stats) if !stats.passSla && bug.priority == "P1" => "P1"
      case Some(stats) if !stats.passSla && bug.priority == "P2" => "P2"
      case _ => "sla"
    })
  }
}