package aia.testdriven;

import aia.testdriven.SilentActorMsgs.GetState;
import aia.testdriven.SilentActorMsgs.SilentMessage;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import akka.testkit.javadsl.TestKit;
import org.junit.jupiter.api.Test;

import static akka.actor.ActorRef.noSender;
import static io.vavr.API.List;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SilentActor01Test extends StopSystemAfterAll{

    @Test
    void internalState() {
        // Given an actor under test
        TestActorRef<SilentActor> ref = TestActorRef.create(system, Props.create(SilentActor.class));

        // When sending a SilentMessage
        /* Note: messages sent to the actor are process synchronously on
         * the current thread and answers may be sent back as usual.
         */
        ref.tell(new SilentMessage("whisper"), noSender());
        assertTrue(ref.underlyingActor().state().contains("whisper"));
    }

    @Test
    void multiThreaded() {
        new TestKit(system) {
            {
                // Cette fois on utilise l'actor system pour créer notre acteur.
                final ActorRef silentActor = system.actorOf(Props.create(SilentActor.class), "S3");

                silentActor.tell(new SilentMessage("whisper1"), noSender());
                silentActor.tell(new SilentMessage("whisper2"), noSender());

                final TestKit probe = new TestKit(system);
                // “inject” the probe by passing it to the test subject
                // like a real resource would be passed in production
                silentActor.tell(new GetState(getRef()), getRef());
                // await the correct response
                expectMsg(List("whisper1", "whisper2"));
                //silentActor.tell(new SilentActorMsgs.GetState(silentActor), noSender());
            }
        };
    }
}
