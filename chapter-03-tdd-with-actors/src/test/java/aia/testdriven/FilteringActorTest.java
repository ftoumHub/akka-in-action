package aia.testdriven;

import aia.testdriven.FilteringActor.Event;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static akka.actor.ActorRef.noSender;
import static akka.japi.JavaPartialFunction.noMatch;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class FilteringActorTest extends StopSystemAfterAll {

    // Listing 3.10 - FilteringActor test with receiveWhile loop
    @DisplayName("filter out particular messages in FilteringActor")
    @Test
    void filteringActorTest() {
        new TestKit(system) {
            {
                final Props props = FilteringActor.props(getRef(), 5);
                final ActorRef filter = system.actorOf(props, "filter-1");

                filter.tell(new Event(1L), noSender());
                filter.tell(new Event(2L), noSender());
                filter.tell(new Event(1L), noSender());
                filter.tell(new Event(3L), noSender());
                filter.tell(new Event(1L), noSender());
                filter.tell(new Event(4L), noSender());
                filter.tell(new Event(5L), noSender());
                filter.tell(new Event(5L), noSender());
                filter.tell(new Event(6L), noSender());

                /**
                 * The test uses a receiveWhile method to collect the messages that the testActor
                 * receives until the case statement doesn't match. In the test, the Event(6) doesn't
                 * match the pattern in the case statement, which defines that all Events with an ID
                 * less than or equal to 5 will be matched, popping us out of the while loop. The
                 * receiveWhile method returns the collected items as they're returned in the partial
                 * function as a list.
                 */
                final List<Long> eventIds = receiveWhile(ofSeconds(5), evt -> {
                    if (evt instanceof Event && (((Event) evt).id > 0) && ((Event) evt).id <= 5) {
                        return ((Event) evt).id;
                    } else {
                        throw noMatch();
                    }
                });

                assertThat(eventIds.stream().filter(Objects::nonNull).collect(toList())).isEqualTo(asList(1L, 2L, 3L, 4L, 5L));
                expectMsg(new Event(6L));
            }
        };
    }

    @DisplayName("filter out particular messages in FilteringActor")
    @Test
    void filteringActorTestNoMsg() {
        new TestKit(system) {
            {
                final Props props = FilteringActor.props(getRef(), 5);
                final ActorRef filter = system.actorOf(props, "filter-2");

                filter.tell(new Event(1L), noSender());
                filter.tell(new Event(2L), noSender());
                expectMsg(new Event(1L));
                expectMsg(new Event(2L));
                filter.tell(new Event(1L), noSender());
                expectNoMessage();
                filter.tell(new Event(3L), noSender());
                expectMsg(new Event(3L));
                filter.tell(new Event(1L), noSender());
                expectNoMessage();
            }
        };
    }
}
