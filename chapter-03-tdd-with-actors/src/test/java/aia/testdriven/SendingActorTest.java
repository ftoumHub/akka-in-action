package aia.testdriven;

import aia.testdriven.SendingActor.Event;
import aia.testdriven.SendingActor.SortEvents;
import aia.testdriven.SendingActor.SortedEvents;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import io.vavr.collection.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static akka.actor.ActorRef.noSender;
import static akka.japi.JavaPartialFunction.noMatch;
import static java.util.stream.Collectors.toList;
import static java.util.stream.LongStream.range;
import static org.assertj.core.api.Assertions.assertThat;

public class SendingActorTest extends StopSystemAfterAll{

    private final Random random = new Random();

    // Listing 3.8 - Sending actor test
    @DisplayName("A Sending Actor must send a message to another actor when it has finished processeing")
    @Test
    void sendingActorTest() {
        new TestKit(system) {
            {
                final Props props = SendingActor.props(getRef());
                final ActorRef sendingActor = system.actorOf(props, "sendingActor");

                final Long size = 1000L;
                final Long maxInclusive = 100000L;

                final List<Event> unsorted = randomEvents(size);
                final SortEvents sortEvents = new SortEvents(unsorted);

                sendingActor.tell(sortEvents, noSender());

                expectMsgPF("match hint",
                        se -> {
                            if (se instanceof SortedEvents) {
                                final List<Event> events = ((SortedEvents) se).events();
                                assertThat(events).hasSize(1000);
                                assertThat(unsorted.sortBy(__ -> __.id)).isEqualTo(events);
                                return "match";
                            } else {
                                throw noMatch();
                            }
                        });
            }
        };
    }

    private List<Event> randomEvents(Long size) {
        return List.ofAll(range(0L, size).mapToObj(__ -> new Event(random.nextLong())).collect(toList()));
    }
}
