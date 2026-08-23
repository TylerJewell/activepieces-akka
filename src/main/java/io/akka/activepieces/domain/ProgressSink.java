package io.akka.activepieces.domain;

/**
 * Called after every write to a run's journal, so a watcher sees the run as it happens rather
 * than only its end state. The source reports progress at the same points — `flow-executor.ts`
 * calls `flowRunProgressReporter.sendUpdate` around each step boundary.
 */
@FunctionalInterface
public interface ProgressSink {

  ProgressSink NONE = state -> {};

  void onJournalWrite(RunState state);
}
