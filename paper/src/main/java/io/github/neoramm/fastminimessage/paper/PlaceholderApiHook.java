package io.github.neoramm.fastminimessage.paper;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.pointer.Pointered;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class PlaceholderApiHook {

    private PlaceholderApiHook() {
    }

    static @NotNull String render(final @Nullable Pointered pointered, final @NotNull String expression) {
        final @Nullable Player player = pointered instanceof final Player candidate ? candidate : null;
        final String formatted = expression.startsWith("%") && expression.endsWith("%")
            ? expression
            : "%" + expression + "%";
        final @Nullable String result = PlaceholderAPI.setPlaceholders(player, formatted);
        return result == null ? formatted : result;
    }
}
