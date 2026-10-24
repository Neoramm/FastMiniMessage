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

public final class GradientResolver implements ComponentResolver {
    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        if (args.isEmpty()) {
            return new GradientPreComponent(
                new TextColor[]{NamedTextColor.WHITE, NamedTextColor.BLACK}, 0.0f, children);
        }
        int count = args.size();
        float phase = 0.0f;

        if (count > 2) {
            final String last = args.get(count - 1);
            TextColor lastColor = TextColor.fromHexString(last);
            if (lastColor == null) lastColor = NamedTextColor.NAMES.value(last);
            if (lastColor == null) {
                try {
                    phase = Float.parseFloat(last);
                    if (phase < -1.0f || phase > 1.0f) phase = 0.0f;
                    count--;
                } catch (final NumberFormatException ignored) {
                    phase = 0.0f;
                }
            }
        }
        final TextColor @NotNull [] colors = new TextColor[count];
        for (int i = 0; i < count; i++) {
            final String raw = args.get(i);
            TextColor color = TextColor.fromHexString(raw);
            if (color == null) color = NamedTextColor.NAMES.value(raw);
            colors[i] = color != null ? color : NamedTextColor.WHITE;
        }
        if (colors.length < 2) throw new IllegalArgumentException("Invalid gradient, not enough colors.");
        return new GradientPreComponent(colors, phase, children);
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return new String[]{"gradient"};
    }
}
