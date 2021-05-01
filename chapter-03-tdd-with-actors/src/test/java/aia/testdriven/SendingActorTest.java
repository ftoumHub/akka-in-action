package aia.testdriven;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.JavaPartialFunction;
import akka.testkit.javadsl.TestKit;
import org.assertj.core.api.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class SendingActorTest extends StopSystemAfterAll{

    private final Random random = new Random();

    @DisplayName("A Sending Actor must send a message to another actor when it has finished processeing")
    @Test
    void singleThreadedUnitTest() {
        new TestKit(system) {
            {
                final Props props = SendingActor.props(getRef());
                final ActorRef sendingActor = system.actorOf(props, "sendingActor");

                final Long size = 1000L;
                final Long maxInclusive = 100000L;

                final io.vavr.collection.List<SendingActor.Event> unsorted = io.vavr.collection.List.ofAll(randomEvents(size));
                final SendingActor.SortEvents sortEvents = new SendingActor.SortEvents(unsorted);

                sendingActor.tell(sortEvents, ActorRef.noSender());

                expectMsgPF("match hint",
                        se -> {
                            if (se instanceof SendingActor.SortedEvents) {
                                final io.vavr.collection.List<SendingActor.Event> events = ((SendingActor.SortedEvents) se).events();
                                assertThat(events).hasSize(1000);
                                assertThat(unsorted.sortBy(__ -> __.id)).isEqualTo(events);
                                return "match";
                            } else {
                                throw JavaPartialFunction.noMatch();
                            }
                        });
            }
        };
    }

    private List<SendingActor.Event> randomEvents(Long size) {
        return LongStream.range(0L, size).mapToObj(__ -> new SendingActor.Event(random.nextLong())).collect(toList());
    }
}
