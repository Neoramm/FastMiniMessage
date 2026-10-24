package io.github.neoramm.fastminimessage.paper;

import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;

/**
 * Builds item hovers from Paper item stacks.
 */
public final class PaperItems {

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
        return item(reference, count).asHoverEvent();
    }

    private static @NotNull ItemStack item(final @NotNull String reference, final int count) {
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(reference));
        } catch (final RuntimeException notItemBytes) {
            final @Nullable Material material = Material.matchMaterial(reference);
            if (material == null || material.isAir()) {
                throw new IllegalArgumentException("Unknown item: " + reference, notItemBytes);
            }
            return ItemStack.of(material, Math.max(1, count));
        }
    }
}
