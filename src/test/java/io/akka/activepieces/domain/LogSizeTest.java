package io.akka.activepieces.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LogSizeTest {

  @Test
  void anOversizedJournalReplacesTheStepAndSetsTheVerdict() {
    Action.CodeAction a = new Action.CodeAction(
        "a", "A", false, "CODE", Handlers.echo("x".repeat(1000)), false, false, 4, 2, 2000, null, null, null);
    ExecutionOptions tinyLimit = new ExecutionOptions(false, 100);

    RunState result = FlowEngine.runChain(a, RunState.empty(), StepPath.EMPTY, tinyLimit);

    assertTrue(result.verdict() instanceof Verdict.LogSizeExceeded);
    StepOutput.LeafOutput out = (StepOutput.LeafOutput) result.steps().get("a");
    assertEquals(StepStatus.FAILED, out.status());
    assertEquals("Flow run data size exceeded the maximum allowed size of 0 MB", out.errorMessage());
  }
}
