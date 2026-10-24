package io.github.neoramm.fastminimessage.paper;

import net.kyori.adventure.dialog.DialogLike;
import org.jetbrains.annotations.NotNull;

/**
 * Builds the dialog opened by an inline dialog tag.
 */
@FunctionalInterface
public interface DialogFactory {

    /**
     * Creates the dialog for the given contents.
     *
     * @param dialog the rendered dialog contents
     * @return the dialog to open
     */
    @NotNull DialogLike create(final @NotNull InlineDialog dialog);
}
