package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;

public final class GroupPreComponent extends PreComponent {

  public GroupPreComponent(PreComponent[] children) {
    this.children = children;
  }

  @Override
  public Component render(Pointered pointered, RenderScope scope) {
    final PreComponent[] kids = children;
    if (kids == null || kids.length == 0) return Component.empty().style(computedStyle != null ? computedStyle : Style.empty());
    final Style style = computedStyle;
    if (kids.length == 1) {
      final Component child = kids[0].render(pointered, scope);
      if (style == null || style.isEmpty()) return child;
      return Component.text().style(style).append(child).build();
    }
    final TextComponent.Builder builder = Component.text();
    if (style != null && !style.isEmpty()) builder.style(style);
    for (final PreComponent child : kids) child.renderInto(pointered, builder, scope);
    return builder.build();
  }

  @Override
  public void renderInto(final Pointered pointered, final TextComponent.Builder builder) {
    renderInto(pointered, builder, RenderScope.EMPTY);
  }

  @Override
  public void renderInto(final Pointered pointered, final TextComponent.Builder builder, final RenderScope scope) {
    final Style style = computedStyle;
    if (style != null && !style.isEmpty()) {
      builder.append(render(pointered, scope));
      return;
    }
    final PreComponent[] kids = children;
    if (kids == null) return;
    for (final PreComponent child : kids) child.renderInto(pointered, builder, scope);
  }

  @Override
  public boolean isStatic() {
    return childrenAreStatic();
  }

  @Override
  public PreComponent deepCopy() {
    PreComponent[] copied = new PreComponent[children.length];
    for (int i = 0; i < children.length; i++) {
      copied[i] = children[i].deepCopy();
    }
    return new GroupPreComponent(copied).setStyle(computedStyle);
  }
}
