package io.akka.activepieces.domain;

/** SPEC-001 §2 — the step a FAILED or LOG_SIZE_EXCEEDED verdict names. */
public record FailedStep(String name, String displayName, String message) {}
