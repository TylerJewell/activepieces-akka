package io.akka.activepieces.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class RunStateTest {

  @Test
  void finishExecutionPromotesOnlyRunning() {
    RunState running = RunState.empty();
    assertEquals(Verdict.SUCCEEDED, running.finishExecution().verdict());

    FailedStep failedStep = new FailedStep("a", "A", "boom");
    RunState failed = RunState.empty().withVerdict(new Verdict.Failed(failedStep));
    assertEquals(failed.verdict(), failed.finishExecution().verdict());

    RunState paused = RunState.empty().withVerdict(Verdict.PAUSED);
    assertEquals(Verdict.PAUSED, paused.finishExecution().verdict());

    RunState logSizeExceeded = RunState.empty().withVerdict(new Verdict.LogSizeExceeded(failedStep));
    assertEquals(logSizeExceeded.verdict(), logSizeExceeded.finishExecution().verdict());

    assertEquals(List.of(), RunState.empty().tags());
  }
}
