package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.StyleInheritance;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.GroupPreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static io.github.neoramm.fastminimessage.resolver.ColorResolver.resolveColor;

public final class TransitionResolver implements ComponentResolver {
    private static final String @NotNull [] NAMES = {"transition"};
    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        if (args.isEmpty()) return new LiteralPreComponent("", inheritedStyle);
        float phase = 0.0f;
        final List<TextColor> colors = new ArrayList<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            final String raw = args.get(i);
            final TextColor color = resolveColor(raw);
            if (color != null) colors.add(color);
            else if (i == args.size() - 1) {
                try {
                    phase = Float.parseFloat(raw);
                    if (phase < -1.0f || phase > 1.0f) throw new IllegalArgumentException("out of range");
                } catch (final NumberFormatException e) {
                    throw new IllegalArgumentException("Unable to parse a color from '" + raw + "'");
                }
            } else throw new IllegalArgumentException("Unable to parse a color from '" + raw + "'");
        }
        if (colors.size() < 2) throw new IllegalArgumentException("Invalid transition, not enough colors.");
        TextColor @NotNull [] palette = colors.toArray(TextColor[]::new);
        final boolean negative = phase < 0;
        final float position = negative ? 1 + phase : phase;
        if (negative) {
            final TextColor @NotNull [] reversed = new TextColor[palette.length];
            for (int i = 0; i < palette.length; i++) reversed[i] = palette[palette.length - 1 - i];
            palette = reversed;
        }
        TextColor result = null;
        final float steps = 1.0f / (palette.length - 1);
        int idx = 1;
        for (; idx < palette.length; idx++) {
            final float bound = idx * steps;
            if (bound >= position) {
                final float factor = 1 + (position - bound) * (palette.length - 1);
                if (negative) result = TextColor.lerp(1 - factor, palette[idx], palette[idx - 1]);
                else result = TextColor.lerp(factor, palette[idx - 1], palette[idx]);
                break;
            }
        }
        if (idx >= palette.length) result = palette[0];
        final Style own = Style.style().color(result).build();
        final @Nullable LiteralPreComponent merged =
            StyleInheritance.mergeIntoOnlyLiteral(children, inheritedStyle, own);
        if (merged != null) return merged;

        if (children.length == 0) return new LiteralPreComponent("", own);
        return new GroupPreComponent(children).setStyle(own);
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return NAMES;
    }
}
