package io.github.neoramm.fastminimessage.compat;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import io.github.neoramm.fastminimessage.model.RenderScope;
import io.github.neoramm.fastminimessage.model.RuntimeTagContext;
import io.github.neoramm.fastminimessage.model.RuntimeTagResolver;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Inserting;
import net.kyori.adventure.text.minimessage.tag.Modifying;
import net.kyori.adventure.text.minimessage.tag.ParserDirective;
import net.kyori.adventure.text.minimessage.tag.PreProcess;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Experimental standalone bridge for Kyori MiniMessage {@link TagResolver} instances without MiniMessage.
 */
@ApiStatus.Experimental
public final class ExperimentalMiniMessageTags {

    private static final int MAX_PARSED_CACHE = 64;
    private static final @Nullable MethodHandle CONTEXT_CONSTRUCTOR;
    private static final @Nullable MethodHandle QUEUE_CONSTRUCTOR;
    private static final @Nullable MethodHandle TOKEN_CONSTRUCTOR;
    private static final @Nullable MethodHandle TAG_PART_CONSTRUCTOR;
    private static final @Nullable Object TEXT_TOKEN_TYPE;

    static {
        MethodHandle contextCtor = null;
        MethodHandle queueCtor = null;
        MethodHandle tokenCtor = null;
        MethodHandle tagPartCtor = null;
        Object textTokenType = null;
        try {
            final Class<?> contextClass = Class.forName("net.kyori.adventure.text.minimessage.ContextImpl");
            final Constructor<?> cContext = contextClass.getDeclaredConstructor(
                boolean.class, boolean.class, Consumer.class, String.class,
                MiniMessage.class, Pointered.class, TagResolver.class,
                UnaryOperator.class, UnaryOperator.class
            );
            cContext.setAccessible(true);
            contextCtor = MethodHandles.lookup().unreflectConstructor(cContext);

            final Class<?> queueClass = Class.forName("net.kyori.adventure.text.minimessage.ArgumentQueueImpl");
            final Constructor<?> cQueue = queueClass.getDeclaredConstructor(Context.class, List.class);
            cQueue.setAccessible(true);
            queueCtor = MethodHandles.lookup().unreflectConstructor(cQueue);

            final Class<?> tagPartClass =
                Class.forName("net.kyori.adventure.text.minimessage.internal.parser.node.TagPart");
            final Class<?> tokenClass =
                Class.forName("net.kyori.adventure.text.minimessage.internal.parser.Token");
            final Class<?> tokenTypeClass =
                Class.forName("net.kyori.adventure.text.minimessage.internal.parser.TokenType");
            final Class<?> tagProviderClass =
                Class.forName("net.kyori.adventure.text.minimessage.internal.parser.TokenParser$TagProvider");

            final Constructor<?> cToken = tokenClass.getConstructor(int.class, int.class, tokenTypeClass);
            final Constructor<?> cTagPart = tagPartClass.getConstructor(String.class, tokenClass, tagProviderClass);

            tokenCtor = MethodHandles.lookup().unreflectConstructor(cToken);
            tagPartCtor = MethodHandles.lookup().unreflectConstructor(cTagPart);
            @SuppressWarnings({"unchecked", "rawtypes"})
            final Object textType = Enum.valueOf((Class<Enum>) tokenTypeClass, "TEXT");
            textTokenType = textType;
        } catch (final Throwable ignored) {
        }
        CONTEXT_CONSTRUCTOR = contextCtor;
        QUEUE_CONSTRUCTOR = queueCtor;
        TOKEN_CONSTRUCTOR = tokenCtor;
        TAG_PART_CONSTRUCTOR = tagPartCtor;
        TEXT_TOKEN_TYPE = textTokenType;
    }

    private ExperimentalMiniMessageTags() {
    }

    /**
     * Adapts MiniMessage resolvers into a FastMiniMessage runtime resolver without relying on MiniMessage instances.
     *
     * @param resolvers the Kyori tag resolvers to adapt
     * @return an experimental runtime tag resolver
     */
    public static @NotNull RuntimeTagResolver of(final @NotNull TagResolver... resolvers) {
        return new StandaloneTagResolver(copy(resolvers));
    }

    /**
     * Adapts a list of MiniMessage resolvers into a FastMiniMessage runtime resolver without MiniMessage instances.
     *
     * @param resolvers the list of Kyori tag resolvers to adapt
     * @return an experimental runtime tag resolver
     */
    public static @NotNull RuntimeTagResolver of(final @NotNull List<@NotNull TagResolver> resolvers) {
        Objects.requireNonNull(resolvers, "resolvers");
        return new StandaloneTagResolver(resolvers.toArray(new TagResolver[0]));
    }

    private static TagResolver @NotNull [] copy(final @NotNull TagResolver... resolvers) {
        Objects.requireNonNull(resolvers, "resolvers");
        final TagResolver[] copy = resolvers.clone();
        for (final TagResolver resolver : copy) Objects.requireNonNull(resolver, "resolvers must not contain null");
        return copy;
    }

    private static final class StandaloneTagResolver implements RuntimeTagResolver {

        private final TagResolver @NotNull [] resolvers;
        private final @NotNull Map<CacheKey, PreComponentTree> parsedCache =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<CacheKey, PreComponentTree> eldest) {
                    return size() > MAX_PARSED_CACHE;
                }
            };

        private StandaloneTagResolver(final TagResolver @NotNull [] resolvers) {
            this.resolvers = resolvers;
        }

        @Override
        public boolean has(final @NotNull String name) {
            final String lookupName = normalize(name);
            for (int i = 0; i < resolvers.length; i++) if (resolvers[i].has(lookupName)) return true;
            return false;
        }

        @Override
        public @Nullable Component render(final @NotNull RuntimeTagContext context) {
            final String lookupName = normalize(context.name());
            if (!has(lookupName)) return null;

            List<Tag.Argument> argsList = null;
            Context kyoriContext = null;
            ArgumentQueue queue = null;
            final boolean noArgs = !context.hasArguments() && context.rawArgs().isEmpty();

            for (int i = 0; i < resolvers.length; i++) {
                final TagResolver resolver = resolvers[i];
                if (!resolver.has(lookupName)) continue;
                final Tag tag;
                try {
                    if (noArgs && resolver instanceof final TagResolver.Single single) {
                        tag = single.tag();
                    } else if (noArgs && resolver instanceof final TagResolver.WithoutArguments withoutArguments) {
                        tag = withoutArguments.resolve(lookupName);
                    } else {
                        if (argsList == null) argsList = buildArgs(context.rawArgs(), context.hasArguments());
                        if (argsList.isEmpty() && resolver instanceof final TagResolver.Single single) {
                            tag = single.tag();
                        } else if (argsList.isEmpty()
                            && resolver instanceof final TagResolver.WithoutArguments withoutArguments) {
                            tag = withoutArguments.resolve(lookupName);
                        } else {
                            if (kyoriContext == null) kyoriContext = createContext(context.pointered());
                            if (kyoriContext != null && queue == null) queue = createQueue(kyoriContext, argsList);
                            if (queue != null) queue.reset();
                            tag = kyoriContext != null && queue != null
                                ? resolver.resolve(lookupName, queue, kyoriContext)
                                : null;
                        }
                    }
                } catch (final ParsingException e) {
                    return null;
                }
                if (tag == null) continue;
                if (tag instanceof ParserDirective) return null;
                final Component rendered = renderTag(tag, context);
                if (rendered != null) return rendered;
            }
            return null;
        }

        private @Nullable Component renderTag(final @NotNull Tag tag, final @NotNull RuntimeTagContext context) {
            if (tag instanceof final Modifying modifying) {
                final Component child = renderChildren(context.children(), context.pointered(), context.scope());
                return modifying.apply(child, 0);
            }
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

    private static @NotNull List<Tag.Argument> buildArgs(final @NotNull String rawArgs, final boolean hasArguments) {
        if (!hasArguments && rawArgs.isEmpty()) return Collections.emptyList();
        final Arguments parsed = Arguments.parse(rawArgs);
        if (parsed.isEmpty()) return Collections.emptyList();
        final List<Tag.Argument> list = new ArrayList<>(parsed.size());
        for (int i = 0; i < parsed.size(); i++) {
            final Tag.Argument arg = createArgument(parsed.get(i, ""));
            if (arg != null) list.add(arg);
        }
        return list;
    }

    private static @Nullable Context createContext(final @Nullable Pointered pointered) {
        if (CONTEXT_CONSTRUCTOR == null) return null;
        try {
            return (Context) CONTEXT_CONSTRUCTOR.invoke(
                false, false, null, "", null, pointered, TagResolver.empty(), null, null
            );
        } catch (final Throwable e) {
            return null;
        }
    }

    private static @Nullable Tag.Argument createArgument(final @NotNull String value) {
        if (TOKEN_CONSTRUCTOR == null || TAG_PART_CONSTRUCTOR == null || TEXT_TOKEN_TYPE == null) return null;
        try {
            final Object token = TOKEN_CONSTRUCTOR.invoke(0, value.length(), TEXT_TOKEN_TYPE);
            return (Tag.Argument) TAG_PART_CONSTRUCTOR.invoke(value, token, null);
        } catch (final Throwable e) {
            return null;
        }
    }

    private static @Nullable ArgumentQueue createQueue(final @NotNull Context context,
        final @NotNull List<Tag.Argument> args) {
        if (QUEUE_CONSTRUCTOR == null) return null;
        try {
            return (ArgumentQueue) QUEUE_CONSTRUCTOR.invoke(context, args);
        } catch (final Throwable e) {
            return null;
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
