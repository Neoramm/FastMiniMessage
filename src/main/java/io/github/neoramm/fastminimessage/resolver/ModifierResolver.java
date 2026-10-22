package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentModifier;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.ModifiedPreComponent;
import io.github.neoramm.fastminimessage.api.ComponentModifier;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.RuntimeResolver;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.format.Style;

public final class ModifierResolver implements ComponentResolver, RuntimeResolver {
  private final String name;
  private final ComponentModifier modifier;

  public ModifierResolver(final String name, final ComponentModifier modifier) {
    this.name = name;
    this.modifier = modifier;
  }

  @Override
  public PreComponent resolve(String resolvedName, Arguments args, PreComponent[] children, Style inheritedStyle, MiniMessageCompiler compiler) {
    return new ModifiedPreComponent(modifier, children);
  }

  public ComponentModifier modifier() {
    return modifier;
  }

  @Override
  public String[] names() {
    return new String[]{name};
  }
}
