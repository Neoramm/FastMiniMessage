package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.StyleInheritance;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.component.ShadowPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static io.github.neoramm.fastminimessage.resolver.ColorResolver.resolveColor;

public final class ShadowResolver implements ComponentResolver {
    private static final String @NotNull [] NAMES = {"shadow"};

    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        if (args.isEmpty()) return new LiteralPreComponent("", inheritedStyle);
        final String colorString = args.getFirst().toLowerCase();
        final ShadowColor color;
        if (colorString.startsWith(TextColor.HEX_PREFIX) && colorString.length() == 9) {
            color = ShadowColor.fromHexString(colorString);
            if (color == null) {
                throw new IllegalArgumentException(
                    "Unable to parse a shadow color from '" + colorString + "'. Please use #RRGGBBAA formatting.");
            }
        } else {
            final TextColor textColor = resolveColor(colorString);
            if (textColor == null) {
                throw new IllegalArgumentException(
                    "Unable to parse a shadow color from '" + colorString
                        + "'. Please use named colors or hex (#RRGGBBAA) colors.");
            }
            final String alphaText = args.get(1, "0.25");
            final double alpha;
            try {
                alpha = Double.parseDouble(alphaText);
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException("Invalid shadow alpha: " + alphaText, e);
            }
            final int alphaInt = (int) Math.round(Math.clamp(alpha, 0.0, 1.0) * 0xff);
            color = ShadowColor.shadowColor(textColor, alphaInt);
        }
        final Style own = Style.style().shadowColor(color).build();
        final @Nullable LiteralPreComponent merged =
            StyleInheritance.mergeIntoOnlyLiteral(children, inheritedStyle, own);
        if (merged != null) return merged;
        return new ShadowPreComponent(color, children).setStyle(inheritedStyle);
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return NAMES;
    }
}
