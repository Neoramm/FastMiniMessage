package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Nullable;

public final class ColorPreComponent extends PreComponent {
  private final TextColor color;

  public ColorPreComponent(TextColor color, PreComponent[] children) {
    this.color = color;
    this.children = children;
  }

  @Override
  public Component render(Pointered pointered, RenderScope scope) {
    TextComponent.Builder builder = Component.text();
    final Style style = computedStyle;
    if (style != null && !style.isEmpty()) builder.style(style);
    builder.color(color);
    final PreComponent[] kids = children;
    if (kids != null) {
      for (final PreComponent child : kids) {
        child.renderInto(pointered, builder, scope);
      }
    }
    return builder.build();
  }

  @Override
  public boolean isStatic() {
    return childrenAreStatic();
  }

  @Nullable TextColor getColor() {
    return color;
  }

  @Override
  public PreComponent deepCopy() {
    PreComponent[] copied = new PreComponent[children.length];
    for (int i = 0; i < children.length; i++) {
      copied[i] = children[i].deepCopy();
    }
    return new ColorPreComponent(color, copied).setStyle(computedStyle);
  }
}