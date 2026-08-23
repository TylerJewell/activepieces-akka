package io.akka.activepieces.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import akka.stream.javadsl.Source;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.akka.activepieces.application.FlowRunEntity;
import io.akka.activepieces.application.FlowRunner;
import io.akka.activepieces.application.FlowRunsView;
import io.akka.activepieces.domain.DemoFlows;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * The routes activepieces' web interface calls, answered from this port.
 *
 * <p>RENDERING.md R3 — the port ships the interface the original already has, so the only thing
 * that changes is where it gets its data. Two kinds of route are answered here and they are
 * different in kind:
 *
 * <ul>
 *   <li><b>The slice.</b> Flow runs, their journals and the flow they ran — produced by this
 *       port's own engine and rendered into the original's shapes by {@link ApShapes}.
 *   <li><b>The shell.</b> Flags, the platform, the project, the signed-in user: everything the
 *       application needs before it will draw anything, none of it in this port's slice. These
 *       come from {@code ap-shell/*.json}, which holds the original's own answers with the
 *       identifiers replaced by this port's. They are configuration the screens sit inside, not
 *       behaviour under comparison.
 * </ul>
 */
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/api/v1")
public class ApCompatEndpoint {

  /** How often the server re-reads a run to see whether it moved, while a client is watching. */
  private static final Duration WATCH_INTERVAL = Duration.ofMillis(400);

  private static final Duration WATCH_LIMIT = Duration.ofMinutes(10);

  private final ComponentClient componentClient;
  private final FlowRunner runner;

  public ApCompatEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
    this.runner = new FlowRunner(componentClient);
  }

  // ------------------------------------------------------------------ the slice

  @Get("/flow-runs")
  public HttpResponse listRuns() {
    return json(ApShapes.seekPage(runRows()));
  }

  @Get("/flow-runs/count-by-status")
  public HttpResponse countByStatus() {
    ObjectNode counts = ApShapes.JSON.createObjectNode();
    for (FlowRunsView.Entry entry : runs().items()) {
      counts.put(entry.status(), counts.path(entry.status()).asInt(0) + 1);
    }
    return json(counts);
  }

  @Get("/flow-runs/{runId}")
  public HttpResponse getRun(String runId) {
    FlowRunEntity.State state = read(runId);
    if (!state.started()) {
      return HttpResponses.notFound();
    }
    return json(ApShapes.flowRun(runId, state, true));
  }

  /**
   * RENDERING.md R1 and SPEC-001 §4.4 — one open connection carries the run. The first frame is
   * always the run as it stands, so a client that has just reconnected sees the same thing as one
   * connecting for the first time, and neither has to ask a second question to find out what it
   * missed.
   */
  @Get("/flow-runs/{runId}/stream")
  public HttpResponse streamRun(String runId) {
    Source<ObjectNode, ?> frames =
        ticks()
            .map(tick -> read(runId))
            .filter(FlowRunEntity.State::started)
            .statefulMapConcat(ApCompatEndpoint::onlyWhenChanged)
            .map(state -> ApShapes.flowRun(runId, state, true));
    return HttpResponses.serverSentEvents(frames);
  }

  /** The same rule for the list: current contents first, then only what changed. */
  @Get("/flow-runs/stream")
  public HttpResponse streamRuns() {
    Source<ObjectNode, ?> frames =
        ticks()
            .map(tick -> ApShapes.seekPage(runRows()))
            .statefulMapConcat(ApCompatEndpoint::onlyWhenDifferentPage);
    return HttpResponses.serverSentEvents(frames);
  }

  @Post("/flow-runs/start")
  public HttpResponse startRun() {
    String runId = "run-" + System.currentTimeMillis();
    runner.start(runId, DemoFlows.STEP_STATE_DEMO);
    return json(ApShapes.flowRun(runId, read(runId), false));
  }

  @Get("/flows")
  public HttpResponse listFlows() {
    ArrayNode data = ApShapes.JSON.createArrayNode();
    data.add(ApShapes.populatedFlow());
    return json(ApShapes.seekPage(data));
  }

  @Get("/flows/{flowId}")
  public HttpResponse getFlow(String flowId) {
    if (!ApShapes.FLOW_ID.equals(flowId)) {
      return HttpResponses.notFound();
    }
    return json(ApShapes.populatedFlow());
  }

  // ------------------------------------------------------------------ the shell

  @Get("/flags")
  public HttpResponse flags() {
    return shell("flags");
  }

  @Get("/users/{userId}")
  public HttpResponse user(String userId) {
    return shell("user");
  }

  @Get("/users")
  public HttpResponse users() {
    return shell("users");
  }

  @Get("/platforms/{platformId}")
  public HttpResponse platform(String platformId) {
    return shell("platform");
  }

  @Get("/projects")
  public HttpResponse projects() {
    return shell("projects");
  }

  @Get("/folders")
  public HttpResponse folders() {
    return emptyPage();
  }

  @Get("/user-invitations")
  public HttpResponse userInvitations() {
    return emptyPage();
  }

  @Get("/ai-providers")
  public HttpResponse aiProviders() {
    return json(ApShapes.JSON.createArrayNode());
  }

  @Get("/sample-data")
  public HttpResponse sampleData() {
    return json(ApShapes.JSON.createObjectNode());
  }

  @Get("/pieces/{scope}/{piece}")
  public HttpResponse piece(String scope, String piece) {
    if ("piece-webhook".equals(piece)) {
      return shell("piece-webhook");
    }
    return HttpResponses.notFound();
  }

  @Get("/pieces")
  public HttpResponse pieces() {
    ArrayNode data = ApShapes.JSON.createArrayNode();
    return json(data);
  }

  // ------------------------------------------------------------------ plumbing

  private FlowRunEntity.State read(String runId) {
    return componentClient.forKeyValueEntity(runId).method(FlowRunEntity::get).invoke();
  }

  private FlowRunsView.Entries runs() {
    return componentClient.forView().method(FlowRunsView::newestFirst).invoke();
  }

  private ArrayNode runRows() {
    ArrayNode data = ApShapes.JSON.createArrayNode();
    for (FlowRunsView.Entry entry : runs().items()) {
      FlowRunEntity.State state =
          new FlowRunEntity.State(
              entry.scenario(),
              entry.status(),
              entry.failedStepName().isEmpty()
                  ? null
                  : new io.akka.activepieces.domain.FailedStep(
                      entry.failedStepName(),
                      entry.failedStepDisplayName(),
                      entry.failedStepMessage()),
              java.util.Map.of(),
              entry.stepsCount(),
              entry.startedAtMillis(),
              entry.finishedAtMillis(),
              0);
      data.add(ApShapes.flowRun(entry.runId(), state, false));
    }
    return data;
  }

  private static Source<String, ?> ticks() {
    return Source.tick(Duration.ZERO, WATCH_INTERVAL, "tick").takeWithin(WATCH_LIMIT);
  }

  /**
   * A run only reaches a watcher when its revision moved. The server re-reads on a timer because
   * an entity has no change feed to subscribe to here; what R1 governs is the browser, which holds
   * one connection open and issues no request of its own.
   */
  private static akka.japi.function.Function<FlowRunEntity.State, Iterable<FlowRunEntity.State>>
      onlyWhenChanged() {
    long[] lastSeen = {-1};
    return state -> {
      if (state.revision() == lastSeen[0]) return java.util.List.of();
      lastSeen[0] = state.revision();
      return java.util.List.of(state);
    };
  }

  private static akka.japi.function.Function<ObjectNode, Iterable<ObjectNode>>
      onlyWhenDifferentPage() {
    ObjectNode[] lastSeen = {null};
    return page -> {
      if (page.equals(lastSeen[0])) return java.util.List.of();
      lastSeen[0] = page;
      return java.util.List.of(page);
    };
  }

  private HttpResponse emptyPage() {
    return json(ApShapes.seekPage(ApShapes.JSON.createArrayNode()));
  }

  private static HttpResponse json(Object node) {
    return HttpResponses.of(
        akka.http.javadsl.model.StatusCodes.OK,
        akka.http.javadsl.model.ContentTypes.APPLICATION_JSON,
        node.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static HttpResponse shell(String name) {
    String resource = "ap-shell/" + name + ".json";
    try (InputStream in = ApCompatEndpoint.class.getClassLoader().getResourceAsStream(resource)) {
      if (in == null) return HttpResponses.notFound();
      return HttpResponses.of(
          akka.http.javadsl.model.StatusCodes.OK,
          akka.http.javadsl.model.ContentTypes.APPLICATION_JSON,
          in.readAllBytes());
    } catch (IOException e) {
      return HttpResponses.internalServerError();
    }
  }
}
