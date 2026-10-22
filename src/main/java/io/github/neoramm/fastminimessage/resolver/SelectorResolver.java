/**
 * Resolver for the SelectorResolver tag.
 */
package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.StaticComponentPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;

public final class SelectorResolver implements ComponentResolver {
  @Override
  public PreComponent resolve(String name, Arguments args, PreComponent[] children, Style inheritedStyle, MiniMessageCompiler compiler) {
    if (args.isEmpty()) throw new IllegalArgumentException("A selection key is required");
    String key = args.get(0);
    Component separator = null;
    if (args.size() > 1 && args.get(1) != null) {
      separator = compiler.deserialize(args.get(1));
    }
    Component selector = separator == null ? Component.selector(key) : Component.selector(key, separator);
    return new StaticComponentPreComponent(selector);
  }

  @Override
  public String[] names() {
    return new String[]{"selector", "sel"};
  }

  @Override
  public boolean isSelfClosing() {
    return true;
  }
}
