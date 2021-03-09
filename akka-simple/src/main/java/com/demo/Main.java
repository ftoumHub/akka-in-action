package com.demo;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;

import java.util.Scanner;

import static akka.actor.ActorRef.noSender;
import static io.vavr.API.println;
import static java.lang.Thread.sleep;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        ActorSystem actorSystem = ActorSystem.create("MultilangSystem");

        ActorRef greeter = actorSystem.actorOf(Props.create(AkkaRecap01.GreeterActor.class), "GreeterActor");

        // Read ranges from the console and process them
        final Scanner scanner = new Scanner(System.in);
        while (true) {
            // Sleep to reduce mixing of log messages with the regular stdout messages.
            sleep(1000);

            // Read input
            println("> Enter ...\n"
                    + "  \"language (en, es, fr, de, pt, zh-CN)\" the language you which to be greeted in,\n"
                    + "  \"kill\" for a hard shutdown:");
            String line = scanner.nextLine();

            if ("kill".equals(line)) {
                greeter.tell(PoisonPill.getInstance(), noSender());
                actorSystem.terminate();
                scanner.close();
                return;
            } else {
                greeter.tell(line, noSender());
            }
        }
    }
}
