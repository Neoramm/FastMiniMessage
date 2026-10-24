package io.github.neoramm.fastminimessage.paper;

import net.kyori.adventure.pointer.Pointered;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves a PlaceholderAPI expression for the rendering context.
 */
@FunctionalInterface
public interface PapiRenderer {

    /**
     * Resolves the expression for the given context.
     *
     * @param pointered the rendering context, usually the viewer
     * @param expression the PlaceholderAPI expression
     * @return the resolved text
     */
    @NotNull String render(final @Nullable Pointered pointered, final @NotNull String expression);
}
