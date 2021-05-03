package aia.testdriven;

import aia.testdriven.SilentActorMsgs.GetState;
import aia.testdriven.SilentActorMsgs.SilentMessage;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import akka.testkit.javadsl.TestKit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static akka.actor.ActorRef.noSender;
import static io.vavr.API.List;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SilentActor01Test extends StopSystemAfterAll{

    // Listing 3.4 - Single-threaded test of internal state
    @DisplayName("change internal state when it receives a message, single")
    @Test
    void singleThreadedUnitTest() {
        // Given an actor under test
        TestActorRef<SilentActor> ref = TestActorRef.create(system, Props.create(SilentActor.class));

        // When sending a SilentMessage
        // Note: messages sent to the actor are processed synchronously on
        // the current thread and answers may be sent back as usual.
        ref.tell(new SilentMessage("whisper"), noSender());
        // The TestActorRef allows access to the underlying actor instance.
        assertTrue(ref.underlyingActor().state().contains("whisper"));
    }

    // Listing 3.6 - Multithreaded test of internal state
    @DisplayName("change internal state when it receives a message, multi")
    @Test
    void multiThreadedUnitTest() {
        new TestKit(system) {
            {
                // Cette fois on utilise l'actor system pour créer notre acteur.
                final ActorRef silentActor = system.actorOf(Props.create(SilentActor.class), "S3");

                silentActor.tell(new SilentMessage("whisper1"), noSender());
                silentActor.tell(new SilentMessage("whisper2"), noSender());

                //final TestKit probe = new TestKit(system);
                // "inject" the probe by passing it to the test subject
                // like a real resource would be passed in production
                silentActor.tell(new GetState(getRef()), getRef());
                // Used to check what message(s) have been sent to the testActor
                expectMsg(List("whisper1", "whisper2"));
            }
        };
    }
}
