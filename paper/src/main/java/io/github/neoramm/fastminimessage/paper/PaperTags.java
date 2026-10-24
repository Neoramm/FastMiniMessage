package io.github.neoramm.fastminimessage.paper;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.ResolverRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Paper API tags for FastMiniMessage.
 *
 * <p>{@link #compiler()} and {@link #compiler(PaperTagOptions)} create a compiler with the Paper
 * tags and the dialog resolver already installed. {@link #withDefaults()} only adds the tag
 * resolvers, for callers that already own a compiler.</p>
 */
public final class PaperTags {

    private PaperTags() {
    }

    /**
     * Creates a registry with the core tags plus the Paper tags.
     *
     * @return the registry
     */
    public static @NotNull ResolverRegistry withDefaults() {
        return withDefaults(PaperTagOptions.defaults());
    }

    /**
     * Creates a registry with the core tags plus the Paper tags using the given factories.
     *
     * @param options the factories for dialogs, items and PlaceholderAPI
     * @return the registry
     */
    public static @NotNull ResolverRegistry withDefaults(final @NotNull PaperTagOptions options) {
        Objects.requireNonNull(options, "options");
        return apply(ResolverRegistry.withDefaults(), options);
    }

    /**
     * Adds the Paper tags to an existing registry.
     *
     * @param registry the registry to extend
     * @param options the factories for dialogs, items and PlaceholderAPI
     * @return the same registry
     */
    public static @NotNull ResolverRegistry apply(final @NotNull ResolverRegistry registry,
        final @NotNull PaperTagOptions options) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(options, "options");
        return registry
            .register(new InlineDialogResolver(options.inlineDialogs()))
            .register(new ItemHoverResolver(options.itemHovers()))
            .register(new PapiResolver(options.placeholders()));
    }

    /**
     * Creates a compiler configured with the default Paper tags.
     *
     * @return the compiler
     */
    public static @NotNull MiniMessageCompiler compiler() {
        return compiler(PaperTagOptions.defaults());
    }

    /**
     * Creates a compiler configured with the given Paper factories.
     *
     * @param options the factories for dialogs, items and PlaceholderAPI
     * @return the compiler
     */
    public static @NotNull MiniMessageCompiler compiler(final @NotNull PaperTagOptions options) {
        return compiler(withDefaults(options), options);
    }

    /**
     * Creates a compiler from an existing registry and the given Paper factories.
     *
     * @param registry a registry that already contains the Paper tags
     * @param options the factories for dialogs, items and PlaceholderAPI
     * @return the compiler
     */
    public static @NotNull MiniMessageCompiler compiler(final @NotNull ResolverRegistry registry,
        final @NotNull PaperTagOptions options) {
        return new MiniMessageCompiler(registry).dialogResolver(options.registryDialogs());
    }
}
