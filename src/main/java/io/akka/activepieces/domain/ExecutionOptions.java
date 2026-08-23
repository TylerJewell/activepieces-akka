package io.akka.activepieces.domain;

/** SPEC-001 — options threaded through every step of a run. */
public record ExecutionOptions(
    boolean singleStepTestMode, long maxLogSizeBytes, ProgressSink progress, Sleeper sleeper) {

  public ExecutionOptions(boolean singleStepTestMode, long maxLogSizeBytes) {
    this(singleStepTestMode, maxLogSizeBytes, ProgressSink.NONE, Sleeper.REAL);
  }

  public static ExecutionOptions defaults() {
    return new ExecutionOptions(false, 5L * 1024 * 1024, ProgressSink.NONE, Sleeper.REAL);
  }

  public ExecutionOptions withSingleStepTestMode(boolean value) {
    return new ExecutionOptions(value, maxLogSizeBytes, progress, sleeper);
  }

  public ExecutionOptions withMaxLogSizeBytes(long value) {
    return new ExecutionOptions(singleStepTestMode, value, progress, sleeper);
  }

  public ExecutionOptions withProgress(ProgressSink value) {
    return new ExecutionOptions(singleStepTestMode, maxLogSizeBytes, value, sleeper);
  }

  public ExecutionOptions withSleeper(Sleeper value) {
    return new ExecutionOptions(singleStepTestMode, maxLogSizeBytes, progress, value);
  }

  public long maxLogSizeMb() {
    return maxLogSizeBytes / (1024 * 1024);
  }
}
