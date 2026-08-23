package io.akka.activepieces.domain;

/**
 * The wait between retry attempts. Separated so a test can read the delays a run asked for
 * without spending them: rule 13's sequence is four, eight and sixteen seconds, and a suite that
 * waits it out is a suite nobody runs.
 */
@FunctionalInterface
public interface Sleeper {

  Sleeper REAL =
      millis -> {
        try {
          Thread.sleep(millis);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      };

  void sleep(long millis);
}
