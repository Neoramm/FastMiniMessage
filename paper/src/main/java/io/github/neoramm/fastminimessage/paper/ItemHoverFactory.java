package io.github.neoramm.fastminimessage.paper;

import net.kyori.adventure.text.event.HoverEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Builds the hover shown by an item hover tag.
 */
@FunctionalInterface
public interface ItemHoverFactory {

    /**
     * Creates the hover event for the given item reference.
     *
     * @param reference the item reference, either item bytes in base64 or a material name
     * @param count the requested stack size
     * @return the hover event
     */
    @NotNull HoverEvent<?> create(final @NotNull String reference, final int count);
}
