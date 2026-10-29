package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.StyleInheritance;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.GroupPreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FontResolver implements ComponentResolver {
    private static final String @NotNull [] NAMES = {"font"};
    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        if (args.isEmpty()) {
            throw new IllegalArgumentException(
                "A font tag must have either arguments of either <value> or <namespace:value>");
        }
        final Key font;
        try {
            font = args.size() == 1 ? Key.key(args.getFirst()) : Key.key(args.getFirst(), args.get(1));
        } catch (final InvalidKeyException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
        final Style own = Style.style().font(font).build();
        final Style mergedStyle = StyleInheritance.compute(own, inheritedStyle);
        final @Nullable LiteralPreComponent merged =
            StyleInheritance.mergeIntoOnlyLiteral(children, inheritedStyle, own);
        if (merged != null) return merged;
        return new GroupPreComponent(children).setStyle(mergedStyle);
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return NAMES;
    }
}
