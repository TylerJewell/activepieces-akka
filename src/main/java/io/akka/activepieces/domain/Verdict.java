package io.akka.activepieces.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** SPEC-001 §2. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "verdict")
@JsonSubTypes({
  @JsonSubTypes.Type(value = Verdict.Running.class, name = "RUNNING"),
  @JsonSubTypes.Type(value = Verdict.Succeeded.class, name = "SUCCEEDED"),
  @JsonSubTypes.Type(value = Verdict.Failed.class, name = "FAILED"),
  @JsonSubTypes.Type(value = Verdict.LogSizeExceeded.class, name = "LOG_SIZE_EXCEEDED"),
  @JsonSubTypes.Type(value = Verdict.Paused.class, name = "PAUSED")
})
public sealed interface Verdict {

  record Running() implements Verdict {}

  record Succeeded() implements Verdict {}

  record Failed(FailedStep failedStep) implements Verdict {}

  record LogSizeExceeded(FailedStep failedStep) implements Verdict {}

  record Paused() implements Verdict {}

  Verdict RUNNING = new Running();
  Verdict SUCCEEDED = new Succeeded();
  Verdict PAUSED = new Paused();

  default boolean isRunning() {
    return this instanceof Running;
  }
}
