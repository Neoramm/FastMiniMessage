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
        final @Nullable Material material = Material.matchMaterial(reference);
        if (material != null && !material.isAir()) return ItemStack.of(material, Math.max(1, count));
        if (!isBase64(reference)) throw new IllegalArgumentException("Unknown item: " + reference);
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(reference));
        } catch (final RuntimeException notItemBytes) {
            throw new IllegalArgumentException("Unknown item: " + reference, notItemBytes);
        }
    }

    private static boolean isBase64(final @NotNull String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                || c == '+' || c == '/' || c == '=')) return false;
        }
        return true;
    }
}
