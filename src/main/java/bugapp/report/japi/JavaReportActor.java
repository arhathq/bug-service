package bugapp.report.japi;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import bugapp.report.ReportDataBuilderActor;
import bugapp.report.model.ReportData;
import bugapp.repository.Bug;
import scala.collection.JavaConversions;

import java.util.Collection;
import java.util.Map;

/**
 * Java Api for actor reports
 *
 * @author Alexander Kuleshov
 */
public abstract class JavaReportActor extends AbstractActor {

    private ActorRef owner;

    public JavaReportActor(ActorRef owner) {
        this.owner = owner;
        receive(ReceiveBuilder.
                        match(ReportDataBuilderActor.ReportDataRequest.class, request -> {
                            String reportId = request.reportId();
                            Collection<Bug> bugs = JavaConversions.seqAsJavaList(request.bugs());
                            Map<String, Object> params = JavaConversions.mapAsJavaMap(request.reportParams());

                            ReportData data = build(bugs, params);

                            owner.tell(new ReportDataBuilderActor.ReportDataResponse<>(reportId, data), self());
                        }).build()
        );
    }

    protected abstract ReportData build(Collection<Bug> bugs, Map<String, Object> params);

}