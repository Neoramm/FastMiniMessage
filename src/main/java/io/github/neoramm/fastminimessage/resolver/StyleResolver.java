package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.GroupPreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.StyleInheritance;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

public final class StyleResolver implements ComponentResolver {
  private final String[] names;
  private final TextDecoration decoration;

  public StyleResolver(TextDecoration decoration, String... names) {
    this.decoration = decoration;
    this.names = names;
  }

  @Override
  public PreComponent resolve(String name, Arguments args, PreComponent[] children, Style inheritedStyle, MiniMessageCompiler compiler) {
    boolean negate = name.startsWith("!");
    Style own = Style.style().decoration(decoration, !negate).build();
    Style merged = StyleInheritance.compute(own, inheritedStyle);
    if (children.length == 0) {
      return new LiteralPreComponent("", merged);
    }
    if (StyleInheritance.mergeIntoOnlyLiteral(children, inheritedStyle, own)) return children[0];
    GroupPreComponent group = new GroupPreComponent(children);
    group.setStyle(merged);
    return group;
  }

  @Override
  public String[] names() {
    return names;
  }
}