package io.github.neoramm.fastminimessage.example;

import io.github.neoramm.fastminimessage.api.ComponentModifier;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Example (non-registered) component modifiers for documentation purposes.
 */
public final class ComponentModifiers {
  private ComponentModifiers() {
  }

  private static String plain(Component c) {
    StringBuilder sb = new StringBuilder();
    if (c instanceof TextComponent t) sb.append(t.content());
    for (Component ch : c.children()) sb.append(plain(ch));
    return sb.toString();
  }

  public static ComponentModifier uppercase() {
    return (rendered, context) -> Component.text(plain(rendered).toUpperCase(), rendered.style());
  }

  public static ComponentModifier staticColor(final NamedTextColor color) {
    return new ComponentModifier() {
      @Override
      public Component modify(Component rendered, Pointered context) {
        return rendered.colorIfAbsent(color);
      }

      @Override
      public boolean isStatic() {
        return true;
      }
    };
  }

  public static ComponentModifier boldAll() {
    return (rendered, context) -> rendered.style(s -> s.decoration(TextDecoration.BOLD, TextDecoration.State.TRUE));
  }
}
