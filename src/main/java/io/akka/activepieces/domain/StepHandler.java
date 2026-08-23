package io.akka.activepieces.domain;

/**
 * A code or piece step's body. SPEC-001 §1 ships two handlers — an echo and a thrower — since a
 * piece integrating somebody else's service is out of scope.
 */
public interface StepHandler {
  Object run(Object input) throws Exception;
}
