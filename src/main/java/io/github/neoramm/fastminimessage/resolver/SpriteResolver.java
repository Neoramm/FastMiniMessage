package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.InlinePreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.object.ObjectContents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SpriteResolver implements ComponentResolver {
    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        final String firstArg = args.get(0);
        final String secondArg = args.get(1);
        if (firstArg == null || firstArg.isBlank()) {
            throw new IllegalArgumentException(
                "<sprite> needs a sprite or an atlas and a sprite, for example <sprite:items:item/diamond_pickaxe>");
        }
        if (secondArg == null) {
            return new InlinePreComponent(Component.object(ObjectContents.sprite(Key.key(firstArg))));
        }
        return new InlinePreComponent(Component.object(ObjectContents.sprite(Key.key(firstArg), Key.key(secondArg))));
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return new String[]{"sprite"};
    }

    @Override
    public boolean isSelfClosing() {
        return true;
    }
}
