package io.akka.activepieces.domain;

/** SPEC-001 rules 30, 31 — the entry to a run. */
public record TriggerStep(String name, String displayName, String type, Action nextAction) {}
