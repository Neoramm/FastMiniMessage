package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.api.ComponentModifier;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.api.DialogResolver;
import io.github.neoramm.fastminimessage.api.TagScanner;
import io.github.neoramm.fastminimessage.component.DeferredTagPreComponent;
import io.github.neoramm.fastminimessage.component.GroupPreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.component.StyleFlattener;
import io.github.neoramm.fastminimessage.component.UnparsedPreComponent;
import io.github.neoramm.fastminimessage.internal.DefaultTagScanner;
import io.github.neoramm.fastminimessage.internal.TagArgumentTypes;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import io.github.neoramm.fastminimessage.model.TagNode;
import io.github.neoramm.fastminimessage.resolver.ModifierResolver;
import net.kyori.adventure.dialog.DialogLike;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Compiles MiniMessage strings into a precompiled component tree that renders to Adventure components.
 */
public final class MiniMessageCompiler {
    private final @NotNull ResolverRegistry registry;
    private final @NotNull TagScanner scanner;
    private @Nullable DialogResolver dialogResolver;

    public MiniMessageCompiler(final @NotNull ResolverRegistry registry) {
        this(registry, new DefaultTagScanner());
    }

    public MiniMessageCompiler(final @NotNull ResolverRegistry registry, final @NotNull TagScanner scanner) {
        this.registry = registry;
        this.scanner = scanner;
    }

    /**
     * Sets the resolver used by dialog-aware tags such as {@code click:show_dialog}.
     *
     * @param dialogResolver the platform dialog resolver, or {@code null} to keep the literal fallback
     * @return this compiler for chaining
     */
    public @NotNull MiniMessageCompiler dialogResolver(final @Nullable DialogResolver dialogResolver) {
        this.dialogResolver = dialogResolver;
        return this;
    }

    /**
     * Resolves a dialog reference through the configured {@link DialogResolver}.
     *
     * @param reference the raw reference from the tag
     * @return the dialog, or {@code null} when no resolver is configured or the reference is unknown
     */
    public @Nullable DialogLike resolveDialog(final @NotNull String reference) {
        final @Nullable DialogResolver resolver = dialogResolver;
        return resolver == null ? null : resolver.resolve(reference);
    }

    /**
     * Compiles the given MiniMessage string into a renderable tree.
     *
     * @param miniMessageString the MiniMessage string to compile
     * @return the compiled tree, or an empty tree when the input is null
     */
    public @NotNull PreComponentTree compile(final @Nullable String miniMessageString) {
        if (miniMessageString == null || miniMessageString.isEmpty()) return PreComponentTree.EMPTY;
        final List<TagNode> roots = flattenSelfClosing(scanner.scan(miniMessageString));
        final List<PreComponent> components = new ArrayList<>();
        for (final TagNode root : roots) components.add(resolveStyled(buildStyledNode(root, Style.empty())));
        final PreComponent rootComponent = components.size() == 1
            ? components.getFirst()
            : new GroupPreComponent(components.toArray(new PreComponent[0]));
        return new PreComponentTree(StyleFlattener.flatten(rootComponent));
    }

    public @NotNull PreComponentTree compileArgument(final @Nullable String input) {
        if (input != null && input.length() > 2 && input.charAt(0) == '<'
            && input.charAt(input.length() - 1) == '>') {
            final @NotNull String name = input.substring(1, input.length() - 1);
            if (name.indexOf(':') == -1 && registry.resolve(name) != null) {
                return new PreComponentTree(new DeferredTagPreComponent(name, "", PreComponent.EMPTY_CHILDREN,
                    Style.empty(), false, false, false, this));
            }
        }
        return compile(input);
    }

    public void registerModifier(final @NotNull String name, final @NotNull ComponentModifier modifier) {
        registry.register(new ModifierResolver(name, modifier));
    }

    public @Nullable ComponentResolver findResolver(final @NotNull String tagName) {
        return registry.resolve(tagName);
    }

    private @NotNull List<@NotNull TagNode> flattenSelfClosing(final @NotNull List<@NotNull TagNode> nodes) {
        final List<TagNode> result = new ArrayList<>(nodes.size());
        for (final TagNode node : nodes) {
            final ComponentResolver resolver = registry.resolve(node.tagName());
            final boolean isSelfClosing = resolver != null && resolver.isSelfClosing();
            final List<TagNode> flattenedChildren = flattenSelfClosing(node.children());

            if (isSelfClosing && !flattenedChildren.isEmpty()) {
                result.add(new TagNode(
                    node.tagName(),
                    node.rawArgs(),
                    node.textContent(),
                    List.of(),
                    node.hasArguments(),
                    false,
                    true,
                    node.explicitlyClosed()
                ));
                result.addAll(flattenedChildren);
            } else if (flattenedChildren == node.children() && isSelfClosing == node.isSelfClosing()) {
                result.add(node);
            } else {
                result.add(new TagNode(
                    node.tagName(),
                    node.rawArgs(),
                    node.textContent(),
                    flattenedChildren,
                    node.hasArguments(),
                    node.isClosingTag(),
                    node.isSelfClosing(),
                    node.explicitlyClosed()
                ));
            }
        }
        return result;
    }

    private @NotNull StyledNode buildStyledNode(final @NotNull TagNode node, final @NotNull Style computed) {
        final Style childInherited = "reset".equalsIgnoreCase(node.tagName()) ? Style.empty() : computed;

        final List<StyledNode> styledChildren = new ArrayList<>(node.children().size());
        for (final TagNode child : node.children()) styledChildren.add(buildStyledNode(child, childInherited));
        return new StyledNode(node, computed, styledChildren);
    }

    private @NotNull PreComponent resolveStyled(final @NotNull StyledNode styled) {
        final TagNode node = styled.node();
        final PreComponent @NotNull [] childComponents = new PreComponent[styled.children().size()];
        for (int i = 0; i < styled.children().size(); i++) childComponents[i] = resolveStyled(styled.children().get(i));
        if (containsPlaceholder(node.rawArgs()) && !isComponentArgument(node)) {
            return new DeferredTagPreComponent(node.tagName(), node.rawArgs(), childComponents, styled.computedStyle(),
                node.hasArguments(), node.isSelfClosing(), node.explicitlyClosed(), this);
        }
        if (node.tagName().isEmpty()) return new LiteralPreComponent(node.textContent(), styled.computedStyle);
        final ComponentResolver resolver = registry.resolve(node.tagName());
        if (node.isSelfClosing()) {
            if (resolver != null) return resolveSafely(resolver, node, new PreComponent[0], styled.computedStyle);
            return new DeferredTagPreComponent(node.tagName(), node.rawArgs(), PreComponent.EMPTY_CHILDREN,
                styled.computedStyle(), node.hasArguments(), true, false, this);
        }
        if (resolver == null) {
            return new DeferredTagPreComponent(node.tagName(), node.rawArgs(), childComponents, styled.computedStyle(),
                node.hasArguments(), node.isSelfClosing(), node.explicitlyClosed(), this);
        }
        return resolveSafely(resolver, node, childComponents, styled.computedStyle);
    }

    private @NotNull PreComponent resolveSafely(final @NotNull ComponentResolver resolver, final @NotNull TagNode node,
        final PreComponent @NotNull [] children, final @NotNull Style style) {
        try {
            return resolver.resolve(node.tagName(), Arguments.parse(node.rawArgs()), children, style, this);
        } catch (final RuntimeException | LinkageError unsupported) {
            final String raw = openTagOf(node.tagName(), node.rawArgs(), node.hasArguments());
            if (children.length == 0 && !node.explicitlyClosed()) {
                return new UnparsedPreComponent(node.tagName(), raw, raw).setStyle(style);
            }
            return new DeferredTagPreComponent(node.tagName(), node.rawArgs(), children, style,
                node.hasArguments(), node.isSelfClosing(), node.explicitlyClosed(), null);
        }
    }

    private static @NotNull String selfTagOf(final @NotNull String tagName, final @NotNull String rawArgs,
        final boolean hasArguments) {
        return hasArguments || !rawArgs.isEmpty()
            ? "<" + tagName + ":" + rawArgs + "/>" : "<" + tagName + "/>";
    }

    private static @NotNull String openTagOf(final @NotNull String tagName, final @NotNull String rawArgs,
        final boolean hasArguments) {
        return hasArguments || !rawArgs.isEmpty()
            ? "<" + tagName + ":" + rawArgs + ">" : "<" + tagName + ">";
    }

    public @NotNull PreComponent resolveTag(final @NotNull String tagName, final @NotNull String rawArgs,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle, final boolean selfClosing,
        final boolean explicitlyClosed) {
        return resolveTag(tagName, rawArgs, children, inheritedStyle, !rawArgs.isEmpty(), selfClosing,
            explicitlyClosed);
    }

    public @NotNull PreComponent resolveTag(final @NotNull String tagName, final @NotNull String rawArgs,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle, final boolean hasArguments,
        final boolean selfClosing, final boolean explicitlyClosed) {
        if (tagName.isEmpty()) return new LiteralPreComponent("", inheritedStyle);
        final ComponentResolver resolver = registry.resolve(tagName);
        if (selfClosing) {
            if (resolver != null) {
                try {
                    return resolver.resolve(
                        tagName, Arguments.parse(rawArgs), PreComponent.EMPTY_CHILDREN, inheritedStyle, this);
                } catch (final RuntimeException | LinkageError unsupported) {
                }
            }
            final String selfTag = selfTagOf(tagName, rawArgs, hasArguments);
            return new UnparsedPreComponent(tagName, selfTag, selfTag).setStyle(inheritedStyle);
        }
        if (resolver == null) {
            return new DeferredTagPreComponent(tagName, rawArgs, children, inheritedStyle, hasArguments,
                selfClosing, explicitlyClosed, this);
        }
        try {
            return resolver.resolve(tagName, Arguments.parse(rawArgs), children, inheritedStyle, this);
        } catch (final RuntimeException | LinkageError unsupported) {
            return new DeferredTagPreComponent(tagName, rawArgs, children, inheritedStyle, hasArguments,
                selfClosing, explicitlyClosed, null);
        }
    }

    private static boolean isComponentArgument(final @NotNull TagNode node) {
        final @NotNull Arguments args = Arguments.parse(node.rawArgs());
        for (int i = 0; i < args.size(); i++) {
            if (!TagArgumentTypes.isComponent(node.tagName(), args, i) && containsPlaceholder(args.get(i))) return false;
        }
        return true;
    }

    private static boolean containsPlaceholder(final @Nullable String rawArgs) {
        if (rawArgs == null || rawArgs.isEmpty()) return false;
        int i = 0;
        final int len = rawArgs.length();
        while (i < len) {
            final int open = rawArgs.indexOf('<', i);
            if (open == -1) break;
            final int close = rawArgs.indexOf('>', open);
            if (close == -1) break;
            if (close > open + 1) {
                boolean identifier = true;
                for (int c = open + 1; c < close; c++) {
                    final char ch = rawArgs.charAt(c);
                    if (!Character.isJavaIdentifierPart(ch) && ch != '.' && ch != '-' && ch != '/' && ch != ':') {
                        identifier = false;
                        break;
                    }
                }
                if (identifier) return true;
            }
            i = close + 1;
        }
        return false;
    }

    private record StyledNode(@NotNull TagNode node, @NotNull Style computedStyle,
        @NotNull List<@NotNull StyledNode> children) {
    }
}
