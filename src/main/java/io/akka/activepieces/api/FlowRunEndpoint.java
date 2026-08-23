package io.akka.activepieces.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import akka.stream.javadsl.Source;
import io.akka.activepieces.application.FlowRunEntity;
import io.akka.activepieces.application.FlowRunner;
import java.time.Duration;
import java.util.List;

/**
 * The port's own surface on the capability: start a run of a named flow, read its journal, or
 * watch it.
 *
 * <p>Separate from {@link ApCompatEndpoint}, which answers in activepieces' shapes so the
 * original's interface can read it. This one answers in the port's own, and is what an ordinary
 * caller — a script, a test, another service — talks to.
 */
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.ALL))
@HttpEndpoint("/flow-runs")
public class FlowRunEndpoint {

  private final ComponentClient componentClient;
  private final FlowRunner runner;

  public FlowRunEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
    this.runner = new FlowRunner(componentClient);
  }

  /**
   * Start {@code scenario} and answer once it has finished, journal and verdict included. A flow
   * whose steps retry waits out rule 13's backoff before it answers — twenty-eight seconds for the
   * demo flow — so a caller that does not want to wait uses the detached route below.
   */
  @Post("/{runId}/{scenario}")
  public HttpResponse start(String runId, String scenario) {
    if (!FlowRunner.knows(scenario)) {
      return unknownScenario(scenario);
    }
    return HttpResponses.ok(RunView.of(runId, runner.startAndAwait(runId, scenario)));
  }

  /** Start {@code scenario} and answer straight away, leaving the run to be watched. */
  @Post("/{runId}/{scenario}/detached")
  public HttpResponse startDetached(String runId, String scenario) {
    if (!FlowRunner.knows(scenario)) {
      return unknownScenario(scenario);
    }
    return HttpResponses.ok(RunView.of(runId, runner.start(runId, scenario)));
  }

  @Get("/{runId}")
  public RunView get(String runId) {
    return RunView.of(runId, read(runId));
  }

  /**
   * SPEC-001 §4.4 — the run as it stands is the first thing on the wire, then every change to it.
   * A client that dropped and came back is in the same position as one that has never connected:
   * it is told the current state rather than left to work out what it missed.
   */
  @Get("/{runId}/stream")
  public HttpResponse stream(String runId) {
    Source<RunView, ?> frames =
        Source.tick(Duration.ZERO, Duration.ofMillis(200), "tick")
            .takeWithin(Duration.ofMinutes(10))
            .map(tick -> read(runId))
            .statefulMapConcat(FlowRunEndpoint::onlyWhenChanged)
            .map(state -> RunView.of(runId, state));
    return HttpResponses.serverSentEvents(frames);
  }

  private FlowRunEntity.State read(String runId) {
    return componentClient.forKeyValueEntity(runId).method(FlowRunEntity::get).invoke();
  }

  /**
   * A name nobody defined is the caller's mistake, and saying so is more use than the correlation
   * id an unhandled exception would turn into.
   */
  private static HttpResponse unknownScenario(String scenario) {
    return HttpResponses.badRequest(
        "unknown scenario: " + scenario + " — known: " + String.join(", ", FlowRunner.known()));
  }

  private static akka.japi.function.Function<FlowRunEntity.State, Iterable<FlowRunEntity.State>>
      onlyWhenChanged() {
    long[] lastSeen = {-1};
    return state -> {
      if (state.revision() == lastSeen[0]) return List.of();
      lastSeen[0] = state.revision();
      return List.of(state);
    };
  }
}
