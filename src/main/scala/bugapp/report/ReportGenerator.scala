package bugapp.report

import java.io._
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.UtilsIO
import bugapp.report.ReportActor.ReportError

import scala.xml.{Elem, XML}

/**
  * Created by arhathq on 28.08.2016.
  */
class ReportGenerator(fopConf: String, reportActor: ActorRef) extends Actor with ActorLogging {
  import bugapp.report.ReportGenerator._

  val reportGenerator: FopReportGenerator = new FopReportGenerator(new URI(fopConf))

  override def receive: Receive = {
    case GenerateReport(reportId, reportTemplate, reportData) =>
      try {
        val output = reportGenerator.generate(inputStream(reportData), inputStream(reportTemplate))

        UtilsIO.write(report(), output)
        log.debug("Report report.pdf saved")

        reportActor ! ReportGenerated(Report(reportId, output))
      } catch {
        case e: Throwable => reportActor ! ReportError(reportId, e.getMessage)
      }
  }
}

object ReportGenerator {

  private val reportDateFormat = DateTimeFormatter.ofPattern("uuuuMMdd")

  case class GenerateReport(reportId: String, reportTemplate: String, source: Elem)
  case class ReportGenerated(report: Report)

  case class Report(reportId: String, data: Array[Byte])

  def props(fopConf: String, reportActor: ActorRef) =
    Props(classOf[ReportGenerator], fopConf, reportActor)

  def inputStream(path: String): InputStream = new FileInputStream(path)

  def inputStream(data: Elem): InputStream = {
    val outputStream = new ByteArrayOutputStream()
    val writer = new OutputStreamWriter(outputStream)
    XML.write(writer, data, "UTF-8", xmlDecl = true, doctype = null)
    writer.close()
    new ByteArrayInputStream(outputStream.toByteArray)
  }

  def report(): String = s"report${reportDateFormat.format(LocalDate.now)}.pdf"
}
