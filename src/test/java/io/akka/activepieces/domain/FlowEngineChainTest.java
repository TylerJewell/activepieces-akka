package io.akka.activepieces.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FlowEngineChainTest {

  private static Action.CodeAction code(String name, boolean skip, StepHandler handler, Action next) {
    return new Action.CodeAction(name, name, skip, "CODE", handler, false, false, 4, 2, 2000, null, null, next);
  }

  @Test
  void walksTheChainAndRecordsEachStep() {
    Action c = code("c", false, Handlers.echo("C"), null);
    Action b = code("b", false, Handlers.echo("B"), c);
    Action a = code("a", false, Handlers.echo("A"), b);

    RunState result = FlowEngine.runChain(a, RunState.empty(), StepPath.EMPTY, ExecutionOptions.defaults());

    assertEquals(3, result.steps().size());
    assertEquals("A", ((StepOutput.LeafOutput) result.steps().get("a")).output());
    assertEquals("B", ((StepOutput.LeafOutput) result.steps().get("b")).output());
    assertEquals("C", ((StepOutput.LeafOutput) result.steps().get("c")).output());
    assertTrue(result.verdict().isRunning());
  }

  @Test
  void stopsAtTheFirstFailureAndRecordsNoLaterStep() {
    Action c = code("c", false, Handlers.echo("C"), null);
    Action b = code("b", false, Handlers.throwing("boom"), c);
    Action a = code("a", false, Handlers.echo("A"), b);

    RunState result = FlowEngine.runChain(a, RunState.empty(), StepPath.EMPTY, ExecutionOptions.defaults());

    assertEquals(2, result.steps().size());
    assertFalse(result.steps().containsKey("c"));
    assertTrue(result.verdict() instanceof Verdict.Failed);
    assertEquals("b", ((Verdict.Failed) result.verdict()).failedStep().name());
  }

  @Test
  void aSkippedStepIsAbsentFromTheJournalAndItsSuccessorRuns() {
    Action c = code("c", false, Handlers.echo("C"), null);
    Action b = code("b", true, Handlers.throwing("never runs"), c);
    Action a = code("a", false, Handlers.echo("A"), b);

    RunState result = FlowEngine.runChain(a, RunState.empty(), StepPath.EMPTY, ExecutionOptions.defaults());

    assertFalse(result.steps().containsKey("b"));
    assertEquals("C", ((StepOutput.LeafOutput) result.steps().get("c")).output());
    assertTrue(result.verdict().isRunning());
  }

  @Test
  void singleStepTestModeIgnoresSkip() {
    Action a = code("a", true, Handlers.echo("A"), null);
    ExecutionOptions opts = ExecutionOptions.defaults().withSingleStepTestMode(true);

    RunState result = FlowEngine.runChain(a, RunState.empty(), StepPath.EMPTY, opts);

    assertEquals("A", ((StepOutput.LeafOutput) result.steps().get("a")).output());
  }

  @Test
  void singleStepTestModeStopsAfterOneStep() {
    Action b = code("b", false, Handlers.echo("B"), null);
    Action a = code("a", false, Handlers.echo("A"), b);
    ExecutionOptions opts = ExecutionOptions.defaults().withSingleStepTestMode(true);

    RunState result = FlowEngine.runChain(a, RunState.empty(), StepPath.EMPTY, opts);

    assertEquals(1, result.steps().size());
    assertFalse(result.steps().containsKey("b"));
  }

  @Test
  void aStepAlreadyInTheJournalIsNotReRunUnlessItIsPaused() {
    Action a = code("a", false, Handlers.throwing("must not run"), null);

    Map<String, StepOutput> existing = new LinkedHashMap<>();
    existing.put("a", new StepOutput.LeafOutput("CODE", StepStatus.SUCCEEDED, null, "already there", null, 0));
    RunState state = RunState.empty().withSteps(existing);

    RunState result = FlowEngine.runChain(a, state, StepPath.EMPTY, ExecutionOptions.defaults());
    assertEquals("already there", ((StepOutput.LeafOutput) result.steps().get("a")).output());
    assertTrue(result.verdict().isRunning());

    Map<String, StepOutput> paused = new LinkedHashMap<>();
    paused.put("a", new StepOutput.LeafOutput("CODE", StepStatus.PAUSED, null, null, null, 0));
    RunState pausedState = RunState.empty().withSteps(paused);

    RunState reran = FlowEngine.runChain(a, pausedState, StepPath.EMPTY, ExecutionOptions.defaults());
    assertTrue(reran.verdict() instanceof Verdict.Failed);
    assertNull(((StepOutput.LeafOutput) reran.steps().get("a")).output());
  }
}
