package bugapp.report.japi;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestKit;
import akka.testkit.TestProbe;
import bugapp.report.ReportDataBuilderActor;
import bugapp.report.ReportParams;
import bugapp.report.model.ReportData;
import bugapp.repository.Bug;
import bugapp.repository.BugCreatedEvent;
import bugapp.repository.BugEvent;
import org.junit.*;
import scala.collection.JavaConversions;
import scala.collection.JavaConverters$;
import scala.concurrent.duration.Duration;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Kuleshov
 */
public class ReportActorTest {

    private static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        Duration finiteDuration = Duration.create(10, TimeUnit.SECONDS);
        TestKit.shutdownActorSystem(system, finiteDuration, true);
        system = null;
    }

    @Test
    public void testTopAssigneesJActor() {
        new JavaTestKit(system) {{
            final TestProbe probe = new TestProbe(system);

            final Props props = TopAssigneesJActor.props(probe.ref());
            final ActorRef reportActor = system.actorOf(props);

            List<Bug> bugs = new ArrayList<>();
            bugs.add(
                    BugBuilder.createWithId(1).withOpened(OffsetDateTime.now()).
                            withPriority("P2").withAssignee("user1@domain").build()
            );
            bugs.add(
                    BugBuilder.createWithId(2).withOpened(OffsetDateTime.now()).
                            withPriority("P2").withAssignee("user2@domain").build()
            );
            bugs.add(
                    BugBuilder.createWithId(3).withOpened(OffsetDateTime.now()).
                            withPriority("P1").withAssignee("user1@domain").build()
            );

            reportActor.tell(ReportActorHelper.createRequest(bugs, new HashMap<>()), probe.ref());

            final ReportDataBuilderActor.ReportDataResponse message = probe.expectMsgClass(ReportDataBuilderActor.ReportDataResponse.class);

            assertTrue(message.result() instanceof ReportData);

            final ReportData reportData = (ReportData) message.result();

            assertTrue("top-asignees".equals(reportData.name()));

        }};
    }

    @Test
    public void testAllOpenBugsNumberByPriorityJActor() {

        new JavaTestKit(system) {{
            final TestProbe probe = new TestProbe(system);

            final Props props = AllOpenBugsNumberByPriorityJActor.props(probe.ref());
            final ActorRef reportActor = system.actorOf(props);

            List<Bug> bugs = new ArrayList<>();
            bugs.add(
                    BugBuilder.createWithId(1).withOpened(OffsetDateTime.now().minusMonths(24)).
                            withPriority("P2").withAssignee("user1@domain").build()
            );
            bugs.add(
                    BugBuilder.createWithId(1).withOpened(OffsetDateTime.now().minusMonths(12)).
                            withPriority("P2").withAssignee("user1@domain").build()
            );
            bugs.add(
                    BugBuilder.createWithId(2).withOpened(OffsetDateTime.now().minusDays(91)).
                            withPriority("P2").withAssignee("user1@domain").build()
            );
            bugs.add(
                    BugBuilder.createWithId(3).withOpened(OffsetDateTime.now().minusDays(31)).
                            withPriority("P2").withAssignee("user1@domain").build()
            );
            bugs.add(
                    BugBuilder.createWithId(4).withOpened(OffsetDateTime.now().minusDays(8)).
                            withPriority("P2").withAssignee("user1@domain").build()
            );
            bugs.add(
                    BugBuilder.createWithId(4).withOpened(OffsetDateTime.now().minusDays(5)).
                            withPriority("P2").withAssignee("user1@domain").build()
            );
            bugs.add(
                    BugBuilder.createWithId(5).withOpened(OffsetDateTime.now()).
                            withPriority("P2").withAssignee("user1@domain").build()
            );

            Map<String, Object> params = new HashMap<>();
            List<String> excludedComponents = new ArrayList<>();
            params.put(ReportParams.ExcludedComponents(), JavaConversions.asScalaBuffer(excludedComponents));

            reportActor.tell(ReportActorHelper.createRequest(bugs, params), probe.ref());

            final ReportDataBuilderActor.ReportDataResponse message = probe.expectMsgClass(ReportDataBuilderActor.ReportDataResponse.class);

            assertTrue(message.result() instanceof ReportData);

            final ReportData reportData = (ReportData) message.result();

            assertTrue("all-open-bugs".equals(reportData.name()));
        }};
    }

    public static class ReportActorHelper {
        static ReportDataBuilderActor.ReportDataRequest createRequest(final List<Bug> bugs, final Map<String, Object> reportParams) {
            final String requestId = UUID.randomUUID().toString();
            return new ReportDataBuilderActor.ReportDataRequest(requestId,
                    JavaConverters$.MODULE$.mapAsScalaMapConverter(reportParams).asScala().toMap(
                            scala.Predef$.MODULE$.<scala.Tuple2<String, Object>>conforms()
                    ),
                    JavaConversions.asScalaBuffer(bugs).toList());
        }

    }

    public static class BugBuilder {
        private int id;
        private String severity;
        private String priority;
        private String status;
        private String resolution;
        private String reporter;
        private OffsetDateTime opened;
        private String assignee;
        private OffsetDateTime changed;
        private String product;
        private String component;
        private String environment;
        private String summary;
        private String hardware;
        private List<BugEvent> events = new ArrayList<>();

        private BugBuilder(int id) {
            this.id = id;
        }

        static BugBuilder createWithId(int id) {
            return new BugBuilder(id);
        }

        public BugBuilder withSeverity(String severity) {
            this.severity = severity;
            return this;
        }

        public BugBuilder withPriority(String priority) {
            this.priority = priority;
            return this;
        }

        public BugBuilder withStatus(String status) {
            this.status = status;
            return this;
        }

        public BugBuilder withResolution(String resolution) {
            this.resolution = resolution;
            return this;
        }

        public BugBuilder withReporter(String reporter) {
            this.reporter = reporter;
            return this;
        }

        public BugBuilder withOpened(OffsetDateTime opened) {
            this.opened = opened;
            return this;
        }

        public BugBuilder withAssignee(String assignee) {
            this.assignee = assignee;
            return this;
        }

        public BugBuilder withChanged(OffsetDateTime changed) {
            this.changed = changed;
            return this;
        }

        public BugBuilder withProduct(String product) {
            this.product = product;
            return this;
        }

        public BugBuilder withComponent(String component) {
            this.component = component;
            return this;
        }

        public BugBuilder withEnvironment(String environment) {
            this.environment = environment;
            return this;
        }

        public BugBuilder withSummary(String summary) {
            this.summary = summary;
            return this;
        }

        public BugBuilder withHardware(String hardware) {
            this.hardware = hardware;
            return this;
        }

        public BugBuilder withBugEvent(BugEvent bugEvent) {
            this.events.add(bugEvent);
            return this;
        }

        public BugBuilder withBugEvents(List<BugEvent> bugEvents) {
            this.events.addAll(bugEvents);
            return this;
        }


        private void validate() {
            if (opened == null) {
                throw new RuntimeException("Filed opened is not defined");
            }

            if (severity == null) {

            } else if (priority == null) {

            }
        }

        public Bug build() {
            validate();
            events.add(0, new BugCreatedEvent(0, id, opened, reporter));
            return new Bug(id, severity, priority, status, resolution,
                    reporter, opened, assignee, changed, product, component,
                    environment, summary, hardware, JavaConversions.asScalaBuffer(events).toList());
        }
    }
}
