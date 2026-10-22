package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.HSVLike;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GradientPreComponent extends PreComponent {
  private static final String[] ASCII = new String[128];

  static {
    for (int i = 0; i < ASCII.length; i++) ASCII[i] = String.valueOf((char) i);
  }

  private final TextColor[] colors;
  private final float phase;
  private final boolean rainbow;
  private final boolean reversed;
  private Component cached;
  private volatile StyleCache styleCache;

  public GradientPreComponent(TextColor[] colors, float phase, PreComponent[] children) {
    this(colors, phase, false, false, children);
  }

  public GradientPreComponent(TextColor[] colors, float phase, boolean rainbow, boolean reversed,
                              PreComponent[] children) {
    this.colors = Objects.requireNonNull(colors, "colors").clone();
    this.phase = phase;
    this.rainbow = rainbow;
    this.reversed = reversed;
    this.children = children;
  }

  @Override
  public Component render(Pointered pointered, RenderScope scope) {
    final Component cached = this.cached;
    if (cached != null && scope.isEmpty()) return cached;
    final TextComponent.Builder builder = Component.text();
    final Style style = requireStyle();
    if (!style.isEmpty()) builder.style(style);
    appendUncached(pointered, builder, false, scope);
    final Component rendered = builder.build();
    if (isStatic() && scope.isEmpty()) this.cached = rendered;
    return rendered;
  }

  @Override
  public void renderInto(Pointered pointered, TextComponent.Builder builder) {
    renderInto(pointered, builder, RenderScope.EMPTY);
  }

  @Override
  public void renderInto(Pointered pointered, TextComponent.Builder builder, RenderScope scope) {
    if (isStatic() && scope.isEmpty()) {
      builder.append(render(pointered, scope));
      return;
    }
    appendUncached(pointered, builder, true, scope);
  }

  @Override
  public boolean isStatic() {
    return childrenAreStatic();
  }

  @Override
  public PreComponent setStyle(Style style) {
    cached = null;
    styleCache = null;
    return super.setStyle(style);
  }

  @Override
  public PreComponent setChildren(PreComponent[] children) {
    cached = null;
    styleCache = null;
    return super.setChildren(children);
  }

  private void appendUncached(Pointered pointered, TextComponent.Builder result, boolean applyComputedStyle, RenderScope scope) {
    final List<Component> flattened = flattenChildren(pointered, scope);
    if (flattened.isEmpty()) {
      final Style style = requireStyle();
      if (applyComputedStyle) {
        if (!style.isEmpty()) result.append(Component.empty().style(style));
      } else {
        result.style(style);
      }
      return;
    }
    if (colors.length == 1) {
      if (applyComputedStyle) {
        final TextComponent.Builder wrapper = Component.text();
        final Style style = requireStyle();
        if (!style.isEmpty()) wrapper.style(style);
        wrapper.color(colors[0]);
        for (final Component c : flattened) wrapper.append(c);
        result.append(wrapper.build());
        return;
      }
      final Style style = requireStyle();
      if (!style.isEmpty()) result.style(style);
      result.color(colors[0]);
      for (final Component c : flattened) result.append(c);
      return;
    }
    final int size = flattened.size();
    final int[] lengths = new int[size];
    int totalLength = 0;
    for (int i = 0; i < size; i++) {
      final int len = contentLength(flattened.get(i));
      lengths[i] = len;
      totalLength += len;
    }
    final Style computed = applyComputedStyle ? requireStyle() : Style.empty();
    if (totalLength == 0) {
      if (applyComputedStyle && !computed.isEmpty()) {
        for (final Component c : flattened) result.append(c.style(merge(computed, c.style())));
      } else {
        for (final Component c : flattened) result.append(c);
      }
      return;
    }
    final boolean uniform = flattened.getFirst().style().color() == null && hasUniformStyle(flattened, computed);
    final Style[] styles = uniform ? stylesFor(styleOf(flattened.getFirst(), computed), totalLength) : null;
    int index = 0;
    for (int s = 0; s < size; s++) {
      final Component c = flattened.get(s);
      final int len = lengths[s];
      if (!(c instanceof TextComponent)) {
        final boolean keepChildColor = c.style().color() != null;
        Component toAppend = c;
        if (!computed.isEmpty()) toAppend = toAppend.style(merge(computed, toAppend.style()));
        if (!keepChildColor) toAppend = toAppend.colorIfAbsent(interpolate(positionFor(index, totalLength)));
        result.append(toAppend);
        index++;
        continue;
      }
      if (len == 0) {
        result.append(applyComputedStyle && !computed.isEmpty() ? c.style(merge(computed, c.style())) : c);
        continue;
      }
      final String text = ((TextComponent) c).content();
      if (styles != null) {
        int charIndex = 0;
        for (int i = 0; i < len; i++) {
          final int codePoint = text.codePointAt(charIndex);
          charIndex += Character.charCount(codePoint);
          result.append(Component.text(charString(codePoint), styles[index++]));
        }
      } else {
        final Style base = applyComputedStyle ? merge(computed, c.style()) : c.style();
        final boolean keepChildColor = c.style().color() != null;
        int charIndex = 0;
        for (int i = 0; i < len; i++) {
          final int codePoint = text.codePointAt(charIndex);
          charIndex += Character.charCount(codePoint);
          result.append(keepChildColor
            ? Component.text(charString(codePoint), base)
            : Component.text(charString(codePoint), base.color(interpolate(positionFor(index, totalLength)))));
          index++;
        }
      }
    }
  }

  private List<Component> flattenChildren(Pointered pointered, RenderScope scope) {
    final List<Component> flattened = new ArrayList<>();
    final PreComponent[] kids = children;
    if (kids != null) {
      for (final PreComponent child : kids) flatten(child.render(pointered, scope), Style.empty(), flattened);
    }
    return flattened;
  }

  private boolean hasUniformStyle(List<Component> flattened, Style computed) {
    final Style first = styleOf(flattened.getFirst(), computed);
    for (int i = 1; i < flattened.size(); i++) {
      if (!styleOf(flattened.get(i), computed).equals(first)) return false;
    }
    return true;
  }

  private static Style styleOf(Component component, Style computed) {
    return computed.isEmpty() ? component.style() : merge(computed, component.style());
  }

  private static Style merge(final Style inherited, final Style own) {
    if (inherited.isEmpty()) return own;
    if (own.isEmpty()) return inherited;
    return inherited.merge(own);
  }

  private Style[] stylesFor(Style base, int length) {
    final StyleCache cache = styleCache;
    if (cache != null && cache.length == length && cache.base.equals(base)) return cache.styles;
    final Style[] styles = new Style[length];
    for (int i = 0; i < length; i++) styles[i] = base.color(interpolate(positionFor(i, length)));
    final StyleCache updated = new StyleCache(base, length, styles);
    styleCache = updated;
    return updated.styles;
  }

  private float positionFor(int index, int length) {
    if (rainbow) {
      final int effective = reversed ? length - 1 - index : index;
      float hue = (float) effective / length + phase;
      hue %= 1.0f;
      if (hue < 0.0f) hue += 1.0f;
      return hue;
    }
    return adjustedPosition(index, Math.max(1, length - 1));
  }

  private float adjustedPosition(int index, float denominator) {
    final float position = index / denominator;
    float adjusted = (position + phase) % 1.0f;
    if (adjusted < 0) adjusted += 1.0f;
    if (adjusted == 0.0f && (position + phase) > 0.0f) adjusted = 1.0f;
    return adjusted;
  }

  private Style requireStyle() {
    final Style style = computedStyle;
    return style != null ? style : Style.empty();
  }

  private static String charString(int codePoint) {
    return codePoint < ASCII.length ? ASCII[codePoint] : new String(Character.toChars(codePoint));
  }

  private static void flatten(Component component, Style inherited, List<Component> out) {
    final Style merged = merge(inherited, component.style());
    if (component instanceof TextComponent text) {
      if (!text.content().isEmpty()) {
        out.add(text.children().isEmpty() && merged.equals(text.style()) ? text : Component.text(text.content(), merged));
      }
    } else {
      out.add(component.children().isEmpty() && merged.equals(component.style())
        ? component
        : component.children(List.of()).style(merged));
    }
    for (final Component child : component.children()) flatten(child, merged, out);
  }

  private static int contentLength(Component c) {
    if (c instanceof TextComponent tc) return tc.content().codePointCount(0, tc.content().length());
    return 1;
  }

  private TextColor interpolate(float position) {
    if (rainbow) return TextColor.color(HSVLike.hsvLike(position, 1.0f, 1.0f));
    if (colors.length == 1) return colors[0];
    final float scaled = position * (colors.length - 1);
    int index = (int) scaled;
    if (index >= colors.length - 1) return colors[colors.length - 1];
    final float local = scaled - index;
    return leap(colors[index], colors[index + 1], local);
  }

  private static TextColor leap(TextColor a, TextColor b, float t) {
    return TextColor.lerp(t, a, b);
  }

  @Override
  public PreComponent deepCopy() {
    final PreComponent[] copied = new PreComponent[children.length];
    for (int i = 0; i < children.length; i++) copied[i] = children[i].deepCopy();
    return new GradientPreComponent(colors, phase, rainbow, reversed, copied).setStyle(computedStyle);
  }

  private static final class StyleCache {
    private final @NotNull Style base;
    private final int length;
    private final @NotNull Style[] styles;

    private StyleCache(@NotNull Style base, int length, @NotNull Style[] styles) {
      this.base = base;
      this.length = length;
      this.styles = styles;
    }
  }
}
