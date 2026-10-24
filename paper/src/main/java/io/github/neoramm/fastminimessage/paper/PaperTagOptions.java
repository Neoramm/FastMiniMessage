package io.github.neoramm.fastminimessage.paper;

import io.github.neoramm.fastminimessage.api.DialogResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Factories used by the Paper tag resolvers.
 *
 * <p>The defaults resolve registry dialogs and build inline dialogs, item hovers and
 * PlaceholderAPI values through the Paper API. Supply custom factories when the tree is
 * compiled outside a running server or in tests.</p>
 *
 * @param registryDialogs resolves dialog references from {@code click:show_dialog}
 * @param inlineDialogs builds the dialogs used by the {@code show_dialog} tag
 * @param itemHovers builds the hovers used by the {@code item_hover} tag
 * @param placeholders resolves the values of the {@code papi} tag
 */
public record PaperTagOptions(
    @NotNull DialogResolver registryDialogs,
    @NotNull DialogFactory inlineDialogs,
    @NotNull ItemHoverFactory itemHovers,
    @NotNull PapiRenderer placeholders
) {

    public PaperTagOptions {
        Objects.requireNonNull(registryDialogs, "registryDialogs");
        Objects.requireNonNull(inlineDialogs, "inlineDialogs");
        Objects.requireNonNull(itemHovers, "itemHovers");
        Objects.requireNonNull(placeholders, "placeholders");
    }

    /**
     * Creates options backed by the Paper API and PlaceholderAPI.
     *
     * @return the default options
     */
    public static @NotNull PaperTagOptions defaults() {
        return new PaperTagOptions(
            PaperDialogs::registryDialog,
            PaperDialogs::inlineDialog,
            PaperItems::hover,
            PapiSupport.renderer()
        );
    }
}
