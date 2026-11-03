package io.github.neoramm.fastminimessage.v2.tag;

import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import io.github.neoramm.fastminimessage.v2.template.TreeTransformer;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.HSVLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class GradientTransform implements TreeTransformer {
    private static final int CACHED_LENGTHS = 64;
    private static final @NotNull Style @NotNull [] EMPTY_STYLES = new Style[0];
    private static final @NotNull String @NotNull [] ASCII = new String[128];

    static {
        for (int i = 0; i < ASCII.length; i++) ASCII[i] = String.valueOf((char) i);
    }

    private final @NotNull TextColor @NotNull [] colors;
    private final float phase;
    private final boolean rainbow;
    private final boolean reversed;
    private volatile @Nullable Palette lastPalette;
    private volatile @Nullable AtomicReferenceArray<@Nullable Palette> paletteCache;

    GradientTransform(final @NotNull TextColor @NotNull [] colors, final float phase,
        final boolean rainbow, final boolean reversed) {
        this.colors = colors.clone();
        this.phase = !rainbow && phase < 0 ? 1 + phase : phase;
        this.rainbow = rainbow;
        this.reversed = reversed;
        if (!rainbow && phase < 0) {
            for (int i = 0; i < colors.length; i++) this.colors[i] = colors[colors.length - 1 - i];
        }
    }

    @Override
    public @NotNull Component transform(final @NotNull Component children, final @NotNull Style style,
        final @NotNull RenderScope context) {
        final int length = length(children);
        if (length == 0) return Component.text().style(style).append(children).build();
        final TextComponent.@NotNull Builder output = Component.text().style(style);
        final @NotNull Style @NotNull [] palette = resolvePalette(length);
        append(children, Style.empty(), output, 0, palette);
        return output.build();
    }

    private @NotNull Style @NotNull [] resolvePalette(final int length) {
        if (length <= 0) return EMPTY_STYLES;
        final @Nullable Palette last = lastPalette;
        if (last != null && last.length == length) return last.styles;
        if (length < CACHED_LENGTHS) {
            final @Nullable AtomicReferenceArray<@Nullable Palette> cache = paletteCache;
            if (cache != null) {
                final @Nullable Palette cached = cache.get(length);
                if (cached != null) {
                    lastPalette = cached;
                    return cached.styles;
                }
            }
        }
        final @NotNull Style @NotNull [] styles = buildPalette(length);
        final @NotNull Palette palette = new Palette(length, styles);
        if (length < CACHED_LENGTHS) cachePalette(length, palette);
        lastPalette = palette;
        return styles;
    }

    private void cachePalette(final int length, final @NotNull Palette palette) {
        @Nullable AtomicReferenceArray<@Nullable Palette> cache = paletteCache;
        if (cache == null) {
            cache = new AtomicReferenceArray<>(CACHED_LENGTHS);
            final @Nullable Palette previous = lastPalette;
            if (previous != null && previous.length < CACHED_LENGTHS) cache.set(previous.length, previous);
            paletteCache = cache;
        }
        cache.set(length, palette);
    }

    private @NotNull Style @NotNull [] buildPalette(final int length) {
        final @NotNull Style @NotNull [] styles = new Style[length];
        for (int i = 0; i < length; i++) styles[i] = Style.style(colorAt(i, length));
        return styles;
    }

    private static int length(final @NotNull Component component) {
        int length = component instanceof final TextComponent text
            ? text.content().codePointCount(0, text.content().length())
            : (component.children().isEmpty() ? 1 : 0);
        final @NotNull List<@NotNull Component> children = component.children();
        for (int i = 0, size = children.size(); i < size; i++) length += length(children.get(i));
        return length;
    }

    private int append(final @NotNull Component component, final @NotNull Style inherited,
        final TextComponent.@NotNull Builder output, final int start, final @NotNull Style @NotNull [] palette) {
        final @NotNull Style merged = inherited.isEmpty() ? component.style() : inherited.merge(component.style());
        int index = start;
        if (component instanceof final TextComponent text) {
            final @NotNull String content = text.content();
            for (int offset = 0; offset < content.length();) {
                final int codePoint = content.codePointAt(offset);
                offset += Character.charCount(codePoint);
                final @NotNull Style colorStyle = merged.color() == null && index < palette.length
                    ? (merged.isEmpty() ? palette[index] : merged.color(palette[index].color()))
                    : merged;
                output.append(Component.text(codePoint < ASCII.length ? ASCII[codePoint]
                    : new String(Character.toChars(codePoint)), colorStyle));
                index++;
            }
            final @NotNull List<@NotNull Component> children = component.children();
            for (int i = 0, size = children.size(); i < size; i++) {
                index = append(children.get(i), merged, output, index, palette);
            }
        } else {
            final @Nullable TextColor color = index < palette.length ? palette[index++].color() : null;
            final @NotNull Component colored = color == null
                ? component.style(merged)
                : component.style(merged).colorIfAbsent(color);
            if (component.children().isEmpty()) {
                output.append(colored);
            } else {
                final TextComponent.@NotNull Builder children = Component.text();
                for (final @NotNull Component child : component.children()) {
                    index = append(child, merged, children, index, palette);
                }
                output.append(colored.children(children.build().children()));
            }
        }
        return index;
    }

    private @NotNull TextColor colorAt(final int index, final int length) {
        if (rainbow) {
            final int effective = reversed ? length - 1 - index : index;
            float hue = ((float) effective / length + phase) % 1.0f;
            if (hue < 0) hue += 1.0f;
            return TextColor.color(HSVLike.hsvLike(hue, 1.0f, 1.0f));
        }
        if (colors.length == 1) return colors[0];
        final double multiplier = length <= 1 ? 0 : (double) (colors.length - 1) / (length - 1);
        final double position = index * multiplier + phase * (colors.length - 1);
        final int low = (int) Math.floor(position);
        final int high = (int) Math.ceil(position);
        return TextColor.lerp((float) position - low, colors[Math.floorMod(low, colors.length)],
            colors[Math.floorMod(high, colors.length)]);
    }

    private record Palette(int length, @NotNull Style @NotNull [] styles) {}
}
