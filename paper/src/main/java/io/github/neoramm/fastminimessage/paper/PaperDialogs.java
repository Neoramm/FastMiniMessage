package io.github.neoramm.fastminimessage.paper;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.dialog.DialogLike;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Builds the dialogs referenced by the Paper tags.
 */
public final class PaperDialogs {

    private PaperDialogs() {
    }

    /**
     * Resolves a dialog from the server dialog registry.
     *
     * @param reference the dialog key, for example {@code minecraft:server_links}
     * @return the registered dialog
     */
    public static @NotNull DialogLike registryDialog(final @NotNull String reference) {
        return RegistryAccess.registryAccess()
            .getRegistry(RegistryKey.DIALOG)
            .getOrThrow(Key.key(reference));
    }

    /**
     * Builds a notice dialog that can close itself.
     *
     * @param dialog the dialog contents
     * @return the inline dialog
     */
    public static @NotNull DialogLike inlineDialog(final @NotNull InlineDialog dialog) {
        return Dialog.create(factory -> {
            final DialogBase.Builder base = DialogBase.builder(dialog.title());
            final @Nullable Component body = dialog.body();
            if (body != null) base.body(List.of(DialogBody.plainMessage(body)));
            factory.empty()
                .base(base.build())
                .type(DialogType.notice(ActionButton.builder(dialog.button()).build()));
        });
    }
}
