package aia.testdriven;

import akka.actor.AbstractActor;
import akka.actor.AbstractLoggingActor;

/**
 * Listing 3.13 The Greeter actor
 *
 * The Greeter does just on thing: it receives a message and outputs it to the console.
 */
public class Greeter01 extends AbstractLoggingActor {

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                // Prints the greeting it receives
                .match(Greeting.class, g -> log().info("Hello {}!", g.message))
                .build();
    }

    public class Greeting{
        private final String message;

        public Greeting(String message) {
            this.message = message;
        }
    }
}
