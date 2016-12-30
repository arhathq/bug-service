package bugapp.report

import java.time.OffsetDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.bugzilla.Metrics
import bugapp.report.ReportActor.createXmlElement
import bugapp.report.ReportActor.formatNumber
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}
import bugapp.repository.Bug
import org.jfree.data.category.DefaultCategoryDataset

import scala.xml._

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

      val totalBugNumber = weeklyBugs.map { tuple =>
        val bugs = tuple._2
        val closedBugsNumber = bugs.getOrElse(Metrics.FixedStatus, Seq()).length
        val invalidBugsNumber = bugs.getOrElse(Metrics.InvalidStatus, Seq()).length
        val openedBugsNumber = bugs.getOrElse(Metrics.OpenStatus, Seq()).length
        (closedBugsNumber, invalidBugsNumber, openedBugsNumber)
      }.foldLeft((0, 0, 0))((acc, tuple) => (acc._1 + tuple._1, acc._2 + tuple._2, acc._3 + tuple._3))

      val data = createXmlElement(s"bugs-by-weeks-$weeks",
        weeklyBugsElem(weeklyBugs),
        chartElem(weeklyBugs)
      )

      owner ! ReportDataResponse(reportId, data)
  }

  private def weeklyBugsElem(bugs: Map[String, Map[String, Seq[Bug]]]): Elem = {
    val marks = bugs.keys.toSeq.sortWith((v1, v2) => v1 < v2)
    val marksElem = createXmlElement("header", Group(marks.map(week => createXmlElement(s"w$week", Text(week)))))

    val fixed = weeklyBugsData(Metrics.FixedStatus, marks, bugs)
    val fixedElem = createXmlElement("row",
      createXmlElement("name", Text("Closed")),
      Group(fixed.map(tuple => createXmlElement("value", Text(formatNumber(tuple._2)))))
    )

    val open = weeklyBugsData(Metrics.OpenStatus, marks, bugs)
    val openElem = createXmlElement("row",
      createXmlElement("name", Text("Open")),
      Group(open.map(tuple => createXmlElement("value", Text(formatNumber(tuple._2)))))
    )

    val invalid = weeklyBugsData(Metrics.InvalidStatus, marks, bugs)
    val invalidElem = createXmlElement("row",
      createXmlElement("name", Text("Invalid")),
      Group(invalid.map(tuple => createXmlElement("value", Text(formatNumber(tuple._2)))))
    )

    createXmlElement("weekly-bugs", marksElem, fixedElem, openElem, invalidElem)
  }

  private def weeklyBugsData(priority: String, marks: Seq[String], bugs: Map[String, Map[String, Seq[Bug]]]): Seq[(String, Int)] = {
    marks.map { mark =>
      val bugsByStatus = bugs.getOrElse(mark, Map())
      val bugsNumber = bugsByStatus.getOrElse(priority, Seq()).length
      (mark, bugsNumber)
    }
  }

  private def chartElem(bugs: Map[String, Map[String, Seq[Bug]]]): Elem = {
    val dataSet = new DefaultCategoryDataset()

    val marks = bugs.keys.toSeq.sortWith((v1, v2) => v1 < v2)
    marks.foreach { mark =>
      val bugsByStatus = bugs.getOrElse(mark, Map())
      dataSet.addValue(bugsByStatus.getOrElse(Metrics.FixedStatus, Seq()).length, "Closed", mark)
      dataSet.addValue(bugsByStatus.getOrElse(Metrics.OpenStatus, Seq()).length, "Open", mark)
      dataSet.addValue(bugsByStatus.getOrElse(Metrics.InvalidStatus, Seq()).length, "Invalid", mark)
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
