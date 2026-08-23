package io.akka.activepieces.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SPEC-001 rules 26, 27 — path-addressed upsert and read over a run's step journal.
 *
 * <p>The top-level map is the journal itself. A non-empty path descends through the named loop's
 * iteration list, growing it with empty maps as needed.
 */
public final class StepJournal {

  private StepJournal() {}

  /** Upsert {@code output} under {@code name} at {@code path}, returning a new top-level map. */
  public static Map<String, StepOutput> upsert(
      Map<String, StepOutput> journal, StepPath path, String name, StepOutput output) {
    Map<String, StepOutput> copy = new LinkedHashMap<>(journal);
    if (path.isEmpty()) {
      copy.put(name, output);
      return copy;
    }
    StepPath.Segment head = path.segments().get(0);
    StepOutput existing = copy.get(head.loopName());
    if (!(existing instanceof StepOutput.LoopOutput loop)) {
      throw new IllegalStateException("no loop named " + head.loopName() + " at this level");
    }
    List<Map<String, StepOutput>> iterations = new ArrayList<>(loop.iterations());
    while (iterations.size() <= head.iteration()) {
      iterations.add(new LinkedHashMap<>());
    }
    Map<String, StepOutput> inner = iterations.get(head.iteration());
    Map<String, StepOutput> updatedInner =
        upsert(inner, new StepPath(path.segments().subList(1, path.segments().size())), name, output);
    iterations.set(head.iteration(), updatedInner);
    copy.put(head.loopName(), new StepOutput.LoopOutput(
        loop.status(), loop.input(), loop.item(), loop.index(), iterations, loop.errorMessage(), loop.durationMs()));
    return copy;
  }

  /** Resolve {@code path} and return only the map that lives there. */
  public static Map<String, StepOutput> read(Map<String, StepOutput> journal, StepPath path) {
    if (path.isEmpty()) {
      return journal;
    }
    StepPath.Segment head = path.segments().get(0);
    StepOutput existing = journal.get(head.loopName());
    if (!(existing instanceof StepOutput.LoopOutput loop)) {
      return Map.of();
    }
    if (head.iteration() >= loop.iterations().size()) {
      return Map.of();
    }
    Map<String, StepOutput> inner = loop.iterations().get(head.iteration());
    return read(inner, new StepPath(path.segments().subList(1, path.segments().size())));
  }
}
