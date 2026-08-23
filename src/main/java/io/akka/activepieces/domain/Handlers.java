package io.akka.activepieces.domain;

/** SPEC-001 §1 — the two handlers a code/piece step body can run: an echo and a thrower. */
public final class Handlers {

  private Handlers() {}

  public static StepHandler echo(Object value) {
    return input -> value;
  }

  public static StepHandler throwing(String message) {
    return input -> {
      throw new RuntimeException(message);
    };
  }
}
