package bugapp.report

import bugapp.repository.Bug

import scala.concurrent.Future

/**
  * @author Alexander Kuleshov
  */
object ReportProtocol {

  case class GenerateReport(weeks: Int)
  case class Bugs(jobId: String, bugs: Future[Seq[Bug]])
  case class ReportError(message: String)
  case class ReportGenerated(jobId: String)

}
