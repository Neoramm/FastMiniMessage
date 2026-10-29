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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GradientPreComponent extends PreComponent {
    private static final String @NotNull [] ASCII = new String[128];

    static {
        for (int i = 0; i < ASCII.length; i++) ASCII[i] = String.valueOf((char) i);
    }

    private final TextColor @NotNull [] colors;
    private final float phase;
    private final boolean rainbow;
    private final boolean reversed;
    private volatile @Nullable Component cached;
    private volatile @Nullable StyleCache styleCache;

    public GradientPreComponent(final TextColor @NotNull [] colors, final float phase,
        final PreComponent @Nullable [] children) {
        this(colors, phase, false, false, children);
    }

    public GradientPreComponent(final TextColor @NotNull [] colors, final float phase, final boolean rainbow,
        final boolean reversed, final PreComponent @Nullable [] children) {
        Objects.requireNonNull(colors, "colors");
        if (!rainbow && phase < 0.0f) {
            this.phase = 1.0f + phase;
            this.colors = new TextColor[colors.length];
            for (int i = 0; i < colors.length; i++) this.colors[i] = colors[colors.length - 1 - i];
        } else {
            this.phase = phase;
            this.colors = colors.clone();
        }
        this.rainbow = rainbow;
        this.reversed = reversed;
        this.children = children;
    }

    @Override
    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
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
    public void renderInto(final @Nullable Pointered pointered, final @NotNull TextComponent.Builder builder,
        final @NotNull RenderScope scope) {
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
    public @NotNull PreComponent setStyle(final @Nullable Style style) {
        cached = null;
        styleCache = null;
        return super.setStyle(style);
    }

    @Override
    public @NotNull PreComponent setChildren(final @Nullable PreComponent[] children) {
        cached = null;
        styleCache = null;
        return super.setChildren(children);
    }

    private void appendUncached(final @Nullable Pointered pointered, final @NotNull TextComponent.Builder result,
        final boolean applyComputedStyle, final @NotNull RenderScope scope) {
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
                for (final Component component : flattened) wrapper.append(component);
                result.append(wrapper.build());
                return;
            }
            final Style style = requireStyle();
            if (!style.isEmpty()) result.style(style);
            result.color(colors[0]);
            for (final Component component : flattened) result.append(component);
            return;
        }
        final int size = flattened.size();
        final int[] lengths = new int[size];
        int totalLength = 0;
        for (int i = 0; i < size; i++) {
            final int length = contentLength(flattened.get(i));
            lengths[i] = length;
            totalLength += length;
        }
        final Style computed = applyComputedStyle ? requireStyle() : Style.empty();
        if (totalLength == 0) {
            if (applyComputedStyle && !computed.isEmpty()) {
                for (final Component component : flattened) {
                    result.append(component.style(merge(computed, component.style())));
                }
            } else {
                for (final Component component : flattened) result.append(component);
            }
            return;
        }
        final boolean uniform = flattened.getFirst().style().color() == null && hasUniformStyle(flattened, computed);
        final Style @Nullable [] styles = uniform
            ? stylesFor(styleOf(flattened.getFirst(), computed), totalLength)
            : null;
        int index = 0;
        for (int segment = 0; segment < size; segment++) {
            final Component component = flattened.get(segment);
            final int length = lengths[segment];
            index = appendGradientChar(result, component, length, index, totalLength, computed, applyComputedStyle,
                styles);
        }
    }

    private int appendGradientChar(final @NotNull TextComponent.Builder result, final @NotNull Component gradient,
        final int length, final int index, final int totalLength, final @NotNull Style computed,
        final boolean applyComputedStyle, final Style @Nullable [] styles) {
        if (!(gradient instanceof final TextComponent text)) {
            return appendNonText(result, gradient, index, totalLength, computed);
        }
        if (length == 0) {
            result.append(applyComputedStyle && !computed.isEmpty()
                ? gradient.style(merge(computed, gradient.style()))
                : gradient);
            return index;
        }
        int next = index;
        if (styles != null) {
            int charIndex = 0;
            for (int i = 0; i < length; i++) {
                final int codePoint = text.content().codePointAt(charIndex);
                charIndex += Character.charCount(codePoint);
                result.append(Component.text(charString(codePoint), styles[next++]));
            }
        } else {
            final Style base = applyComputedStyle ? merge(computed, gradient.style()) : gradient.style();
            final boolean keepChildColor = gradient.style().color() != null;
            int charIndex = 0;
            for (int i = 0; i < length; i++) {
                final int codePoint = text.content().codePointAt(charIndex);
                charIndex += Character.charCount(codePoint);
                result.append(keepChildColor
                    ? Component.text(charString(codePoint), base)
                    : Component.text(charString(codePoint), base.color(colorAt(next, totalLength))));
                next++;
            }
        }
        return next;
    }

    private int appendNonText(final @NotNull TextComponent.Builder result, final @NotNull Component component,
        final int index, final int totalLength, final @NotNull Style computed) {
        final boolean keepChildColor = component.style().color() != null;
        Component toAppend = component;
        if (!computed.isEmpty()) toAppend = toAppend.style(merge(computed, toAppend.style()));
        if (!keepChildColor) toAppend = toAppend.colorIfAbsent(colorAt(index, totalLength));
        result.append(toAppend);
        return index + 1;
    }

    private @NotNull List<@NotNull Component> flattenChildren(final @Nullable Pointered pointered,
        final @NotNull RenderScope scope) {
        final List<Component> flattened = new ArrayList<>();
        final PreComponent @Nullable [] kids = children;
        if (kids != null) {
            for (final PreComponent child : kids) flatten(child.render(pointered, scope), Style.empty(), flattened);
        }
        return flattened;
    }

    private boolean hasUniformStyle(final @NotNull List<@NotNull Component> flattened, final @NotNull Style computed) {
        final Style first = styleOf(flattened.getFirst(), computed);
        for (int i = 1; i < flattened.size(); i++) {
            if (!styleOf(flattened.get(i), computed).equals(first)) return false;
        }
        return true;
    }

    private static @NotNull Style styleOf(final @NotNull Component component, final @NotNull Style computed) {
        return computed.isEmpty() ? component.style() : merge(computed, component.style());
    }

    private static @NotNull Style merge(final @NotNull Style inherited, final @NotNull Style own) {
        if (inherited.isEmpty()) return own;
        if (own.isEmpty()) return inherited;
        return inherited.merge(own);
    }

    private Style @NotNull [] stylesFor(final @NotNull Style base, final int length) {
        final @Nullable StyleCache cache = styleCache;
        if (cache != null && cache.length() == length && cache.base().equals(base)) return cache.styles();
        final Style @NotNull [] styles = new Style[length];
        for (int i = 0; i < length; i++) styles[i] = base.color(colorAt(i, length));
        final StyleCache updated = new StyleCache(base, length, styles);
        styleCache = updated;
        return updated.styles();
    }

    private @NotNull TextColor colorAt(final int index, final int length) {
        if (rainbow) {
            final int effective = reversed ? length - 1 - index : index;
            float hue = (float) effective / length + phase;
            hue %= 1.0f;
            if (hue < 0.0f) hue += 1.0f;
            return TextColor.color(HSVLike.hsvLike(hue, 1.0f, 1.0f));
        }
        if (colors.length == 1) return colors[0];
        final double multiplier = length <= 1 ? 0.0 : (double) (colors.length - 1) / (length - 1);
        final double scaledPhase = phase * (colors.length - 1);
        final double position = (index * multiplier) + scaledPhase;
        final int lowUnclamped = (int) Math.floor(position);
        final int low = ((lowUnclamped % colors.length) + colors.length) % colors.length;
        final int highUnclamped = (int) Math.ceil(position);
        final int high = ((highUnclamped % colors.length) + colors.length) % colors.length;
        final float fraction = (float) position - lowUnclamped;
        return TextColor.lerp(fraction, colors[low], colors[high]);
    }

    private @NotNull Style requireStyle() {
        final @Nullable Style style = computedStyle;
        return style != null ? style : Style.empty();
    }

    private static @NotNull String charString(final int codePoint) {
        return codePoint < ASCII.length ? ASCII[codePoint] : new String(Character.toChars(codePoint));
    }

    private static void flatten(final @NotNull Component component, final @NotNull Style inherited,
        final @NotNull List<@NotNull Component> out) {
        final Style merged = merge(inherited, component.style());
        if (component instanceof final TextComponent text) {
            if (!text.content().isEmpty()) {
                out.add(text.children().isEmpty() && merged.equals(text.style())
                    ? text
                    : Component.text(text.content(), merged));
            }
        } else {
            out.add(component.children().isEmpty() && merged.equals(component.style())
                ? component
                : component.children(List.of()).style(merged));
        }
        for (final Component child : component.children()) flatten(child, merged, out);
    }

    private static int contentLength(final @NotNull Component component) {
        if (!(component instanceof final TextComponent text)) return 1;
        return text.content().codePointCount(0, text.content().length());
    }

    @Override
    public @NotNull PreComponent deepCopy() {
        return new GradientPreComponent(colors, phase, rainbow, reversed, copyChildren()).setStyle(computedStyle);
    }

    private record StyleCache(@NotNull Style base, int length, Style @NotNull [] styles) {
    }
}
