package io.github.neoramm.fastminimessage.model;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves an otherwise unknown tag at render time.
 *
 * <p>This is the integration point for external tag systems such as MiniMessage's
 * {@code TagResolver}, without adding a runtime dependency to the core module.</p>
 */
public interface RuntimeTagResolver extends RuntimeResolver {

    /**
     * Indicates whether this resolver can handle the given tag name.
     */
    boolean has(final @NotNull String name);

    /**
     * Renders the tag for the current context.
     *
     * @param context the tag inputs and rendering context
     * @return the rendered component, or {@code null} when the tag should keep its literal fallback
     */
    @Nullable Component render(final @NotNull RuntimeTagContext context);
}
