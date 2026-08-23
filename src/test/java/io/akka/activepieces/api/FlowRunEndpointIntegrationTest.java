package io.akka.activepieces.api;

import static org.assertj.core.api.Assertions.assertThat;

import akka.javasdk.testkit.TestKitSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

/**
 * The capability driven the way something outside a test would drive it — over HTTP, through the
 * endpoints, with nothing reaching in past them, on a started runtime.
 *
 * <p>Its sibling tests call the engine directly, which is a shorter path and says nothing about
 * whether the surface exists or works. A port whose logic is only ever reached through its own
 * classes has no reachable capability at all, which is a gap of the same kind as an unanswered
 * rendering manifest.
 */
public class FlowRunEndpointIntegrationTest extends TestKitSupport {

  private static final ObjectMapper JSON = new ObjectMapper();

  private RunView start(String runId, String scenario) {
    return httpClient
        .POST("/flow-runs/" + runId + "/" + scenario)
        .responseBodyAs(RunView.class)
        .invoke()
        .body();
  }

  @Test
  void aFlowIsRunAndItsJournalReadBackOverHttp() {
    String runId = "chain-" + UUID.randomUUID();

    RunView started = start(runId, "happy");
    assertThat(started.status()).isEqualTo("SUCCEEDED");
    assertThat(started.steps().keySet()).containsExactly("trigger", "step1", "step2");

    RunView readBack =
        httpClient.GET("/flow-runs/" + runId).responseBodyAs(RunView.class).invoke().body();
    assertThat(readBack.status()).isEqualTo("SUCCEEDED");
    assertThat(readBack.steps()).hasSize(3);
  }

  @Test
  void aFailingFlowNamesItsFailedStepOverHttp() {
    String runId = "failing-" + UUID.randomUUID();

    RunView state = start(runId, "router");
    assertThat(state.status()).isEqualTo("SUCCEEDED");

    String loopRunId = "loop-" + UUID.randomUUID();
    RunView loop = start(loopRunId, "loop");
    assertThat(loop.status()).isEqualTo("SUCCEEDED");
    assertThat(loop.steps()).containsKey("loop");
  }

  @Test
  void streamSendsTheCurrentStateOnConnectThenLiveUpdates() {
    String runId = "streamed-" + UUID.randomUUID();
    start(runId, "happy");

    // Connecting after the run has finished still gets the run: the stream's first frame is
    // the state as it stands, which is what SPEC-001 §4.4 promises a reconnecting client.
    try (SseSession session = new SseSession(testKit.getPort(), "/flow-runs/" + runId + "/stream")) {
      var frames = session.awaitFrames(1, Duration.ofSeconds(20));
      assertThat(frames).isNotEmpty();
      JsonNode first = parse(frames.get(0).data());
      assertThat(first.path("status").asText()).isEqualTo("SUCCEEDED");
      assertThat(first.path("steps").size()).isEqualTo(3);
    }
  }

  @Test
  void aWatcherSeesTheStepsArriveRatherThanOnlyTheEndState() {
    String runId = "watched-" + UUID.randomUUID();

    // A flow whose steps all return at once finishes inside one tick of the watcher's
    // connection, so it is not evidence either way. `retry-then-fail` waits out rule 13's
    // backoff between its three attempts, which spans several seconds of a live run.
    try (SseSession session = new SseSession(testKit.getPort(), "/flow-runs/" + runId + "/stream")) {
      httpClient.POST("/flow-runs/" + runId + "/retry-then-fail/detached").invoke();

      var frames = session.awaitFrames(3, Duration.ofSeconds(40));
      assertThat(frames.size())
          .as("a run spread over twelve seconds should reach a watcher in more than one frame")
          .isGreaterThan(1);
      assertThat(parse(frames.get(0).data()).path("status").asText())
          .as("the first frame is the run in progress, not its end state")
          .isEqualTo("RUNNING");

      Awaitility.await()
          .atMost(Duration.ofSeconds(40))
          .pollInterval(Duration.ofMillis(500))
          .untilAsserted(
              () -> {
                var seen = session.awaitFrames(1, Duration.ofMillis(1));
                JsonNode last = parse(seen.get(seen.size() - 1).data());
                assertThat(last.path("status").asText()).isEqualTo("FAILED");
              });
    }
  }

  @Test
  void anUnknownScenarioIsRefusedRatherThanTurnedIntoAServerError() {
    var response =
        httpClient
            .POST("/flow-runs/nope-" + UUID.randomUUID() + "/not-a-scenario")
            .invoke();
    assertThat(response.httpResponse().status().intValue()).isEqualTo(400);
    assertThat(response.body().utf8String()).contains("unknown scenario: not-a-scenario");
  }

  @Test
  void theInterfacesOwnRoutesAnswerFromTheSameRun() {
    httpClient.POST("/api/v1/flow-runs/start").invoke();

    Awaitility.await()
        .atMost(Duration.ofSeconds(60))
        .pollInterval(Duration.ofSeconds(1))
        .untilAsserted(
            () -> {
              JsonNode page = parse(getBody("/api/v1/flow-runs"));
              assertThat(page.path("data").size()).isGreaterThan(0);
              assertThat(page.path("data").get(0).path("status").asText()).isEqualTo("FAILED");
            });

    JsonNode page = parse(getBody("/api/v1/flow-runs"));
    String runId = page.path("data").get(0).path("id").asText();

    JsonNode run = parse(getBody("/api/v1/flow-runs/" + runId));
    assertThat(run.path("flowId").asText()).isEqualTo("step-state-demo");
    assertThat(run.path("failedStep").path("name").asText()).isEqualTo("step_4");
    assertThat(run.path("steps").path("loop_1").path("output").path("iterations").size())
        .isEqualTo(3);
    assertThat(run.path("steps").path("router_1").path("output").path("branches").get(0)
            .path("evaluation").asBoolean())
        .isTrue();

    // The definition the canvas is drawn from is the same tree the run executed.
    JsonNode flow = parse(getBody("/api/v1/flows/step-state-demo"));
    assertThat(flow.path("version").path("trigger").path("nextAction").path("name").asText())
        .isEqualTo("step_1");
  }

  private String getBody(String path) {
    return httpClient.GET(path).invoke().body().utf8String();
  }

  private static JsonNode parse(String body) {
    try {
      return JSON.readTree(body);
    } catch (Exception e) {
      throw new AssertionError("not JSON: " + body, e);
    }
  }
}
