package com.demo;

import akka.actor.*;

import static io.vavr.API.println;

/**
 * Un premier example d'acteur simple.
 *
 * For our first example, we use a multilanguage greeter; that is, we enter a particular language and the
 * program responds by replying “Good day,” in the language specified.
 */
public class AkkaRecap01 {

  public static void main(String[] args) throws InterruptedException {
    ActorSystem actorSystem = ActorSystem.create("MultilangSystem");

    ActorRef greeter = actorSystem.actorOf(Props.create(GreeterActor.class), "GreeterActor");

    greeter.tell("en", greeter);
    greeter.tell("es", greeter);
    greeter.tell("fr", greeter);
    greeter.tell("de", greeter);
    greeter.tell("pt", greeter);
    greeter.tell("zh-CN", greeter);

    Thread.sleep(3000);

    actorSystem.terminate();
  }

  public static class GreeterActor extends AbstractLoggingActor {

    @Override
    public Receive createReceive() {
      return receiveBuilder()
              .matchEquals("en", m -> log().info("GoodDay"))
              .matchEquals("es", m -> log().info("Buen dia"))
              .matchEquals("fr", m -> log().info("Bonjour"))
              .matchEquals("de", m -> log().info("Guten Tag"))
              .matchEquals("pt", m -> log().info("Bom dia"))
              .matchAny(m -> log().info(":(")) // cas par défaut
              .build();
    }
  }
}
