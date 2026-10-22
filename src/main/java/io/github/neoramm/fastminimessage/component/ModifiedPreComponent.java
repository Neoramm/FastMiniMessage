package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.api.ComponentModifier;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ModifiedPreComponent extends PreComponent {
  private final @NotNull ComponentModifier modifier;
  private @Nullable Component cached;

  public ModifiedPreComponent(final @NotNull ComponentModifier modifier, final @NotNull PreComponent[] children) {
    this.modifier = modifier;
    this.children = children;
  }

  @Override
  public @NotNull Component render(final @NotNull Pointered pointered, final @NotNull RenderScope scope) {
    final boolean cacheable = isStatic() && scope.isEmpty();
    if (cacheable) {
      final Component cached = this.cached;
      if (cached != null) return cached;
    }
    final PreComponent[] kids = children;
    final TextComponent.Builder builder = Component.text();
    final Style style = computedStyle != null ? computedStyle : Style.empty();
    if (!style.isEmpty()) builder.style(style);
    if (kids != null) {
      for (final PreComponent child : kids) child.renderInto(pointered, builder, scope);
    }
    final Component rendered = builder.build();
    final Component modified = modifier.modify(rendered, pointered);
    final Component result = style.isEmpty() ? modified : modified.style(style.merge(modified.style()));
    if (cacheable) this.cached = result;
    return result;
  }

  @Override
  public void renderInto(final @NotNull Pointered pointered, final @NotNull TextComponent.Builder builder) {
    builder.append(render(pointered));
  }

  @Override
  public boolean isStatic() {
    return modifier.isStatic() && childrenAreStatic();
  }

  @Override
  public @NotNull PreComponent setStyle(final @Nullable Style style) {
    cached = null;
    return super.setStyle(style);
  }

  @Override
  public @NotNull PreComponent setChildren(final @NotNull PreComponent[] children) {
    cached = null;
    return super.setChildren(children);
  }

  @Override
  public @NotNull PreComponent deepCopy() {
    final PreComponent[] copied = new PreComponent[children.length];
    for (int i = 0; i < children.length; i++) copied[i] = children[i].deepCopy();
    final ModifiedPreComponent copy = new ModifiedPreComponent(modifier, copied);
    copy.setStyle(computedStyle != null ? computedStyle : Style.empty());
    return copy;
  }
}
