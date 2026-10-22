package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.StyleInheritance;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.ColorPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;

public final class ColorResolver implements ComponentResolver {
  private final String[] names;
  private final TextColor color;

  public ColorResolver(String name, TextColor color) {
    this.names = new String[]{name};
    this.color = color;
  }

  @Override
  public PreComponent resolve(String name, Arguments args, PreComponent[] children, Style inheritedStyle, MiniMessageCompiler compiler) {
    if (name.equals("color")) {
      final String color = args.get(0);
      final TextColor resolved = resolveColor(color);
      final Style own = Style.style().color(resolved).build();
      if (StyleInheritance.mergeIntoOnlyLiteral(children, inheritedStyle, own)) return children[0];
      return new ColorPreComponent(resolved, children);
    }
    final Style own = Style.style().color(color).build();
    if (StyleInheritance.mergeIntoOnlyLiteral(children, inheritedStyle, own)) return children[0];
    return new ColorPreComponent(color, children);
  }

  static TextColor resolveColor(String colorName) {
    if (colorName == null || colorName.isEmpty()) {
      return null;
    }
    final TextColor color;
    if (colorName.charAt(0) == TextColor.HEX_CHARACTER) {
      color = TextColor.fromHexString(colorName);
    } else {
      color = NamedTextColor.NAMES.value(colorName);
    }
    return color;
  }

  @Override
  public String[] names() {
    return names;
  }
}