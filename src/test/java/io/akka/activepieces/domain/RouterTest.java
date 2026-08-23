package io.akka.activepieces.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RouterTest {

  private static Condition trueCondition() {
    return Condition.of("BOOLEAN_IS_TRUE", true, null);
  }

  private static Condition falseCondition() {
    return Condition.of("BOOLEAN_IS_FALSE", true, null);
  }

  @Test
  void everyBranchIsEvaluatedBeforeAnyRuns() {
    Action.CodeAction runA = new Action.CodeAction("a", "A", false, "CODE", Handlers.echo("ranA"), false, false, 4, 2, 2000, null, null, null);
    Action.CodeAction runB = new Action.CodeAction("b", "B", false, "CODE", Handlers.echo("ranB"), false, false, 4, 2, 2000, null, null, null);
    Action.RouterBranch branchA = new Action.RouterBranch("branchA", false, List.of(List.of(trueCondition())), runA);
    Action.RouterBranch branchB = new Action.RouterBranch("branchB", false, List.of(List.of(trueCondition())), runB);
    Action.RouterAction router = new Action.RouterAction("router", "Router", false, Action.ExecutionType.EXECUTE_ALL_MATCH, List.of(branchA, branchB), null);

    RunState result = FlowEngine.runChain(router, RunState.empty(), StepPath.EMPTY, ExecutionOptions.defaults());

    StepOutput.RouterOutput out = (StepOutput.RouterOutput) result.steps().get("router");
    assertEquals(2, out.branches().size());
    assertTrue(out.branches().get(0).evaluation());
    assertTrue(out.branches().get(1).evaluation());
    assertEquals(1, out.branches().get(0).branchIndex());
    assertEquals(2, out.branches().get(1).branchIndex());
    assertEquals("ranA", ((StepOutput.LeafOutput) result.steps().get("a")).output());
    assertEquals("ranB", ((StepOutput.LeafOutput) result.steps().get("b")).output());
  }

  @Test
  void aFallbackIsTrueOnlyWhenEveryOtherBranchIsFalse() {
    Action.RouterBranch matching = new Action.RouterBranch("matching", false, List.of(List.of(trueCondition())), null);
    Action.RouterBranch fallback = new Action.RouterBranch("fallback", true, List.of(), null);
    Action.RouterAction router = new Action.RouterAction("router", "Router", false, Action.ExecutionType.EXECUTE_ALL_MATCH, List.of(matching, fallback), null);

    RunState result = FlowEngine.runChain(router, RunState.empty(), StepPath.EMPTY, ExecutionOptions.defaults());
    StepOutput.RouterOutput out = (StepOutput.RouterOutput) result.steps().get("router");
    assertTrue(out.branches().get(0).evaluation());
    assertFalse(out.branches().get(1).evaluation());

    Action.RouterBranch nonMatching = new Action.RouterBranch("nonMatching", false, List.of(List.of(falseCondition())), null);
    Action.RouterAction router2 = new Action.RouterAction("router2", "Router2", false, Action.ExecutionType.EXECUTE_ALL_MATCH, List.of(nonMatching, fallback), null);
    RunState result2 = FlowEngine.runChain(router2, RunState.empty(), StepPath.EMPTY, ExecutionOptions.defaults());
    StepOutput.RouterOutput out2 = (StepOutput.RouterOutput) result2.steps().get("router2");
    assertFalse(out2.branches().get(0).evaluation());
    assertTrue(out2.branches().get(1).evaluation());
  }

  @Test
  void twoFallbacksCancelEachOtherAndNothingRuns() {
    Action.RouterBranch fallback1 = new Action.RouterBranch("f1", true, List.of(), null);
    Action.RouterBranch fallback2 = new Action.RouterBranch("f2", true, List.of(), null);
    Action.RouterAction router = new Action.RouterAction("router", "Router", false, Action.ExecutionType.EXECUTE_ALL_MATCH, List.of(fallback1, fallback2), null);

    RunState result = FlowEngine.runChain(router, RunState.empty(), StepPath.EMPTY, ExecutionOptions.defaults());
    StepOutput.RouterOutput out = (StepOutput.RouterOutput) result.steps().get("router");
    assertFalse(out.branches().get(0).evaluation());
    assertFalse(out.branches().get(1).evaluation());
  }

  @Test
  void firstMatchRunsAtMostOneBranch() {
    Action.CodeAction runA = new Action.CodeAction("a", "A", false, "CODE", Handlers.echo("ranA"), false, false, 4, 2, 2000, null, null, null);
    Action.CodeAction runB = new Action.CodeAction("b", "B", false, "CODE", Handlers.echo("ranB"), false, false, 4, 2, 2000, null, null, null);
    Action.RouterBranch branchA = new Action.RouterBranch("branchA", false, List.of(List.of(trueCondition())), runA);
    Action.RouterBranch branchB = new Action.RouterBranch("branchB", false, List.of(List.of(trueCondition())), runB);
    Action.RouterAction router = new Action.RouterAction("router", "Router", false, Action.ExecutionType.EXECUTE_FIRST_MATCH, List.of(branchA, branchB), null);

    RunState result = FlowEngine.runChain(router, RunState.empty(), StepPath.EMPTY, ExecutionOptions.defaults());

    assertTrue(result.steps().containsKey("a"));
    assertFalse(result.steps().containsKey("b"));
  }
}
