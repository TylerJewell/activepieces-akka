package io.akka.activepieces.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FlowEngineLoopTest {

  @Test
  void aNonListFailsTheLoopWithTheSourcesMessage() {
    Action.LoopAction loop = new Action.LoopAction("loop", "Loop", false, "not-a-list", null, null);

    RunState result = FlowEngine.runChain(loop, RunState.empty(), StepPath.EMPTY, ExecutionOptions.defaults());

    StepOutput.LoopOutput out = (StepOutput.LoopOutput) result.steps().get("loop");
    assertEquals(StepStatus.FAILED, out.status());
    assertEquals("The items you have selected must be a list.", out.errorMessage());
    assertTrue(result.verdict() instanceof Verdict.Failed);
  }

  @Test
  void anEmptyListSucceedsWithNoIterations() {
    Action next = new Action.CodeAction("after", "After", false, "CODE", Handlers.echo("ran"), false, false, 4, 2, 2000, null, null, null);
    Action.LoopAction loop = new Action.LoopAction("loop", "Loop", false, List.of(), null, next);

    RunState result = FlowEngine.runChain(loop, RunState.empty(), StepPath.EMPTY, ExecutionOptions.defaults());

    StepOutput.LoopOutput out = (StepOutput.LoopOutput) result.steps().get("loop");
    assertEquals(StepStatus.SUCCEEDED, out.status());
    assertEquals(0, out.index());
    assertTrue(out.iterations().isEmpty());
    assertEquals("ran", ((StepOutput.LeafOutput) result.steps().get("after")).output());
  }

  @Test
  void indexIsOneBasedAndItemTracksTheCurrentItem() {
    Action.CodeAction body = new Action.CodeAction("body", "Body", false, "CODE", Handlers.echo("x"), false, false, 4, 2, 2000, null, null, null);
    Action.LoopAction loop = new Action.LoopAction("loop", "Loop", false, List.of("a", "b", "c"), body, null);

    RunState result = FlowEngine.runChain(loop, RunState.empty(), StepPath.EMPTY, ExecutionOptions.defaults());

    StepOutput.LoopOutput out = (StepOutput.LoopOutput) result.steps().get("loop");
    assertEquals(StepStatus.SUCCEEDED, out.status());
    assertEquals(3, out.index());
    assertEquals("c", out.item());
    assertEquals(3, out.iterations().size());
    for (var iteration : out.iterations()) {
      assertEquals("x", ((StepOutput.LeafOutput) iteration.get("body")).output());
    }
  }

  @Test
  void aFailureInsideALoopLeavesTheLoopStepSucceeded() {
    Action.CodeAction body = new Action.CodeAction(
        "body", "Body", false, "CODE", Handlers.throwing("boom"), false, false, 4, 2, 2000, null, null, null);
    Action.LoopAction loop = new Action.LoopAction("loop", "Loop", false, List.of("a", "b"), body, null);

    RunState result = FlowEngine.runChain(loop, RunState.empty(), StepPath.EMPTY, ExecutionOptions.defaults());

    StepOutput.LoopOutput out = (StepOutput.LoopOutput) result.steps().get("loop");
    assertEquals(StepStatus.SUCCEEDED, out.status()); // the loop itself, despite its body failing
    assertEquals(1, out.iterations().size()); // stopped after the first iteration begun
    assertTrue(result.verdict() instanceof Verdict.Failed);
  }

  @Test
  void nestedLoopsNestTheJournalAndAPathAddressesTheLeaf() {
    Action.CodeAction innerBody = new Action.CodeAction("innerBody", "Inner", false, "CODE", Handlers.echo("deep"), false, false, 4, 2, 2000, null, null, null);
    Action.LoopAction innerLoop = new Action.LoopAction("innerLoop", "Inner Loop", false, List.of("x"), innerBody, null);
    Action.LoopAction outerLoop = new Action.LoopAction("outerLoop", "Outer Loop", false, List.of("a"), innerLoop, null);

    RunState result = FlowEngine.runChain(outerLoop, RunState.empty(), StepPath.EMPTY, ExecutionOptions.defaults());

    StepPath deepPath = StepPath.EMPTY.extend("outerLoop", 0).extend("innerLoop", 0);
    Map<String, StepOutput> deep = StepJournal.read(result.steps(), deepPath);
    assertEquals("deep", ((StepOutput.LeafOutput) deep.get("innerBody")).output());
  }
}
