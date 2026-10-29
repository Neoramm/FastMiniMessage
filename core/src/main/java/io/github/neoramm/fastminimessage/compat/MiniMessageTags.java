package io.github.neoramm.fastminimessage.compat;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import io.github.neoramm.fastminimessage.model.RenderScope;
import io.github.neoramm.fastminimessage.model.RuntimeTagContext;
import io.github.neoramm.fastminimessage.model.RuntimeTagResolver;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Inserting;
import net.kyori.adventure.text.minimessage.tag.Modifying;
import net.kyori.adventure.text.minimessage.tag.ParserDirective;
import net.kyori.adventure.text.minimessage.tag.PreProcess;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Bridges MiniMessage {@link TagResolver} instances into FastMiniMessage's runtime tag resolution.
 *
 * <p>The fast path supports resolvers that do not depend on MiniMessage's sealed {@code Context}:
 * {@link TagResolver.Single} (including every {@code Placeholder} factory) and
 * {@link TagResolver.WithoutArguments}, including composed resolvers such as
 * {@code TagResolver.resolver(a, b)} that MiniMessage represents as a map.</p>
 *
 * <p>Argument-based resolvers and {@link Modifying} tags cannot be evaluated through the public
 * tag API. When a {@link MiniMessage} instance is supplied through
 * {@link #of(MiniMessage, TagResolver...)}, those tags are delegated to MiniMessage on a small
 * reconstructed fragment instead; without an instance they keep the literal fallback.</p>
 *
 * <p>{@link ParserDirective} tags cannot be delegated because they alter parser state outside the
 * reconstructed fragment; they always keep the literal fallback.</p>
 *
 * <p>Tags that reject arguments keep the library's literal fallback instead of throwing. Bridged
 * tag names are matched case-insensitively, as MiniMessage does.</p>
 *
 * <p>{@code Placeholder.parsed} values are compiled once and inserted as a component. A parsed
 * value that opens a tag and closes it outside the placeholder boundary cannot be reproduced
 * because the surrounding input was precompiled before the value was known.</p>
 *
 * <p>This class is optional: the core module does not require {@code adventure-text-minimessage}.
 * Add it to the classpath only when this bridge is used.</p>
 */
public final class MiniMessageTags {

    private static final int MAX_PARSED_CACHE = 64;
    private static final @NotNull String CHILD_TAG = "fastminimessage_children";

    private MiniMessageTags() {
    }

    /**
     * Adapts MiniMessage tag resolvers so they can be passed to
     * {@code PreComponentTree#render(Pointered, RuntimeResolver...)}.
     *
     * <p>Resolvers that the fast path cannot handle keep the literal fallback. Use
     * {@link #of(MiniMessage, TagResolver...)} to delegate those cases to a real
     * MiniMessage instance instead.</p>
     *
     * @param resolvers the MiniMessage resolvers to adapt
     * @return a runtime tag resolver backed by the given resolvers
     */
    public static @NotNull RuntimeTagResolver of(final @NotNull TagResolver... resolvers) {
        return new AdventureTagResolver(null, copy(resolvers));
    }

    /**
     * Adapts MiniMessage tag resolvers, delegating unsupported tags to the given MiniMessage
     * instance.
     *
     * <p>Placeholders and fixed tags still use the fast path. Argument-based resolvers and
     * {@code Modifying} tags from the supplied array are rendered by MiniMessage on a small
     * reconstructed fragment, so they keep their original semantics at the cost of a slower
     * fallback. Tags must be present in the resolver array to be recognized; the instance supplies
     * the parser and its configured tag set for the delegated fragment.</p>
     *
     * @param miniMessage the MiniMessage instance used to resolve unsupported tags
     * @param resolvers the MiniMessage resolvers to adapt
     * @return a runtime tag resolver backed by the given resolvers
     */
    public static @NotNull RuntimeTagResolver of(final @NotNull MiniMessage miniMessage,
        final @NotNull TagResolver... resolvers) {
        Objects.requireNonNull(miniMessage, "miniMessage");
        return new AdventureTagResolver(miniMessage, copy(resolvers));
    }

    private static TagResolver @NotNull [] copy(final @NotNull TagResolver... resolvers) {
        Objects.requireNonNull(resolvers, "resolvers");
        final TagResolver[] copy = resolvers.clone();
        for (final TagResolver resolver : copy) Objects.requireNonNull(resolver, "resolvers must not contain null");
        return copy;
    }

    private static final class AdventureTagResolver implements RuntimeTagResolver {

        private final @Nullable MiniMessage miniMessage;
        private final TagResolver @NotNull [] resolvers;
        private final @NotNull Map<CacheKey, PreComponentTree> parsedCache =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<CacheKey, PreComponentTree> eldest) {
                    return size() > MAX_PARSED_CACHE;
                }
            };

        private AdventureTagResolver(final @Nullable MiniMessage miniMessage,
            final TagResolver @NotNull [] resolvers) {
            this.miniMessage = miniMessage;
            this.resolvers = resolvers;
        }

        @Override
        public boolean has(final @NotNull String name) {
            final String lookupName = normalize(name);
            for (final TagResolver resolver : resolvers) if (resolver.has(lookupName)) return true;
            return false;
        }

        @Override
        public @Nullable Component render(final @NotNull RuntimeTagContext context) {
            final String lookupName = normalize(context.name());
            if (context.hasArguments() || !context.rawArgs().isEmpty()) {
                return hasAny(lookupName) ? delegate(context) : null;
            }
            boolean matched = false;
            for (final TagResolver resolver : resolvers) {
                if (!resolver.has(lookupName)) continue;
                matched = true;
                final Tag tag;
                try {
                    tag = resolveTag(resolver, lookupName);
                } catch (final ParsingException malformed) {
                    return delegate(context);
                }
                if (tag == null) continue;
                if (tag instanceof ParserDirective) return null;
                final Component rendered = renderTag(tag, context);
                if (rendered != null) return rendered;
                return delegate(context);
            }
            return matched || hasAny(lookupName) ? delegate(context) : null;
        }

        private boolean hasAny(final @NotNull String lookupName) {
            return has(lookupName);
        }

        private @Nullable Component delegate(final @NotNull RuntimeTagContext context) {
            final MiniMessage mini = miniMessage;
            if (mini == null) return null;
            final PreComponent @NotNull [] children = context.children();
            final boolean hasChildren = children.length > 0;
            final TagResolver[] delegated;
            if (hasChildren) {
                delegated = new TagResolver[resolvers.length + 1];
                System.arraycopy(resolvers, 0, delegated, 0, resolvers.length);
                delegated[resolvers.length] = Placeholder.component(CHILD_TAG,
                    renderChildren(children, context.pointered(), context.scope()));
            } else {
                delegated = resolvers;
            }
            final String fragment = fragment(context.name(), context.rawArgs(), context.hasArguments(),
                context.selfClosing(), context.explicitlyClosed(), hasChildren);
            try {
                return context.pointered() == null
                    ? mini.deserialize(fragment, delegated)
                    : mini.deserialize(fragment, context.pointered(), delegated);
            } catch (final RuntimeException malformed) {
                return null;
            }
        }

        private @Nullable Tag resolveTag(final @NotNull TagResolver resolver, final @NotNull String name) {
            if (resolver instanceof final TagResolver.Single single) return single.tag();
            if (resolver instanceof final TagResolver.WithoutArguments withoutArguments) {
                return withoutArguments.resolve(name);
            }
            return null;
        }

        private @Nullable Component renderTag(final @NotNull Tag tag, final @NotNull RuntimeTagContext context) {
            if (tag instanceof Modifying) return null;
            if (tag instanceof final PreProcess preProcess) {
                if (context.selfClosing()) return Component.empty();
                final Component parsed = parsed(context.compiler(), preProcess.value())
                    .render(context.pointered(), context.scope());
                return assembleSiblings(parsed, context.name(), context.children(), false, context.explicitlyClosed(),
                    context.pointered(), context.scope());
            }
            if (tag instanceof final Inserting inserting) {
                final Component value = inserting.value();
                if (inserting.allowsChildren()) {
                    return appendChildren(value, context.children(), context.pointered(), context.scope());
                }
                return assembleSiblings(value, context.name(), context.children(), context.selfClosing(),
                    context.explicitlyClosed(), context.pointered(), context.scope());
            }
            return null;
        }

        private @NotNull PreComponentTree parsed(final @NotNull MiniMessageCompiler compiler,
            final @NotNull String value) {
            final CacheKey key = new CacheKey(compiler, value);
            synchronized (parsedCache) {
                PreComponentTree cached = parsedCache.get(key);
                if (cached == null) {
                    cached = compiler.compile(value);
                    parsedCache.put(key, cached);
                }
                return cached;
            }
        }
    }

    private static @NotNull Component renderChildren(final PreComponent @NotNull [] children,
        final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        if (children.length == 0) return Component.empty();
        if (children.length == 1) return children[0].render(pointered, scope);
        final TextComponent.Builder builder = Component.text();
        for (final PreComponent child : children) builder.append(child.render(pointered, scope));
        return builder.build();
    }

    private static @NotNull String fragment(final @NotNull String name, final @NotNull String rawArgs,
        final boolean hasArguments, final boolean selfClosing, final boolean explicitlyClosed,
        final boolean hasChildren) {
        final StringBuilder out = new StringBuilder(name.length() + rawArgs.length() + 32);
        out.append('<').append(name);
        if (hasArguments || !rawArgs.isEmpty()) out.append(':').append(rawArgs);
        if (selfClosing) return out.append("/>").toString();
        out.append('>');
        if (hasChildren) out.append('<').append(CHILD_TAG).append("/>");
        if (explicitlyClosed) out.append("</").append(name).append('>');
        return out.toString();
    }

    private static @NotNull Component appendChildren(final @NotNull Component base,
        final PreComponent @NotNull [] children, final @Nullable Pointered pointered,
        final @NotNull RenderScope scope) {
        if (children.length == 0) return base;
        final List<Component> combined = new ArrayList<>(base.children().size() + children.length);
        combined.addAll(base.children());
        for (final PreComponent child : children) combined.add(child.render(pointered, scope));
        return base.children(combined);
    }

    private static @NotNull Component assembleSiblings(final @NotNull Component base, final @NotNull String name,
        final PreComponent @NotNull [] children, final boolean selfClosing, final boolean explicitlyClosed,
        final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        final boolean closing = !selfClosing && explicitlyClosed;
        if (children.length == 0 && !closing) return base;
        final TextComponent.Builder builder = Component.text().append(base);
        for (final PreComponent child : children) builder.append(child.render(pointered, scope));
        if (closing) builder.append(Component.text("</" + name + ">"));
        return builder.build();
    }

    private static @NotNull String normalize(final @NotNull String name) {
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (c >= 'A' && c <= 'Z') return name.toLowerCase(Locale.ROOT);
        }
        return name;
    }

    private record CacheKey(@NotNull MiniMessageCompiler compiler, @NotNull String value) {
    }
}
