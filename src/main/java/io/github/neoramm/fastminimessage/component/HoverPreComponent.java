package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.pointer.Pointered;

import java.util.function.BiFunction;

public final class HoverPreComponent extends PreComponent {
  private final BiFunction<Pointered, RenderScope, HoverEvent<?>> eventFunction;

  public HoverPreComponent(BiFunction<Pointered, RenderScope, HoverEvent<?>> eventFunction, PreComponent[] children) {
    this.eventFunction = eventFunction;
    this.children = children;
  }

  @Override
  public Component render(Pointered pointered, RenderScope scope) {
    TextComponent.Builder builder = Component.text()
      .style(computedStyle)
      .hoverEvent(eventFunction.apply(pointered, scope));
    for (PreComponent child : getChildren()) {
      child.renderInto(pointered, builder, scope);
    }
    return builder.build();
  }

  @Override
  public PreComponent deepCopy() {
    PreComponent[] copied = new PreComponent[children.length];
    for (int i = 0; i < children.length; i++) {
      copied[i] = children[i].deepCopy();
    }
    return new HoverPreComponent(eventFunction, copied).setStyle(computedStyle);
  }
}