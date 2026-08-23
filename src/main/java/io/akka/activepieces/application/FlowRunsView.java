package io.akka.activepieces.application;

import akka.javasdk.annotations.Component;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;
import java.util.List;

/** Every run this service has started, newest first, for the list the surface opens on. */
@Component(id = "flow-runs-by-start")
public class FlowRunsView extends View {

  /**
   * @param failedStepName empty rather than absent when the run did not fail — a view row with a
   *     null field is not queryable
   */
  public record Entry(
      String runId,
      String scenario,
      String status,
      long startedAtMillis,
      long finishedAtMillis,
      int stepsCount,
      String failedStepName,
      String failedStepDisplayName,
      String failedStepMessage) {}

  public record Entries(List<Entry> items) {}

  @Consume.FromKeyValueEntity(FlowRunEntity.class)
  public static class Runs extends TableUpdater<Entry> {

    public Effect<Entry> onUpdate(FlowRunEntity.State state) {
      if (!state.started()) {
        return effects().ignore();
      }
      return effects()
          .updateRow(
              new Entry(
                  updateContext().eventSubject().orElseThrow(),
                  state.scenario() == null ? "" : state.scenario(),
                  state.status(),
                  state.startedAtMillis(),
                  state.finishedAtMillis(),
                  state.stepsCount(),
                  state.failedStep() == null ? "" : state.failedStep().name(),
                  state.failedStep() == null ? "" : state.failedStep().displayName(),
                  state.failedStep() == null ? "" : state.failedStep().message()));
    }
  }

  @Query("SELECT * AS items FROM runs ORDER BY startedAtMillis DESC LIMIT 50")
  public QueryEffect<Entries> newestFirst() {
    return queryResult();
  }
}
