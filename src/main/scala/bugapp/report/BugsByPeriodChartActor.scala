package bugapp.report

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportActor.formatNumber
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.repository.Bug
import org.jfree.data.category.DefaultCategoryDataset

import scala.xml.{Elem, Group, Null, TopScope}

/**
  * @author Alexander Kuleshov
  */
class BugsByPeriodChartActor(owner: ActorRef, weeks: Int) extends Actor with ActorLogging {
  private implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, reportParams, bugs) =>

      val endDate = reportParams(ReportParams.EndDate).asInstanceOf[OffsetDateTime]
      val startDate = endDate.minusWeeks(weeks)

      val filteredBugs = bugs.filter(bug => bug.opened.isAfter(startDate))

      val weeklyBugs: Map[String, Map[String, Seq[Bug]]] = filteredBugs.groupBy { bug =>
        bug.stats.openMonth
      }. map { tuple =>
        val week = tuple._1
        val bugs = tuple._2
        (week, bugs.groupBy(bug => bug.stats.status))
      }

      val weeklyBugsElems = weeklyBugs.map { tuple =>
        val week = tuple._1
        val bugs = tuple._2
        val closedBugsNumber = bugs.getOrElse(Metrics.FixedStatus, Seq()).length
        val invalidBugsNumber = bugs.getOrElse(Metrics.InvalidStatus, Seq()).length
        val openedBugsNumber = bugs.getOrElse(Metrics.OpenStatus, Seq()).length
        weeklyBugsElem(week, closedBugsNumber, invalidBugsNumber, openedBugsNumber)
      }.toSeq.sortWith((el1, el2) => el1.text < el2.text)

      val totalBugNumber = weeklyBugs.map { tuple =>
        val bugs = tuple._2
        val closedBugsNumber = bugs.getOrElse(Metrics.FixedStatus, Seq()).length
        val invalidBugsNumber = bugs.getOrElse(Metrics.InvalidStatus, Seq()).length
        val openedBugsNumber = bugs.getOrElse(Metrics.OpenStatus, Seq()).length
        (closedBugsNumber, invalidBugsNumber, openedBugsNumber)
      }.foldLeft((0, 0, 0))((acc, tuple) => (acc._1 + tuple._1, acc._2 + tuple._2, acc._3 + tuple._3))

      val data =
        Elem.apply(null, s"bugs-by-weeks-$weeks", Null, TopScope, true,
          Group(weeklyBugsElems),
          weeklyBugsElem("Grand Total", totalBugNumber._1, totalBugNumber._2, totalBugNumber._3),
          chartElem(weeklyBugs)
        )

      owner ! ReportDataResponse(reportId, data)
  }

  private def weeklyBugsElem(week: String, closed: Int, invalid: Int, opened: Int): Elem = {
    <weekly-bugs>
      <week>{week}</week>
      <closed>{formatNumber(closed)}</closed>
      <invalid>{formatNumber(invalid)}</invalid>
      <opened>{formatNumber(opened)}</opened>
      <total>{formatNumber(closed + invalid + opened)}</total>
    </weekly-bugs>
  }

  private def chartElem(bugs: Map[String, Map[String, Seq[Bug]]]): Elem = {
    val dataSet = new DefaultCategoryDataset()

    val marks = bugs.keys.toSeq.sortWith((v1, v2) => v1 < v2)
    marks.foreach { mark =>
      val bugsByStatus = bugs.getOrElse(mark, Map())
      bugsByStatus.foreach { tuple =>
        val status = tuple._1
        val bugs = tuple._2
        dataSet.addValue(bugs.length, status, mark)
      }
    }
    <image>
      <content-type>image/jpeg</content-type>
      <content-value>{ChartGenerator.generateBase64BugsFromLast15Weeks(dataSet)}</content-value>
    </image>
  }
}

object BugsByPeriodChartActor {
  def props(owner: ActorRef, weeks: Int) = Props(classOf[BugsByPeriodChartActor], owner, weeks)
}
