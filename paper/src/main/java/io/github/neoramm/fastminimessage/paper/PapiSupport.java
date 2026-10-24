package io.github.neoramm.fastminimessage.paper;

import net.kyori.adventure.pointer.Pointered;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class PapiSupport {

    private static volatile boolean available;

    private PapiSupport() {
    }

    static @NotNull PapiRenderer renderer() {
        return PapiSupport::render;
    }

    private static @NotNull String render(final @Nullable Pointered pointered, final @NotNull String expression) {
        if (!isAvailable()) return expression;
        return PlaceholderApiHook.render(pointered, expression);
    }

    private static boolean isAvailable() {
        if (available) return true;
        final boolean enabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (enabled) available = true;
        return enabled;
    }
}
