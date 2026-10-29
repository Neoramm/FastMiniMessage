package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.StyleInheritance;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.GroupPreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StyleResolver implements ComponentResolver {
    private final @NotNull String @NotNull [] names;
    private final @NotNull TextDecoration decoration;

    public StyleResolver(final @NotNull TextDecoration decoration, final @NotNull String... names) {
        this.decoration = decoration;
        this.names = names;
    }

    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        final boolean negate = name.startsWith("!");
        final Style own = Style.style().decoration(decoration, !negate).build();
        final Style merged = StyleInheritance.compute(own, inheritedStyle);
        if (children.length == 0) return new LiteralPreComponent("", merged);
        final @Nullable LiteralPreComponent styledLiteral =
            StyleInheritance.mergeIntoOnlyLiteral(children, inheritedStyle, own);
        if (styledLiteral != null) return styledLiteral;
        return new GroupPreComponent(children).setStyle(merged);
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return names;
    }
}
