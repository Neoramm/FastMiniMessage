package io.github.neoramm.fastminimessage.v2.model;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RuntimeTagBridge {
    boolean has(final @NotNull String name);

    default @Nullable RuntimeTagBridge resolve(final @NotNull String name) {
        return has(name) ? this : null;
    }

    default boolean rendersChildren() {
        return false;
    }

    default boolean renderInto(final @NotNull RuntimeTagContext context, final TextComponent.@NotNull Builder output) {
        final @Nullable Component resolved = render(context);
        if (resolved == null) return false;
        output.append(resolved);
        return true;
    }

    @Nullable Component render(final @NotNull RuntimeTagContext context);

    @FunctionalInterface
    interface Renderer {
        @Nullable Component render(final @NotNull Arguments arguments, final @NotNull RenderScope scope);
    }
}
