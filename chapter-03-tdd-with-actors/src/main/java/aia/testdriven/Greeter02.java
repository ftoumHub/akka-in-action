package aia.testdriven;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import io.vavr.control.Option;

import static io.vavr.API.None;

/**
 * Listing 3.13 The Greeter actor
 *
 * The Greeter does just on thing: it receives a message and outputs it to the console.
 */
public class Greeter02 extends AbstractLoggingActor {

    static public Props props(Option<ActorRef> listener) {
        return Props.create(Greeter02.class, () -> new Greeter02(listener));
    }

    private final Option<ActorRef> listener;

    private Greeter02(Option<ActorRef> listener) {
        this.listener = listener.orElse(None());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                // Prints the greeting it receives
                .match(Greeting.class, who -> {
                    String message = "Hello " + who + "!";
                    log().info(message);
                    listener.forEach(__ -> __.tell(message, ActorRef.noSender()));
                })
                .build();
    }


    public static class Greeting{
        private final String message;

        public Greeting(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return  message;
        }
    }
}
