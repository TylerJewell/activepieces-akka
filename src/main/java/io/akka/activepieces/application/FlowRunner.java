package io.akka.activepieces.application;

import akka.javasdk.client.ComponentClient;
import io.akka.activepieces.domain.DemoFlows;
import io.akka.activepieces.domain.ExecutionOptions;
import io.akka.activepieces.domain.FlowEngine;
import io.akka.activepieces.domain.RunState;
import io.akka.activepieces.domain.TriggerStep;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Drives a flow off the request thread and writes the journal to {@link FlowRunEntity} at every
 * step boundary. Off the request thread because a step that retries waits out its backoff — a run
 * of the demo flow takes about fourteen seconds — and because a watcher exists to see the steps
 * arrive one at a time rather than all at the end.
 */
public final class FlowRunner {

  /**
   * The journal is the run entity's whole state, and a state past a megabyte stops replicating
   * between regions — the target probe found a six-megabyte write refused outright
   * (question-log row 4). SPEC-001 rule 29's limit is a setting, so this service sets it below
   * the ceiling it actually has rather than at the five megabytes the rule defaults to; the
   * verdict a run gets on crossing it is the same either way.
   */
  private static final long MAX_JOURNAL_BYTES = 512L * 1024;

  private static final ExecutorService RUNS =
      Executors.newCachedThreadPool(
          r -> {
            Thread t = new Thread(r, "flow-run");
            t.setDaemon(true);
            return t;
          });

  private final ComponentClient componentClient;

  public FlowRunner(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public static boolean knows(String scenario) {
    return DemoFlows.NAMES.contains(scenario);
  }

  public static java.util.List<String> known() {
    return DemoFlows.NAMES;
  }

  /** Start {@code scenario} under {@code runId} and return once the run is registered as begun. */
  public FlowRunEntity.State start(String runId, String scenario) {
    TriggerStep trigger = DemoFlows.byName(scenario);
    FlowRunEntity.State begun =
        componentClient.forKeyValueEntity(runId).method(FlowRunEntity::begin).invoke(scenario);
    Object payload =
        DemoFlows.STEP_STATE_DEMO.equals(scenario)
            ? DemoFlows.DEMO_PAYLOAD
            : Map.of("scenario", scenario);
    RUNS.execute(() -> execute(runId, scenario, trigger, payload));
    return begun;
  }

  /** Run {@code scenario} to completion on the calling thread, for callers that want the answer. */
  public FlowRunEntity.State startAndAwait(String runId, String scenario) {
    TriggerStep trigger = DemoFlows.byName(scenario);
    componentClient.forKeyValueEntity(runId).method(FlowRunEntity::begin).invoke(scenario);
    Object payload =
        DemoFlows.STEP_STATE_DEMO.equals(scenario)
            ? DemoFlows.DEMO_PAYLOAD
            : Map.of("scenario", scenario);
    execute(runId, scenario, trigger, payload);
    return componentClient.forKeyValueEntity(runId).method(FlowRunEntity::get).invoke();
  }

  private void execute(String runId, String scenario, TriggerStep trigger, Object payload) {
    ExecutionOptions options =
        ExecutionOptions.defaults()
            .withMaxLogSizeBytes(MAX_JOURNAL_BYTES)
            .withProgress(state -> write(runId, scenario, state, false));
    RunState finished = FlowEngine.runFlow(trigger, payload, options);
    write(runId, scenario, finished, true);
  }

  private void write(String runId, String scenario, RunState state, boolean finished) {
    componentClient
        .forKeyValueEntity(runId)
        .method(FlowRunEntity::record)
        .invoke(new FlowRunEntity.Snapshot(scenario, state, finished));
  }
}
