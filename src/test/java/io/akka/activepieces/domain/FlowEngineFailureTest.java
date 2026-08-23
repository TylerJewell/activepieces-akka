package io.akka.activepieces.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FlowEngineFailureTest {

  @Test
  void aThrowingStepIsRecordedFailedAndNamesTheVerdict() {
    Action.CodeAction a = new Action.CodeAction(
        "a", "A", false, "CODE", Handlers.throwing("kaboom"), false, false, 4, 2, 2000, null, null, null);

    RunState result = FlowEngine.runChain(a, RunState.empty(), StepPath.EMPTY, ExecutionOptions.defaults());

    StepOutput.LeafOutput out = (StepOutput.LeafOutput) result.steps().get("a");
    assertEquals(StepStatus.FAILED, out.status());
    assertEquals("kaboom", out.errorMessage());
    assertTrue(result.verdict() instanceof Verdict.Failed);
    assertEquals("a", ((Verdict.Failed) result.verdict()).failedStep().name());
  }

  @Test
  void continueOnFailureResetsTheVerdictAndLeavesTheStepFailed() {
    Action.CodeAction a = new Action.CodeAction(
        "a", "A", false, "CODE", Handlers.throwing("kaboom"), true, false, 4, 2, 2000, null, null, null);

    RunState result = FlowEngine.runChain(a, RunState.empty(), StepPath.EMPTY, ExecutionOptions.defaults());

    StepOutput.LeafOutput out = (StepOutput.LeafOutput) result.steps().get("a");
    assertEquals(StepStatus.FAILED, out.status());
    assertEquals("kaboom", out.errorMessage());
    assertTrue(result.verdict().isRunning());
  }

  @Test
  void continueOnFailureRunsTheBranchMatchingWhatTheStepDid() {
    Action onFailure = new Action.CodeAction(
        "recover", "Recover", false, "CODE", Handlers.echo("recovered"), false, false, 4, 2, 2000, null, null, null);
    Action.CodeAction failing = new Action.CodeAction(
        "a", "A", false, "CODE", Handlers.throwing("kaboom"), true, false, 4, 2, 2000, null, onFailure, null);

    RunState failedResult = FlowEngine.runChain(failing, RunState.empty(), StepPath.EMPTY, ExecutionOptions.defaults());
    assertEquals("recovered", ((StepOutput.LeafOutput) failedResult.steps().get("recover")).output());
    assertTrue(failedResult.verdict().isRunning());

    Action onSuccess = new Action.CodeAction(
        "onOk", "OnOk", false, "CODE", Handlers.echo("ok-branch"), false, false, 4, 2, 2000, null, null, null);
    Action.CodeAction succeeding = new Action.CodeAction(
        "b", "B", false, "CODE", Handlers.echo("done"), true, false, 4, 2, 2000, onSuccess, null, null);

    RunState succeededResult = FlowEngine.runChain(succeeding, RunState.empty(), StepPath.EMPTY, ExecutionOptions.defaults());
    assertEquals("ok-branch", ((StepOutput.LeafOutput) succeededResult.steps().get("onOk")).output());
  }
}
