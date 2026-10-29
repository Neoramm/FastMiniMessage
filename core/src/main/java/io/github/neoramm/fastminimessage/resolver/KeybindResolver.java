package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.KeybindPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class KeybindResolver implements ComponentResolver {
    private static final String @NotNull [] NAMES = {"key"};
    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        final String keybind = args.getFirst("key.jump");
        return new KeybindPreComponent(keybind, inheritedStyle);
    }

    @Override
    public boolean isSelfClosing() {
        return true;
    }
    @Override
    public @NotNull String @NotNull [] names() {
        return NAMES;
    }
}
