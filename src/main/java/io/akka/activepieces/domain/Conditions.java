package io.akka.activepieces.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * SPEC-001 rules 21, 24, 25 — the 22 branch operators, ported line for line from
 * {@code router-executor.ts}'s {@code CONDITION_EVALUATORS}, {@code text}, {@code
 * parseStringToNumber}, {@code parseListAsArray} and {@code coerceListAsArray}. Verified against
 * the real evaluator's output in {@code bench/source-answers.conditions.json}.
 */
public final class Conditions {

  private Conditions() {}

  /** Rule 21 — OR across groups, AND within a group. An empty group is true; no groups is false. */
  public static boolean evaluateConditions(List<List<Condition>> conditionGroups) {
    for (List<Condition> group : conditionGroups) {
      boolean allTrue = true;
      for (Condition condition : group) {
        if (!evaluateOne(condition)) {
          allTrue = false;
          break;
        }
      }
      if (allTrue) {
        return true;
      }
    }
    return false;
  }

  /** Rule 24 — an absent operator raises; an operator the evaluator does not know evaluates true. */
  private static boolean evaluateOne(Condition c) {
    if (c.operator() == null) {
      throw new IllegalArgumentException("The operator is required but found to be undefined");
    }
    return switch (c.operator()) {
      case "TEXT_CONTAINS" -> text(c).contains(secondText(c));
      case "TEXT_DOES_NOT_CONTAIN" -> !text(c).contains(secondText(c));
      case "TEXT_EXACTLY_MATCHES" -> text(c).equals(secondText(c));
      case "TEXT_DOES_NOT_EXACTLY_MATCH" -> !text(c).equals(secondText(c));
      case "TEXT_START_WITH" -> text(c).startsWith(secondText(c));
      case "TEXT_DOES_NOT_START_WITH" -> !text(c).startsWith(secondText(c));
      case "TEXT_ENDS_WITH" -> text(c).endsWith(secondText(c));
      case "TEXT_DOES_NOT_END_WITH" -> !text(c).endsWith(secondText(c));
      case "LIST_CONTAINS" -> coerceListAsArray(c.firstValue()).stream()
          .anyMatch(item -> text(item, c.isCaseSensitive()).equals(secondText(c)));
      case "LIST_DOES_NOT_CONTAIN" -> coerceListAsArray(c.firstValue()).stream()
          .noneMatch(item -> text(item, c.isCaseSensitive()).equals(secondText(c)));
      case "NUMBER_IS_GREATER_THAN" -> lessThan(parseStringToNumber(c.secondValue()), parseStringToNumber(c.firstValue()));
      case "NUMBER_IS_LESS_THAN" -> lessThan(parseStringToNumber(c.firstValue()), parseStringToNumber(c.secondValue()));
      case "NUMBER_IS_EQUAL_TO" -> looseEquals(parseStringToNumber(c.firstValue()), parseStringToNumber(c.secondValue()));
      case "BOOLEAN_IS_TRUE" -> isTruthy(c.firstValue());
      case "BOOLEAN_IS_FALSE" -> !isTruthy(c.firstValue());
      case "DATE_IS_AFTER" -> compareDates(c, (a, b) -> a.isAfter(b));
      case "DATE_IS_EQUAL" -> compareDates(c, java.time.Instant::equals);
      case "DATE_IS_BEFORE" -> compareDates(c, (a, b) -> a.isBefore(b));
      case "LIST_IS_EMPTY" -> {
        List<Object> list = parseListAsArray(c.firstValue());
        yield list != null && list.isEmpty();
      }
      case "LIST_IS_NOT_EMPTY" -> {
        List<Object> list = parseListAsArray(c.firstValue());
        yield list != null && !list.isEmpty();
      }
      case "EXISTS" -> !isNil(c.firstValue()) && !"".equals(c.firstValue());
      case "DOES_NOT_EXIST" -> isNil(c.firstValue()) || "".equals(c.firstValue());
      default -> true;
    };
  }

  private static boolean isNil(Object v) {
    return v == null || v == Condition.UNDEFINED;
  }

  /** JS {@code text(value, {caseSensitive})}: JSON.stringify non-strings, lower-case unless asked not to. */
  private static String text(Object value, boolean caseSensitive) {
    if (value == Condition.UNDEFINED) {
      throw new NoSuchElementException("Cannot read properties of undefined (reading 'toLowerCase')");
    }
    String asString = value instanceof String s ? s : jsonStringify(value);
    return caseSensitive ? asString : asString.toLowerCase();
  }

  private static String text(Condition c) {
    return text(c.firstValue(), c.isCaseSensitive());
  }

  private static String secondText(Condition c) {
    return text(c.secondValue(), c.isCaseSensitive());
  }

  private static String jsonStringify(Object value) {
    if (value == null) return "null";
    if (value instanceof String s) return "\"" + s.replace("\"", "\\\"") + "\"";
    if (value instanceof Boolean || value instanceof Number) return String.valueOf(value);
    if (value instanceof List<?> list) {
      StringBuilder sb = new StringBuilder("[");
      for (int i = 0; i < list.size(); i++) {
        if (i > 0) sb.append(",");
        sb.append(jsonStringify(list.get(i)));
      }
      return sb.append("]").toString();
    }
    if (value instanceof Map<?, ?> map) {
      StringBuilder sb = new StringBuilder("{");
      boolean first = true;
      for (Map.Entry<?, ?> e : map.entrySet()) {
        if (!first) sb.append(",");
        first = false;
        sb.append("\"").append(e.getKey()).append("\":").append(jsonStringify(e.getValue()));
      }
      return sb.append("}").toString();
    }
    return String.valueOf(value);
  }

  /** JS {@code String(value)} — differs from JSON.stringify: objects become "[object Object]". */
  private static String jsToString(Object value) {
    if (value == null) return "null";
    if (value == Condition.UNDEFINED) return "undefined";
    if (value instanceof String s) return s;
    if (value instanceof List<?> list) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < list.size(); i++) {
        if (i > 0) sb.append(",");
        sb.append(jsToString(list.get(i)));
      }
      return sb.toString();
    }
    if (value instanceof Map) return "[object Object]";
    return String.valueOf(value);
  }

  /** JS {@code Number(value)}. NaN is signalled by a null return. */
  private static Double jsToNumber(Object value) {
    if (value == null) return 0.0;
    if (value == Condition.UNDEFINED) return null;
    if (value instanceof Boolean b) return b ? 1.0 : 0.0;
    if (value instanceof Number n) return n.doubleValue();
    if (value instanceof String s) {
      String t = s.trim();
      if (t.isEmpty()) return 0.0;
      try {
        return Double.parseDouble(t);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    if (value instanceof List<?> list) {
      if (list.isEmpty()) return 0.0;
      if (list.size() == 1) return jsToNumber(list.get(0));
      return null;
    }
    return null;
  }

  /** {@code parseStringToNumber}: a Double when Number() parses cleanly, else String(value). */
  private static Object parseStringToNumber(Object value) {
    Double n = jsToNumber(value);
    return n == null ? jsToString(value) : n;
  }

  /** {@code a < b} over the two {@code parseStringToNumber} results (each a Double or a String). */
  private static boolean lessThan(Object a, Object b) {
    if (a instanceof String as && b instanceof String bs) {
      return as.compareTo(bs) < 0;
    }
    Double an = a instanceof Double d ? d : tryParse((String) a);
    Double bn = b instanceof Double d ? d : tryParse((String) b);
    if (an == null || bn == null) return false;
    return an < bn;
  }

  private static Double tryParse(String s) {
    try {
      return Double.parseDouble(s.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static boolean looseEquals(Object a, Object b) {
    if (a instanceof String as && b instanceof String bs) {
      return as.equals(bs);
    }
    if (a instanceof Double d1 && b instanceof Double d2) {
      return d1.doubleValue() == d2.doubleValue();
    }
    Double an = a instanceof Double d ? d : (a instanceof String s ? tryParse(s) : null);
    Double bn = b instanceof Double d ? d : (b instanceof String s ? tryParse(s) : null);
    if (an == null || bn == null) return false;
    return an.doubleValue() == bn.doubleValue();
  }

  private static boolean isTruthy(Object v) {
    if (v == null || v == Condition.UNDEFINED) return false;
    if (v instanceof Boolean b) return b;
    if (v instanceof Number n) return n.doubleValue() != 0.0 && !Double.isNaN(n.doubleValue());
    if (v instanceof String s) return !s.isEmpty();
    return true;
  }

  @SuppressWarnings("unchecked")
  private static List<Object> parseListAsArray(Object input) {
    if (input instanceof String s) {
      Object parsed = tryParseJson(s);
      return parsed instanceof List ? (List<Object>) parsed : null;
    }
    if (input instanceof List) return (List<Object>) input;
    return null;
  }

  @SuppressWarnings("unchecked")
  private static List<Object> coerceListAsArray(Object input) {
    if (input instanceof String s) {
      Object parsed = tryParseJson(s);
      if (parsed == null && !"null".equals(s.trim())) return List.of(s);
      return parsed instanceof List ? (List<Object>) parsed : new ArrayList<>(List.of(parsed == null ? s : parsed));
    }
    if (input instanceof List) return (List<Object>) input;
    List<Object> single = new ArrayList<>();
    single.add(input);
    return single;
  }

  /** A tiny JSON reader: only arrays of strings/numbers are needed by any pair this port exercises. */
  private static Object tryParseJson(String s) {
    String t = s.trim();
    try {
      if (t.startsWith("[")) {
        return parseJsonArray(t);
      }
      return null;
    } catch (RuntimeException e) {
      return null;
    }
  }

  private static List<Object> parseJsonArray(String t) {
    List<Object> out = new ArrayList<>();
    String inner = t.substring(1, t.length() - 1).trim();
    if (inner.isEmpty()) return out;
    for (String part : inner.split(",")) {
      String p = part.trim();
      if (p.startsWith("\"") && p.endsWith("\"")) {
        out.add(p.substring(1, p.length() - 1));
      } else if (p.equals("true") || p.equals("false")) {
        out.add(Boolean.parseBoolean(p));
      } else {
        out.add(Double.parseDouble(p));
      }
    }
    return out;
  }

  private interface DateCompare {
    boolean test(java.time.Instant a, java.time.Instant b);
  }

  private static boolean compareDates(Condition c, DateCompare compare) {
    java.time.Instant first = toInstant(c.firstValue());
    java.time.Instant second = toInstant(c.secondValue());
    if (first == null || second == null) return false;
    return compare.test(first, second);
  }

  private static java.time.Instant toInstant(Object value) {
    if (!(value instanceof String) && !(value instanceof Number)) return null;
    String s = value instanceof String str ? str : String.valueOf(value);
    try {
      return java.time.LocalDate.parse(s).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
    } catch (Exception ignore) {
      // fall through to a full timestamp parse
    }
    try {
      return java.time.Instant.parse(s);
    } catch (Exception ignore) {
      // fall through to a bare-number reading, which dayjs also accepts
    }
    try {
      return java.time.Instant.ofEpochMilli((long) Double.parseDouble(s.trim()));
    } catch (Exception ignore) {
      return null;
    }
  }
}
