# bug-service

##Add new Report

1. ReportActor.scala

object ReportTypes {
  ...
  case object SimpleReport extends ReportType {
    override def name = "simple"
  }

  ...
  def from(name: String): Either[String, ReportType] = name match {
    ...
    case "simple" => Right(SimpleReport)
    ...
  }

  def reportNames = List(..., SimpleReport.name)

}

2. Workers.scala

class ReportWorkers(context: ActorContext) extends Workers {
  ...
  override def create(reportType: ReportType): Set[ActorRef] = reportType match {
    ...
    case SimpleReport => Set(
      context.actorOf(EmptyReportActor.props(self))
    )
  }
}

3. application.conf

reports {
  ...
  types = {
    ...
    simple = {
      template = "templates/simple.xsl"
    }
  }
}

4. Create xsl template simple.xsl