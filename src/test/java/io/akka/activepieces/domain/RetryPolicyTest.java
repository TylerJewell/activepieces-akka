package io.akka.activepieces.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {

  /**
   * For the cases about which attempts happen rather than how long the gaps are. Rule 13's real
   * gaps total twenty-eight seconds, and a test that spends them to check something else is a
   * test that gets skipped.
   */
  private static final ExecutionOptions NO_WAITING =
      ExecutionOptions.defaults().withSleeper(millis -> {});

  @Test
  void attemptsExactlyMaxAttemptsWhenRetryIsOn() {
    AtomicInteger calls = new AtomicInteger();
    Action.CodeAction a = new Action.CodeAction(
        "a", "A", false, "CODE",
        input -> {
          calls.incrementAndGet();
          throw new RuntimeException("always fails");
        },
        false, true, 4, 2, 2000, null, null, null);

    FlowEngine.runChain(a, RunState.empty(), StepPath.EMPTY, NO_WAITING);

    assertEquals(4, calls.get());
  }

  @Test
  void onlyFailedRetriesAcrossEveryVerdict() {
    for (StepStatus status : StepStatus.values()) {
      boolean expected = status == StepStatus.FAILED;
      assertEquals(expected, RetryPolicy.shouldRetry(status, 0, 4, true, false), status.toString());
    }
  }

  @Test
  void retryIsGatedByTheStepFlagAndBySingleStepTestMode() {
    assertFalse(RetryPolicy.shouldRetry(StepStatus.FAILED, 0, 4, false, false));
    assertFalse(RetryPolicy.shouldRetry(StepStatus.FAILED, 0, 4, true, true));
    assertTrue(RetryPolicy.shouldRetry(StepStatus.FAILED, 0, 4, true, false));
    assertFalse(RetryPolicy.shouldRetry(StepStatus.FAILED, 4, 4, true, false));
  }

  @Test
  void everyAttemptSeesTheStateFromBeforeTheFirstAttempt() {
    AtomicInteger attempt = new AtomicInteger();
    Action.CodeAction sibling = new Action.CodeAction(
        "sibling", "Sibling", false, "CODE", Handlers.echo("first"), false, false, 4, 2, 2000, null, null, null);
    Action.CodeAction flaky = new Action.CodeAction(
        "flaky", "Flaky", false, "CODE",
        input -> {
          if (attempt.incrementAndGet() < 2) throw new RuntimeException("not yet");
          return "eventually";
        },
        false, true, 4, 2, 2000, null, null, null);

    RunState afterSibling = FlowEngine.runChain(sibling, RunState.empty(), StepPath.EMPTY, NO_WAITING);
    RunState result = FlowEngine.runChain(flaky, afterSibling, StepPath.EMPTY, NO_WAITING);

    // "flaky"'s own failed first attempt never lingers under a different name; only the final
    // succeeding attempt's output is in the journal, alongside the untouched sibling entry.
    assertEquals("eventually", ((StepOutput.LeafOutput) result.steps().get("flaky")).output());
    assertEquals("first", ((StepOutput.LeafOutput) result.steps().get("sibling")).output());
    assertEquals(2, result.steps().size());
  }

  @Test
  void backoffDelaysDoubleFromTheConfiguredInterval() {
    // n attempts already made -> retryExponential^n * retryInterval: 4s, 8s, 16s.
    assertEquals(4000, RetryPolicy.backoffMillis(1, 2, 2000));
    assertEquals(8000, RetryPolicy.backoffMillis(2, 2, 2000));
    assertEquals(16000, RetryPolicy.backoffMillis(3, 2, 2000));
  }

  @Test
  void theEngineWaitsThoseDelaysBetweenAttempts() {
    List<Long> waited = new ArrayList<>();
    Action.CodeAction a = new Action.CodeAction(
        "a", "A", false, "CODE",
        input -> {
          throw new RuntimeException("always fails");
        },
        false, true, 4, 2, 2000, null, null, null);

    FlowEngine.runChain(
        a, RunState.empty(), StepPath.EMPTY, ExecutionOptions.defaults().withSleeper(waited::add));

    // Four attempts, so three waits, and no wait after the last one.
    assertEquals(List.of(4000L, 8000L, 16000L), waited);
  }

  @Test
  void aStepThatSucceedsWaitsForNothing() {
    List<Long> waited = new ArrayList<>();
    Action.CodeAction a = new Action.CodeAction(
        "a", "A", false, "CODE", Handlers.echo("ok"), false, true, 4, 2, 2000, null, null, null);

    FlowEngine.runChain(
        a, RunState.empty(), StepPath.EMPTY, ExecutionOptions.defaults().withSleeper(waited::add));

    assertEquals(List.of(), waited);
  }
}
