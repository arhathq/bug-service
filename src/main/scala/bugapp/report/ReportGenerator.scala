package bugapp.report

import java.io._
import java.net.URI

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.report.ReportActor.{Report, ReportError, ReportGenerated}

import scala.xml.{Elem, XML}

/**
  * @author Alexander Kuleshov
  */
class ReportGenerator(fopConf: String, reportDir: String, reportActor: ActorRef) extends Actor with ActorLogging {
  import bugapp.report.ReportGenerator._

  val reportGenerator: FopReportGenerator = new FopReportGenerator(new URI(fopConf))

  override def receive: Receive = {
    case GenerateReport(reportId, reportName, reportTemplate, reportData) =>
      val output = reportGenerator.generate(inputStream(reportData), inputStream(reportTemplate))
      log.debug(s"Report $reportName created")
      reportActor ! ReportGenerated(Report(reportId, reportName, formatType, output))
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    message.get match {
      case GenerateReport(reportId, _, _, _) =>
        reportActor ! ReportError(reportId, reason.getMessage)
    }

  }
}

object ReportGenerator {

  case class GenerateReport(reportId: String, reportName: String, reportTemplate: String, source: Elem)

  def props(fopConf: String, reportDir: String, reportActor: ActorRef) =
    Props(classOf[ReportGenerator], fopConf, reportDir, reportActor)

  def inputStream(path: String): InputStream = new FileInputStream(path)

  def inputStream(data: Elem): InputStream = {
    val outputStream = new ByteArrayOutputStream()
    val writer = new OutputStreamWriter(outputStream)
    XML.write(writer, data, "UTF-8", xmlDecl = true, doctype = null)
    writer.close()
    new ByteArrayInputStream(outputStream.toByteArray)
  }

  def formatType: String = "pdf"

}
