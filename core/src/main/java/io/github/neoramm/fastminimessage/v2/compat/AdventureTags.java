package io.github.neoramm.fastminimessage.v2.compat;

import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.v2.model.Arguments;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import io.github.neoramm.fastminimessage.v2.model.RuntimeTagBridge;
import io.github.neoramm.fastminimessage.v2.model.RuntimeTagContext;
import io.github.neoramm.fastminimessage.v2.template.TemplateNode;
import java.lang.System.Logger.Level;
import java.lang.System.Logger;
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
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.internal.parser.TokenParser.TagProvider;
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

@ApiStatus.Experimental
public final class AdventureTags implements RuntimeTagBridge {

    private static final @NotNull Logger LOGGER = System.getLogger(AdventureTags.class.getName());
    private static final int MAX_PARSED_CACHE = 64;
    private static final @NotNull TagProvider NO_PREPROCESSING =
        (final var name, final var arguments, final var token) -> null;
    private static final @Nullable MethodHandle CONTEXT_CONSTRUCTOR;
    private static final @Nullable MethodHandle QUEUE_CONSTRUCTOR;
    private static final @Nullable MethodHandle TOKEN_CONSTRUCTOR;
    private static final @Nullable MethodHandle TAG_PART_CONSTRUCTOR;
    private static final @Nullable Object TEXT_TOKEN_TYPE;

    private final @NotNull RuntimeTagBridge bridge;

    static {
        MethodHandle contextCtor = null;
        MethodHandle queueCtor = null;
        MethodHandle tokenCtor = null;
        MethodHandle tagPartCtor = null;
        Object textTokenType = null;
        try {
            final @NotNull Class<?> contextClass = Class.forName("net.kyori.adventure.text.minimessage.ContextImpl");
            final @NotNull Constructor<?> cContext = contextClass.getDeclaredConstructor(
                boolean.class, boolean.class, Consumer.class, String.class,
                MiniMessage.class, Pointered.class, TagResolver.class,
                UnaryOperator.class, UnaryOperator.class
            );
            cContext.setAccessible(true);
            contextCtor = MethodHandles.lookup().unreflectConstructor(cContext);

            final @NotNull Class<?> queueClass =
                Class.forName("net.kyori.adventure.text.minimessage.ArgumentQueueImpl");
            final @NotNull Constructor<?> cQueue = queueClass.getDeclaredConstructor(Context.class, List.class);
            cQueue.setAccessible(true);
            queueCtor = MethodHandles.lookup().unreflectConstructor(cQueue);

            final @NotNull Class<?> tagPartClass =
                Class.forName("net.kyori.adventure.text.minimessage.internal.parser.node.TagPart");
            final @NotNull Class<?> tokenClass =
                Class.forName("net.kyori.adventure.text.minimessage.internal.parser.Token");
            final @NotNull Class<?> tokenTypeClass =
                Class.forName("net.kyori.adventure.text.minimessage.internal.parser.TokenType");
            final @NotNull Class<?> tagProviderClass =
                Class.forName("net.kyori.adventure.text.minimessage.internal.parser.TokenParser$TagProvider");

            final @NotNull Constructor<?> cToken = tokenClass.getConstructor(int.class, int.class, tokenTypeClass);
            final @NotNull Constructor<?> cTagPart =
                tagPartClass.getConstructor(String.class, tokenClass, tagProviderClass);

            tokenCtor = MethodHandles.lookup().unreflectConstructor(cToken);
            tagPartCtor = MethodHandles.lookup().unreflectConstructor(cTagPart);
            @SuppressWarnings({"unchecked", "rawtypes"})
            final @NotNull Object textType = Enum.valueOf((Class<Enum>) tokenTypeClass, "TEXT");
            textTokenType = textType;
        } catch (final ReflectiveOperationException | SecurityException | LinkageError unavailable) {
            LOGGER.log(Level.WARNING, "Experimental MiniMessage bridge is unavailable", unavailable);
        }
        CONTEXT_CONSTRUCTOR = contextCtor;
        QUEUE_CONSTRUCTOR = queueCtor;
        TOKEN_CONSTRUCTOR = tokenCtor;
        TAG_PART_CONSTRUCTOR = tagPartCtor;
        TEXT_TOKEN_TYPE = textTokenType;
    }

    private AdventureTags(final @NotNull RuntimeTagBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public boolean has(final @NotNull String name) {
        return bridge.has(name);
    }

    @Override
    public boolean rendersChildren() {
        return bridge.rendersChildren();
    }

    @Override
    public @Nullable Component render(final @NotNull RuntimeTagContext context) {
        return bridge.render(context);
    }

    @Override
    public boolean renderInto(final @NotNull RuntimeTagContext context,
        final TextComponent.@NotNull Builder output) {
        return bridge.renderInto(context, output);
    }

    public static @NotNull AdventureTags of(final @NotNull TagResolver... resolvers) {
        return new AdventureTags(new StandaloneTagResolver(copy(resolvers)));
    }

    public static @NotNull AdventureTags of(final @NotNull List<@NotNull TagResolver> resolvers) {
        Objects.requireNonNull(resolvers, "resolvers");
        return new AdventureTags(new StandaloneTagResolver(resolvers.toArray(new TagResolver[0])));
    }

    private static TagResolver @NotNull [] copy(final @NotNull TagResolver... resolvers) {
        Objects.requireNonNull(resolvers, "resolvers");
        final @NotNull TagResolver[] copy = resolvers.clone();
        for (final @NotNull TagResolver resolver : copy) {
            Objects.requireNonNull(resolver, "resolvers must not contain null");
        }
        return copy;
    }

    private static final class StandaloneTagResolver implements RuntimeTagBridge {

        private final @NotNull TagResolver @NotNull [] resolvers;
        private final @NotNull TagResolver combined;
        private final @NotNull Object cacheLock = new Object();
        private volatile @NotNull Map<@NotNull CacheKey, @NotNull PreComponentTree> parsedCache = Map.of();

        private StandaloneTagResolver(final @NotNull TagResolver @NotNull [] resolvers) {
            this.resolvers = resolvers;
            this.combined = TagResolver.resolver(resolvers);
        }

        @Override
        public boolean rendersChildren() {
            return true;
        }

        @Override
        public boolean has(final @NotNull String name) {
            final @NotNull String lookupName = normalize(name);
            for (int i = 0; i < resolvers.length; i++) if (resolvers[i].has(lookupName)) return true;
            return false;
        }

        @Override
        public @Nullable Component render(final @NotNull RuntimeTagContext context) {
            final @Nullable Tag tag = resolveTag(context);
            return tag == null ? null : renderTag(tag, context);
        }

        @Override
        public boolean renderInto(final @NotNull RuntimeTagContext context,
            final TextComponent.@NotNull Builder output) {
            final @Nullable Tag tag = resolveTag(context);
            if (tag == null || tag instanceof ParserDirective) return false;
            if (tag instanceof final Inserting inserting && !inserting.allowsChildren()) {
                assembleSiblingsInto(inserting.value(), context, context.selfClosing(), output);
                return true;
            }
            if (tag instanceof final PreProcess preProcess) {
                if (!context.selfClosing()) {
                    assembleSiblingsInto(parsed(context.compiler(), preProcess.value())
                        .render(context.scope().withViewer(context.pointered())), context, false, output);
                }
                return true;
            }
            final @Nullable Component rendered = renderTag(tag, context);
            if (rendered == null) return false;
            output.append(rendered);
            return true;
        }

        private @Nullable Tag resolveTag(final @NotNull RuntimeTagContext context) {
            final @NotNull String lookupName = normalize(context.name());
            List<Tag.Argument> argsList = null;
            Context kyoriContext = null;
            ArgumentQueue queue = null;
            final boolean noArgs = !context.hasArguments() && context.arguments().isEmpty();

            for (int i = 0; i < resolvers.length; i++) {
                final @NotNull TagResolver resolver = resolvers[i];
                if (!resolver.has(lookupName)) continue;
                final @Nullable Tag tag;
                try {
                    if (noArgs && resolver instanceof final TagResolver.Single single) {
                        tag = single.tag();
                    } else if (noArgs && resolver instanceof final TagResolver.WithoutArguments withoutArguments) {
                        tag = withoutArguments.resolve(lookupName);
                    } else {
                        if (argsList == null) argsList = buildArgs(context.arguments());
                        if (argsList.isEmpty() && resolver instanceof final TagResolver.Single single) {
                            tag = single.tag();
                        } else if (argsList.isEmpty()
                            && resolver instanceof final TagResolver.WithoutArguments withoutArguments) {
                            tag = withoutArguments.resolve(lookupName);
                        } else {
                            if (kyoriContext == null) kyoriContext = createContext(context.pointered(), combined);
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
                if (tag instanceof Modifying || tag instanceof PreProcess || tag instanceof Inserting) return tag;
            }
            return null;
        }

        private @Nullable Component renderTag(final @Nullable Tag tag, final @NotNull RuntimeTagContext context) {
            if (tag instanceof final Modifying modifying) {
                final @NotNull Component child =
                    renderChildren(context.children(), context.pointered(), context.scope());
                return applyModifying(modifying, child, 0);
            }
            if (tag instanceof final PreProcess preProcess) {
                if (context.selfClosing()) return Component.empty();
                final @NotNull Component parsed = parsed(context.compiler(), preProcess.value())
                    .render(context.scope().withViewer(context.pointered()));
                return assembleSiblings(parsed, context.name(), context.children(), false, context.explicitlyClosed(),
                    context.pointered(), context.scope());
            }
            if (tag instanceof final Inserting inserting) {
                final @NotNull Component value = inserting.value();
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
            final @NotNull CacheKey key = new CacheKey(compiler, value);
            final @Nullable PreComponentTree cached = parsedCache.get(key);
            if (cached != null) return cached;
            synchronized (cacheLock) {
                final @Nullable PreComponentTree concurrent = parsedCache.get(key);
                if (concurrent != null) return concurrent;
                final @NotNull PreComponentTree compiled = compiler.compile(value);
                final @NotNull LinkedHashMap<@NotNull CacheKey, @NotNull PreComponentTree> updated =
                    new LinkedHashMap<>(parsedCache);
                if (updated.size() == MAX_PARSED_CACHE) updated.pollFirstEntry();
                updated.put(key, compiled);
                parsedCache = Collections.unmodifiableMap(updated);
                return compiled;
            }
        }
    }

    private static @NotNull List<Tag.Argument> buildArgs(final @NotNull Arguments parsed) {
        if (parsed.isEmpty()) return Collections.emptyList();
        final @NotNull List<Tag.Argument> list = new ArrayList<>(parsed.size());
        for (int i = 0; i < parsed.size(); i++) {
            final Tag.@Nullable Argument arg = createArgument(parsed.get(i, ""));
            if (arg != null) list.add(arg);
        }
        return list;
    }

    private static @Nullable Context createContext(final @Nullable Pointered pointered,
        final @NotNull TagResolver resolvers) {
        if (CONTEXT_CONSTRUCTOR == null) return null;
        try {
            return (Context) CONTEXT_CONSTRUCTOR.invoke(
                false, false, null, "", MiniMessage.miniMessage(), pointered, resolvers, null, null
            );
        } catch (final Throwable failure) {
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG, "MiniMessage bridge invocation failed", failure);
            }
            return null;
        }
    }

    private static @Nullable Tag.Argument createArgument(final @NotNull String value) {
        if (TOKEN_CONSTRUCTOR == null || TAG_PART_CONSTRUCTOR == null || TEXT_TOKEN_TYPE == null) return null;
        try {
            final @NotNull String escaped = value.replace("\\", "\\\\").replace("'", "\\'");
            final @NotNull String source = !value.isEmpty() && (value.charAt(0) == '\'' || value.charAt(0) == '"')
                ? "'" + escaped + "'" : escaped;
            final @NotNull Object token = TOKEN_CONSTRUCTOR.invoke(0, source.length(), TEXT_TOKEN_TYPE);
            return (Tag.Argument) TAG_PART_CONSTRUCTOR.invoke(source, token, NO_PREPROCESSING);
        } catch (final Throwable failure) {
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG, "MiniMessage bridge invocation failed", failure);
            }
            return null;
        }
    }

    private static @Nullable ArgumentQueue createQueue(final @NotNull Context context,
        final @NotNull List<Tag.Argument> args) {
        if (QUEUE_CONSTRUCTOR == null) return null;
        try {
            return (ArgumentQueue) QUEUE_CONSTRUCTOR.invoke(context, args);
        } catch (final Throwable failure) {
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG, "MiniMessage bridge invocation failed", failure);
            }
            return null;
        }
    }

    private static @NotNull Component renderChildren(final @NotNull List<@NotNull TemplateNode> children,
        final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        if (children.isEmpty()) return Component.empty();
        if (children.size() == 1) return children.getFirst().render(scope.withViewer(pointered));
        final TextComponent.@NotNull Builder builder = Component.text();
        final @NotNull RenderScope context = scope.withViewer(pointered);
        for (final @NotNull TemplateNode child : children) child.renderInto(context, builder);
        return builder.build();
    }

    private static @NotNull Component applyModifying(final @NotNull Modifying modifying,
        final @NotNull Component current, final int depth) {
        final @NotNull List<@NotNull Component> children = current.children();
        final @NotNull Component self = children.isEmpty() ? current : current.children(List.of());
        final @NotNull Component result = modifying.apply(self, depth);
        if (children.isEmpty()) return result;
        final @NotNull List<@NotNull Component> combined =
            new ArrayList<>(result.children().size() + children.size());
        combined.addAll(result.children());
        for (int i = 0, size = children.size(); i < size; i++) {
            final @NotNull Component child = applyModifying(modifying, children.get(i), depth + 1);
            if (child != Component.empty()) combined.add(child);
        }
        return result.children(combined);
    }

    private static void assembleSiblingsInto(final @NotNull Component base, final @NotNull RuntimeTagContext context,
        final boolean selfClosing, final TextComponent.@NotNull Builder output) {
        output.append(base);
        final @NotNull RenderScope render = context.scope().withViewer(context.pointered());
        final @NotNull List<@NotNull TemplateNode> children = context.children();
        for (int i = 0, size = children.size(); i < size; i++) children.get(i).renderInto(render, output);
        if (!selfClosing && context.explicitlyClosed()) output.append(Component.text("</" + context.name() + ">"));
    }

    private static @NotNull Component appendChildren(final @NotNull Component base,
        final @NotNull List<@NotNull TemplateNode> children, final @Nullable Pointered pointered,
        final @NotNull RenderScope scope) {
        if (children.isEmpty()) return base;
        final @NotNull List<@NotNull Component> combined = new ArrayList<>(base.children().size() + children.size());
        combined.addAll(base.children());
        final @NotNull RenderScope context = scope.withViewer(pointered);
        for (final @NotNull TemplateNode child : children) combined.add(child.render(context));
        return base.children(combined);
    }

    private static @NotNull Component assembleSiblings(final @NotNull Component base, final @NotNull String name,
        final @NotNull List<@NotNull TemplateNode> children, final boolean selfClosing, final boolean explicitlyClosed,
        final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        final boolean closing = !selfClosing && explicitlyClosed;
        if (children.isEmpty() && !closing) return base;
        final TextComponent.@NotNull Builder builder = Component.text().append(base);
        final @NotNull RenderScope context = scope.withViewer(pointered);
        for (final @NotNull TemplateNode child : children) child.renderInto(context, builder);
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
