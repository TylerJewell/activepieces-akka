package io.akka.activepieces.domain;

/**
 * One router branch condition. {@link #UNDEFINED} stands in for a JS key that was never set —
 * distinct from a Java {@code null}, which stands in for a JS {@code null} literal. The
 * distinction matters: {@code text(null)} is the string {@code "null"}, but {@code
 * text(undefined)} throws, exactly as {@code JSON.stringify} behaves in the source.
 */
public record Condition(String operator, Object firstValue, Object secondValue, Boolean caseSensitive) {

  public static final Object UNDEFINED = new Object() {
    @Override
    public String toString() {
      return "undefined";
    }
  };

  public static Condition of(String operator, Object firstValue, Object secondValue) {
    return new Condition(operator, firstValue, secondValue, null);
  }

  public boolean isCaseSensitive() {
    return Boolean.TRUE.equals(caseSensitive);
  }
}
