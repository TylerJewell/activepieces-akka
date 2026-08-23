package io.akka.activepieces.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ResumeTest {

  private static StepOutput leaf(StepStatus status) {
    return new StepOutput.LeafOutput("CODE", status, null, "x", null, 0);
  }

  @Test
  void everyStatusAndReasonMatchesTheSourcesTable() {
    for (StepStatus status : StepStatus.values()) {
      Map<String, StepOutput> journal = new LinkedHashMap<>();
      journal.put("s", leaf(status));

      Map<String, StepOutput> nonWaitpoint = FlowEngine.filterForResume(journal, "OTHER");
      Map<String, StepOutput> waitpoint = FlowEngine.filterForResume(journal, "WAITPOINT");

      switch (status) {
        case SUCCEEDED, PAUSED -> {
          assertTrue(nonWaitpoint.containsKey("s"), status.toString());
          assertTrue(waitpoint.containsKey("s"), status.toString());
        }
        case FAILED -> {
          assertFalse(nonWaitpoint.containsKey("s"), status.toString());
          assertTrue(waitpoint.containsKey("s"), status.toString());
        }
        case RUNNING, STOPPED -> {
          assertFalse(nonWaitpoint.containsKey("s"), status.toString());
          assertFalse(waitpoint.containsKey("s"), status.toString());
        }
      }
    }
  }

  @Test
  void theFilterReachesInsideLoopIterations() {
    Map<String, StepOutput> iter0 = new LinkedHashMap<>();
    iter0.put("kept", leaf(StepStatus.SUCCEEDED));
    iter0.put("dropped", leaf(StepStatus.RUNNING));
    StepOutput.LoopOutput loop = new StepOutput.LoopOutput(
        StepStatus.SUCCEEDED, List.of("a"), "a", 1, List.of(iter0), null, 0);
    Map<String, StepOutput> journal = new LinkedHashMap<>();
    journal.put("loop", loop);

    Map<String, StepOutput> filtered = FlowEngine.filterForResume(journal, "OTHER");

    StepOutput.LoopOutput filteredLoop = (StepOutput.LoopOutput) filtered.get("loop");
    assertEquals("a", filteredLoop.item()); // item/index preserved
    assertEquals(1, filteredLoop.index());
    assertTrue(filteredLoop.iterations().get(0).containsKey("kept"));
    assertFalse(filteredLoop.iterations().get(0).containsKey("dropped"));
  }
}
