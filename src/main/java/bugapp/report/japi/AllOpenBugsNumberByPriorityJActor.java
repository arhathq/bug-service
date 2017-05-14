package bugapp.report.japi;

import akka.actor.ActorRef;
import akka.actor.Props;
import bugapp.bugzilla.Metrics;
import bugapp.report.ReportActor;
import bugapp.report.ReportParams;
import bugapp.report.japi.model.ReportDataBuilder;
import bugapp.report.japi.model.ReportFieldBuilder;
import bugapp.report.japi.model.ReportValueBuilder;
import bugapp.report.model.ReportData;
import bugapp.repository.Bug;
import scala.collection.JavaConversions;
import scala.collection.Seq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;

/**
 * @author Alexander Kuleshov
 */
public class AllOpenBugsNumberByPriorityJActor extends JavaReportActor {

    public AllOpenBugsNumberByPriorityJActor(ActorRef owner) {
        super(owner);
    }

    @Override
    protected ReportData build(Collection<Bug> bugs, Map<String, Object> params) {
        // Taking list of excluded components from params
        List<String> excludedComponents = JavaConversions.seqAsJavaList((Seq<String>) params.get(ReportParams.ExcludedComponents()));

        // Filter and collect only open bugs
        List<Bug> openBugs =  bugs.stream().
                filter(bug -> bug.actualStatus().equals(Metrics.OpenStatus())).
                collect(toList());

        // Group open bugs by its priority
        Map<String, List<Bug>> prioritizedOpenBugs = openBugs.stream().collect(groupingBy(Bug::priority));

        // Getting open bugs by priorities
        List<Bug> p1OpenBugs = prioritizedOpenBugs.getOrDefault(Metrics.P1Priority(), new ArrayList<>());
        List<Bug> p2OpenBugs = prioritizedOpenBugs.getOrDefault(Metrics.P2Priority(), new ArrayList<>());
        List<Bug> p3OpenBugs = prioritizedOpenBugs.getOrDefault(Metrics.P3Priority(), new ArrayList<>());
        List<Bug> p4OpenBugs = prioritizedOpenBugs.getOrDefault(Metrics.P4Priority(), new ArrayList<>());
        List<Bug> npOpenBugs = prioritizedOpenBugs.getOrDefault(Metrics.NPPriority(), new ArrayList<>());

        // Building ...
        List<ReportValueBuilder> allOpenBugsData = new ArrayList<>();
        allOpenBugsData.add(prioritizedBugsData(Metrics.NPPriority(), npOpenBugs));
        allOpenBugsData.add(prioritizedBugsData(Metrics.P1Priority(), p1OpenBugs));
        allOpenBugsData.add(prioritizedBugsData(Metrics.P2Priority(), p2OpenBugs));
        allOpenBugsData.add(prioritizedBugsData(Metrics.P3Priority(), p3OpenBugs));
        allOpenBugsData.add(prioritizedBugsData(Metrics.P4Priority(), p4OpenBugs));
        allOpenBugsData.add(prioritizedBugsData("Grand Total", openBugs));

        // Create and return report data with all open production bugs grouped by priority
        return ReportDataBuilder.createFor("all-open-bugs").
                withField(
                        ReportFieldBuilder.createFor("prioritized-bugs").
                                withValue(ReportValueBuilder.listValue().withValues(allOpenBugsData))
                ).
                withField(
                        ReportFieldBuilder.createFor("excludedComponents").
                                withValue(ReportValueBuilder.stringValue(excludedComponents.toString()))
                ).
                build();
    }

    private ReportValueBuilder prioritizedBugsData(String priority, Collection<Bug> bugs) {
        // Grouping bugs by periods of open days
        Map<String, Long> bugsByOpenPeriod = bugs.stream().
                collect(groupingBy(bug -> {
                    if (bug.daysOpen() < 3) {
                        return "period1";
                    } else if (bug.daysOpen() < 7) {
                        return "period2";
                    } else if (bug.daysOpen() < 31) {
                        return "period3";
                    } else if (bug.daysOpen() < 91) {
                        return "period4";
                    } else if (bug.daysOpen() < 365) {
                        return "period5";
                    } else {
                        return "period6";
                    }
                },
                        counting()
                ));

        List<ReportFieldBuilder> fields = new ArrayList<>();
        // Added priority of open bugs
        fields.add(ReportFieldBuilder.createFor("priority").withValue(ReportValueBuilder.stringValue(priority)));
        // Added number of open bugs for each period
        bugsByOpenPeriod.forEach((key, value) -> fields.add(ReportFieldBuilder.createFor(key).withValue(ReportValueBuilder.intValue(value.intValue()))));
        // Added number of open bugs for all periods
        int total = ((Long) bugsByOpenPeriod.entrySet().stream().map(Map.Entry::getValue).mapToLong(value -> value).sum()).intValue();
        fields.add(ReportFieldBuilder.createFor("total").withValue(ReportValueBuilder.stringValue(ReportActor.formatNumber(total))));

        return ReportValueBuilder.mapValue().withFields(fields);
    }


    /**
     * Method that creates the instance of AllOpenBugsNumberByPriorityJActor
     */
    public static Props props(ActorRef owner) {
        return Props.create(AllOpenBugsNumberByPriorityJActor.class, owner);
    }
}
