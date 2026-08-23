package io.akka.activepieces.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SPEC-001 §3 — the executor: the chain walk, loops, routers, retry, the journal's size
 * accounting, the trigger entry and the resume filter. Ported from {@code flow-executor.ts},
 * {@code loop-executor.ts}, {@code router-executor.ts} and {@code base-executor.ts}.
 */
public final class FlowEngine {

  private FlowEngine() {}

  // ------------------------------------------------------------------ trigger and resume

  /** Rules 30, 31 — the trigger becomes a journal entry, then execution continues into it. */
  public static RunState runFlow(TriggerStep trigger, Object payload, ExecutionOptions opts) {
    return runFlow(trigger, payload, RunState.empty(), opts);
  }

  public static RunState runFlow(TriggerStep trigger, Object payload, RunState initialState, ExecutionOptions opts) {
    StepOutput.LeafOutput triggerOutput =
        new StepOutput.LeafOutput(trigger.type(), StepStatus.SUCCEEDED, null, payload, null, 0);
    RunState state = upsertAndCheckSize(initialState, StepPath.EMPTY, trigger.name(), triggerOutput, opts);
    if (state.verdict() instanceof Verdict.LogSizeExceeded) {
      return state;
    }
    if (trigger.nextAction() != null) {
      state = runChain(trigger.nextAction(), state, StepPath.EMPTY, opts);
    }
    return state.finishExecution();
  }

  /** Rules 32, 33 — which of a prior run's steps survive a resume, applied recursively into loops. */
  public static Map<String, StepOutput> filterForResume(Map<String, StepOutput> priorSteps, String resumeReason) {
    Map<String, StepOutput> out = new LinkedHashMap<>();
    for (Map.Entry<String, StepOutput> entry : priorSteps.entrySet()) {
      StepOutput so = entry.getValue();
      boolean keep =
          switch (so.status()) {
            case SUCCEEDED, PAUSED -> true;
            case FAILED -> "WAITPOINT".equals(resumeReason);
            case RUNNING, STOPPED -> false;
          };
      if (!keep) continue;
      if (so instanceof StepOutput.LoopOutput lo) {
        List<Map<String, StepOutput>> filteredIterations = new ArrayList<>();
        for (Map<String, StepOutput> iter : lo.iterations()) {
          filteredIterations.add(filterForResume(iter, resumeReason));
        }
        out.put(
            entry.getKey(),
            new StepOutput.LoopOutput(
                lo.status(), lo.input(), lo.item(), lo.index(), filteredIterations, lo.errorMessage(), lo.durationMs()));
      } else {
        out.put(entry.getKey(), so);
      }
    }
    return out;
  }

  // ------------------------------------------------------------------ the chain

  /** Rules 1-5, 28 — walk {@code action.nextAction} until none is left or a rule stops it. */
  public static RunState runChain(Action start, RunState state, StepPath path, ExecutionOptions opts) {
    Action current = start;
    while (current != null) {
      if (!state.verdict().isRunning()) break; // rule 3

      boolean forceExecuteDespiteSkip = opts.singleStepTestMode();
      if (current.skip() && !forceExecuteDespiteSkip) {
        current = current.nextAction();
        continue; // rule 2
      }

      Map<String, StepOutput> here = StepJournal.read(state.steps(), path);
      StepOutput existing = here.get(current.name());
      if (existing == null || existing.status() == StepStatus.PAUSED) {
        state = executeAction(current, state, path, opts);
      } // else rule 28: already present and not paused — stands, do not re-run

      if (opts.singleStepTestMode()) break; // rule 4
      if (!state.verdict().isRunning()) break; // rule 3
      current = current.nextAction();
    }
    return state;
  }

  private static RunState executeAction(Action action, RunState state, StepPath path, ExecutionOptions opts) {
    if (action instanceof Action.CodeAction code) return executeCode(code, state, path, opts);
    if (action instanceof Action.LoopAction loop) return executeLoop(loop, state, path, opts);
    if (action instanceof Action.RouterAction router) return executeRouter(router, state, path, opts);
    throw new IllegalStateException("unknown action type: " + action);
  }

  // ------------------------------------------------------------------ code/piece steps, retry

  private static RunState executeCode(Action.CodeAction code, RunState preAttemptState, StepPath path, ExecutionOptions opts) {
    int attempts = 0;
    while (true) {
      attempts++;
      StepOutput.LeafOutput result;
      // Timed around the body alone: the retry waits are the run's time, not the step's, and the
      // recorded figure is the attempt that produced the entry rather than every attempt summed.
      long startedAt = System.nanoTime();
      try {
        Object output = code.handler().run(null);
        result = new StepOutput.LeafOutput(code.type(), StepStatus.SUCCEEDED, null, output, null, elapsedMillis(startedAt));
      } catch (Exception e) {
        result = new StepOutput.LeafOutput(code.type(), StepStatus.FAILED, null, null, formatError(e), elapsedMillis(startedAt));
      }

      RunState candidate = upsertAndCheckSize(preAttemptState, path, code.name(), result, opts);
      if (candidate.verdict() instanceof Verdict.LogSizeExceeded) {
        return candidate; // rule 29 pre-empts everything below
      }

      if (result.status() == StepStatus.SUCCEEDED) {
        if (code.onSuccessAction() != null) {
          return runChain(code.onSuccessAction(), candidate, path, opts);
        }
        return candidate;
      }

      // failed
      if (RetryPolicy.shouldRetry(StepStatus.FAILED, attempts, code.maxAttempts(), code.retryOnFailure(), opts.singleStepTestMode())) {
        // rule 13, then rule 12: the wait comes before the attempt, and that attempt starts from
        // preAttemptState again, discarding whatever the failed one wrote.
        opts.sleeper().sleep(
            RetryPolicy.backoffMillis(attempts, code.retryExponential(), code.retryIntervalMs()));
        continue;
      }

      FailedStep failedStep = new FailedStep(code.name(), code.displayName(), result.errorMessage());
      RunState failedState = candidate.withVerdict(new Verdict.Failed(failedStep));
      if (code.continueOnFailure() && !opts.singleStepTestMode()) {
        RunState resumed = failedState.withVerdict(Verdict.RUNNING); // rule 7
        if (code.onFailureAction() != null) {
          resumed = runChain(code.onFailureAction(), resumed, path, opts); // rule 8
        }
        return resumed;
      }
      return failedState;
    }
  }

  // ------------------------------------------------------------------ loops

  private static RunState executeLoop(Action.LoopAction loop, RunState state, StepPath path, ExecutionOptions opts) {
    long startedAt = System.nanoTime();
    List<Object> items = asList(loop.items());
    if (items == null) {
      StepOutput.LoopOutput failedOutput = new StepOutput.LoopOutput(
          StepStatus.FAILED, loop.items(), null, 0, List.of(), "The items you have selected must be a list.",
          elapsedMillis(startedAt));
      RunState candidate = upsertAndCheckSize(state, path, loop.name(), failedOutput, opts);
      if (candidate.verdict() instanceof Verdict.LogSizeExceeded) return candidate;
      return candidate.withVerdict(
          new Verdict.Failed(new FailedStep(loop.name(), loop.displayName(), failedOutput.errorMessage())));
    }

    if (items.isEmpty()) {
      StepOutput.LoopOutput emptyOutput =
          new StepOutput.LoopOutput(
              StepStatus.SUCCEEDED, loop.items(), null, 0, List.of(), null, elapsedMillis(startedAt));
      return upsertAndCheckSize(state, path, loop.name(), emptyOutput, opts);
    }

    RunState working = state;
    List<Map<String, StepOutput>> iterations = new ArrayList<>();
    Object lastItem = null;
    int lastIndex = 0;
    for (int i = 0; i < items.size(); i++) {
      lastItem = items.get(i);
      lastIndex = i + 1;
      while (iterations.size() <= i) iterations.add(new LinkedHashMap<>());

      StepOutput.LoopOutput progress =
          new StepOutput.LoopOutput(StepStatus.RUNNING, loop.items(), lastItem, lastIndex, iterations, null, 0);
      working = upsertAndCheckSize(working, path, loop.name(), progress, opts);
      if (working.verdict() instanceof Verdict.LogSizeExceeded) return working;

      if (loop.firstLoopAction() != null) {
        working = runChain(loop.firstLoopAction(), working, path.extend(loop.name(), i), opts);
      }

      StepOutput refreshed = StepJournal.read(working.steps(), path).get(loop.name());
      if (refreshed instanceof StepOutput.LoopOutput lo) iterations = lo.iterations();

      if (!working.verdict().isRunning()) break; // rule 18
    }

    if (working.verdict() instanceof Verdict.LogSizeExceeded) return working;
    StepOutput.LoopOutput finalOutput =
        new StepOutput.LoopOutput(
            StepStatus.SUCCEEDED, loop.items(), lastItem, lastIndex, iterations, null, elapsedMillis(startedAt));
    return upsertAndCheckSize(working, path, loop.name(), finalOutput, opts); // rule 18: the loop itself stays SUCCEEDED
  }

  @SuppressWarnings("unchecked")
  private static List<Object> asList(Object items) {
    return items instanceof List ? (List<Object>) items : null;
  }

  // ------------------------------------------------------------------ routers

  private static RunState executeRouter(Action.RouterAction router, RunState state, StepPath path, ExecutionOptions opts) {
    long startedAt = System.nanoTime();
    int n = router.branches().size();
    boolean[] preFallback = new boolean[n];
    for (int i = 0; i < n; i++) {
      Action.RouterBranch branch = router.branches().get(i);
      preFallback[i] = branch.fallback() || Conditions.evaluateConditions(branch.conditionGroups());
    }
    boolean[] finalEval = new boolean[n];
    for (int i = 0; i < n; i++) {
      Action.RouterBranch branch = router.branches().get(i);
      if (!branch.fallback()) {
        finalEval[i] = preFallback[i];
        continue;
      }
      boolean anyOtherTrue = false;
      for (int j = 0; j < n; j++) {
        if (j != i && preFallback[j]) anyOtherTrue = true;
      }
      finalEval[i] = !anyOtherTrue; // rule 22
    }

    List<StepOutput.Branch> branchRecords = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      branchRecords.add(new StepOutput.Branch(router.branches().get(i).name(), i + 1, finalEval[i]));
    }
    StepOutput.RouterOutput evaluated = new StepOutput.RouterOutput(StepStatus.RUNNING, null, branchRecords, null, 0);
    RunState working = upsertAndCheckSize(state, path, router.name(), evaluated, opts);
    if (working.verdict() instanceof Verdict.LogSizeExceeded) return working;

    for (int i = 0; i < n; i++) {
      if (!finalEval[i]) continue;
      Action head = router.branches().get(i).firstAction();
      if (head != null) {
        working = runChain(head, working, path, opts);
      }
      if (!working.verdict().isRunning()) break;
      if (router.executionType() == Action.ExecutionType.EXECUTE_FIRST_MATCH) break; // rule 23
    }

    if (working.verdict() instanceof Verdict.LogSizeExceeded) return working;
    StepOutput.RouterOutput finalOutput =
        new StepOutput.RouterOutput(StepStatus.SUCCEEDED, null, branchRecords, null, elapsedMillis(startedAt));
    return upsertAndCheckSize(working, path, router.name(), finalOutput, opts);
  }

  // ------------------------------------------------------------------ the journal's size accounting

  /** Rule 29 — an oversized journal replaces the offending step and sets the run's verdict. */
  private static RunState upsertAndCheckSize(
      RunState state, StepPath path, String name, StepOutput output, ExecutionOptions opts) {
    Map<String, StepOutput> steps = StepJournal.upsert(state.steps(), path, name, output);
    long size = approxSizeBytes(steps);
    if (size <= opts.maxLogSizeBytes()) {
      RunState written = state.withSteps(steps).withLogSizeBytes(size);
      opts.progress().onJournalWrite(written);
      return written;
    }
    String message = "Flow run data size exceeded the maximum allowed size of " + opts.maxLogSizeMb() + " MB";
    StepOutput replaced = withFailureMessage(output, message);
    Map<String, StepOutput> replacedSteps = StepJournal.upsert(state.steps(), path, name, replaced);
    FailedStep failedStep = new FailedStep(name, name, message);
    RunState written =
        state
            .withSteps(replacedSteps)
            .withVerdict(new Verdict.LogSizeExceeded(failedStep))
            .withLogSizeBytes(approxSizeBytes(replacedSteps));
    opts.progress().onJournalWrite(written);
    return written;
  }

  private static StepOutput withFailureMessage(StepOutput output, String message) {
    if (output instanceof StepOutput.LeafOutput leaf) {
      return new StepOutput.LeafOutput(leaf.type(), StepStatus.FAILED, leaf.input(), null, message, leaf.durationMs());
    }
    if (output instanceof StepOutput.LoopOutput loop) {
      return new StepOutput.LoopOutput(
          StepStatus.FAILED, loop.input(), loop.item(), loop.index(), loop.iterations(), message, loop.durationMs());
    }
    StepOutput.RouterOutput router = (StepOutput.RouterOutput) output;
    return new StepOutput.RouterOutput(StepStatus.FAILED, router.input(), router.branches(), message, router.durationMs());
  }

  /**
   * A stand-in for the journal's serialized size: not JSON, since matching the source's exact
   * byte count is not part of this capability (§1) — only that the run stops once it is exceeded.
   */
  private static long approxSizeBytes(Map<String, StepOutput> steps) {
    return String.valueOf(steps).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
  }

  private static long elapsedMillis(long startedAtNanos) {
    return (System.nanoTime() - startedAtNanos) / 1_000_000;
  }

  private static String formatError(Exception e) {
    return e.getMessage() != null ? e.getMessage() : e.toString();
  }
}
