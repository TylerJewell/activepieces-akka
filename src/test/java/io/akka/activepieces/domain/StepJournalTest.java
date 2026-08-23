package io.akka.activepieces.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StepJournalTest {

  private static StepOutput.LeafOutput leaf(Object output) {
    return new StepOutput.LeafOutput("CODE", StepStatus.SUCCEEDED, null, output, null, 0);
  }

  @Test
  void upsertAtAPathGrowsTheIterationList() {
    Map<String, StepOutput> journal = new LinkedHashMap<>();
    StepOutput.LoopOutput loop = new StepOutput.LoopOutput(StepStatus.RUNNING, List.of("a", "b"), "b", 2, List.of(new LinkedHashMap<>()), null, 0);
    journal.put("myLoop", loop);

    StepPath path = StepPath.EMPTY.extend("myLoop", 1);
    Map<String, StepOutput> updated = StepJournal.upsert(journal, path, "inner", leaf("x"));

    StepOutput.LoopOutput updatedLoop = (StepOutput.LoopOutput) updated.get("myLoop");
    assertEquals(2, updatedLoop.iterations().size()); // grown to hold index 1
    assertEquals(leaf("x"), updatedLoop.iterations().get(1).get("inner"));
    assertTrue(updatedLoop.iterations().get(0).isEmpty());
  }

  @Test
  void readResolvesAlongTheCurrentPath() {
    Map<String, StepOutput> journal = new LinkedHashMap<>();
    Map<String, StepOutput> iter0 = new LinkedHashMap<>();
    iter0.put("inner", leaf("first"));
    StepOutput.LoopOutput loop = new StepOutput.LoopOutput(StepStatus.RUNNING, List.of("a"), "a", 1, List.of(iter0), null, 0);
    journal.put("myLoop", loop);

    Map<String, StepOutput> atTop = StepJournal.read(journal, StepPath.EMPTY);
    assertEquals(journal, atTop);

    Map<String, StepOutput> atPath = StepJournal.read(journal, StepPath.EMPTY.extend("myLoop", 0));
    assertEquals(1, atPath.size());
    assertEquals(leaf("first"), atPath.get("inner"));
  }
}
