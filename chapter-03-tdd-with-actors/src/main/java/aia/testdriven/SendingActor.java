package aia.testdriven;

import akka.actor.AbstractActor;
import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import io.vavr.collection.List;
import io.vavr.collection.Vector;

public class SendingActor extends AbstractLoggingActor {

    static public Props props(ActorRef receiver) {
        return Props.create(SendingActor.class, () -> new SendingActor(receiver));
    }

    private final ActorRef receiver;

    private SendingActor(ActorRef receiver) {
        this.receiver = receiver;
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(SortEvents.class, sortEvents -> receiver.tell(new SortedEvents(sortEvents.unsorted.sortBy(__ -> __.id)), ActorRef.noSender()))
                .build();
    }

    public static class Event{
        public final Long id;

        public Event(Long id) {
            this.id = id;
        }
    }

    public static class SortEvents{
        private final List<Event> unsorted;

        public SortEvents(List<Event> unsorted) {
            this.unsorted = unsorted;
        }
    }

    public class SortedEvents{
        private final List<Event> sorted;

        public SortedEvents(List<Event> sorted) {
            this.sorted = sorted;
        }

        public List<Event> events() {
            return sorted;
        }
    }
}
