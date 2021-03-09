package aia.testdriven;

import aia.testdriven.SilentActorMsgs.GetState;
import aia.testdriven.SilentActorMsgs.SilentMessage;
import akka.actor.AbstractActor;
import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import io.vavr.collection.List;

import static io.vavr.collection.List.empty;


public class SilentActor extends AbstractActor {

    private ActorRef target = null;
    private List<String> internalState = empty();

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(SilentMessage.class, msg -> internalState = internalState.append(msg.data))
                .match(GetState.class, msg -> {
                            target = msg.receiver;
                            getSender().tell(state(), getSelf());
                        })
                .build();
    }

    public List<String> state() {
        return internalState;
    }
}
