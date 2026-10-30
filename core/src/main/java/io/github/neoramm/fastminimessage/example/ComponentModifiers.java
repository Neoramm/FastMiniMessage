package io.github.neoramm.fastminimessage.example;

import io.github.neoramm.fastminimessage.api.ComponentModifier;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Example (non-registered) component modifiers for documentation purposes.
 */
public final class ComponentModifiers {
    private ComponentModifiers() {
    }

    private static @NotNull String plain(final @NotNull Component component) {
        final StringBuilder builder = new StringBuilder();
        if (component instanceof final TextComponent text) builder.append(text.content());
        for (final Component child : component.children()) builder.append(plain(child));
        return builder.toString();
    }

    public static @NotNull ComponentModifier uppercase() {
        return (final @NotNull Component rendered, final @Nullable Pointered context) ->
            Component.text(plain(rendered).toUpperCase(Locale.ROOT), rendered.style());
    }

    public static @NotNull ComponentModifier staticColor(final @NotNull NamedTextColor color) {
        return new ComponentModifier() {
            @Override
            public @NotNull Component modify(final @NotNull Component rendered, final @Nullable Pointered context) {
                return rendered.colorIfAbsent(color);
            }

            @Override
            public boolean isStatic() {
                return true;
            }
        };
    }

    public static @NotNull ComponentModifier boldAll() {
        return (final @NotNull Component rendered, final @Nullable Pointered context) ->
            rendered.style(s -> s.decoration(TextDecoration.BOLD, TextDecoration.State.TRUE));
    }
}
