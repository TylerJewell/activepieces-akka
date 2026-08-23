package io.akka.activepieces.domain;

import java.util.List;

/**
 * A node in the flow's action tree. SPEC-001 §1: code/piece steps, loops and routers, chained
 * through {@code nextAction} the way the source's `action.nextAction` chain works.
 */
public sealed interface Action permits Action.CodeAction, Action.LoopAction, Action.RouterAction {

  String name();

  String displayName();

  boolean skip();

  Action nextAction();

  record CodeAction(
      String name,
      String displayName,
      boolean skip,
      String type,
      StepHandler handler,
      boolean continueOnFailure,
      boolean retryOnFailure,
      int maxAttempts,
      int retryExponential,
      long retryIntervalMs,
      Action onSuccessAction,
      Action onFailureAction,
      Action nextAction)
      implements Action {

    public CodeAction {
      if (maxAttempts <= 0) maxAttempts = 4;
      if (retryExponential <= 0) retryExponential = 2;
      if (retryIntervalMs <= 0) retryIntervalMs = 2000;
    }
  }

  record LoopAction(
      String name,
      String displayName,
      boolean skip,
      Object items,
      Action firstLoopAction,
      Action nextAction)
      implements Action {}

  enum ExecutionType {
    EXECUTE_FIRST_MATCH,
    EXECUTE_ALL_MATCH
  }

  record RouterBranch(
      String name, boolean fallback, List<List<Condition>> conditionGroups, Action firstAction) {}

  record RouterAction(
      String name,
      String displayName,
      boolean skip,
      ExecutionType executionType,
      List<RouterBranch> branches,
      Action nextAction)
      implements Action {}
}
