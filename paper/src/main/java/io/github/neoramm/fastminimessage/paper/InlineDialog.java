package io.github.neoramm.fastminimessage.paper;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Contents used to build an inline dialog from a tag.
 *
 * @param title the dialog title
 * @param body the optional message shown below the title
 * @param button the label of the button that closes the dialog
 */
public record InlineDialog(
    @NotNull Component title,
    @Nullable Component body,
    @NotNull Component button
) {
}
