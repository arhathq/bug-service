package bugapp.report

import java.io.InputStream
import java.net.URI

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.report.ReportProtocol.{PrepareReport, ReportFailed, ReportPrepared}

/**
  * Created by arhathq on 28.08.2016.
  */
class ReportGenerator(fopConf: String, templatePath: String, reportActor: ActorRef) extends Actor with ActorLogging {

  val reportGenerator: FopReportGenerator = new FopReportGenerator(new URI(fopConf))
  val template: InputStream = classOf[FopReportGenerator].getClassLoader.getResourceAsStream(templatePath)

  override def receive: Receive = {
    case PrepareReport(reportId, dataXml) =>
      val data: InputStream = classOf[FopReportGenerator].getClassLoader.getResourceAsStream("bug-report.xml")

      try {
        val output: Array[Byte] = reportGenerator.generate(data, template)
        reportActor ! ReportPrepared(reportId, output)
      } catch {
        case e: Throwable => reportActor ! ReportFailed(e.getMessage)
      }
  }
}

object ReportGenerator {
  def props(fopConf: String, templatePath: String, reportActor: ActorRef) =
    Props(classOf[ReportGenerator], fopConf, templatePath, reportActor)
}
