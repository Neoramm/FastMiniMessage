package io.github.neoramm.fastminimessage.api;

import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import net.kyori.adventure.text.format.Style;

/**
 * Resolves a MiniMessage-style tag into a precompiled component.
 */
public interface ComponentResolver {

  /**
   * Builds the component produced by this tag.
   *
   * @param name the normalized tag name
   * @param args the parsed tag arguments
   * @param children the already-resolved child components
   * @param inheritedStyle the style inherited from enclosing tags
   * @param compiler the compiler driving the resolution
   * @return the resolved component
   */
  PreComponent resolve(
    String name,
    Arguments args,
    PreComponent[] children,
    Style inheritedStyle,
    MiniMessageCompiler compiler
  );

  /**
   * Returns every tag name handled by this resolver.
   */
  String[] names();

  /**
   * Relative priority used when two resolvers claim the same name.
   */
  default int priority() {
    return 0;
  }

  /**
   * Whether the tag is self-closing and takes no children.
   */
  default boolean isSelfClosing() {
    return false;
  }
}