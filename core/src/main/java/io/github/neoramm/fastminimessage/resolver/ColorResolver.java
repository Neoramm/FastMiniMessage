package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.StyleInheritance;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.ColorPreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class ColorResolver implements ComponentResolver {
    private final @NotNull String @NotNull [] names;
    private final @Nullable TextColor color;

    public ColorResolver(final @NotNull String name, final @Nullable TextColor color) {
        this(new String[]{name}, color);
    }

    public ColorResolver(final @NotNull String @NotNull [] names, final @Nullable TextColor color) {
        this.names = names;
        this.color = color;
    }

    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        final TextColor targetColor = color != null
            ? color
            : (name.startsWith("#") ? resolveColor(name) : resolveColor(args.getFirst(null)));
        if (name.startsWith("#") && targetColor == null) {
            throw new IllegalArgumentException("Unable to parse a color from '" + name + "'");
        }
        final Style own = Style.style().color(targetColor).build();
        final Style mergedStyle = StyleInheritance.compute(own, inheritedStyle);
        final @Nullable LiteralPreComponent merged =
            StyleInheritance.mergeIntoOnlyLiteral(children, inheritedStyle, own);
        if (merged != null) return merged;
        return new ColorPreComponent(targetColor, children).setStyle(mergedStyle);
    }

    static @Nullable TextColor resolveColor(final @Nullable String colorName) {
        if (colorName == null || colorName.isEmpty()) return null;
        if (colorName.charAt(0) == TextColor.HEX_CHARACTER) return TextColor.fromHexString(colorName);
        if ("grey".equalsIgnoreCase(colorName)) return NamedTextColor.GRAY;
        if ("dark_grey".equalsIgnoreCase(colorName)) return NamedTextColor.DARK_GRAY;
        return NamedTextColor.NAMES.value(colorName.toLowerCase(Locale.ROOT));
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return names;
    }
}
