package com.demo;

import akka.actor.*;
import akka.persistence.AbstractPersistentActor;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// ===============================================================
// Demo of an Event-driven Architecture in Akka and Java.
//
// Show-casing:
//   1. Events-first Domain Driven Design using Commands and Events
//   2. Asynchronous communication through an Event Stream
//   3. Asynchronous Process Manager driving the workflow
//   4. Event-sourced Aggregates
//
// Used in my talk on 'How Events Are Reshaping Modern Systems'.
//
// NOTE: This is a very much simplified and dumbed down sample
//       that is by no means a template for production use.
//       F.e. in a real-world app I would not use Serializable
//       but a JSON, Protobuf, Avro, or some other good lib.
//       I would not use Akka's built in EventStream, but
//       probably Kafka or Kinesis. Etc.
// ===============================================================

// =========================================================
// Commands
// =========================================================
interface Command extends Serializable {
}

class CreateOrder implements Command {
    final int userId;
    final int productId;

    CreateOrder(int userId, int productId) {
        this.userId = userId;
        this.productId = productId;
    }
}

class ReserveProduct implements Command {
    final int userId;
    final int productId;

    ReserveProduct(int userId, int productId) {
        this.userId = userId;
        this.productId = productId;
    }
}

class SubmitPayment implements Command {
    final int userId;
    final int productId;

    SubmitPayment(int userId, int productId) {
        this.userId = userId;
        this.productId = productId;
    }
}

class ShipProduct implements Command {
    final int userId;
    final int txId;

    ShipProduct(int userId, int txId) {
        this.userId = userId;
        this.txId = txId;
    }
}

// =========================================================
// Events
// =========================================================
interface Event extends Serializable {
}

class ProductReserved implements Event {
    final int userId;
    final int txId;

    ProductReserved(int userId, int txId) {
        this.userId = userId;
        this.txId = txId;
    }
}

class ProductOutOfStock implements Event {
    final int userId;
    final int productId;

    ProductOutOfStock(int userId, int productId) {
        this.userId = userId;
        this.productId = productId;
    }
}

class PaymentAuthorized implements Event {
    final int userId;
    final int txId;

    PaymentAuthorized(int userId, int txId) {
        this.userId = userId;
        this.txId = txId;
    }
}

class PaymentDeclined implements Event {
    final int userId;
    final int txId;

    PaymentDeclined(int userId, int txId) {
        this.userId = userId;
        this.txId = txId;
    }
}

class ProductShipped implements Event {
    final int userId;
    final int txId;

    ProductShipped(int userId, int txId) {
        this.userId = userId;
        this.txId = txId;
    }
}

class OrderCompleted implements Event {
    final int userId;
    final int txId;

    OrderCompleted(int userId, int txId) {
        this.userId = userId;
        this.txId = txId;
    }
}

// =========================================================
// Top-level service functioning as a Process Manager
// Coordinating the workflow on behalf of the Client
// =========================================================
class Orders extends AbstractActor {

    final ActorRef client;
    final ActorRef inventory;
    final ActorRef payment;

    public Orders(ActorRef client, ActorRef inventory, ActorRef payment) {
        this.client = client;
        this.inventory = inventory;
        this.payment = payment;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(CreateOrder.class, cmd -> {
                    System.out.println("COMMAND:\t\t" + cmd + " => " + getSelf().path().name());
                    inventory.tell(new ReserveProduct(cmd.userId, cmd.productId), getSelf());
                })
                .match(ProductReserved.class, evt -> {
                    System.out.println("EVENT:\t\t\t" + evt + " => " + getSelf().path().name());
                    payment.tell(new SubmitPayment(evt.userId, evt.txId), getSelf());
                })
                .match(PaymentAuthorized.class, evt -> {
                    System.out.println("EVENT:\t\t\t" + evt + " => " + getSelf().path().name());
                    inventory.tell(new ShipProduct(evt.userId, evt.txId), getSelf());
                })
                .match(ProductShipped.class, evt -> {
                    System.out.println("EVENT:\t\t\t" + evt + " => " + getSelf().path().name());
                    client.tell(new OrderCompleted(evt.userId, evt.txId), getSelf());
                })
                .build();
    }

    @Override
    public void preStart() {
        // Subscribe to Events from the Event Stream
        getContext().system().eventStream().subscribe(getSelf(), ProductReserved.class);
        getContext().system().eventStream().subscribe(getSelf(), ProductOutOfStock.class);
        getContext().system().eventStream().subscribe(getSelf(), ProductShipped.class);
        getContext().system().eventStream().subscribe(getSelf(), PaymentAuthorized.class);
        getContext().system().eventStream().subscribe(getSelf(), PaymentDeclined.class);
    }
}


// =========================================================
// Event Sourced Aggregate
// =========================================================
class Inventory extends AbstractPersistentActor {

    @Override
    public String persistenceId() {
        return "inventory";
    }

    int nrOfProductsShipped = 0; // Mutable state, persisted in memory (AKA Memory Image)

    Event reserveProduct(int userId, int productId) {
        System.out.println("SIDE-EFFECT:\tReserving Product => " + getSelf().path().name());
        return new ProductReserved(userId, productId);
    }

    Event shipProduct(int userId, int txId) {
        nrOfProductsShipped += 1; // Update internal state
        System.out.println("SIDE-EFFECT:\tShipping Product => " + getSelf().path().name() +
                " - ProductsShipped: " + nrOfProductsShipped);
        return new ProductShipped(userId, txId);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ReserveProduct.class, cmd -> {                                // Receive ReserveProduct Command
                    System.out.println("COMMAND:\t\t" + cmd + " => " + getSelf().path().name());
                    Event productStatus = reserveProduct(cmd.userId, cmd.productId); // Try to reserve the product
                    persist(productStatus, evt -> {                                  // Try to persist the Event
                        getContext().system().eventStream().publish(evt);            // Publish Event to Event Stream
                    });

                })
                .match(ShipProduct.class, cmd -> {                                   // Receive ShipProduct Command
                    System.out.println("COMMAND:\t\t" + cmd + " => " + getSelf().path().name());
                    Event shippingStatus = shipProduct(cmd.userId, cmd.txId);        // Try to ship the product
                    persist(shippingStatus, evt -> {                                 // Try to persist the Event
                        getContext().system().eventStream().publish(evt);            // Publish Event to Event Stream
                    });
                })
                .build();
    }

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(ProductReserved.class, evt -> { // Replay ProductReserved
                    System.out.println("EVENT (REPLAY):\t" + evt + " => " + getSelf().path().name());
                })
                .match(ProductShipped.class, evt -> {  // Replay ProductShipped
                    nrOfProductsShipped += 1;          // Update the internal state
                    System.out.println("EVENT (REPLAY):\t" + evt + " => " + getSelf().path().name() +
                            " - ProductsShipped: " + nrOfProductsShipped);
                })
                .build();
    }

}


// =========================================================
// Event Sourced Aggregate
// =========================================================
class Payment extends AbstractPersistentActor {

    @Override
    public String persistenceId() {
        return "payment";
    }

    int uniqueTransactionNr = 0; // Mutable state, persisted in memory (AKA Memory Image)

    Event processPayment(int userId, int txId) {
        uniqueTransactionNr += 1;  // Update the internal state
        System.out.println("SIDE-EFFECT:\tProcessing payment => " + getSelf().path().name() +
                " - TxNumber: " + uniqueTransactionNr);
        return new PaymentAuthorized(userId, uniqueTransactionNr);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SubmitPayment.class, cmd -> {                                 // Receive SubmitPayment Command
                    System.out.println("COMMAND:\t\t" + cmd + " => " + getSelf().path().name());
                    Event paymentStatus = processPayment(cmd.userId, cmd.productId); // Try to pay product
                    persist(paymentStatus, evt -> {                                  // Try to persist the Event
                        getContext().system().eventStream().publish(evt);            // Publish Event to Event Stream
                    });

                })
                .build();
    }


    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(PaymentAuthorized.class, evt -> { // Replay PaymentAuthorized
                    uniqueTransactionNr += 1;            // Update the internal state
                    System.out.println("EVENT (REPLAY):\t" + evt + " => " + getSelf().path().name() +
                            " - TxNumber: " + uniqueTransactionNr);
                })
                .build();
    }
}

// =========================================================
// Running the Order Management simulation
// =========================================================
public class OrderManagement {
    public static void main(String... args) throws Exception {

        // Create the Order Management actor system
        final ActorSystem system = ActorSystem.create("OrderManagement");

        // Plumbing for "client"
        final Inbox clientInbox = Inbox.create(system);
        final ActorRef client = clientInbox.getRef();


        // Create the services
        final ActorRef inventory = system.actorOf(Props.create(Inventory.class), "Inventory");
        final ActorRef payment = system.actorOf(Props.create(Payment.class), "Payment");
        final ActorRef orders = system.actorOf(Props.create(Orders.class, client, inventory, payment), "Orders");

        // Send a CreateOrder Command to the Orders service
        clientInbox.send(orders, new CreateOrder(9, 1337));

        try {
            // Wait for the order confirmation
            Object confirmation = clientInbox.receive(Duration.create(5, TimeUnit.SECONDS));
            System.out.println("EVENT:\t\t\t" + confirmation + " => Client");

        } catch (TimeoutException e) {
            System.out.println("Waited 5 seconds for the OrderCompleted event, giving up...");
        }

        System.out.println("Order completed. Shutting down system.");

        system.terminate();
    }
}
