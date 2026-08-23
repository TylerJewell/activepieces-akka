package io.akka.activepieces.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.akka.activepieces.application.FlowRunEntity;
import io.akka.activepieces.domain.Action;
import io.akka.activepieces.domain.Condition;
import io.akka.activepieces.domain.DemoFlows;
import io.akka.activepieces.domain.StepOutput;
import io.akka.activepieces.domain.TriggerStep;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The port's flow and its runs, written in the shapes activepieces' own web interface reads.
 *
 * <p>RENDERING.md R3: the interface this port ships is the original's, so what changes is where it
 * gets its data, not what the data looks like. The flow definition here is rendered from the same
 * {@link DemoFlows#stepStateDemo()} tree the engine executes, so the boxes drawn on the canvas and
 * the steps that ran cannot describe different flows.
 */
final class ApShapes {

  static final ObjectMapper JSON = new ObjectMapper();

  static final String PROJECT_ID = "port-project";
  static final String PLATFORM_ID = "port-platform";
  static final String USER_ID = "port-user";
  static final String FLOW_ID = "step-state-demo";
  static final String FLOW_VERSION_ID = "step-state-demo-v1";
  static final String FLOW_DISPLAY_NAME = "Step state demo";

  /** A fixed instant for everything that is a property of the flow rather than of a run. */
  private static final String FLOW_TIMESTAMP = "2026-08-22T00:00:00.000Z";

  /**
   * The flow this port runs does not change, and every row of the run list carries the whole
   * version the way the original's rows do. Rendered once: a list of fifty runs otherwise walks
   * the same action tree fifty times, on every frame the list's open connection sends.
   */
  private static final ObjectNode FLOW_VERSION = buildFlowVersion();

  /** Declared after FLOW_VERSION: it embeds it, and a static field reads as null before its turn. */
  private static final ObjectNode POPULATED_FLOW = buildPopulatedFlow();

  private ApShapes() {}

  // ------------------------------------------------------------------ the flow definition

  static ObjectNode populatedFlow() {
    return POPULATED_FLOW;
  }

  static ObjectNode flowVersion() {
    return FLOW_VERSION;
  }

  private static ObjectNode buildPopulatedFlow() {
    ObjectNode flow = JSON.createObjectNode();
    flow.put("id", FLOW_ID);
    flow.put("created", FLOW_TIMESTAMP);
    flow.put("updated", FLOW_TIMESTAMP);
    flow.put("projectId", PROJECT_ID);
    flow.put("externalId", FLOW_ID);
    flow.put("ownerId", USER_ID);
    flow.putNull("folderId");
    flow.put("status", "ENABLED");
    flow.put("publishedVersionId", FLOW_VERSION_ID);
    flow.putNull("metadata");
    flow.put("operationStatus", "NONE");
    flow.putNull("timeSavedPerRun");
    flow.putNull("templateId");
    flow.putNull("createdBy");
    flow.set("version", flowVersion());
    return flow;
  }

  private static ObjectNode buildFlowVersion() {
    ObjectNode version = JSON.createObjectNode();
    version.put("id", FLOW_VERSION_ID);
    version.put("created", FLOW_TIMESTAMP);
    version.put("updated", FLOW_TIMESTAMP);
    version.put("flowId", FLOW_ID);
    version.put("displayName", FLOW_DISPLAY_NAME);
    version.put("schemaVersion", "23");
    version.set("trigger", trigger(DemoFlows.stepStateDemo()));
    version.set("connectionIds", JSON.createArrayNode());
    version.set("agentIds", JSON.createArrayNode());
    version.put("updatedBy", USER_ID);
    version.put("valid", true);
    version.put("state", "LOCKED");
    version.putNull("backupFiles");
    version.set("notes", JSON.createArrayNode());
    return version;
  }

  private static ObjectNode trigger(TriggerStep step) {
    ObjectNode node = JSON.createObjectNode();
    node.put("name", step.name());
    node.put("type", step.type());
    node.put("valid", true);
    ObjectNode settings = JSON.createObjectNode();
    ObjectNode input = JSON.createObjectNode();
    input.put("authType", "none");
    settings.set("input", input);
    settings.put("pieceName", "@activepieces/piece-webhook");
    settings.put("triggerName", "catch_webhook");
    settings.put("pieceVersion", "0.1.41");
    settings.set("propertySettings", JSON.createObjectNode());
    node.set("settings", settings);
    if (step.nextAction() != null) node.set("nextAction", action(step.nextAction()));
    node.put("displayName", step.displayName());
    node.put("lastUpdatedDate", FLOW_TIMESTAMP);
    return node;
  }

  private static ObjectNode action(Action action) {
    ObjectNode node = JSON.createObjectNode();
    node.put("name", action.name());
    node.put("valid", true);
    if (action instanceof Action.CodeAction code) {
      node.put("type", "CODE");
      ObjectNode settings = JSON.createObjectNode();
      settings.set("input", JSON.createObjectNode());
      settings.set("sampleData", JSON.createObjectNode());
      ObjectNode source = JSON.createObjectNode();
      source.put("code", DemoFlows.bodyTextOf(code.name()));
      source.put("packageJson", "{}");
      settings.set("sourceCode", source);
      ObjectNode errorHandling = JSON.createObjectNode();
      errorHandling.set("retryOnFailure", flag(code.retryOnFailure()));
      errorHandling.set("continueOnFailure", flag(code.continueOnFailure()));
      settings.set("errorHandlingOptions", errorHandling);
      node.set("settings", settings);
    } else if (action instanceof Action.LoopAction loop) {
      node.put("type", "LOOP_ON_ITEMS");
      ObjectNode settings = JSON.createObjectNode();
      settings.put("items", String.valueOf(loop.items()));
      settings.set("sampleData", JSON.createObjectNode());
      node.set("settings", settings);
      if (loop.firstLoopAction() != null) {
        node.set("firstLoopAction", action(loop.firstLoopAction()));
      }
    } else {
      Action.RouterAction router = (Action.RouterAction) action;
      node.put("type", "ROUTER");
      ArrayNode children = JSON.createArrayNode();
      ArrayNode branches = JSON.createArrayNode();
      for (Action.RouterBranch branch : router.branches()) {
        if (branch.firstAction() == null) children.addNull();
        else children.add(action(branch.firstAction()));
        branches.add(branchNode(branch));
      }
      node.set("children", children);
      ObjectNode settings = JSON.createObjectNode();
      settings.set("branches", branches);
      settings.set("sampleData", JSON.createObjectNode());
      settings.put("executionType", router.executionType().name());
      node.set("settings", settings);
    }
    if (action.nextAction() != null) node.set("nextAction", action(action.nextAction()));
    node.put("displayName", action.displayName());
    node.put("lastUpdatedDate", FLOW_TIMESTAMP);
    return node;
  }

  private static ObjectNode branchNode(Action.RouterBranch branch) {
    ObjectNode node = JSON.createObjectNode();
    node.put("branchName", branch.name());
    if (branch.fallback()) {
      node.put("branchType", "FALLBACK");
      return node;
    }
    node.put("branchType", "CONDITION");
    ArrayNode groups = JSON.createArrayNode();
    for (List<Condition> group : branch.conditionGroups()) {
      ArrayNode conditions = JSON.createArrayNode();
      for (Condition condition : group) {
        ObjectNode c = JSON.createObjectNode();
        c.put("operator", apOperator(condition.operator()));
        c.put("firstValue", String.valueOf(condition.firstValue()));
        c.put("secondValue", String.valueOf(condition.secondValue()));
        c.put("caseSensitive", condition.isCaseSensitive());
        conditions.add(c);
      }
      groups.add(conditions);
    }
    node.set("conditions", groups);
    return node;
  }

  /** The one operator whose wire name differs from its enum name in the original. */
  private static String apOperator(String operator) {
    return "TEXT_STARTS_WITH".equals(operator) ? "TEXT_START_WITH" : operator;
  }

  private static ObjectNode flag(boolean value) {
    ObjectNode node = JSON.createObjectNode();
    node.put("value", value);
    return node;
  }

  // ------------------------------------------------------------------ a run

  static ObjectNode flowRun(String runId, FlowRunEntity.State state, boolean withSteps) {
    ObjectNode run = JSON.createObjectNode();
    run.put("id", runId);
    run.put("created", iso(state.startedAtMillis()));
    run.put(
        "updated",
        iso(state.finishedAtMillis() == 0 ? state.startedAtMillis() : state.finishedAtMillis()));
    run.put("projectId", PROJECT_ID);
    run.put("flowId", FLOW_ID);
    run.put("flowVersionId", FLOW_VERSION_ID);
    run.put("environment", "PRODUCTION");
    // Where this run's journal is. The original names a file in its log store; here the journal
    // lives in the run's own entity, so the run's id is the answer. The interface reads this as
    // "there are logs" and refuses to draw a step's output without it.
    run.put("logsFileId", runId);
    run.putNull("parentRunId");
    run.put("failParentOnFailure", false);
    run.put("status", state.status());
    run.set("tags", JSON.createArrayNode());
    run.put("startTime", iso(state.startedAtMillis()));
    run.putNull("triggeredBy");
    if (state.finishedAtMillis() == 0) run.putNull("finishTime");
    else run.put("finishTime", iso(state.finishedAtMillis()));
    run.set("timeline", timeline(state));
    if (state.failedStep() == null) {
      run.putNull("failedStep");
    } else {
      ObjectNode failed = JSON.createObjectNode();
      failed.put("name", state.failedStep().name());
      failed.put("message", state.failedStep().message());
      failed.put("displayName", state.failedStep().displayName());
      run.set("failedStep", failed);
    }
    run.putNull("archivedAt");
    run.putNull("stepNameToTest");
    run.put("stepsCount", state.stepsCount());
    run.putNull("pauseMetadata");
    run.set("flowVersion", flowVersion());
    if (withSteps) run.set("steps", steps(state.steps()));
    return run;
  }

  /**
   * The original reports four legs — queue, provision, boot and run — because a run there is
   * handed to a worker before it starts. This port executes in process, so the only leg it can
   * measure is the run itself, and the others would be invented.
   */
  private static ObjectNode timeline(FlowRunEntity.State state) {
    long end = state.finishedAtMillis() == 0 ? System.currentTimeMillis() : state.finishedAtMillis();
    long durationMs = state.startedAtMillis() == 0 ? 0 : end - state.startedAtMillis();
    ObjectNode leg = JSON.createObjectNode();
    leg.put("name", "RUN");
    leg.put("durationMs", durationMs);
    ArrayNode inner = JSON.createArrayNode();
    inner.add(leg);
    ArrayNode legs = JSON.createArrayNode();
    legs.add(inner);
    ObjectNode timeline = JSON.createObjectNode();
    timeline.set("legs", legs);
    return timeline;
  }

  static ObjectNode steps(Map<String, StepOutput> steps) {
    ObjectNode node = JSON.createObjectNode();
    for (Map.Entry<String, StepOutput> entry : steps.entrySet()) {
      node.set(entry.getKey(), stepOutput(entry.getValue()));
    }
    return node;
  }

  private static ObjectNode stepOutput(StepOutput output) {
    ObjectNode node = JSON.createObjectNode();
    if (output instanceof StepOutput.LeafOutput leaf) {
      node.put("type", leaf.type());
      node.put("status", leaf.status().name());
      node.set("input", objectOrEmpty(leaf.input()));
      node.set("output", value(leaf.output()));
      if (leaf.errorMessage() != null) node.put("errorMessage", stepError(leaf.errorMessage()));
      node.put("duration", leaf.durationMs());
    } else if (output instanceof StepOutput.LoopOutput loop) {
      node.put("type", "LOOP_ON_ITEMS");
      node.put("status", loop.status().name());
      ObjectNode input = JSON.createObjectNode();
      input.set("items", value(loop.input()));
      node.set("input", input);
      ObjectNode body = JSON.createObjectNode();
      body.set("item", value(loop.item()));
      body.put("index", loop.index());
      ArrayNode iterations = JSON.createArrayNode();
      for (Map<String, StepOutput> iteration : loop.iterations()) iterations.add(steps(iteration));
      body.set("iterations", iterations);
      node.set("output", body);
      if (loop.errorMessage() != null) node.put("errorMessage", loop.errorMessage());
      node.put("duration", loop.durationMs());
    } else {
      StepOutput.RouterOutput router = (StepOutput.RouterOutput) output;
      node.put("type", "ROUTER");
      node.put("status", router.status().name());
      node.set("input", objectOrEmpty(router.input()));
      ObjectNode body = JSON.createObjectNode();
      ArrayNode branches = JSON.createArrayNode();
      for (StepOutput.Branch branch : router.branches()) {
        ObjectNode b = JSON.createObjectNode();
        b.put("branchName", branch.name());
        b.put("branchIndex", branch.branchIndex());
        b.put("evaluation", branch.evaluation());
        branches.add(b);
      }
      body.set("branches", branches);
      node.set("output", body);
      if (router.errorMessage() != null) node.put("errorMessage", router.errorMessage());
      node.put("duration", router.durationMs());
    }
    return node;
  }

  /**
   * How the interface reads a failed step: a tagged envelope it unpacks into a message and a
   * technical-details panel, rather than a bare string it would print verbatim. The message is
   * this port's; only the envelope is the interface's contract. {@code raw} would carry the
   * original's stack trace, and this port has none to offer, so it carries the message again.
   */
  private static String stepError(String message) {
    ObjectNode envelope = JSON.createObjectNode();
    envelope.put("__apErrorVersion", 1);
    envelope.put("message", message);
    envelope.put("raw", message);
    return envelope.toString();
  }

  private static JsonNode objectOrEmpty(Object raw) {
    return raw == null ? JSON.createObjectNode() : JSON.valueToTree(raw);
  }

  private static JsonNode value(Object raw) {
    return raw == null ? JSON.nullNode() : JSON.valueToTree(raw);
  }

  static String iso(long millis) {
    return Instant.ofEpochMilli(millis == 0 ? System.currentTimeMillis() : millis).toString();
  }

  static ObjectNode seekPage(ArrayNode data) {
    ObjectNode page = JSON.createObjectNode();
    page.set("data", data);
    page.putNull("next");
    page.putNull("previous");
    return page;
  }
}
