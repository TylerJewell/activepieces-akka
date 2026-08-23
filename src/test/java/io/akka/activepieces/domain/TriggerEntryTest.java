package io.akka.activepieces.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TriggerEntryTest {

  @Test
  void theTriggerPayloadBecomesTheTriggerStepsOutput() {
    Action.CodeAction next = new Action.CodeAction(
        "step1", "Step 1", false, "CODE", Handlers.echo("ran"), false, false, 4, 2, 2000, null, null, null);
    TriggerStep trigger = new TriggerStep("trigger", "Trigger", "PIECE_TRIGGER", next);

    RunState result = FlowEngine.runFlow(trigger, "payload", ExecutionOptions.defaults());

    StepOutput.LeafOutput triggerOut = (StepOutput.LeafOutput) result.steps().get("trigger");
    assertEquals(StepStatus.SUCCEEDED, triggerOut.status());
    assertEquals("payload", triggerOut.output());
    assertEquals("ran", ((StepOutput.LeafOutput) result.steps().get("step1")).output());
    assertEquals(Verdict.SUCCEEDED, result.verdict());
  }

  @Test
  void aTriggerWithNoNextActionFinishesSucceeded() {
    TriggerStep trigger = new TriggerStep("trigger", "Trigger", "PIECE_TRIGGER", null);

    RunState result = FlowEngine.runFlow(trigger, "payload", ExecutionOptions.defaults());

    assertEquals(1, result.steps().size());
    assertEquals(Verdict.SUCCEEDED, result.verdict());
  }
}
