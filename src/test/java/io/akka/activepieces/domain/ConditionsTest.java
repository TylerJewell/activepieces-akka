package io.akka.activepieces.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** SPEC-001 rule 25 -- every operator answers as bench/source-answers.conditions.json records. */
class ConditionsTest {

  private static Map<String, Object> linked(Object... kv) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
    return m;
  }

  private static boolean eval(String operator, Object first, Object second, Boolean caseSensitive) {
    return Conditions.evaluateConditions(List.of(List.of(new Condition(operator, first, second, caseSensitive))));
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_TEXT_CONTAINS() {
    assertEquals(true, eval("TEXT_CONTAINS", "abc", "b", null));  // abc/b
    assertEquals(true, eval("TEXT_CONTAINS", "abc", "ABC", null));  // abc/ABC
    assertEquals(false, eval("TEXT_CONTAINS", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(true, eval("TEXT_CONTAINS", "abc", "abc", null));  // abc/abc
    assertEquals(false, eval("TEXT_CONTAINS", "abc", "z", null));  // abc/z
    assertEquals(true, eval("TEXT_CONTAINS", "", "", null));  // empty/empty
    assertEquals(false, eval("TEXT_CONTAINS", null, "x", null));  // null/x
    assertThrows(RuntimeException.class, () -> eval("TEXT_CONTAINS", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(false, eval("TEXT_CONTAINS", "10", "9", null));  // 10/9
    assertEquals(true, eval("TEXT_CONTAINS", "10", "10", null));  // 10/10
    assertEquals(false, eval("TEXT_CONTAINS", "ten", "9", null));  // ten/9
    assertThrows(RuntimeException.class, () -> eval("TEXT_CONTAINS", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_CONTAINS", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_CONTAINS", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(true, eval("TEXT_CONTAINS", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(true, eval("TEXT_CONTAINS", List.of("a", "b"), "b", null));  // list arr/b
    assertThrows(RuntimeException.class, () -> eval("TEXT_CONTAINS", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_CONTAINS", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(true, eval("TEXT_CONTAINS", "a", "a", null));  // scalar as list/a
    assertEquals(false, eval("TEXT_CONTAINS", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(true, eval("TEXT_CONTAINS", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(false, eval("TEXT_CONTAINS", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(false, eval("TEXT_CONTAINS", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_TEXT_DOES_NOT_CONTAIN() {
    assertEquals(false, eval("TEXT_DOES_NOT_CONTAIN", "abc", "b", null));  // abc/b
    assertEquals(false, eval("TEXT_DOES_NOT_CONTAIN", "abc", "ABC", null));  // abc/ABC
    assertEquals(true, eval("TEXT_DOES_NOT_CONTAIN", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(false, eval("TEXT_DOES_NOT_CONTAIN", "abc", "abc", null));  // abc/abc
    assertEquals(true, eval("TEXT_DOES_NOT_CONTAIN", "abc", "z", null));  // abc/z
    assertEquals(false, eval("TEXT_DOES_NOT_CONTAIN", "", "", null));  // empty/empty
    assertEquals(true, eval("TEXT_DOES_NOT_CONTAIN", null, "x", null));  // null/x
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_CONTAIN", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(true, eval("TEXT_DOES_NOT_CONTAIN", "10", "9", null));  // 10/9
    assertEquals(false, eval("TEXT_DOES_NOT_CONTAIN", "10", "10", null));  // 10/10
    assertEquals(true, eval("TEXT_DOES_NOT_CONTAIN", "ten", "9", null));  // ten/9
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_CONTAIN", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_CONTAIN", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_CONTAIN", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(false, eval("TEXT_DOES_NOT_CONTAIN", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(false, eval("TEXT_DOES_NOT_CONTAIN", List.of("a", "b"), "b", null));  // list arr/b
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_CONTAIN", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_CONTAIN", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(false, eval("TEXT_DOES_NOT_CONTAIN", "a", "a", null));  // scalar as list/a
    assertEquals(true, eval("TEXT_DOES_NOT_CONTAIN", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(false, eval("TEXT_DOES_NOT_CONTAIN", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(true, eval("TEXT_DOES_NOT_CONTAIN", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(true, eval("TEXT_DOES_NOT_CONTAIN", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_TEXT_EXACTLY_MATCHES() {
    assertEquals(false, eval("TEXT_EXACTLY_MATCHES", "abc", "b", null));  // abc/b
    assertEquals(true, eval("TEXT_EXACTLY_MATCHES", "abc", "ABC", null));  // abc/ABC
    assertEquals(false, eval("TEXT_EXACTLY_MATCHES", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(true, eval("TEXT_EXACTLY_MATCHES", "abc", "abc", null));  // abc/abc
    assertEquals(false, eval("TEXT_EXACTLY_MATCHES", "abc", "z", null));  // abc/z
    assertEquals(true, eval("TEXT_EXACTLY_MATCHES", "", "", null));  // empty/empty
    assertEquals(false, eval("TEXT_EXACTLY_MATCHES", null, "x", null));  // null/x
    assertThrows(RuntimeException.class, () -> eval("TEXT_EXACTLY_MATCHES", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(false, eval("TEXT_EXACTLY_MATCHES", "10", "9", null));  // 10/9
    assertEquals(true, eval("TEXT_EXACTLY_MATCHES", "10", "10", null));  // 10/10
    assertEquals(false, eval("TEXT_EXACTLY_MATCHES", "ten", "9", null));  // ten/9
    assertThrows(RuntimeException.class, () -> eval("TEXT_EXACTLY_MATCHES", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_EXACTLY_MATCHES", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_EXACTLY_MATCHES", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(false, eval("TEXT_EXACTLY_MATCHES", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(false, eval("TEXT_EXACTLY_MATCHES", List.of("a", "b"), "b", null));  // list arr/b
    assertThrows(RuntimeException.class, () -> eval("TEXT_EXACTLY_MATCHES", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_EXACTLY_MATCHES", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(true, eval("TEXT_EXACTLY_MATCHES", "a", "a", null));  // scalar as list/a
    assertEquals(false, eval("TEXT_EXACTLY_MATCHES", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(true, eval("TEXT_EXACTLY_MATCHES", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(false, eval("TEXT_EXACTLY_MATCHES", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(false, eval("TEXT_EXACTLY_MATCHES", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_TEXT_DOES_NOT_EXACTLY_MATCH() {
    assertEquals(true, eval("TEXT_DOES_NOT_EXACTLY_MATCH", "abc", "b", null));  // abc/b
    assertEquals(false, eval("TEXT_DOES_NOT_EXACTLY_MATCH", "abc", "ABC", null));  // abc/ABC
    assertEquals(true, eval("TEXT_DOES_NOT_EXACTLY_MATCH", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(false, eval("TEXT_DOES_NOT_EXACTLY_MATCH", "abc", "abc", null));  // abc/abc
    assertEquals(true, eval("TEXT_DOES_NOT_EXACTLY_MATCH", "abc", "z", null));  // abc/z
    assertEquals(false, eval("TEXT_DOES_NOT_EXACTLY_MATCH", "", "", null));  // empty/empty
    assertEquals(true, eval("TEXT_DOES_NOT_EXACTLY_MATCH", null, "x", null));  // null/x
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_EXACTLY_MATCH", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(true, eval("TEXT_DOES_NOT_EXACTLY_MATCH", "10", "9", null));  // 10/9
    assertEquals(false, eval("TEXT_DOES_NOT_EXACTLY_MATCH", "10", "10", null));  // 10/10
    assertEquals(true, eval("TEXT_DOES_NOT_EXACTLY_MATCH", "ten", "9", null));  // ten/9
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_EXACTLY_MATCH", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_EXACTLY_MATCH", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_EXACTLY_MATCH", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(true, eval("TEXT_DOES_NOT_EXACTLY_MATCH", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(true, eval("TEXT_DOES_NOT_EXACTLY_MATCH", List.of("a", "b"), "b", null));  // list arr/b
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_EXACTLY_MATCH", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_EXACTLY_MATCH", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(false, eval("TEXT_DOES_NOT_EXACTLY_MATCH", "a", "a", null));  // scalar as list/a
    assertEquals(true, eval("TEXT_DOES_NOT_EXACTLY_MATCH", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(false, eval("TEXT_DOES_NOT_EXACTLY_MATCH", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(true, eval("TEXT_DOES_NOT_EXACTLY_MATCH", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(true, eval("TEXT_DOES_NOT_EXACTLY_MATCH", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_TEXT_START_WITH() {
    assertEquals(false, eval("TEXT_START_WITH", "abc", "b", null));  // abc/b
    assertEquals(true, eval("TEXT_START_WITH", "abc", "ABC", null));  // abc/ABC
    assertEquals(false, eval("TEXT_START_WITH", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(true, eval("TEXT_START_WITH", "abc", "abc", null));  // abc/abc
    assertEquals(false, eval("TEXT_START_WITH", "abc", "z", null));  // abc/z
    assertEquals(true, eval("TEXT_START_WITH", "", "", null));  // empty/empty
    assertEquals(false, eval("TEXT_START_WITH", null, "x", null));  // null/x
    assertThrows(RuntimeException.class, () -> eval("TEXT_START_WITH", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(false, eval("TEXT_START_WITH", "10", "9", null));  // 10/9
    assertEquals(true, eval("TEXT_START_WITH", "10", "10", null));  // 10/10
    assertEquals(false, eval("TEXT_START_WITH", "ten", "9", null));  // ten/9
    assertThrows(RuntimeException.class, () -> eval("TEXT_START_WITH", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_START_WITH", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_START_WITH", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(false, eval("TEXT_START_WITH", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(false, eval("TEXT_START_WITH", List.of("a", "b"), "b", null));  // list arr/b
    assertThrows(RuntimeException.class, () -> eval("TEXT_START_WITH", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_START_WITH", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(true, eval("TEXT_START_WITH", "a", "a", null));  // scalar as list/a
    assertEquals(false, eval("TEXT_START_WITH", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(true, eval("TEXT_START_WITH", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(false, eval("TEXT_START_WITH", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(false, eval("TEXT_START_WITH", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_TEXT_DOES_NOT_START_WITH() {
    assertEquals(true, eval("TEXT_DOES_NOT_START_WITH", "abc", "b", null));  // abc/b
    assertEquals(false, eval("TEXT_DOES_NOT_START_WITH", "abc", "ABC", null));  // abc/ABC
    assertEquals(true, eval("TEXT_DOES_NOT_START_WITH", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(false, eval("TEXT_DOES_NOT_START_WITH", "abc", "abc", null));  // abc/abc
    assertEquals(true, eval("TEXT_DOES_NOT_START_WITH", "abc", "z", null));  // abc/z
    assertEquals(false, eval("TEXT_DOES_NOT_START_WITH", "", "", null));  // empty/empty
    assertEquals(true, eval("TEXT_DOES_NOT_START_WITH", null, "x", null));  // null/x
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_START_WITH", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(true, eval("TEXT_DOES_NOT_START_WITH", "10", "9", null));  // 10/9
    assertEquals(false, eval("TEXT_DOES_NOT_START_WITH", "10", "10", null));  // 10/10
    assertEquals(true, eval("TEXT_DOES_NOT_START_WITH", "ten", "9", null));  // ten/9
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_START_WITH", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_START_WITH", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_START_WITH", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(true, eval("TEXT_DOES_NOT_START_WITH", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(true, eval("TEXT_DOES_NOT_START_WITH", List.of("a", "b"), "b", null));  // list arr/b
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_START_WITH", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_START_WITH", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(false, eval("TEXT_DOES_NOT_START_WITH", "a", "a", null));  // scalar as list/a
    assertEquals(true, eval("TEXT_DOES_NOT_START_WITH", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(false, eval("TEXT_DOES_NOT_START_WITH", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(true, eval("TEXT_DOES_NOT_START_WITH", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(true, eval("TEXT_DOES_NOT_START_WITH", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_TEXT_ENDS_WITH() {
    assertEquals(false, eval("TEXT_ENDS_WITH", "abc", "b", null));  // abc/b
    assertEquals(true, eval("TEXT_ENDS_WITH", "abc", "ABC", null));  // abc/ABC
    assertEquals(false, eval("TEXT_ENDS_WITH", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(true, eval("TEXT_ENDS_WITH", "abc", "abc", null));  // abc/abc
    assertEquals(false, eval("TEXT_ENDS_WITH", "abc", "z", null));  // abc/z
    assertEquals(true, eval("TEXT_ENDS_WITH", "", "", null));  // empty/empty
    assertEquals(false, eval("TEXT_ENDS_WITH", null, "x", null));  // null/x
    assertThrows(RuntimeException.class, () -> eval("TEXT_ENDS_WITH", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(false, eval("TEXT_ENDS_WITH", "10", "9", null));  // 10/9
    assertEquals(true, eval("TEXT_ENDS_WITH", "10", "10", null));  // 10/10
    assertEquals(false, eval("TEXT_ENDS_WITH", "ten", "9", null));  // ten/9
    assertThrows(RuntimeException.class, () -> eval("TEXT_ENDS_WITH", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_ENDS_WITH", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_ENDS_WITH", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(false, eval("TEXT_ENDS_WITH", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(false, eval("TEXT_ENDS_WITH", List.of("a", "b"), "b", null));  // list arr/b
    assertThrows(RuntimeException.class, () -> eval("TEXT_ENDS_WITH", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_ENDS_WITH", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(true, eval("TEXT_ENDS_WITH", "a", "a", null));  // scalar as list/a
    assertEquals(false, eval("TEXT_ENDS_WITH", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(true, eval("TEXT_ENDS_WITH", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(false, eval("TEXT_ENDS_WITH", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(false, eval("TEXT_ENDS_WITH", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_TEXT_DOES_NOT_END_WITH() {
    assertEquals(true, eval("TEXT_DOES_NOT_END_WITH", "abc", "b", null));  // abc/b
    assertEquals(false, eval("TEXT_DOES_NOT_END_WITH", "abc", "ABC", null));  // abc/ABC
    assertEquals(true, eval("TEXT_DOES_NOT_END_WITH", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(false, eval("TEXT_DOES_NOT_END_WITH", "abc", "abc", null));  // abc/abc
    assertEquals(true, eval("TEXT_DOES_NOT_END_WITH", "abc", "z", null));  // abc/z
    assertEquals(false, eval("TEXT_DOES_NOT_END_WITH", "", "", null));  // empty/empty
    assertEquals(true, eval("TEXT_DOES_NOT_END_WITH", null, "x", null));  // null/x
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_END_WITH", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(true, eval("TEXT_DOES_NOT_END_WITH", "10", "9", null));  // 10/9
    assertEquals(false, eval("TEXT_DOES_NOT_END_WITH", "10", "10", null));  // 10/10
    assertEquals(true, eval("TEXT_DOES_NOT_END_WITH", "ten", "9", null));  // ten/9
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_END_WITH", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_END_WITH", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_END_WITH", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(true, eval("TEXT_DOES_NOT_END_WITH", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(true, eval("TEXT_DOES_NOT_END_WITH", List.of("a", "b"), "b", null));  // list arr/b
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_END_WITH", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertThrows(RuntimeException.class, () -> eval("TEXT_DOES_NOT_END_WITH", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(false, eval("TEXT_DOES_NOT_END_WITH", "a", "a", null));  // scalar as list/a
    assertEquals(true, eval("TEXT_DOES_NOT_END_WITH", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(false, eval("TEXT_DOES_NOT_END_WITH", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(true, eval("TEXT_DOES_NOT_END_WITH", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(true, eval("TEXT_DOES_NOT_END_WITH", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_NUMBER_IS_GREATER_THAN() {
    assertEquals(false, eval("NUMBER_IS_GREATER_THAN", "abc", "b", null));  // abc/b
    assertEquals(true, eval("NUMBER_IS_GREATER_THAN", "abc", "ABC", null));  // abc/ABC
    assertEquals(true, eval("NUMBER_IS_GREATER_THAN", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(false, eval("NUMBER_IS_GREATER_THAN", "abc", "abc", null));  // abc/abc
    assertEquals(false, eval("NUMBER_IS_GREATER_THAN", "abc", "z", null));  // abc/z
    assertEquals(false, eval("NUMBER_IS_GREATER_THAN", "", "", null));  // empty/empty
    assertEquals(false, eval("NUMBER_IS_GREATER_THAN", null, "x", null));  // null/x
    assertEquals(false, eval("NUMBER_IS_GREATER_THAN", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(true, eval("NUMBER_IS_GREATER_THAN", "10", "9", null));  // 10/9
    assertEquals(false, eval("NUMBER_IS_GREATER_THAN", "10", "10", null));  // 10/10
    assertEquals(false, eval("NUMBER_IS_GREATER_THAN", "ten", "9", null));  // ten/9
    assertEquals(false, eval("NUMBER_IS_GREATER_THAN", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertEquals(false, eval("NUMBER_IS_GREATER_THAN", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertEquals(false, eval("NUMBER_IS_GREATER_THAN", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(false, eval("NUMBER_IS_GREATER_THAN", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(false, eval("NUMBER_IS_GREATER_THAN", List.of("a", "b"), "b", null));  // list arr/b
    assertEquals(false, eval("NUMBER_IS_GREATER_THAN", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertEquals(false, eval("NUMBER_IS_GREATER_THAN", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(false, eval("NUMBER_IS_GREATER_THAN", "a", "a", null));  // scalar as list/a
    assertEquals(true, eval("NUMBER_IS_GREATER_THAN", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(false, eval("NUMBER_IS_GREATER_THAN", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(true, eval("NUMBER_IS_GREATER_THAN", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(false, eval("NUMBER_IS_GREATER_THAN", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_NUMBER_IS_LESS_THAN() {
    assertEquals(true, eval("NUMBER_IS_LESS_THAN", "abc", "b", null));  // abc/b
    assertEquals(false, eval("NUMBER_IS_LESS_THAN", "abc", "ABC", null));  // abc/ABC
    assertEquals(false, eval("NUMBER_IS_LESS_THAN", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(false, eval("NUMBER_IS_LESS_THAN", "abc", "abc", null));  // abc/abc
    assertEquals(true, eval("NUMBER_IS_LESS_THAN", "abc", "z", null));  // abc/z
    assertEquals(false, eval("NUMBER_IS_LESS_THAN", "", "", null));  // empty/empty
    assertEquals(false, eval("NUMBER_IS_LESS_THAN", null, "x", null));  // null/x
    assertEquals(true, eval("NUMBER_IS_LESS_THAN", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(false, eval("NUMBER_IS_LESS_THAN", "10", "9", null));  // 10/9
    assertEquals(false, eval("NUMBER_IS_LESS_THAN", "10", "10", null));  // 10/10
    assertEquals(false, eval("NUMBER_IS_LESS_THAN", "ten", "9", null));  // ten/9
    assertEquals(false, eval("NUMBER_IS_LESS_THAN", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertEquals(false, eval("NUMBER_IS_LESS_THAN", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertEquals(false, eval("NUMBER_IS_LESS_THAN", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(true, eval("NUMBER_IS_LESS_THAN", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(true, eval("NUMBER_IS_LESS_THAN", List.of("a", "b"), "b", null));  // list arr/b
    assertEquals(false, eval("NUMBER_IS_LESS_THAN", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertEquals(true, eval("NUMBER_IS_LESS_THAN", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(false, eval("NUMBER_IS_LESS_THAN", "a", "a", null));  // scalar as list/a
    assertEquals(false, eval("NUMBER_IS_LESS_THAN", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(false, eval("NUMBER_IS_LESS_THAN", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(false, eval("NUMBER_IS_LESS_THAN", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(true, eval("NUMBER_IS_LESS_THAN", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_NUMBER_IS_EQUAL_TO() {
    assertEquals(false, eval("NUMBER_IS_EQUAL_TO", "abc", "b", null));  // abc/b
    assertEquals(false, eval("NUMBER_IS_EQUAL_TO", "abc", "ABC", null));  // abc/ABC
    assertEquals(false, eval("NUMBER_IS_EQUAL_TO", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(true, eval("NUMBER_IS_EQUAL_TO", "abc", "abc", null));  // abc/abc
    assertEquals(false, eval("NUMBER_IS_EQUAL_TO", "abc", "z", null));  // abc/z
    assertEquals(true, eval("NUMBER_IS_EQUAL_TO", "", "", null));  // empty/empty
    assertEquals(false, eval("NUMBER_IS_EQUAL_TO", null, "x", null));  // null/x
    assertEquals(false, eval("NUMBER_IS_EQUAL_TO", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(false, eval("NUMBER_IS_EQUAL_TO", "10", "9", null));  // 10/9
    assertEquals(true, eval("NUMBER_IS_EQUAL_TO", "10", "10", null));  // 10/10
    assertEquals(false, eval("NUMBER_IS_EQUAL_TO", "ten", "9", null));  // ten/9
    assertEquals(false, eval("NUMBER_IS_EQUAL_TO", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertEquals(false, eval("NUMBER_IS_EQUAL_TO", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertEquals(false, eval("NUMBER_IS_EQUAL_TO", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(false, eval("NUMBER_IS_EQUAL_TO", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(false, eval("NUMBER_IS_EQUAL_TO", List.of("a", "b"), "b", null));  // list arr/b
    assertEquals(false, eval("NUMBER_IS_EQUAL_TO", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertEquals(false, eval("NUMBER_IS_EQUAL_TO", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(true, eval("NUMBER_IS_EQUAL_TO", "a", "a", null));  // scalar as list/a
    assertEquals(false, eval("NUMBER_IS_EQUAL_TO", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(true, eval("NUMBER_IS_EQUAL_TO", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(false, eval("NUMBER_IS_EQUAL_TO", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(false, eval("NUMBER_IS_EQUAL_TO", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_BOOLEAN_IS_TRUE() {
    assertEquals(true, eval("BOOLEAN_IS_TRUE", "abc", "b", null));  // abc/b
    assertEquals(true, eval("BOOLEAN_IS_TRUE", "abc", "ABC", null));  // abc/ABC
    assertEquals(true, eval("BOOLEAN_IS_TRUE", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(true, eval("BOOLEAN_IS_TRUE", "abc", "abc", null));  // abc/abc
    assertEquals(true, eval("BOOLEAN_IS_TRUE", "abc", "z", null));  // abc/z
    assertEquals(false, eval("BOOLEAN_IS_TRUE", "", "", null));  // empty/empty
    assertEquals(false, eval("BOOLEAN_IS_TRUE", null, "x", null));  // null/x
    assertEquals(false, eval("BOOLEAN_IS_TRUE", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(true, eval("BOOLEAN_IS_TRUE", "10", "9", null));  // 10/9
    assertEquals(true, eval("BOOLEAN_IS_TRUE", "10", "10", null));  // 10/10
    assertEquals(true, eval("BOOLEAN_IS_TRUE", "ten", "9", null));  // ten/9
    assertEquals(true, eval("BOOLEAN_IS_TRUE", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertEquals(false, eval("BOOLEAN_IS_TRUE", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertEquals(false, eval("BOOLEAN_IS_TRUE", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(true, eval("BOOLEAN_IS_TRUE", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(true, eval("BOOLEAN_IS_TRUE", List.of("a", "b"), "b", null));  // list arr/b
    assertEquals(true, eval("BOOLEAN_IS_TRUE", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertEquals(true, eval("BOOLEAN_IS_TRUE", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(true, eval("BOOLEAN_IS_TRUE", "a", "a", null));  // scalar as list/a
    assertEquals(true, eval("BOOLEAN_IS_TRUE", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(true, eval("BOOLEAN_IS_TRUE", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(true, eval("BOOLEAN_IS_TRUE", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(true, eval("BOOLEAN_IS_TRUE", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_BOOLEAN_IS_FALSE() {
    assertEquals(false, eval("BOOLEAN_IS_FALSE", "abc", "b", null));  // abc/b
    assertEquals(false, eval("BOOLEAN_IS_FALSE", "abc", "ABC", null));  // abc/ABC
    assertEquals(false, eval("BOOLEAN_IS_FALSE", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(false, eval("BOOLEAN_IS_FALSE", "abc", "abc", null));  // abc/abc
    assertEquals(false, eval("BOOLEAN_IS_FALSE", "abc", "z", null));  // abc/z
    assertEquals(true, eval("BOOLEAN_IS_FALSE", "", "", null));  // empty/empty
    assertEquals(true, eval("BOOLEAN_IS_FALSE", null, "x", null));  // null/x
    assertEquals(true, eval("BOOLEAN_IS_FALSE", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(false, eval("BOOLEAN_IS_FALSE", "10", "9", null));  // 10/9
    assertEquals(false, eval("BOOLEAN_IS_FALSE", "10", "10", null));  // 10/10
    assertEquals(false, eval("BOOLEAN_IS_FALSE", "ten", "9", null));  // ten/9
    assertEquals(false, eval("BOOLEAN_IS_FALSE", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertEquals(true, eval("BOOLEAN_IS_FALSE", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertEquals(true, eval("BOOLEAN_IS_FALSE", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(false, eval("BOOLEAN_IS_FALSE", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(false, eval("BOOLEAN_IS_FALSE", List.of("a", "b"), "b", null));  // list arr/b
    assertEquals(false, eval("BOOLEAN_IS_FALSE", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertEquals(false, eval("BOOLEAN_IS_FALSE", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(false, eval("BOOLEAN_IS_FALSE", "a", "a", null));  // scalar as list/a
    assertEquals(false, eval("BOOLEAN_IS_FALSE", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(false, eval("BOOLEAN_IS_FALSE", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(false, eval("BOOLEAN_IS_FALSE", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(false, eval("BOOLEAN_IS_FALSE", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_DATE_IS_BEFORE() {
    assertEquals(false, eval("DATE_IS_BEFORE", "abc", "b", null));  // abc/b
    assertEquals(false, eval("DATE_IS_BEFORE", "abc", "ABC", null));  // abc/ABC
    assertEquals(false, eval("DATE_IS_BEFORE", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(false, eval("DATE_IS_BEFORE", "abc", "abc", null));  // abc/abc
    assertEquals(false, eval("DATE_IS_BEFORE", "abc", "z", null));  // abc/z
    assertEquals(false, eval("DATE_IS_BEFORE", "", "", null));  // empty/empty
    assertEquals(false, eval("DATE_IS_BEFORE", null, "x", null));  // null/x
    assertEquals(false, eval("DATE_IS_BEFORE", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(false, eval("DATE_IS_BEFORE", "10", "9", null));  // 10/9
    assertEquals(false, eval("DATE_IS_BEFORE", "10", "10", null));  // 10/10
    assertEquals(false, eval("DATE_IS_BEFORE", "ten", "9", null));  // ten/9
    assertEquals(false, eval("DATE_IS_BEFORE", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertEquals(false, eval("DATE_IS_BEFORE", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertEquals(false, eval("DATE_IS_BEFORE", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(false, eval("DATE_IS_BEFORE", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(false, eval("DATE_IS_BEFORE", List.of("a", "b"), "b", null));  // list arr/b
    assertEquals(false, eval("DATE_IS_BEFORE", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertEquals(false, eval("DATE_IS_BEFORE", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(false, eval("DATE_IS_BEFORE", "a", "a", null));  // scalar as list/a
    assertEquals(false, eval("DATE_IS_BEFORE", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(false, eval("DATE_IS_BEFORE", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(false, eval("DATE_IS_BEFORE", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(false, eval("DATE_IS_BEFORE", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_DATE_IS_EQUAL() {
    assertEquals(false, eval("DATE_IS_EQUAL", "abc", "b", null));  // abc/b
    assertEquals(false, eval("DATE_IS_EQUAL", "abc", "ABC", null));  // abc/ABC
    assertEquals(false, eval("DATE_IS_EQUAL", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(false, eval("DATE_IS_EQUAL", "abc", "abc", null));  // abc/abc
    assertEquals(false, eval("DATE_IS_EQUAL", "abc", "z", null));  // abc/z
    assertEquals(false, eval("DATE_IS_EQUAL", "", "", null));  // empty/empty
    assertEquals(false, eval("DATE_IS_EQUAL", null, "x", null));  // null/x
    assertEquals(false, eval("DATE_IS_EQUAL", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(false, eval("DATE_IS_EQUAL", "10", "9", null));  // 10/9
    assertEquals(true, eval("DATE_IS_EQUAL", "10", "10", null));  // 10/10
    assertEquals(false, eval("DATE_IS_EQUAL", "ten", "9", null));  // ten/9
    assertEquals(false, eval("DATE_IS_EQUAL", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertEquals(false, eval("DATE_IS_EQUAL", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertEquals(false, eval("DATE_IS_EQUAL", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(false, eval("DATE_IS_EQUAL", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(false, eval("DATE_IS_EQUAL", List.of("a", "b"), "b", null));  // list arr/b
    assertEquals(false, eval("DATE_IS_EQUAL", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertEquals(false, eval("DATE_IS_EQUAL", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(false, eval("DATE_IS_EQUAL", "a", "a", null));  // scalar as list/a
    assertEquals(false, eval("DATE_IS_EQUAL", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(true, eval("DATE_IS_EQUAL", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(false, eval("DATE_IS_EQUAL", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(false, eval("DATE_IS_EQUAL", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_DATE_IS_AFTER() {
    assertEquals(false, eval("DATE_IS_AFTER", "abc", "b", null));  // abc/b
    assertEquals(false, eval("DATE_IS_AFTER", "abc", "ABC", null));  // abc/ABC
    assertEquals(false, eval("DATE_IS_AFTER", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(false, eval("DATE_IS_AFTER", "abc", "abc", null));  // abc/abc
    assertEquals(false, eval("DATE_IS_AFTER", "abc", "z", null));  // abc/z
    assertEquals(false, eval("DATE_IS_AFTER", "", "", null));  // empty/empty
    assertEquals(false, eval("DATE_IS_AFTER", null, "x", null));  // null/x
    assertEquals(false, eval("DATE_IS_AFTER", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(true, eval("DATE_IS_AFTER", "10", "9", null));  // 10/9
    assertEquals(false, eval("DATE_IS_AFTER", "10", "10", null));  // 10/10
    assertEquals(false, eval("DATE_IS_AFTER", "ten", "9", null));  // ten/9
    assertEquals(false, eval("DATE_IS_AFTER", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertEquals(false, eval("DATE_IS_AFTER", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertEquals(false, eval("DATE_IS_AFTER", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(false, eval("DATE_IS_AFTER", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(false, eval("DATE_IS_AFTER", List.of("a", "b"), "b", null));  // list arr/b
    assertEquals(false, eval("DATE_IS_AFTER", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertEquals(false, eval("DATE_IS_AFTER", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(false, eval("DATE_IS_AFTER", "a", "a", null));  // scalar as list/a
    assertEquals(true, eval("DATE_IS_AFTER", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(false, eval("DATE_IS_AFTER", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(false, eval("DATE_IS_AFTER", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(false, eval("DATE_IS_AFTER", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_LIST_CONTAINS() {
    assertEquals(false, eval("LIST_CONTAINS", "abc", "b", null));  // abc/b
    assertEquals(true, eval("LIST_CONTAINS", "abc", "ABC", null));  // abc/ABC
    assertEquals(false, eval("LIST_CONTAINS", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(true, eval("LIST_CONTAINS", "abc", "abc", null));  // abc/abc
    assertEquals(false, eval("LIST_CONTAINS", "abc", "z", null));  // abc/z
    assertEquals(true, eval("LIST_CONTAINS", "", "", null));  // empty/empty
    assertEquals(false, eval("LIST_CONTAINS", null, "x", null));  // null/x
    assertThrows(RuntimeException.class, () -> eval("LIST_CONTAINS", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(false, eval("LIST_CONTAINS", "10", "9", null));  // 10/9
    assertEquals(true, eval("LIST_CONTAINS", "10", "10", null));  // 10/10
    assertEquals(false, eval("LIST_CONTAINS", "ten", "9", null));  // ten/9
    assertThrows(RuntimeException.class, () -> eval("LIST_CONTAINS", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertThrows(RuntimeException.class, () -> eval("LIST_CONTAINS", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertThrows(RuntimeException.class, () -> eval("LIST_CONTAINS", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(true, eval("LIST_CONTAINS", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(true, eval("LIST_CONTAINS", List.of("a", "b"), "b", null));  // list arr/b
    assertEquals(false, eval("LIST_CONTAINS", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertEquals(false, eval("LIST_CONTAINS", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(true, eval("LIST_CONTAINS", "a", "a", null));  // scalar as list/a
    assertEquals(false, eval("LIST_CONTAINS", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(true, eval("LIST_CONTAINS", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(false, eval("LIST_CONTAINS", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(false, eval("LIST_CONTAINS", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_LIST_DOES_NOT_CONTAIN() {
    assertEquals(true, eval("LIST_DOES_NOT_CONTAIN", "abc", "b", null));  // abc/b
    assertEquals(false, eval("LIST_DOES_NOT_CONTAIN", "abc", "ABC", null));  // abc/ABC
    assertEquals(true, eval("LIST_DOES_NOT_CONTAIN", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(false, eval("LIST_DOES_NOT_CONTAIN", "abc", "abc", null));  // abc/abc
    assertEquals(true, eval("LIST_DOES_NOT_CONTAIN", "abc", "z", null));  // abc/z
    assertEquals(false, eval("LIST_DOES_NOT_CONTAIN", "", "", null));  // empty/empty
    assertEquals(true, eval("LIST_DOES_NOT_CONTAIN", null, "x", null));  // null/x
    assertThrows(RuntimeException.class, () -> eval("LIST_DOES_NOT_CONTAIN", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(true, eval("LIST_DOES_NOT_CONTAIN", "10", "9", null));  // 10/9
    assertEquals(false, eval("LIST_DOES_NOT_CONTAIN", "10", "10", null));  // 10/10
    assertEquals(true, eval("LIST_DOES_NOT_CONTAIN", "ten", "9", null));  // ten/9
    assertThrows(RuntimeException.class, () -> eval("LIST_DOES_NOT_CONTAIN", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertThrows(RuntimeException.class, () -> eval("LIST_DOES_NOT_CONTAIN", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertThrows(RuntimeException.class, () -> eval("LIST_DOES_NOT_CONTAIN", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(false, eval("LIST_DOES_NOT_CONTAIN", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(false, eval("LIST_DOES_NOT_CONTAIN", List.of("a", "b"), "b", null));  // list arr/b
    assertEquals(true, eval("LIST_DOES_NOT_CONTAIN", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertEquals(true, eval("LIST_DOES_NOT_CONTAIN", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(false, eval("LIST_DOES_NOT_CONTAIN", "a", "a", null));  // scalar as list/a
    assertEquals(true, eval("LIST_DOES_NOT_CONTAIN", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(false, eval("LIST_DOES_NOT_CONTAIN", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(true, eval("LIST_DOES_NOT_CONTAIN", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(true, eval("LIST_DOES_NOT_CONTAIN", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_LIST_IS_EMPTY() {
    assertEquals(false, eval("LIST_IS_EMPTY", "abc", "b", null));  // abc/b
    assertEquals(false, eval("LIST_IS_EMPTY", "abc", "ABC", null));  // abc/ABC
    assertEquals(false, eval("LIST_IS_EMPTY", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(false, eval("LIST_IS_EMPTY", "abc", "abc", null));  // abc/abc
    assertEquals(false, eval("LIST_IS_EMPTY", "abc", "z", null));  // abc/z
    assertEquals(false, eval("LIST_IS_EMPTY", "", "", null));  // empty/empty
    assertEquals(false, eval("LIST_IS_EMPTY", null, "x", null));  // null/x
    assertEquals(false, eval("LIST_IS_EMPTY", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(false, eval("LIST_IS_EMPTY", "10", "9", null));  // 10/9
    assertEquals(false, eval("LIST_IS_EMPTY", "10", "10", null));  // 10/10
    assertEquals(false, eval("LIST_IS_EMPTY", "ten", "9", null));  // ten/9
    assertEquals(false, eval("LIST_IS_EMPTY", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertEquals(false, eval("LIST_IS_EMPTY", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertEquals(false, eval("LIST_IS_EMPTY", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(false, eval("LIST_IS_EMPTY", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(false, eval("LIST_IS_EMPTY", List.of("a", "b"), "b", null));  // list arr/b
    assertEquals(true, eval("LIST_IS_EMPTY", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertEquals(true, eval("LIST_IS_EMPTY", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(false, eval("LIST_IS_EMPTY", "a", "a", null));  // scalar as list/a
    assertEquals(false, eval("LIST_IS_EMPTY", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(false, eval("LIST_IS_EMPTY", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(false, eval("LIST_IS_EMPTY", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(false, eval("LIST_IS_EMPTY", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_LIST_IS_NOT_EMPTY() {
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", "abc", "b", null));  // abc/b
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", "abc", "ABC", null));  // abc/ABC
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", "abc", "abc", null));  // abc/abc
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", "abc", "z", null));  // abc/z
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", "", "", null));  // empty/empty
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", null, "x", null));  // null/x
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", "10", "9", null));  // 10/9
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", "10", "10", null));  // 10/10
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", "ten", "9", null));  // ten/9
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(true, eval("LIST_IS_NOT_EMPTY", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(true, eval("LIST_IS_NOT_EMPTY", List.of("a", "b"), "b", null));  // list arr/b
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", "a", "a", null));  // scalar as list/a
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(false, eval("LIST_IS_NOT_EMPTY", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_EXISTS() {
    assertEquals(true, eval("EXISTS", "abc", "b", null));  // abc/b
    assertEquals(true, eval("EXISTS", "abc", "ABC", null));  // abc/ABC
    assertEquals(true, eval("EXISTS", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(true, eval("EXISTS", "abc", "abc", null));  // abc/abc
    assertEquals(true, eval("EXISTS", "abc", "z", null));  // abc/z
    assertEquals(false, eval("EXISTS", "", "", null));  // empty/empty
    assertEquals(false, eval("EXISTS", null, "x", null));  // null/x
    assertEquals(false, eval("EXISTS", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(true, eval("EXISTS", "10", "9", null));  // 10/9
    assertEquals(true, eval("EXISTS", "10", "10", null));  // 10/10
    assertEquals(true, eval("EXISTS", "ten", "9", null));  // ten/9
    assertEquals(true, eval("EXISTS", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertEquals(true, eval("EXISTS", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertEquals(true, eval("EXISTS", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(true, eval("EXISTS", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(true, eval("EXISTS", List.of("a", "b"), "b", null));  // list arr/b
    assertEquals(true, eval("EXISTS", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertEquals(true, eval("EXISTS", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(true, eval("EXISTS", "a", "a", null));  // scalar as list/a
    assertEquals(true, eval("EXISTS", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(true, eval("EXISTS", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(true, eval("EXISTS", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(true, eval("EXISTS", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  @Test
  void everyOperatorMatchesTheSourcesRecordedTable_DOES_NOT_EXIST() {
    assertEquals(false, eval("DOES_NOT_EXIST", "abc", "b", null));  // abc/b
    assertEquals(false, eval("DOES_NOT_EXIST", "abc", "ABC", null));  // abc/ABC
    assertEquals(false, eval("DOES_NOT_EXIST", "abc", "ABC", Boolean.TRUE));  // abc/ABC cs
    assertEquals(false, eval("DOES_NOT_EXIST", "abc", "abc", null));  // abc/abc
    assertEquals(false, eval("DOES_NOT_EXIST", "abc", "z", null));  // abc/z
    assertEquals(true, eval("DOES_NOT_EXIST", "", "", null));  // empty/empty
    assertEquals(true, eval("DOES_NOT_EXIST", null, "x", null));  // null/x
    assertEquals(true, eval("DOES_NOT_EXIST", Condition.UNDEFINED, "x", null));  // undef/x
    assertEquals(false, eval("DOES_NOT_EXIST", "10", "9", null));  // 10/9
    assertEquals(false, eval("DOES_NOT_EXIST", "10", "10", null));  // 10/10
    assertEquals(false, eval("DOES_NOT_EXIST", "ten", "9", null));  // ten/9
    assertEquals(false, eval("DOES_NOT_EXIST", Boolean.TRUE, Condition.UNDEFINED, null));  // true/–
    assertEquals(false, eval("DOES_NOT_EXIST", Boolean.FALSE, Condition.UNDEFINED, null));  // false/–
    assertEquals(false, eval("DOES_NOT_EXIST", Double.valueOf(0d), Condition.UNDEFINED, null));  // 0/–
    assertEquals(false, eval("DOES_NOT_EXIST", "[\"a\",\"b\"]", "b", null));  // list json/b
    assertEquals(false, eval("DOES_NOT_EXIST", List.of("a", "b"), "b", null));  // list arr/b
    assertEquals(false, eval("DOES_NOT_EXIST", List.of(), Condition.UNDEFINED, null));  // list empty/–
    assertEquals(false, eval("DOES_NOT_EXIST", "[]", Condition.UNDEFINED, null));  // list json empty/–
    assertEquals(false, eval("DOES_NOT_EXIST", "a", "a", null));  // scalar as list/a
    assertEquals(false, eval("DOES_NOT_EXIST", "2024-01-01", "2023-01-01", null));  // date 2024/2023
    assertEquals(false, eval("DOES_NOT_EXIST", "2024-01-01", "2024-01-01", null));  // date 2024/2024
    assertEquals(false, eval("DOES_NOT_EXIST", "not-a-date", "2024-01-01", null));  // date bad/2024
    assertEquals(false, eval("DOES_NOT_EXIST", linked("a", Double.valueOf(1d)), "b", null));  // obj/b
  }

  /**
   * SPEC-001 rule 21. The seven arrangements the source was asked for, not a sample:
   * `probes/conditions.probe.ts` Q8 ran exactly these against the real evaluator.
   */
  @Test
  void groupsAreOrOfAnds() {
    Condition t = Condition.of("BOOLEAN_IS_TRUE", true, null);
    Condition f = Condition.of("BOOLEAN_IS_FALSE", true, null);

    assertEquals(true, Conditions.evaluateConditions(List.of(List.of(t))));
    assertEquals(false, Conditions.evaluateConditions(List.of(List.of(f))));
    assertEquals(false, Conditions.evaluateConditions(List.of(List.of(t, f))));
    assertEquals(true, Conditions.evaluateConditions(List.of(List.of(t), List.of(f))));
    assertEquals(false, Conditions.evaluateConditions(List.of(List.of(f), List.of(f))));
    assertEquals(true, Conditions.evaluateConditions(List.of(List.of())));
    assertEquals(false, Conditions.evaluateConditions(List.of()));
  }

  /**
   * SPEC-001 rule 24. An unrecognised operator does not fail its branch — it passes it, which is
   * the opposite of what the name suggests and is why the source was asked rather than read.
   * `probes/conditions.probe.ts` Q9.
   */
  @Test
  void anAbsentOperatorRaisesAndAnUnknownOperatorPasses() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Conditions.evaluateConditions(List.of(List.of(Condition.of(null, "a", null)))));

    assertEquals(
        true, Conditions.evaluateConditions(List.of(List.of(Condition.of("NOPE", "a", null)))));
  }
}
