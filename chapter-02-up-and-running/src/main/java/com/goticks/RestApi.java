package com.goticks;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import com.goticks.IBoxOffice.*;
import com.goticks.IEventMarshalling.*;
import io.vavr.control.Option;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.marshallers.jackson.Jackson.marshaller;
import static akka.http.javadsl.model.StatusCodes.*;
import static akka.http.javadsl.server.PathMatchers.segment;
import static akka.pattern.PatternsCS.ask;
import static io.vavr.control.Option.ofOptional;

/**
 * Création d'un évènement {@link #createEvent(String)} via une requête POST
 */
public class RestApi extends AllDirectives {

  private final Duration timeout;
  private final LoggingAdapter log;
  private final ActorRef boxOfficeActor;
  private final String msg = "      📩 {}";

  RestApi(ActorSystem system, Duration timeout) {
    this.timeout = timeout;
    log = Logging.getLogger(system, this);
    boxOfficeActor = system.actorOf(BoxOffice.props(timeout), "boxOfficeActor");
  }

  public Route createRoute() {
    return route(
        pathPrefix("events", () -> route(
            getEvents(),
            pathPrefix(segment(), (String name) -> route(
                getEvent(name),
                createEvent(name),
                cancelEvent(name)
            )),
            pathPrefix(segment().slash(segment("tickets")), (String event) -> route(
                requestTickets(event)
            ))
        ))
    );
  }

  private Route getEvents() {
    // [Get all events] GET /events/
    return get(() ->
        pathEndOrSingleSlash(() -> {
          log.debug("---------- GET /events/ ----------");

          CompletionStage<Events> events =
              ask(boxOfficeActor, new GetEvents(), timeout)
                  .thenApply((Events.class::cast));

          return onSuccess(() -> events, maybeEvent -> {
            log.debug(msg, maybeEvent);
            return completeOK(maybeEvent, marshaller());
          });
        })
    );
  }

  @SuppressWarnings("unchecked")
  private Route getEvent(String name) {
    // [Get an event] GET /events/:name/
    return pathEndOrSingleSlash(() ->
        get(() -> {
          log.debug("---------- GET /events/{}/ ----------", name);

          CompletionStage<Optional<Event>> futureEvent =
              ask(boxOfficeActor, new GetEvent(name), timeout)
                  .thenApply(obj -> (Optional<Event>) obj);

          return onSuccess(() -> futureEvent, maybeEvent -> {
            log.debug(msg, maybeEvent);
            return ofOptional(maybeEvent)
                    .fold(
                            () -> complete(NOT_FOUND),
                            event -> completeOK(event, marshaller())
                    );
          });
        })
    );
  }

  private Route createEvent(String name) {
    // [Create an event] POST /events/:name/ tickets:=:tickets
    return pathEndOrSingleSlash(() ->
        post(() ->
            entity(Jackson.unmarshaller(EventDescription.class), event -> {
              log.debug("---------- POST /events/{}/ {\"tickets\":{}} ----------", name, event.getTickets());

              CompletionStage<EventResponse> futureEventResponse =
                  ask(boxOfficeActor, new CreateEvent(name, event.getTickets()), timeout)
                      .thenApply(EventResponse.class::cast);

              return onSuccess(() -> futureEventResponse, maybeEventResponse -> {
                log.debug(msg, maybeEventResponse);

                if (maybeEventResponse instanceof EventCreated) {
                  Event maybeEvent = ((EventCreated) maybeEventResponse).getEvent();
                  return complete(CREATED, maybeEvent, marshaller());
                } else {
                  EventError err = new EventError(name + " exists already.");
                  return complete(BAD_REQUEST, err, marshaller());
                }
              });
            })
        )
    );
  }

  private Route requestTickets(String event) {
    // [Buy tickets] POST /events/:event/tickets/ tickets:=:request
    return pathEndOrSingleSlash(() ->
        post(() ->
            entity(Jackson.unmarshaller(TicketRequest.class), request -> {
              log.debug("---------- POST /events/{}/tickets/ {\"tickets\":{}} ----------", event, request.getTickets());

              CompletionStage<TicketSeller.Tickets> futureTickets =
                  ask(boxOfficeActor, new GetTickets(event, request.getTickets()), timeout)
                      .thenApply(TicketSeller.Tickets.class::cast);

              return onSuccess(() -> futureTickets, maybeTickets -> {
                log.debug(msg, maybeTickets);

                if (maybeTickets.getEntries().isEmpty())
                  return complete(NOT_FOUND);
                else
                  return complete(CREATED, maybeTickets, marshaller());
              });
            })
        )
    );
  }

  @SuppressWarnings("unchecked")
  private Route cancelEvent(String name) {
    // [Cancel an event] DELETE /events/:name/
    return pathEndOrSingleSlash(() ->
        delete(() -> {
          log.debug("---------- DELETE /events/{}/ ----------", name);

          CompletionStage<Optional<Event>> futureEvent =
              ask(boxOfficeActor, new CancelEvent(name), timeout)
                  .thenApply(obj -> (Optional<Event>) obj);

          return onSuccess(() -> futureEvent, maybeEvent -> {
            log.debug(msg, maybeEvent);

            if (maybeEvent.isPresent())
              return completeOK(maybeEvent.get(), marshaller());
            else
              return complete(NOT_FOUND);
          });
        })
    );
  }

}
