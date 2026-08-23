package io.akka.activepieces.domain;

import java.util.List;

/**
 * Flow definitions the endpoints can run, to exercise the engine end to end.
 *
 * <p>{@code step-state-demo} is the one the rendered surface shows. It is the same flow the
 * original was driven with when the baseline screens were captured — a webhook trigger, a code
 * step, a loop over three items, a router with a matching branch and a fallback, and a step that
 * always throws with retry on — so the two interfaces are photographed showing the same run.
 */
public final class DemoFlows {

  public static final String STEP_STATE_DEMO = "step-state-demo";

  /** What a code step's body is, as the surface displays it beside the step. */
  public static final String ECHO_SOURCE =
      "export const code = async (inputs) => { return { ok: true, at: 'step' }; };";

  public static final String THROW_SOURCE =
      "export const code = async (inputs) => { throw new Error('this step always fails'); };";

  public static final Object DEMO_PAYLOAD =
      java.util.Map.of(
          "method", "POST",
          "body", java.util.Map.of("items", List.of("alpha", "beta", "gamma"), "greeting", "hello world"),
          "queryParams", java.util.Map.of());

  /** Every scenario {@link #byName} answers to, in the order they are offered. */
  public static final List<String> NAMES =
      List.of(STEP_STATE_DEMO, "happy", "retry-then-fail", "loop", "router");

  private DemoFlows() {}

  public static TriggerStep byName(String scenario) {
    return switch (scenario) {
      case STEP_STATE_DEMO -> stepStateDemo();
      case "happy" -> happy();
      case "retry-then-fail" -> retryThenFail();
      case "loop" -> loopOverItems();
      case "router" -> router();
      default -> throw new IllegalArgumentException("unknown scenario: " + scenario);
    };
  }

  /** The flow behind the rendered surface. */
  public static TriggerStep stepStateDemo() {
    Action step4 = new Action.CodeAction(
        "step_4", "Always fails", false, "CODE", Handlers.throwing("Error: this step always fails"),
        false, true, 4, 2, 2000, null, null, null);

    Action step3 = new Action.CodeAction(
        "step_3", "Branch step", false, "CODE", Handlers.echo(echoOutput()),
        false, false, 4, 2, 2000, null, null, null);
    Action.RouterBranch matched = new Action.RouterBranch(
        "Matched", false,
        List.of(List.of(Condition.of("TEXT_STARTS_WITH", "hello world", "hello"))),
        step3);
    Action.RouterBranch otherwise = new Action.RouterBranch("Otherwise", true, List.of(), null);
    Action router = new Action.RouterAction(
        "router_1", "Router", false, Action.ExecutionType.EXECUTE_FIRST_MATCH,
        List.of(matched, otherwise), step4);

    Action step2 = new Action.CodeAction(
        "step_2", "Process item", false, "CODE", Handlers.echo(echoOutput()),
        false, false, 4, 2, 2000, null, null, null);
    Action loop = new Action.LoopAction(
        "loop_1", "For each item", false, List.of("alpha", "beta", "gamma"), step2, router);

    Action step1 = new Action.CodeAction(
        "step_1", "First step", false, "CODE", Handlers.echo(echoOutput()),
        false, false, 4, 2, 2000, null, null, loop);

    return new TriggerStep("trigger", "Catch Webhook", "PIECE_TRIGGER", step1);
  }

  /**
   * The body text a code step shows beside itself. The engine runs a handler rather than this
   * text (SPEC-001 §1), so the two are kept together here rather than inside {@link Action}.
   */
  public static String bodyTextOf(String stepName) {
    return "step_4".equals(stepName) ? THROW_SOURCE : ECHO_SOURCE;
  }

  private static java.util.Map<String, Object> echoOutput() {
    return java.util.Map.of("ok", true, "at", "step");
  }

  private static TriggerStep happy() {
    Action step2 = new Action.CodeAction(
        "step2", "Step 2", false, "CODE", Handlers.echo("done"), false, false, 4, 2, 2000, null, null, null);
    Action step1 = new Action.CodeAction(
        "step1", "Step 1", false, "CODE", Handlers.echo("hello"), false, false, 4, 2, 2000, null, null, step2);
    return new TriggerStep("trigger", "Trigger", "PIECE_TRIGGER", step1);
  }

  private static TriggerStep retryThenFail() {
    Action step1 = new Action.CodeAction(
        "step1", "Step 1", false, "CODE", Handlers.throwing("always fails"), false, true, 3, 2, 2000, null, null, null);
    return new TriggerStep("trigger", "Trigger", "PIECE_TRIGGER", step1);
  }

  private static TriggerStep loopOverItems() {
    Action body = new Action.CodeAction(
        "process", "Process", false, "CODE", Handlers.echo("processed"), false, false, 4, 2, 2000, null, null, null);
    Action loop = new Action.LoopAction("loop", "For each item", false, List.of("a", "b", "c"), body, null);
    return new TriggerStep("trigger", "Trigger", "PIECE_TRIGGER", loop);
  }

  private static TriggerStep router() {
    Action branchTrue = new Action.CodeAction(
        "onTrue", "On true", false, "CODE", Handlers.echo("branch ran"), false, false, 4, 2, 2000, null, null, null);
    Action.RouterBranch trueBranch = new Action.RouterBranch(
        "trueBranch", false, List.of(List.of(Condition.of("BOOLEAN_IS_TRUE", true, null))), branchTrue);
    Action.RouterBranch fallback = new Action.RouterBranch("fallback", true, List.of(), null);
    Action router = new Action.RouterAction(
        "router", "Router", false, Action.ExecutionType.EXECUTE_FIRST_MATCH, List.of(trueBranch, fallback), null);
    return new TriggerStep("trigger", "Trigger", "PIECE_TRIGGER", router);
  }
}
