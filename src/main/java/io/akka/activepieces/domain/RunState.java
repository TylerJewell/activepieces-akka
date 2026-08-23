package io.akka.activepieces.domain;

import java.util.List;
import java.util.Map;

/** SPEC-001 §2. */
public record RunState(
    Map<String, StepOutput> steps,
    Verdict verdict,
    List<String> tags,
    int stepsCount,
    long logSizeBytes,
    long durationMs) {

  public static RunState empty() {
    return new RunState(Map.of(), Verdict.RUNNING, List.of(), 0, 0, 0);
  }

  public RunState withSteps(Map<String, StepOutput> newSteps) {
    return new RunState(newSteps, verdict, tags, newSteps.size(), logSizeBytes, durationMs);
  }

  public RunState withVerdict(Verdict newVerdict) {
    return new RunState(steps, newVerdict, tags, stepsCount, logSizeBytes, durationMs);
  }

  public RunState withLogSizeBytes(long bytes) {
    return new RunState(steps, verdict, tags, stepsCount, bytes, durationMs);
  }

  /** Rule 5 — finishExecution turns RUNNING into SUCCEEDED and leaves every other verdict alone. */
  public RunState finishExecution() {
    return verdict.isRunning() ? withVerdict(Verdict.SUCCEEDED) : this;
  }
}
