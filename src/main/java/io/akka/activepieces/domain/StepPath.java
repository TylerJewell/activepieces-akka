package io.akka.activepieces.domain;

import java.util.ArrayList;
import java.util.List;

/** SPEC-001 §2 — a list of (loopName, iteration) pairs, 0-based. The empty path is the top level. */
public record StepPath(List<Segment> segments) {

  public record Segment(String loopName, int iteration) {}

  public static final StepPath EMPTY = new StepPath(List.of());

  public StepPath extend(String loopName, int iteration) {
    List<Segment> next = new ArrayList<>(segments);
    next.add(new Segment(loopName, iteration));
    return new StepPath(next);
  }

  public boolean isEmpty() {
    return segments.isEmpty();
  }
}
