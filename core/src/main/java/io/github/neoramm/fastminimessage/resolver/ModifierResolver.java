package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentModifier;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.ModifiedPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RuntimeResolver;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ModifierResolver implements ComponentResolver, RuntimeResolver {
    private final @NotNull String name;
    private final @NotNull ComponentModifier modifier;

    public ModifierResolver(final @NotNull String name, final @NotNull ComponentModifier modifier) {
        this.name = name;
        this.modifier = modifier;
    }

    @Override
    public @NotNull PreComponent resolve(final @NotNull String resolvedName, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        return new ModifiedPreComponent(modifier, children);
    }

    public @NotNull ComponentModifier modifier() {
        return modifier;
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return new String[]{name};
    }
}
