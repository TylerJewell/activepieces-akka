package io.akka.activepieces.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import java.util.Map;

/**
 * SPEC-001 §2 — one journal entry. A closed set of three shapes.
 *
 * <p>The names are carried in the serialized form because a loop's iterations hold step maps of
 * this same interface, so a stored journal is a tree whose branches are only distinguishable by
 * their tag.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "shape")
@JsonSubTypes({
  @JsonSubTypes.Type(value = StepOutput.LeafOutput.class, name = "leaf"),
  @JsonSubTypes.Type(value = StepOutput.LoopOutput.class, name = "loop"),
  @JsonSubTypes.Type(value = StepOutput.RouterOutput.class, name = "router")
})
public sealed interface StepOutput permits StepOutput.LeafOutput, StepOutput.LoopOutput, StepOutput.RouterOutput {

  StepStatus status();

  StepOutput withStatus(StepStatus status);

  record LeafOutput(
      String type,
      StepStatus status,
      Object input,
      Object output,
      String errorMessage,
      long durationMs)
      implements StepOutput {
    @Override
    public StepOutput withStatus(StepStatus newStatus) {
      return new LeafOutput(type, newStatus, input, output, errorMessage, durationMs);
    }
  }

  record LoopOutput(
      StepStatus status,
      Object input,
      Object item,
      int index,
      List<Map<String, StepOutput>> iterations,
      String errorMessage,
      long durationMs)
      implements StepOutput {
    @Override
    public StepOutput withStatus(StepStatus newStatus) {
      return new LoopOutput(newStatus, input, item, index, iterations, errorMessage, durationMs);
    }
  }

  record Branch(String name, int branchIndex, boolean evaluation) {}

  record RouterOutput(
      StepStatus status, Object input, List<Branch> branches, String errorMessage, long durationMs)
      implements StepOutput {
    @Override
    public StepOutput withStatus(StepStatus newStatus) {
      return new RouterOutput(newStatus, input, branches, errorMessage, durationMs);
    }
  }
}
