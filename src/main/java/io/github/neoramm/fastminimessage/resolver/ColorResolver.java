package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.StyleInheritance;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.ColorPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ColorResolver implements ComponentResolver {
    private final @NotNull String @NotNull [] names;
    private final @Nullable TextColor color;

    public ColorResolver(final @NotNull String name, final @Nullable TextColor color) {
        this.names = new String[]{name};
        this.color = color;
    }

    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        if ("color".equals(name)) {
            final String colorName = args.get(0);
            final TextColor resolved = resolveColor(colorName);
            final Style own = Style.style().color(resolved).build();
            if (StyleInheritance.mergeIntoOnlyLiteral(children, inheritedStyle, own)) return children[0];
            return new ColorPreComponent(resolved, children);
        }
        final Style own = Style.style().color(color).build();
        if (StyleInheritance.mergeIntoOnlyLiteral(children, inheritedStyle, own)) return children[0];
        return new ColorPreComponent(color, children);
    }

    static @Nullable TextColor resolveColor(final @Nullable String colorName) {
        if (colorName == null || colorName.isEmpty()) return null;
        return colorName.charAt(0) == TextColor.HEX_CHARACTER
            ? TextColor.fromHexString(colorName)
            : NamedTextColor.NAMES.value(colorName);
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return names;
    }
}
