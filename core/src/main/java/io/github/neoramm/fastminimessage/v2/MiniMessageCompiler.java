package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.api.ComponentModifier;
import io.github.neoramm.fastminimessage.v2.api.DialogResolver;
import io.github.neoramm.fastminimessage.v2.api.TagScanner;
import io.github.neoramm.fastminimessage.v2.internal.DefaultTagScanner;
import io.github.neoramm.fastminimessage.v2.internal.TokenPatterns;
import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import io.github.neoramm.fastminimessage.v2.model.TagNode;
import io.github.neoramm.fastminimessage.v2.tag.CompileContext;
import io.github.neoramm.fastminimessage.v2.tag.TagArgs;
import io.github.neoramm.fastminimessage.v2.tag.TagSpec;
import io.github.neoramm.fastminimessage.v2.template.FoldPass;
import io.github.neoramm.fastminimessage.v2.template.TagSource;
import io.github.neoramm.fastminimessage.v2.template.TemplateNode;
import io.github.neoramm.fastminimessage.v2.template.TreeTransformer;
import io.github.neoramm.fastminimessage.v2.template.ValueSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.dialog.DialogLike;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MiniMessageCompiler {
    private static final @NotNull MiniMessageCompiler DEFAULTS =
        new MiniMessageCompiler(ResolverRegistry.withDefaults());

    private final @NotNull ResolverRegistry registry;
    private final @NotNull TagScanner scanner;
    private final @NotNull TagArgs emptyArgs;
    private volatile @Nullable DialogResolver dialogResolver;

    /**
     * Returns a shared compiler backed by the default resolver registry.
     */
    public static @NotNull MiniMessageCompiler defaults() {
        return DEFAULTS;
    }

    public MiniMessageCompiler(final @NotNull ResolverRegistry registry) {
        this(registry, new DefaultTagScanner());
    }

    public MiniMessageCompiler(final @NotNull ResolverRegistry registry, final @NotNull TagScanner scanner) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.scanner = Objects.requireNonNull(scanner, "scanner");
        this.emptyArgs = new TagArgs("", this);
    }

    public @NotNull MiniMessageCompiler dialogResolver(final @Nullable DialogResolver resolver) {
        if (this == DEFAULTS) {
            throw new UnsupportedOperationException(
                "Cannot configure dialogs on default compiler; create a new MiniMessageCompiler instance instead");
        }
        dialogResolver = resolver;
        return this;
    }

    public @Nullable DialogLike resolveDialog(final @NotNull String reference) {
        Objects.requireNonNull(reference, "reference");
        return dialogResolver == null ? null : dialogResolver.resolve(reference);
    }

    public @NotNull PreComponentTree compile(final @Nullable String input) {
        if (input == null || input.isEmpty()) {
            return registry.payloadSchema().size() == 0 ? PreComponentTree.EMPTY
                : new PreComponentTree(PreComponentTree.EMPTY.root(), registry.payloadSchema());
        }
        return new PreComponentTree(
            FoldPass.compactTree(FoldPass.fold(compileNodes(scanner.scan(input), Style.empty()))),
            registry.payloadSchema());
    }

    public @NotNull PayloadSchema payloadSchema() {
        return registry.payloadSchema();
    }

    public int payloadCount() {
        return registry.payloadSchema().size();
    }

    public RenderScope.@NotNull Builder scopeBuilder() {
        return RenderScope.builder(registry.payloadSchema());
    }

    public RenderScope.@NotNull Mutable mutableScope() {
        return RenderScope.mutable(registry.payloadSchema());
    }

    public @NotNull PreComponentTree compileArgument(final @Nullable String input) {
        if (input != null && input.length() > 2 && input.charAt(0) == '<'
            && input.charAt(input.length() - 1) == '>') {
            final @NotNull String key = input.substring(1, input.length() - 1);
            if (!key.startsWith("#") && TokenPatterns.isScalarKey(key)
                && !"reset".equalsIgnoreCase(key)
                && registry.resolve(key) == null) {
                final @NotNull TagNode node = new TagNode(key, "", "", List.of(), false, false, false, false);
                return new PreComponentTree(slot(node, Style.empty(), List.of()), registry.payloadSchema());
            }
        }
        return compile(input);
    }

    public void registerModifier(final @NotNull String name, final @NotNull ComponentModifier modifier) {
        if (this == DEFAULTS) {
            throw new UnsupportedOperationException(
                "Cannot register modifiers on default compiler; create a new MiniMessageCompiler instance instead");
        }
        Objects.requireNonNull(modifier, "modifier");
        final @NotNull TreeTransformer transform = (final var content, final var style, final var context) ->
            RenderScope.applyStyle(modifier.modify(content, context), style);
        registry.register(new TagSpec.TransformRule(Set.of(name), (final var args, final var context) ->
            modifier.isStatic() ? new ValueSource.Constant<>(transform)
                : new ValueSource.Computed<>((final var render) -> transform)));
    }

    public @Nullable TagSpec findResolver(final @NotNull String name) {
        return registry.resolve(name);
    }

    private @NotNull List<@NotNull TemplateNode> compileNodes(final @NotNull List<@NotNull TagNode> nodes,
        final @NotNull Style inherited) {
        final @NotNull List<@NotNull TemplateNode> result = new ArrayList<>(nodes.size());
        for (final @NotNull TagNode node : nodes) {
            final @Nullable TagSpec spec = registry.resolve(node.tagName());
            result.add(compileNode(node, inherited, spec));
        }
        return result;
    }

    private @NotNull TemplateNode compileNode(final @NotNull TagNode node, final @NotNull Style inherited,
        final @Nullable TagSpec spec) {
        if (node.tagName().isEmpty()) return new TemplateNode.Static(Component.text(node.textContent(), inherited));
        if (spec == null) return slot(node, inherited, compileNodes(node.children(), Style.empty()));
        final @NotNull TagArgs args = arguments(node.rawArgs());
        final @NotNull CompileContext context = new CompileContext(node.tagName(), this);
        try {
            return switch (spec) {
                case TagSpec.PlaceholderRule placeholder -> new TemplateNode.RegisteredPlaceholder(node.tagName(),
                    inherited, compileNodes(node.children(), Style.empty()), placeholder.renderer());
                case TagSpec.ModifierRule modifier -> modifierNode(node, inherited, modifier.modifier());
                case TagSpec.StyleRule style -> styleNode(node, inherited, style.rule().apply(args, context));
                case TagSpec.LeafRule leaf -> leafNode(node, inherited, leaf.rule().apply(args, context));
                case TagSpec.TransformRule transform ->
                    transformNode(node, inherited, transform.rule().apply(args, context));
            };
        } catch (final IllegalArgumentException | InvalidKeyException malformed) {
            return slot(node, inherited, compileNodes(node.children(), Style.empty()));
        }
    }

    private @NotNull TemplateNode modifierNode(final @NotNull TagNode node, final @NotNull Style inherited,
        final @NotNull ComponentModifier modifier) {
        final @NotNull TreeTransformer transform = (final var content, final var style, final var scope) ->
            RenderScope.applyStyle(Objects.requireNonNull(modifier.modify(content, scope), "modifier result"), style);
        final @NotNull ValueSource<@NotNull TreeTransformer> source = modifier.isStatic()
            ? new ValueSource.Constant<>(transform) : new ValueSource.Computed<>((final var scope) -> transform);
        return FoldPass.transform(inherited, compileNodes(node.children(), Style.empty()), source);
    }

    private @NotNull TemplateNode styleNode(final @NotNull TagNode node, final @NotNull Style inherited,
        final @NotNull ValueSource<@NotNull Style> source) {
        if (source instanceof final ValueSource.Constant<@NotNull Style> fixed) {
            return FoldPass.fold(compileNodes(node.children(), inherited.merge(fixed.value())));
        }
        final @NotNull List<@NotNull TemplateNode> children =
            FoldPass.children(FoldPass.fold(compileNodes(node.children(), Style.empty())));
        final @NotNull TagSource fallback = source(node);
        return new TemplateNode.DynamicTransform(inherited, children,
            (final var content, final var base, final var ctx) -> {
            try {
                return RenderScope.applyStyle(content, base.merge(source.evaluate(ctx)));
            } catch (final IllegalArgumentException | InvalidKeyException malformed) {
                return literalFallback(fallback, content, base);
            }
        });
    }

    private @NotNull TemplateNode transformNode(final @NotNull TagNode node, final @NotNull Style inherited,
        final @NotNull ValueSource<@NotNull TreeTransformer> source) {
        final @NotNull List<@NotNull TemplateNode> children = compileNodes(node.children(), Style.empty());
        if (source.isConstant()) return FoldPass.transform(inherited, children, source);
        final @NotNull TagSource fallback = source(node);
        return new TemplateNode.DynamicTransform(inherited, FoldPass.children(FoldPass.fold(children)),
            (final var content, final var style, final var context) -> {
                try {
                    return source.evaluate(context).transform(content, style, context);
                } catch (final IllegalArgumentException | InvalidKeyException malformed) {
                    return literalFallback(fallback, content, style);
                }
            });
    }

    private static @NotNull Component literalFallback(final @NotNull TagSource source,
        final @NotNull Component content, final @NotNull Style style) {
        return Component.text().style(style).append(source.opening()).append(content).append(source.closing())
            .build().compact();
    }

    private @NotNull TemplateNode leafNode(final @NotNull TagNode node, final @NotNull Style style,
        final @NotNull ValueSource<@NotNull Component> source) {
        final @NotNull List<@NotNull TemplateNode> children = compileNodes(node.children(), style);
        if (source instanceof final ValueSource.Constant<@NotNull Component> fixed) {
            children.addFirst(new TemplateNode.Static(RenderScope.applyStyle(fixed.value(), style)));
            return FoldPass.fold(children);
        }
        final @NotNull TagSource fallback = source(node);
        if (children.isEmpty()) {
            return new TemplateNode.DynamicLeaf(style, (final var context) -> {
                try {
                    return source.evaluate(context);
                } catch (final IllegalArgumentException | InvalidKeyException malformed) {
                    return literalFallback(fallback, Component.empty(), Style.empty());
                }
            });
        }
        return new TemplateNode.DynamicTransform(style, FoldPass.children(FoldPass.fold(children)),
            (final var content, final var inherited, final var context) -> {
                try {
                    final @NotNull Component value = source.evaluate(context);
                    if (content.equals(Component.empty())) return RenderScope.applyStyle(value, inherited);
                    return Component.text().style(inherited).append(value).append(content).build();
                } catch (final IllegalArgumentException | InvalidKeyException malformed) {
                    return literalFallback(fallback, content, inherited);
                }
            });
    }

    private @NotNull TagArgs arguments(final @NotNull String input) {
        return input.isEmpty() ? emptyArgs : new TagArgs(input, this);
    }

    private @NotNull TemplateNode slot(final @NotNull TagNode node, final @NotNull Style inherited,
        final @NotNull List<@NotNull TemplateNode> children) {
        return new TemplateNode.Slot(node.tagName(), arguments(node.rawArgs()).values(),
            inherited, children, source(node));
    }

    private @NotNull TagSource source(final @NotNull TagNode node) {
        return new TagSource(node.tagName(), node.rawArgs(), node.hasArguments(), node.isSelfClosing(),
            node.explicitlyClosed(), this);
    }
}
