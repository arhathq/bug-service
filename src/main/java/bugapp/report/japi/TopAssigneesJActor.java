package bugapp.report.japi;

import akka.actor.ActorRef;
import akka.actor.Props;
import bugapp.bugzilla.Metrics;
import bugapp.report.japi.model.ReportDataBuilder;
import bugapp.report.japi.model.ReportFieldBuilder;
import bugapp.report.japi.model.ReportValueBuilder;
import bugapp.report.model.ReportData;
import bugapp.repository.Bug;

import java.util.*;

import static java.util.stream.Collectors.*;

/**
 * Actor class that calculates data Top Assignees report
 *
 * @author Alexander Kuleshov
 */
public class TopAssigneesJActor extends JavaReportActor {

    /**
     *
     *
     * @param owner
     */
    public TopAssigneesJActor(ActorRef owner) {
        super(owner);
    }

    @Override
    protected ReportData build(Collection<Bug> bugs, Map<String, Object> params) {

        // Filter only opened bugs and grouping them by assignee
        Map<String, Long> bugsByAssignee =  bugs.stream().
                filter(bug -> bug.actualStatus().equals(Metrics.OpenStatus())).
                collect(groupingBy(Bug::assignee, counting()));

        // Create new sorted map for ass
        Map<String, Long> topBugAssignees = new LinkedHashMap<>();
        bugsByAssignee.entrySet().stream().
                sorted(Map.Entry.<String, Long>comparingByValue().reversed()).
                forEachOrdered(entry -> topBugAssignees.put(entry.getKey(), entry.getValue()));

        // Take only first 15 top assignees and create map value for each assignee
        List<ReportValueBuilder> assignees = topBugAssignees.entrySet().stream().
                limit(15).
                map(entry -> ReportValueBuilder.mapValue().
                        withField(ReportFieldBuilder.createFor("name").withValue(ReportValueBuilder.stringValue(entry.getKey()))).
                        withField(ReportFieldBuilder.createFor("count").withValue(ReportValueBuilder.intValue(entry.getValue().intValue())))).
                collect(toList());

        // Create and return report data with top assignees
        return ReportDataBuilder.createFor("top-asignees").
                withField(
                        ReportFieldBuilder.createFor("asignee").
                                withValue(ReportValueBuilder.listValue().withValues(assignees))
                ).
                build();
    }

    /**
     * Method that creates the instance of TopAssigneesJActor
     */
    public static Props props(ActorRef owner) {
        return Props.create(TopAssigneesJActor.class, owner);
    }
}
