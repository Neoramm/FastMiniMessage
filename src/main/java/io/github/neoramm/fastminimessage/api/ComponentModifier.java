package io.github.neoramm.fastminimessage.api;

import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Transforms a rendered component at render time.
 */
@FunctionalInterface
public interface ComponentModifier {
    /**
      * Modifies the rendered component for the given context.
      *
      * @param rendered the rendered child component
      * @param context the rendering context
      * @return the modified component
      */
    Component modify(final @NotNull Component rendered, final @Nullable Pointered context);

    /**
      * Whether the modification is independent of the rendering context.
      */
    default boolean isStatic() {
        return false;
    }
}
