package bugapp.report.japi;

import akka.actor.ActorRef;
import akka.actor.Props;
import bugapp.bugzilla.Metrics;
import bugapp.report.ChartGenerator;
import bugapp.report.ReportActor;
import bugapp.report.ReportParams;
import bugapp.report.japi.model.ReportDataBuilder;
import bugapp.report.japi.model.ReportFieldBuilder;
import bugapp.report.japi.model.ReportValueBuilder;
import bugapp.report.model.ReportData;
import bugapp.repository.Bug;
import org.jfree.data.category.DefaultCategoryDataset;
import scala.collection.JavaConversions;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * @author Alexander Kuleshov
 */
public class BugsByPeriodChartJActor extends JavaReportActor {

    private int weeks;

    public BugsByPeriodChartJActor(ActorRef owner, int weeks) {
        super(owner);
        this.weeks = weeks;
    }

    @Override
    protected ReportData build(Collection<Bug> bugs, Map<String, Object> params) {

        OffsetDateTime endDate = (OffsetDateTime) params.get(ReportParams.EndDate());
        OffsetDateTime startDate = endDate.minusWeeks(weeks).truncatedTo(ChronoUnit.DAYS);

        // Getting list of bugs from startDate to endDate
        List<Bug> filteredBugs =  bugs.stream().
                filter(bug -> bug.actualDate().isAfter(startDate) && bug.actualDate().isBefore(endDate)).
                collect(toList());
        log().debug("Filtered bugs number: " + filteredBugs.size());

        // Grouping bugs by weeks and actual statuses
        Map<String, Map<String, List<Bug>>> weeklyBugs = filteredBugs.stream().
                collect(groupingBy(bug -> Metrics.weekFormat(bug.actualDate()))).
                entrySet().stream().
                collect(toMap(Map.Entry::getKey, entry -> entry.getValue().stream().collect(groupingBy(Bug::actualStatus))));

        // Calculating total number of bugs with (only for debug purposes)
        int[] totalBugNumber = weeklyBugs.entrySet().stream().map(entry -> {
            Map<String, List<Bug>> bugsMap = entry.getValue();
            int closedBugsNumber = bugsMap.getOrDefault(Metrics.FixedStatus(), new ArrayList<>()).size();
            int invalidBugsNumber = bugsMap.getOrDefault(Metrics.InvalidStatus(), new ArrayList<>()).size();
            int openedBugsNumber = bugsMap.getOrDefault(Metrics.OpenStatus(), new ArrayList<>()).size();
            return new int[] {closedBugsNumber, invalidBugsNumber, openedBugsNumber};
        }).reduce(new int[] {0, 0, 0}, (int[] acc, int[] val) -> {
            acc[0] = acc[0] + val[0];
            acc[1] = acc[1] + val[1];
            acc[2] = acc[2] + val[2];
            return acc;
        });
        log().debug("Total bug number: " + totalBugNumber[0] + " " + totalBugNumber[1] + " " + totalBugNumber[2]);

        // Transformation from Scala collection to Java collection
        List<String> marks = JavaConversions.seqAsJavaList(Metrics.marksByDates(startDate, endDate));


        return ReportDataBuilder.createFor("bugs-by-weeks-" + weeks).
                withField(chartData(marks, weeklyBugs)).
                withField(weeklyBugsData(marks, weeklyBugs)).
                build();

    }

    // preparing chart data
    private ReportFieldBuilder chartData(List<String> marks, Map<String, Map<String, List<Bug>>> bugs) {
        DefaultCategoryDataset dataSet = new DefaultCategoryDataset();

        // getting number of bugs of each status for every week
        marks.forEach(mark -> {
            Map<String, List<Bug>> bugsByStatus = bugs.getOrDefault(mark, new HashMap<>());
            dataSet.addValue(bugsByStatus.getOrDefault(Metrics.InvalidStatus(), new ArrayList<>()).size(), "Invalid", mark);
            dataSet.addValue(bugsByStatus.getOrDefault(Metrics.FixedStatus(), new ArrayList<>()).size(), "Closed", mark);
            dataSet.addValue(bugsByStatus.getOrDefault(Metrics.OpenStatus(), new ArrayList<>()).size(), "Open", mark);
        });

        return ReportFieldBuilder.createFor("image").withValue(ReportValueBuilder.mapValue().
                withField(ReportFieldBuilder.createFor("content-type").withValue(ReportValueBuilder.stringValue("image/jpeg"))).
                withField(ReportFieldBuilder.createFor("content-value").
                        withValue(ReportValueBuilder.stringValue(ChartGenerator.generateBase64BugsFromLast15Weeks1(dataSet))))
        );
    }

    //
    private ReportFieldBuilder weeklyBugsData(List<String> marks, Map<String, Map<String, List<Bug>>> bugs) {

        ReportFieldBuilder marksData = ReportFieldBuilder.createFor("header").
                withValue(ReportValueBuilder.mapValue().withFields(
                        marks.stream().
                                map(week -> ReportFieldBuilder.createFor("w" + week).withValue(ReportValueBuilder.stringValue(week))).
                                collect(toList())
                ));

        Map<String, Integer> fixed = weeklyBugsData(Metrics.FixedStatus(), marks, bugs);
        ReportValueBuilder fixedData = ReportValueBuilder.mapValue().
                withField(ReportFieldBuilder.createFor("name").withValue(ReportValueBuilder.stringValue("Closed"))).
                withField(ReportFieldBuilder.createFor("value").withValue(ReportValueBuilder.listValue().withValues(
                        fixed.entrySet().stream().
                                map(entry -> ReportValueBuilder.stringValue(ReportActor.formatNumber(entry.getValue()))).
                                collect(Collectors.toList())
                )));

        Map<String, Integer> open = weeklyBugsData(Metrics.OpenStatus(), marks, bugs);
        ReportValueBuilder openData = ReportValueBuilder.mapValue().
                withField(ReportFieldBuilder.createFor("name").withValue(ReportValueBuilder.stringValue("Open"))).
                withField(ReportFieldBuilder.createFor("value").withValue(ReportValueBuilder.listValue().withValues(
                        open.entrySet().stream().
                                map(entry -> ReportValueBuilder.stringValue(ReportActor.formatNumber(entry.getValue()))).
                                collect(Collectors.toList())
                )));

        Map<String, Integer> invalid = weeklyBugsData(Metrics.InvalidStatus(), marks, bugs);
        ReportValueBuilder invalidData = ReportValueBuilder.mapValue().
                withField(ReportFieldBuilder.createFor("name").withValue(ReportValueBuilder.stringValue("Invalid"))).
                withField(ReportFieldBuilder.createFor("value").withValue(ReportValueBuilder.listValue().withValues(
                        invalid.entrySet().stream().
                                map(entry -> ReportValueBuilder.stringValue(ReportActor.formatNumber(entry.getValue()))).
                                collect(Collectors.toList())
                )));

        return ReportFieldBuilder.createFor("weekly-bugs").withValue(
                ReportValueBuilder.mapValue().
                        withField(marksData).
                        withField(ReportFieldBuilder.createFor("row").
                                withValue(ReportValueBuilder.listValue().
                                        withValue(openData).
                                        withValue(fixedData).
                                        withValue(invalidData))));
    }

    // returns number of bugs of specified priority for every week
    private Map<String, Integer> weeklyBugsData(String priority, List<String> marks, Map<String, Map<String, List<Bug>>> bugs) {
        return marks.stream().
                collect(toMap(mark -> mark, mark -> {
                    Map<String, List<Bug>> bugsByStatus = bugs.getOrDefault(mark, new HashMap<>());
                    // return bugsNumber
                    return bugsByStatus.getOrDefault(priority, new ArrayList<>()).size();
                }, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    /**
     * Method that creates the instance of BugsByPeriodChartJActor
     */
    public static Props props(ActorRef owner, int weeks) {
        return Props.create(BugsByPeriodChartJActor.class, owner, weeks);
    }
}
