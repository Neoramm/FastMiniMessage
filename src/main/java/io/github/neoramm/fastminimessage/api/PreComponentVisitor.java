package io.github.neoramm.fastminimessage.api;

import io.github.neoramm.fastminimessage.model.PreComponent;

/**
 * Visits every node of a precompiled component tree.
 */
public interface PreComponentVisitor {
  /**
   * Called for each component in the tree.
   *
   * @param component the visited component
   */
  void visit(PreComponent component);
}