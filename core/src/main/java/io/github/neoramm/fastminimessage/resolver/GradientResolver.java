package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.GradientPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static io.github.neoramm.fastminimessage.resolver.ColorResolver.resolveColor;

public final class GradientResolver implements ComponentResolver {
    private static final String @NotNull [] NAMES = {"gradient"};
    private static final TextColor @NotNull [] DEFAULT_COLORS = {NamedTextColor.WHITE, NamedTextColor.BLACK};

    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        if (args.isEmpty()) return new GradientPreComponent(DEFAULT_COLORS, 0.0f, children).setStyle(inheritedStyle);
        int count = args.size();
        float phase = 0.0f;

        final String last = args.getLast();
        final TextColor lastColor = resolveColor(last);
        if (lastColor == null) {
            try {
                final float parsedPhase = Float.parseFloat(last);
                if (parsedPhase < -1.0f || parsedPhase > 1.0f) {
                    throw new IllegalArgumentException(
                        "Gradient phase is out of range (" + parsedPhase + "). Must be in [-1.0, 1.0].");
                }
                phase = parsedPhase;
                count--;
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException("Unable to parse a color from '" + last + "'");
            }
        }
        if (count == 0) return new GradientPreComponent(DEFAULT_COLORS, phase, children).setStyle(inheritedStyle);
        if (count == 1) throw new IllegalArgumentException("Invalid gradient, not enough colors.");
        final TextColor @NotNull [] colors = new TextColor[count];
        for (int i = 0; i < count; i++) {
            final String raw = args.get(i);
            final TextColor color = resolveColor(raw);
            if (color == null) throw new IllegalArgumentException("Unable to parse a color from '" + raw + "'");
            colors[i] = color;
        }
        return new GradientPreComponent(colors, phase, children).setStyle(inheritedStyle);
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return NAMES;
    }
}
