package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentModifier;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.ModifiedPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.TagResolver;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ModifierResolver implements ComponentResolver, TagResolver {
    private final @NotNull String name;
    private final @NotNull ComponentModifier modifier;
    private final @NotNull String @NotNull [] names;

    public ModifierResolver(final @NotNull String name, final @NotNull ComponentModifier modifier) {
        this.name = name;
        this.modifier = modifier;
        this.names = new String[]{name};
    }

    @Override
    public boolean isModifying() {
        return true;
    }

    @Override
    public @NotNull PreComponent resolve(final @NotNull String resolvedName, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        return new ModifiedPreComponent(modifier, children).setStyle(inheritedStyle);
    }

    public @NotNull String name() {
        return name;
    }

    public @NotNull ComponentModifier modifier() {
        return modifier;
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return names;
    }
}
