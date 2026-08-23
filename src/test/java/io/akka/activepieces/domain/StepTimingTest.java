package io.akka.activepieces.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * SPEC-001 §2 — every journal entry carries how long its step took. The surface prints it beside
 * the step, so an entry that always reads zero is indistinguishable from one nobody measured.
 */
class StepTimingTest {

  private static final ExecutionOptions NO_WAITING =
      ExecutionOptions.defaults().withSleeper(millis -> {});

  /** Busy for at least {@code millis}, so the measurement has something to find. */
  private static StepHandler takingAtLeast(long millis) {
    return input -> {
      long until = System.nanoTime() + millis * 1_000_000;
      while (System.nanoTime() < until) {
        Thread.onSpinWait();
      }
      return "done";
    };
  }

  @Test
  void aCodeStepRecordsHowLongItsBodyTook() {
    Action.CodeAction slow = new Action.CodeAction(
        "slow", "Slow", false, "CODE", takingAtLeast(20), false, false, 4, 2, 2000, null, null, null);

    RunState state = FlowEngine.runChain(slow, RunState.empty(), StepPath.EMPTY, NO_WAITING);

    StepOutput.LeafOutput out = (StepOutput.LeafOutput) state.steps().get("slow");
    assertTrue(out.durationMs() >= 20, "recorded " + out.durationMs() + " ms for a 20 ms body");
  }

  @Test
  void aFailedStepRecordsItsDurationToo() {
    Action.CodeAction failing = new Action.CodeAction(
        "failing", "Failing", false, "CODE",
        input -> {
          long until = System.nanoTime() + 20_000_000;
          while (System.nanoTime() < until) {
            Thread.onSpinWait();
          }
          throw new RuntimeException("no");
        },
        false, false, 4, 2, 2000, null, null, null);

    RunState state = FlowEngine.runChain(failing, RunState.empty(), StepPath.EMPTY, NO_WAITING);

    StepOutput.LeafOutput out = (StepOutput.LeafOutput) state.steps().get("failing");
    assertEquals(StepStatus.FAILED, out.status());
    assertTrue(out.durationMs() >= 20, "recorded " + out.durationMs() + " ms for a 20 ms body");
  }

  @Test
  void aRetriedStepRecordsTheAttemptThatProducedTheEntryNotTheWaiting() {
    Action.CodeAction flaky = new Action.CodeAction(
        "flaky", "Flaky", false, "CODE",
        input -> {
          throw new RuntimeException("always");
        },
        false, true, 4, 2, 2000, null, null, null);

    RunState state = FlowEngine.runChain(flaky, RunState.empty(), StepPath.EMPTY, NO_WAITING);

    StepOutput.LeafOutput out = (StepOutput.LeafOutput) state.steps().get("flaky");
    assertTrue(out.durationMs() < 1000, "recorded " + out.durationMs() + " ms, which is the waiting");
  }

  @Test
  void aLoopRecordsTheWholeStepIncludingItsIterations() {
    Action.CodeAction body = new Action.CodeAction(
        "body", "Body", false, "CODE", takingAtLeast(10), false, false, 4, 2, 2000, null, null, null);
    Action.LoopAction loop =
        new Action.LoopAction("loop", "Loop", false, List.of("a", "b", "c"), body, null);

    RunState state = FlowEngine.runChain(loop, RunState.empty(), StepPath.EMPTY, NO_WAITING);

    StepOutput.LoopOutput out = (StepOutput.LoopOutput) state.steps().get("loop");
    assertEquals(3, out.iterations().size());
    assertTrue(out.durationMs() >= 30, "recorded " + out.durationMs() + " ms for three 10 ms bodies");
    for (Map<String, StepOutput> iteration : out.iterations()) {
      StepOutput.LeafOutput inner = (StepOutput.LeafOutput) iteration.get("body");
      assertTrue(inner.durationMs() >= 10, "iteration recorded " + inner.durationMs() + " ms");
    }
  }

  @Test
  void aRouterRecordsTheWholeStepIncludingTheBranchItRan() {
    Action.CodeAction branchBody = new Action.CodeAction(
        "branchBody", "Branch body", false, "CODE", takingAtLeast(20), false, false, 4, 2, 2000, null, null, null);
    Action.RouterBranch taken = new Action.RouterBranch(
        "taken", false, List.of(List.of(Condition.of("BOOLEAN_IS_TRUE", true, null))), branchBody);
    Action.RouterAction router = new Action.RouterAction(
        "router", "Router", false, Action.ExecutionType.EXECUTE_FIRST_MATCH, List.of(taken), null);

    RunState state = FlowEngine.runChain(router, RunState.empty(), StepPath.EMPTY, NO_WAITING);

    StepOutput.RouterOutput out = (StepOutput.RouterOutput) state.steps().get("router");
    assertTrue(out.durationMs() >= 20, "recorded " + out.durationMs() + " ms for a 20 ms branch");
  }
}
