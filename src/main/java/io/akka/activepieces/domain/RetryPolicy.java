package io.akka.activepieces.domain;

/** SPEC-001 rules 9-13 — attempt gating and backoff. */
public final class RetryPolicy {

  private RetryPolicy() {}

  /**
   * Rule 9, 10 — attempt again only when the verdict is exactly FAILED, attempts remain,
   * retryOnFailure is set, and single-step-test mode is off.
   */
  public static boolean shouldRetry(
      StepStatus status, int attemptsMade, int maxAttempts, boolean retryOnFailure, boolean singleStepTestMode) {
    return status == StepStatus.FAILED
        && attemptsMade < maxAttempts
        && retryOnFailure
        && !singleStepTestMode;
  }

  /** Rule 13 — delay before attempt n+1, n being the number of attempts already made. */
  public static long backoffMillis(int attemptsMade, int retryExponential, long retryIntervalMs) {
    return (long) Math.pow(retryExponential, attemptsMade) * retryIntervalMs;
  }
}
