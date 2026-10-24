package io.github.neoramm.fastminimessage.api;

import net.kyori.adventure.dialog.DialogLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves a dialog reference written in a tag into an Adventure dialog.
 *
 * <p>Implementations are platform specific; the Paper module resolves references through the
 * server dialog registry.</p>
 */
@FunctionalInterface
public interface DialogResolver {

    /**
     * Resolves the given dialog reference.
     *
     * @param reference the raw reference from the tag
     * @return the dialog, or {@code null} when the reference cannot be resolved
     */
    @Nullable DialogLike resolve(final @NotNull String reference);
}
