package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.resolver.ClickResolver;
import io.github.neoramm.fastminimessage.resolver.ColorResolver;
import io.github.neoramm.fastminimessage.resolver.FontResolver;
import io.github.neoramm.fastminimessage.resolver.GradientResolver;
import io.github.neoramm.fastminimessage.resolver.HoverResolver;
import io.github.neoramm.fastminimessage.resolver.InsertResolver;
import io.github.neoramm.fastminimessage.resolver.KeybindResolver;
import io.github.neoramm.fastminimessage.resolver.NbtResolver;
import io.github.neoramm.fastminimessage.resolver.NewlineResolver;
import io.github.neoramm.fastminimessage.resolver.PlayerHeadResolver;
import io.github.neoramm.fastminimessage.resolver.PrideResolver;
import io.github.neoramm.fastminimessage.resolver.RainbowResolver;
import io.github.neoramm.fastminimessage.resolver.ResetResolver;
import io.github.neoramm.fastminimessage.resolver.ScoreResolver;
import io.github.neoramm.fastminimessage.resolver.SelectorResolver;
import io.github.neoramm.fastminimessage.resolver.ShadowResolver;
import io.github.neoramm.fastminimessage.resolver.SpriteResolver;
import io.github.neoramm.fastminimessage.resolver.StyleResolver;
import io.github.neoramm.fastminimessage.resolver.TransitionResolver;
import io.github.neoramm.fastminimessage.resolver.TranslatableFallbackResolver;
import io.github.neoramm.fastminimessage.resolver.TranslatableResolver;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registry of tag resolvers used by the compiler.
 */
public final class ResolverRegistry {

    private final @NotNull Map<@NotNull String, @NotNull ComponentResolver> resolvers;

    private ResolverRegistry(final @NotNull Map<@NotNull String, @NotNull ComponentResolver> resolvers) {
        this.resolvers = resolvers;
    }

    public static @NotNull ResolverRegistry create() {
        return new ResolverRegistry(new HashMap<>());
    }

    /**
     * Creates a registry populated with all built-in MiniMessage tag resolvers.
     *
     * @return a new registry with default resolvers
     */
    public static @NotNull ResolverRegistry withDefaults() {
        final ResolverRegistry registry = create();
        for (final NamedTextColor color : NamedTextColor.NAMES.values()) {
            registry.register(new ColorResolver(color.toString(), color));
        }
        registry
            .register(new ColorResolver("grey", NamedTextColor.GRAY))
            .register(new ColorResolver("dark_grey", NamedTextColor.DARK_GRAY))
            .register(new ColorResolver(new String[]{"color", "colour", "c"}, null))
            .register(new StyleResolver(TextDecoration.BOLD, "bold", "b", "!bold", "!b"))
            .register(new StyleResolver(TextDecoration.ITALIC, "italic", "i", "em", "!italic", "!i", "!em"))
            .register(new StyleResolver(TextDecoration.UNDERLINED, "underlined", "!underlined", "u", "!u"))
            .register(new StyleResolver(TextDecoration.STRIKETHROUGH, "strikethrough", "!strikethrough", "st", "!st"))
            .register(new StyleResolver(TextDecoration.OBFUSCATED, "obfuscated", "!obfuscated", "obf", "!obf"))
            .register(new ResetResolver())
            .register(new HoverResolver())
            .register(new ClickResolver())
            .register(new GradientResolver())
            .register(new RainbowResolver())
            .register(new KeybindResolver())
            .register(new TranslatableResolver())
            .register(new TranslatableFallbackResolver())
            .register(new ShadowResolver())
            .register(new InsertResolver())
            .register(new NewlineResolver())
            .register(new PrideResolver());
        try {
            registry.register(new SpriteResolver());
        } catch (final LinkageError olderAdventure) {
            registry.unregister("sprite");
        }
        try {
            registry.register(new PlayerHeadResolver());
        } catch (final LinkageError olderAdventure) {
            registry.unregister("head");
        }
        registry
            .register(new FontResolver())
            .register(new TransitionResolver())
            .register(new SelectorResolver())
            .register(new ScoreResolver())
            .register(new NbtResolver());
        return registry;
    }

    /**
     * Registers a resolver, replacing any existing resolver with lower priority for the same names.
     *
     * @param resolver the resolver to register
     * @return this registry for chaining
     */
    public @NotNull ResolverRegistry register(final @NotNull ComponentResolver resolver) {
        for (final String name : resolver.names()) {
            final ComponentResolver existing = resolvers.get(name);
            if (existing == null || resolver.priority() > existing.priority()) {
                resolvers.put(name, resolver);
                if (hasUpperCase(name)) resolvers.putIfAbsent(name.toLowerCase(Locale.ROOT), resolver);
            }
        }
        return this;
    }

    public @NotNull ResolverRegistry registerAll(final @NotNull Collection<@NotNull ComponentResolver> others) {
        for (final ComponentResolver resolver : others) register(resolver);
        return this;
    }

    public @NotNull ResolverRegistry unregister(final @NotNull String tagName) {
        resolvers.remove(tagName);
        return this;
    }

    /**
     * Returns the resolver registered for the given tag name, or null when none exists.
     *
     * @param tagName the tag name to look up
     * @return the matching resolver, or null
     */
    public @Nullable ComponentResolver resolve(final @NotNull String tagName) {
        final @Nullable ComponentResolver resolver = resolvers.get(tagName);
        if (resolver != null) return resolver;
        if (hasUpperCase(tagName)) {
            final @Nullable ComponentResolver lower = resolvers.get(tagName.toLowerCase(Locale.ROOT));
            if (lower != null) return lower;
        }
        if (tagName.startsWith("#")) return resolvers.get("color");
        return null;
    }

    private static boolean hasUpperCase(final @NotNull String str) {
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);
            if (c >= 'A' && c <= 'Z') return true;
        }
        return false;
    }
}
