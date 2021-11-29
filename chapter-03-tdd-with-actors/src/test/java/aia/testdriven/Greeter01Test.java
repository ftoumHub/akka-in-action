package aia.testdriven;

import akka.actor.*;
import akka.event.Logging;
import akka.testkit.CallingThreadDispatcher;
import akka.testkit.javadsl.EventFilter;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class Greeter01Test {
    private static ActorSystem system;

    @BeforeAll
    static void beforeClass() {
        final Config config = ConfigFactory.parseString("akka.loggers = [akka.testkit.TestEventListener]");
        system = ActorSystem.create("testsystem", config);
    }

    @AfterAll
    static void afterClass() {
        TestKit.shutdownActorSystem(system);
    }

    public static class Greeter extends AbstractLoggingActor {

        @Override
        public AbstractActor.Receive createReceive() {
            return receiveBuilder()
                    // Prints the greeting it receives
                    .match(Greeting.class, g -> log().info("Hello {}!", g.message))
                    .build();
        }
    }

    public static class Greeting{
        private final String message;

        public Greeting(String message) {
            this.message = message;
        }
    }

    @Test
    void testReceiveBuilder() {
        TestKit probe = new TestKit(system);
        final String dispatcherId = CallingThreadDispatcher.Id();
        // Single threaded environment
        final Props props = Props.create(Greeter.class).withDispatcher(dispatcherId);

        ActorRef greeter = system.actorOf(props);
        new EventFilter(Logging.Info.class, system) {
            protected Void run() {
                greeter.tell(new Greeting("World"), probe.getRef());
                return null;
            }
        }.occurrences(1).intercept(() -> new String("Hello World!"));
    }
}