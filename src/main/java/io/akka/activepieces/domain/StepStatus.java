package io.akka.activepieces.domain;

/** SPEC-001 §2 — status recorded on a journal entry. */
public enum StepStatus {
  SUCCEEDED,
  FAILED,
  PAUSED,
  RUNNING,
  STOPPED
}
