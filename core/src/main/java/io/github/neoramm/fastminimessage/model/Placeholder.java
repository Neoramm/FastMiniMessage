package io.github.neoramm.fastminimessage.model;

import io.github.neoramm.fastminimessage.component.InlinePreComponent;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Binds a placeholder key to a component substituted at render time.
 */
public record Placeholder(@NotNull String key, @NotNull PreComponent preComponent) implements TagResolver {

    @Override
    public @NotNull String name() {
        return key;
    }

    /**
     * Creates an unparsed placeholder that renders the given plain text.
     *
     * @param key   the placeholder key
     * @param plain the plain text to render
     * @return the placeholder
     */
    public static @NotNull Placeholder ofUnparsed(final @NotNull String key, final @NotNull String plain) {
        return new Placeholder(key, new InlinePreComponent(Component.text(plain)));
    }

    /**
     * Creates a parsed placeholder that renders the given component.
     *
     * @param key       the placeholder key
     * @param component the component to render
     * @return the placeholder
     */
    public static @NotNull Placeholder ofParsed(final @NotNull String key, final @NotNull Component component) {
        return new Placeholder(key, new InlinePreComponent(component));
    }

    public static @NotNull Placeholder of(final @NotNull String key, final @NotNull PreComponent preComponent) {
        return new Placeholder(key, preComponent);
    }
}
