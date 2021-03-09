package aia.testdriven;

import akka.actor.ActorRef;

// A companion object which keeps related messages together
public interface SilentActorMsgs {

    // The message type that the silent actor can process
    class SilentMessage {
        public final String data;

        public SilentMessage(String data) {
            this.data = data;
        }
    }

    class GetState {
        public final ActorRef receiver;

        public GetState(ActorRef receiver) {
            this.receiver = receiver;
        }
    }

}
