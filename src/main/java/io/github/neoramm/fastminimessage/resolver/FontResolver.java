package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.StyleInheritance;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.GroupPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FontResolver implements ComponentResolver {
    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        if (args.isEmpty()) {
            throw new IllegalArgumentException(
                "A font tag must have either arguments of either <value> or <namespace:value>");
        }        final Key font;
        try {
            if (args.size() == 1) {
                font = Key.key(args.get(0));
            } else {
                font = Key.key(args.get(0), args.get(1));
            }
        } catch (final InvalidKeyException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
        final Style own = Style.style().font(font).build();
        if (StyleInheritance.mergeIntoOnlyLiteral(children, inheritedStyle, own)) return children[0];
        return new GroupPreComponent(children).setStyle(own);
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return new String[]{"font"};
    }
}
