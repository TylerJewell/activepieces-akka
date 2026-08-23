package io.akka.activepieces.api;

import io.akka.activepieces.application.FlowRunEntity;
import io.akka.activepieces.domain.StepOutput;
import java.util.Map;

/**
 * What {@link FlowRunEndpoint} answers with: a run's verdict, the step that failed if one did,
 * and the journal as the engine holds it.
 *
 * <p>Named here rather than reusing the entity's own record so the endpoint's contract is owned by
 * the layer that publishes it. The journal itself is the domain's {@link StepOutput} tree on
 * purpose: this route exists for a caller that wants the journal, and flattening it would be the
 * one thing the route is for.
 */
public record RunView(
    String runId,
    String scenario,
    String status,
    FailedStepView failedStep,
    Map<String, StepOutput> steps,
    int stepsCount,
    long startedAtMillis,
    long finishedAtMillis) {

  public record FailedStepView(String name, String displayName, String message) {}

  static RunView of(String runId, FlowRunEntity.State state) {
    return new RunView(
        runId,
        state.scenario(),
        state.status(),
        state.failedStep() == null
            ? null
            : new FailedStepView(
                state.failedStep().name(),
                state.failedStep().displayName(),
                state.failedStep().message()),
        state.steps(),
        state.stepsCount(),
        state.startedAtMillis(),
        state.finishedAtMillis());
  }
}
