package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class UnparsedPreComponent extends PreComponent {
  private final @NotNull String unparsedTag;
  private final @Nullable String fallback;
  private @Nullable PreComponent parsedComponent;
  private @Nullable Component cachedFallback;

  public UnparsedPreComponent(final @NotNull String unparsedTag) {
    this(unparsedTag, null);
  }

  public UnparsedPreComponent(final @NotNull String unparsedTag, final @Nullable String fallback) {
    this.unparsedTag = unparsedTag;
    this.fallback = fallback;
  }

  @Override
  public @NotNull Component render(final @NotNull Pointered pointered, final @NotNull RenderScope scope) {
    final Style style = computedStyle;
    final PreComponent parsed = parsedComponent;
    if (parsed != null) {
      final Component rendered = parsed.render(pointered, scope);
      if (style == null || style.isEmpty()) return rendered;
      return rendered.style(style.merge(rendered.style()));
    }
    final PreComponent placeholder = scope.resolve(unparsedTag);
    if (placeholder != null) {
      final Component rendered = placeholder.render(pointered, scope);
      if (style == null || style.isEmpty()) return rendered;
      return rendered.style(style.merge(rendered.style()));
    }
    Component cached = cachedFallback;
    if (cached == null) {
      final Style effective = style != null ? style : Style.empty();
      cached = fallback != null ? Component.text(fallback, effective) : Component.empty().style(effective);
      cachedFallback = cached;
    }
    return cached;
  }

  @Override
  public @NotNull PreComponent setStyle(final @Nullable Style style) {
    cachedFallback = null;
    return super.setStyle(style);
  }

  @Override
  public @NotNull PreComponent deepCopy() {
    final UnparsedPreComponent copy = new UnparsedPreComponent(unparsedTag, fallback);
    copy.parsedComponent = parsedComponent;
    copy.setStyle(computedStyle);
    return copy;
  }

  @Override
  public boolean isStatic() {
    final PreComponent parsed = parsedComponent;
    return parsed == null || parsed.isStatic();
  }

  public @NotNull String getUnparsedTag() {
    return unparsedTag;
  }

  public void setParsedComponent(final @Nullable PreComponent parsedComponent) {
    this.parsedComponent = parsedComponent;
  }
}
