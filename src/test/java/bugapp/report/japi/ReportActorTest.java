package bugapp.report.japi;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.testkit.JavaTestKit;
import akka.testkit.TestKit;
import bugapp.report.ReportDataBuilderActor;
import bugapp.repository.Bug;
import org.junit.*;
import scala.collection.JavaConversions;
import scala.collection.JavaConverters$;
import scala.concurrent.duration.Duration;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Alexander Kuleshov
 */
public class ReportActorTest {

    private static ActorSystem system;

    public static class SomeActor extends UntypedActor {
        ActorRef target = null;

        public void onReceive(Object msg) {

            if (msg.equals("hello")) {
                getSender().tell("world", getSelf());
                if (target != null) target.forward(msg, getContext());

            } else if (msg instanceof ActorRef) {
                target = (ActorRef) msg;
                getSender().tell("done", getSelf());
            }
        }
    }

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
            final ActorRef owner = system.actorOf(Props.create(SomeActor.class));

            final Props props = TopAssigneesJActor.props(owner);
            final ActorRef reportActor = system.actorOf(props);

            reportActor.tell(ReportActorHelper.createRequest(new ArrayList<>(), new HashMap<>()), owner);
        }};
    }

    @Test
    public void testAllOpenBugsNumberByPriorityJActor() {

        new JavaTestKit(system) {{
            final ActorRef owner = system.actorOf(Props.create(SomeActor.class));

            final Props props = AllOpenBugsNumberByPriorityJActor.props(owner);
            final ActorRef reportActor = system.actorOf(props);

            reportActor.tell(ReportActorHelper.createRequest(new ArrayList<>(), new HashMap<>()), owner);
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
}
