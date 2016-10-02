package bugapp.report

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import bugapp.report.ReportDataBuilder.{ReportDataRequest, ReportDataResponse}

/**
  * @author Alexander Kuleshov
  */
class AllOpenBugsReportActor(owner: ActorRef) extends Actor with ActorLogging {

  implicit val execution = context.dispatcher

  def receive: Receive = {
    case ReportDataRequest(reportId, bugs) =>
        log.info(s"Bugs to process: ${bugs.size}")
        val data =
          <all-open-bugs>
            <prioritized-bugs>
              <priority>not prioritized</priority>
              <period1>2</period1>
              <period2/>
              <period3/>
              <period4/>
              <period5/>
              <period6/>
              <total>2</total>
            </prioritized-bugs>
            <prioritized-bugs>
              <priority>P1</priority>
              <period1>1</period1>
              <period2/>
              <period3>1</period3>
              <period4/>
              <period5/>
              <period6/>
              <total>2</total>
            </prioritized-bugs>
            <prioritized-bugs>
              <priority>P2</priority>
              <period1>7</period1>
              <period2>2</period2>
              <period3>2</period3>
              <period4/>
              <period5/>
              <period6/>
              <total>11</total>
            </prioritized-bugs>
            <prioritized-bugs>
              <priority>P3</priority>
              <period1>1</period1>
              <period2>2</period2>
              <period3>11</period3>
              <period4>22</period4>
              <period5>12</period5>
              <period6>3</period6>
              <total>51</total>
            </prioritized-bugs>
            <prioritized-bugs>
              <priority>P4</priority>
              <period1/>
              <period2/>
              <period3/>
              <period4/>
              <period5/>
              <period6/>
              <total>0</total>
            </prioritized-bugs>
            <prioritized-bugs>
              <priority>Grand Total</priority>
              <period1>9</period1>
              <period2>6</period2>
              <period3>14</period3>
              <period4>22</period4>
              <period5>12</period5>
              <period6>3</period6>
              <total>66</total>
            </prioritized-bugs>
            <image>
              <content-type>image/jpeg</content-type>
              <content-value></content-value>
            </image>
          </all-open-bugs>
         owner ! ReportDataResponse(reportId, data)
  }
}

object AllOpenBugsReportActor {
  def props(owner: ActorRef) = Props(classOf[AllOpenBugsReportActor], owner)
}