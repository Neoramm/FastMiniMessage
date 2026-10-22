package io.github.neoramm.fastminimessage.model;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.PreComponentVisitor;
import io.github.neoramm.fastminimessage.component.InlinePreComponent;
import io.github.neoramm.fastminimessage.resolver.ModifierResolver;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * A compiled, renderable component tree produced by {@link MiniMessageCompiler}.
 */
public final class PreComponentTree {
    public static final @NotNull PreComponentTree EMPTY =
        new PreComponentTree(new InlinePreComponent(Component.empty()));
    private final @NotNull PreComponent root;

    public PreComponentTree(final @NotNull PreComponent root) {
        this.root = root;
    }

    /**
     * Renders the tree for the given context without {@link Pointered}.
     *
     * @return the rendered component
     */
    public @NotNull Component render() {
        return root.render(null, RenderScope.EMPTY);
    }

    /**
     * Renders the tree for the given context.
     *
     * @param pointered the context (usually the viewer)
     * @return the rendered component
     */
    public @NotNull Component render(final @Nullable Pointered pointered) {
        return root.render(pointered, RenderScope.EMPTY);
    }

    public @NotNull Component render(final @Nullable Pointered pointered, final @Nullable RenderScope scope) {
        return root.render(pointered, scope != null ? scope : RenderScope.EMPTY);
    }

    public @NotNull Component render(final @Nullable Pointered pointered,
        final @NotNull ModifierResolver... modifiers) {
        return root.render(pointered, RenderScope.of(modifiers));
    }

    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RuntimeResolver... resolvers) {
        final List<Placeholder> placeholders = new ArrayList<>();
        final List<ModifierResolver> modifiers = new ArrayList<>();
        if (resolvers != null) {
            for (final RuntimeResolver resolver : resolvers) {
                if (resolver instanceof final Placeholder placeholder) placeholders.add(placeholder);
                else if (resolver instanceof final ModifierResolver modifier) modifiers.add(modifier);
            }
        }
        return root.render(pointered, RenderScope.of(placeholders, modifiers));
    }

    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull PreComponent extra) {
        final Component base = root.render(pointered, RenderScope.EMPTY);
        final Component appended = extra.render(pointered, RenderScope.EMPTY);
        return Component.text().append(base).append(appended).build();
    }

    public @NotNull Component render(final @Nullable Pointered pointered, final PreComponent @NotNull [] extras) {
        final TextComponent.Builder builder = Component.text().append(root.render(pointered, RenderScope.EMPTY));
        for (final PreComponent extra : extras) builder.append(extra.render(pointered, RenderScope.EMPTY));
        return builder.build();
    }

    public @NotNull Component render(final @Nullable Pointered pointered,
        final @NotNull List<Placeholder> placeholders) {
        return root.render(pointered, RenderScope.of(placeholders));
    }

    public @NotNull Component render(final @Nullable Pointered pointered, final Placeholder @NotNull ... placeholders) {
        return root.render(pointered, RenderScope.of(placeholders));
    }

    public @NotNull PreComponentTree copy() {
        return new PreComponentTree(root.deepCopy());
    }

    public void visit(final @NotNull PreComponentVisitor visitor) {
        visitDepthFirst(root, visitor);
    }

    private void visitDepthFirst(final @NotNull PreComponent node, final @NotNull PreComponentVisitor visitor) {
        visitor.visit(node);
        final PreComponent @NotNull [] childArray = node.getChildren();
        for (final PreComponent child : childArray) visitDepthFirst(child, visitor);
    }

    public @NotNull Component render(final @Nullable Pointered pointered,
        final @NotNull UnaryOperator<PreComponent> modifier) {
        final PreComponent modifiedRoot = modifyDepthFirst(root.deepCopy(), modifier);
        return modifiedRoot.render(pointered, RenderScope.EMPTY);
    }

    private @NotNull PreComponent modifyDepthFirst(final @NotNull PreComponent node,
        final @NotNull UnaryOperator<PreComponent> modifier) {
        final PreComponent modified = modifier.apply(node);
        final PreComponent @NotNull [] childArray = modified.getChildren();
        for (int i = 0; i < childArray.length; i++) childArray[i] = modifyDepthFirst(childArray[i], modifier);
        return modified;
    }
}
