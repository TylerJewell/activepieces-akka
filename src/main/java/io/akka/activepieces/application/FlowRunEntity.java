package io.akka.activepieces.application;

import akka.javasdk.annotations.Component;
import akka.javasdk.keyvalueentity.KeyValueEntity;
import akka.javasdk.keyvalueentity.KeyValueEntityContext;
import io.akka.activepieces.domain.FailedStep;
import io.akka.activepieces.domain.RunState;
import io.akka.activepieces.domain.StepOutput;
import io.akka.activepieces.domain.Verdict;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One flow run's journal. The engine writes here at every step boundary, so what a watcher reads
 * is the run as it stands rather than only its end state.
 */
@Component(id = "flow-run")
public class FlowRunEntity extends KeyValueEntity<FlowRunEntity.State> {

  /**
   * @param revision incremented on every write, so a stream can tell a repeat poll of unchanged
   *     state from a genuine change without comparing whole journals
   */
  public record State(
      String scenario,
      String status,
      FailedStep failedStep,
      Map<String, StepOutput> steps,
      int stepsCount,
      long startedAtMillis,
      long finishedAtMillis,
      long revision) {

    public static State empty() {
      return new State(null, "NOT_STARTED", null, Map.of(), 0, 0, 0, 0);
    }

    public boolean started() {
      return !"NOT_STARTED".equals(status);
    }
  }

  /** What the runner hands over after a journal write. */
  public record Snapshot(String scenario, RunState run, boolean finished) {}

  public FlowRunEntity(KeyValueEntityContext context) {}

  @Override
  public State emptyState() {
    return State.empty();
  }

  public Effect<State> begin(String scenario) {
    State state =
        new State(scenario, "RUNNING", null, Map.of(), 0, System.currentTimeMillis(), 0,
            currentState().revision() + 1);
    return effects().updateState(state).thenReply(state);
  }

  public Effect<State> record(Snapshot snapshot) {
    State previous = currentState();
    RunState run = snapshot.run();
    long finishedAt = snapshot.finished() ? System.currentTimeMillis() : 0;
    State state =
        new State(
            snapshot.scenario(),
            snapshot.finished() ? statusOf(run.verdict()) : "RUNNING",
            failedStepOf(run.verdict()),
            new LinkedHashMap<>(run.steps()),
            run.stepsCount(),
            previous.startedAtMillis() == 0 ? System.currentTimeMillis() : previous.startedAtMillis(),
            finishedAt,
            previous.revision() + 1);
    return effects().updateState(state).thenReply(state);
  }

  public ReadOnlyEffect<State> get() {
    return effects().reply(currentState());
  }

  static String statusOf(Verdict verdict) {
    if (verdict instanceof Verdict.Running) return "RUNNING";
    if (verdict instanceof Verdict.Succeeded) return "SUCCEEDED";
    if (verdict instanceof Verdict.Paused) return "PAUSED";
    if (verdict instanceof Verdict.Failed) return "FAILED";
    if (verdict instanceof Verdict.LogSizeExceeded) return "LOG_SIZE_EXCEEDED";
    throw new IllegalStateException("unknown verdict: " + verdict);
  }

  static FailedStep failedStepOf(Verdict verdict) {
    if (verdict instanceof Verdict.Failed f) return f.failedStep();
    if (verdict instanceof Verdict.LogSizeExceeded f) return f.failedStep();
    return null;
  }
}
