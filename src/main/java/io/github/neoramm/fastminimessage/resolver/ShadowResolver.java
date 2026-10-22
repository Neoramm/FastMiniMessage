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
    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        if (args.isEmpty()) return new LiteralPreComponent("", inheritedStyle);
        final String colorString = args.get(0).toLowerCase();
        final ShadowColor color;
        if (colorString.startsWith(TextColor.HEX_PREFIX) && colorString.length() == 9) {
            color = ShadowColor.fromHexString(colorString);
            if (color == null) {
                throw new IllegalArgumentException(String.format(
                    "Unable to parse a shadow color from '%s'. Please use #RRGGBBAA formatting.", colorString));
            }
        } else {
            final TextColor textColor = resolveColor(colorString);
            final String alphaText = args.get(1, "0.25");
            final double alpha = Double.parseDouble(alphaText);
            color = ShadowColor.shadowColor(textColor, (int) (alpha * 0xff));
        }
        final Style own = Style.style().shadowColor(color).build();
        if (StyleInheritance.mergeIntoOnlyLiteral(children, inheritedStyle, own)) return children[0];
        return new ShadowPreComponent(color, children);
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return new String[]{"shadow"};
    }
}
