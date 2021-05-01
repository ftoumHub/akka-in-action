package aia.testdriven;

import akka.actor.AbstractActor;
import akka.actor.AbstractLoggingActor;

public class Greeter extends AbstractLoggingActor {

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
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
