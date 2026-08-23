package io.akka.activepieces.bench;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.akka.activepieces.domain.Action;
import io.akka.activepieces.domain.Condition;
import io.akka.activepieces.domain.ExecutionOptions;
import io.akka.activepieces.domain.FlowEngine;
import io.akka.activepieces.domain.RunState;
import io.akka.activepieces.domain.StepOutput;
import io.akka.activepieces.domain.StepPath;
import io.akka.activepieces.domain.StepStatus;
import io.akka.activepieces.domain.Verdict;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the same {@code bench/workloads.json} the source side runs, and records the same shape of
 * answer, so section 1 of the report compares two sequences rather than two descriptions of one.
 *
 * <p>What is driven is {@link FlowEngine}: every rule in SPEC-001 section 3 lives there, and the
 * entity only turns commands into calls on it, so this drives the deciding code rather than a copy
 * of it. The retry wait is stood in for — a no-op sleeper — because rule 13's delays are seconds
 * and are not what this measures; the source side is given the same treatment through its
 * harness's retry constants.
 *
 * <p>Deliberately not a test. It writes files and reads a clock, and {@code mvn verify} should
 * depend on neither.
 */
public final class BenchRunner {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** A window aims for tens of milliseconds; the figure is the median of several windows. */
  private static final long TARGET_WINDOW_NS = 50_000_000L;

  private static final int WINDOWS = 5;
  private static final int WARMUP_RUNS = 2_000;

  /** Rule 13's waits are not what is being timed, and neither side spends them. */
  private static final ExecutionOptions OPTIONS =
      ExecutionOptions.defaults().withSleeper(millis -> {});

  private BenchRunner() {}

  public static void main(String[] args) throws Exception {
    Path bench = Path.of(args.length > 0 ? args[0] : "bench");
    JsonNode workloads = MAPPER.readTree(Files.readString(bench.resolve("workloads.json")));

    ObjectNode answers = MAPPER.createObjectNode();
    ObjectNode timings = MAPPER.createObjectNode();

    for (JsonNode workload : workloads) {
      String name = workload.get("name").asText();
      // One row per step, each carrying the field the workload declares must move, which is the
      // shape toolkit/sequence_probe.py reads.
      ArrayNode rows = MAPPER.createArrayNode();
      List<String> outcomes = run(workload);
      for (int i = 0; i < outcomes.size(); i++) {
        ObjectNode row = MAPPER.createObjectNode();
        row.put("step", i);
        row.put("outcome", outcomes.get(i));
        rows.add(row);
      }
      answers.set(name, rows);
      System.out.println(name + " => " + outcomes);

      if (workload.path("timed").asBoolean()) {
        timings.set(name, time(workload));
        System.out.println("  timing " + timings.get(name));
      }
    }

    ObjectNode answersDocument = MAPPER.createObjectNode();
    answersDocument.set("answers", answers);
    ObjectNode timingsDocument = MAPPER.createObjectNode();
    timingsDocument.set("timing", timings);
    Files.writeString(bench.resolve("port-answers.json"), answersDocument.toPrettyString() + "\n");
    Files.writeString(bench.resolve("port-timings.json"), timingsDocument.toPrettyString() + "\n");
    System.out.println("wrote port-answers.json and port-timings.json");
  }

  // ------------------------------------------------------------------ running a workload

  private static List<String> run(JsonNode workload) {
    RunState state = RunState.empty();
    List<String> outcomes = new ArrayList<>();
    if (workload.has("batches")) {
      for (JsonNode batch : workload.get("batches")) {
        state = FlowEngine.runChain(chain(batch), state, StepPath.EMPTY, OPTIONS);
        outcomes.add(summarise(state));
      }
      return outcomes;
    }
    for (JsonNode step : workload.get("steps")) {
      state = apply(state, step);
      outcomes.add(summarise(state));
    }
    return outcomes;
  }

  /** Chain a batch's actions through nextAction, so a batch is one walk of the executor. */
  private static Action chain(JsonNode batch) {
    Action head = null;
    for (int i = batch.size() - 1; i >= 0; i--) {
      ObjectNode node = batch.get(i).deepCopy();
      if (head == null) node.remove("next");
      head = buildWithNext(node, head);
    }
    return head;
  }

  private static Action buildWithNext(ObjectNode node, Action next) {
    Action built = build(node);
    return switch (built) {
      case Action.CodeAction c -> new Action.CodeAction(
          c.name(), c.displayName(), c.skip(), c.type(), c.handler(), c.continueOnFailure(),
          c.retryOnFailure(), c.maxAttempts(), c.retryExponential(), c.retryIntervalMs(),
          c.onSuccessAction(), c.onFailureAction(), next);
      case Action.LoopAction l -> new Action.LoopAction(
          l.name(), l.displayName(), l.skip(), l.items(), l.firstLoopAction(), next);
      case Action.RouterAction r -> new Action.RouterAction(
          r.name(), r.displayName(), r.skip(), r.executionType(), r.branches(), next);
    };
  }

  private static RunState apply(RunState state, JsonNode step) {
    return switch (step.get("op").asText()) {
      case "reset" -> RunState.empty();
      case "setStatus" -> {
        Map<String, StepOutput> steps = new LinkedHashMap<>(state.steps());
        String name = step.get("step").asText();
        steps.put(name, steps.get(name).withStatus(StepStatus.valueOf(step.get("status").asText())));
        yield state.withSteps(steps);
      }
      case "resume" ->
          RunState.empty()
              .withSteps(FlowEngine.filterForResume(state.steps(), step.get("reason").asText()));
      case "runChain" ->
          FlowEngine.runChain(build(step.get("flow")), state, StepPath.EMPTY, OPTIONS);
      default -> throw new IllegalArgumentException("unknown op " + step.get("op").asText());
    };
  }

  // ------------------------------------------------------------------ building the flow

  private static Action build(JsonNode node) {
    if (node == null || node.isNull()) return null;
    String kind = node.get("kind").asText();
    if (kind.equals("code")) {
      Object input = MAPPER.convertValue(node.get("input"), Object.class);
      boolean throws_ = "throw".equals(node.path("body").asText("echo"))
          || node.get("name").asText().startsWith("runtime");
      return new Action.CodeAction(
          node.get("name").asText(),
          node.get("name").asText(),
          node.path("skip").asBoolean(),
          "CODE",
          throws_
              ? in -> {
                  throw new RuntimeException("runtime fixture threw");
                }
              : in -> input,
          node.path("continueOnFailure").asBoolean(),
          node.path("retry").asBoolean(),
          node.path("maxAttempts").asInt(4),
          1,
          1,
          null,
          null,
          build(node.get("next")));
    }
    if (kind.equals("loop")) {
      List<Object> items = new ArrayList<>();
      for (JsonNode item : node.get("items")) items.add(MAPPER.convertValue(item, Object.class));
      return new Action.LoopAction(
          node.get("name").asText(),
          node.get("name").asText(),
          false,
          items,
          build(node.get("body")),
          build(node.get("next")));
    }
    List<Action.RouterBranch> branches = new ArrayList<>();
    for (JsonNode branch : node.get("branches")) {
      List<List<Condition>> groups = new ArrayList<>();
      for (JsonNode group : branch.path("conditions")) {
        List<Condition> conditions = new ArrayList<>();
        for (JsonNode c : group) {
          conditions.add(
              new Condition(
                  c.get("operator").asText(),
                  MAPPER.convertValue(c.get("firstValue"), Object.class),
                  c.has("secondValue")
                      ? MAPPER.convertValue(c.get("secondValue"), Object.class)
                      : null,
                  c.has("caseSensitive") ? c.get("caseSensitive").asBoolean() : null));
        }
        groups.add(conditions);
      }
      branches.add(
          new Action.RouterBranch(
              branch.get("branchName").asText(),
              branch.path("fallback").asBoolean(),
              groups,
              build(branch.get("first"))));
    }
    return new Action.RouterAction(
        node.get("name").asText(),
        node.get("name").asText(),
        false,
        Action.ExecutionType.valueOf(node.path("executionType").asText("EXECUTE_FIRST_MATCH")),
        branches,
        build(node.get("next")));
  }

  // ------------------------------------------------------------------ the canonical answer

  /**
   * One line per journal, in the shape the source side writes. Durations are left out on purpose:
   * a code step is a child process there and a handler call here, so comparing them would compare
   * what the two are made of rather than what they decided. Error text is left out for the same
   * reason — the shapes differ and the README says so; what is compared is that the same step
   * failed.
   */
  private static String summarise(RunState state) {
    String head =
        switch (state.verdict()) {
          case Verdict.Failed f -> "verdict=FAILED/failed=" + f.failedStep().name();
          case Verdict.LogSizeExceeded f ->
              "verdict=LOG_SIZE_EXCEEDED/failed=" + f.failedStep().name();
          case Verdict.Succeeded ignored -> "verdict=SUCCEEDED";
          case Verdict.Paused ignored -> "verdict=PAUSED";
          case Verdict.Running ignored -> "verdict=RUNNING";
        };
    return head + ";" + entries(state.steps());
  }

  private static String entries(Map<String, StepOutput> steps) {
    List<String> parts = new ArrayList<>();
    for (Map.Entry<String, StepOutput> entry : steps.entrySet()) {
      parts.add(entry(entry.getKey(), entry.getValue()));
    }
    return String.join(",", parts);
  }

  private static String entry(String name, StepOutput output) {
    if (output instanceof StepOutput.LoopOutput loop) {
      StringBuilder iterations = new StringBuilder();
      for (Map<String, StepOutput> iteration : loop.iterations()) {
        iterations.append('[').append(entries(iteration)).append(']');
      }
      return name + ":LOOP/" + loop.status() + "/index=" + loop.index()
          + "/item=" + json(loop.item()) + iterations;
    }
    if (output instanceof StepOutput.RouterOutput router) {
      List<String> branches = new ArrayList<>();
      for (StepOutput.Branch branch : router.branches()) {
        branches.add(branch.name() + "#" + branch.branchIndex() + "=" + branch.evaluation());
      }
      return name + ":ROUTER/" + router.status() + "/branches=" + String.join(";", branches);
    }
    StepOutput.LeafOutput leaf = (StepOutput.LeafOutput) output;
    return name + ":LEAF/" + leaf.status() + "/out=" + json(leaf.output());
  }

  /**
   * The source writes `undefined` where a step produced nothing, because that is what its journal
   * holds; this port holds a Java null in the same place. The two mean the same thing and the
   * canonical form says so with one word rather than leaving the comparison to disagree on it.
   */
  private static String json(Object value) {
    if (value == null) return "undefined";
    try {
      return MAPPER.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalStateException("cannot serialise " + value, e);
    }
  }

  // ------------------------------------------------------------------ timing

  private static ObjectNode time(JsonNode workload) {
    long pilotStart = System.nanoTime();
    run(workload);
    long pilotNs = Math.max(System.nanoTime() - pilotStart, 1);
    int repetitions = (int) Math.max(1, Math.ceilDiv(TARGET_WINDOW_NS, pilotNs));

    for (int i = 0; i < Math.min(WARMUP_RUNS, repetitions * 3L); i++) run(workload);

    long[] readings = new long[WINDOWS];
    for (int w = 0; w < WINDOWS; w++) {
      long start = System.nanoTime();
      for (int i = 0; i < repetitions; i++) run(workload);
      readings[w] = (System.nanoTime() - start) / repetitions;
    }
    java.util.Arrays.sort(readings);

    ObjectNode node = MAPPER.createObjectNode();
    node.put("nanosPerRun", readings[WINDOWS / 2]);
    node.put("windowNanos", readings[WINDOWS / 2] * repetitions);
    node.put("repetitions", repetitions);
    node.put("windows", WINDOWS);
    return node;
  }
}
