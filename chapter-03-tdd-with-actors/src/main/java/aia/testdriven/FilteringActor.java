package aia.testdriven;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import io.vavr.collection.List;

import java.util.Objects;

/**
 * The FilteringActor keeps a buffer of the last messages that it received in a List
 * and adds every received message to that buffer if it doesn't already exist in the list.
 * Only messages that aren't in the buffer are sent to the nextActor. The oldest message
 * that was received is discarded when a max bufferSize is reached to prevent the last-
 * Messages list from growing too large and possibly causing us to run out of space.
 */
public class FilteringActor extends AbstractLoggingActor {

    static public Props props(ActorRef nextActor, Integer bufferSize) {
        return Props.create(FilteringActor.class, () -> new FilteringActor(nextActor, bufferSize));
    }

    private final ActorRef nextActor;
    private final Integer bufferSize;
    List<Event> lastMessages = List.empty();

    // Max size for the buffer is passed into constructor
    private FilteringActor(ActorRef nextActor, Integer bufferSize) {
        this.nextActor = nextActor;
        this.bufferSize = bufferSize;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        Event.class,
                        msg -> {
                            if (!lastMessages.contains(msg)) {
                                lastMessages = lastMessages.append(msg);
                                // Event is sent to next actor if it's not
                                // found in the buffer
                                nextActor.tell(msg, getSelf());
                                if (lastMessages.size() > bufferSize) {
                                    // Oldest event in the buffer is discarded when max
                                    // buffer size is reached
                                    lastMessages = lastMessages.tail();
                                }
                            }
                        }
                )
                .build();
    }

    public static class Event{
        public final Long id;

        public Event(Long id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Event event = (Event) o;
            return Objects.equals(id, event.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }
}
