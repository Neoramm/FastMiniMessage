package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.GradientPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RainbowResolver implements ComponentResolver {
    private static final TextColor @NotNull [] NO_COLORS = new TextColor[0];

    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        boolean reversed = false;
        float phase = 0.0f;
        if (!args.isEmpty()) {
            String value = args.get(0, "");
            if (value.startsWith("!")) {
                reversed = true;
                value = value.substring(1);
            }
            if (!value.isEmpty()) {
                try {
                    phase = value.indexOf('.') >= 0 ? Float.parseFloat(value) : Integer.parseInt(value) / 10.0f;
                } catch (final NumberFormatException ignored) {
                    phase = 0.0f;
                }
            }
        }
        return new GradientPreComponent(NO_COLORS, phase, true, reversed, children);
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return new String[]{"rainbow"};
    }
}
