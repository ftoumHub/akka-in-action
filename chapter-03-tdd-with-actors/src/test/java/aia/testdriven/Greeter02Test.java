package aia.testdriven;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.testkit.CallingThreadDispatcher;
import akka.testkit.javadsl.EventFilter;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.vavr.API.Some;

class Greeter02Test {
    private static ActorSystem system;

    @BeforeAll
    static void beforeClass() {
        system = ActorSystem.create("testsystem");
    }

    @AfterAll
    static void afterClass() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    void sayHelloWorldWhenAGreeting() {
        TestKit probe = new TestKit(system);
        final Props props = Greeter02.props(Some(probe.getRef()));

        ActorRef greeter = system.actorOf(props, "greeter02-1");
        greeter.tell(new Greeter02.Greeting("World"), ActorRef.noSender());
        probe.expectMsg("Hello World!");
    }
}