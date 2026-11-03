package io.github.neoramm.fastminimessage.v2.paper;

import io.github.neoramm.fastminimessage.v2.internal.TokenPatterns;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Base64;
import java.util.Objects;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds item hovers from Paper item stacks.
 */
public final class PaperItems {
    private static final @NotNull Logger LOGGER = System.getLogger(PaperItems.class.getName());

    private PaperItems() {
    }

    /**
     * Builds the hover event for an item.
     *
     * <p>The reference is either the base64 output of {@link ItemStack#serializeAsBytes()} or a
     * material name. Material references use the requested count; byte references keep the count
     * stored in the item.</p>
     *
     * @param reference the item reference
     * @param count the stack size for material references
     * @return the hover event
     */
    public static @NotNull HoverEvent<?> hover(final @NotNull String reference, final int count) {
        Objects.requireNonNull(reference, "reference");
        if (!TokenPatterns.isSerializedItem(reference)) return item(reference, count).asHoverEvent();
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(reference)).asHoverEvent();
        } catch (final RuntimeException notItemBytes) {
            if (LOGGER.isLoggable(Level.DEBUG)) LOGGER.log(Level.DEBUG, "Invalid serialized item", notItemBytes);
            throw new IllegalArgumentException("Invalid serialized item", notItemBytes);
        }
    }

    private static @NotNull ItemStack item(final @NotNull String reference, final int count) {
        final @Nullable Material material = Material.matchMaterial(reference);
        if (material == null || material.isAir() || !material.isItem()) {
            throw new IllegalArgumentException("Unknown item: " + reference);
        }
        return ItemStack.of(material, Math.max(1, count));
    }

}
