package bugapp.report

import bugapp.repository.Bug

import scala.concurrent.Future
import scala.xml.Elem

/**
  * @author Alexander Kuleshov
  */
object ReportProtocol {

  trait Command
  case class GenerateReport(weeks: Int) extends Command
  case class PrepareReportData(reportId: String, bugs: Future[Seq[Bug]]) extends Command
  case class PrepareReport(reportId: String, dataXml: Elem) extends Command

  trait Event
  case class ReportGenerated(report: Array[Byte]) extends Event
  case class ReportFailed(message: String) extends Event
  case class ReportDataPrepared(reportId: String, dataXml: Elem) extends Event
  case class ReportPrepared(reportId: String, report: Array[Byte]) extends Event

  case class ReportData(reportId: String, data: Elem)

}
