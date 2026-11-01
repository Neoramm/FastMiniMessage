package io.github.neoramm.fastminimessage.model;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.PreComponentVisitor;
import io.github.neoramm.fastminimessage.component.InlinePreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.resolver.ModifierResolver;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * A compiled, renderable component tree produced by {@link MiniMessageCompiler}.
 */
public final class PreComponentTree {
    public static final @NotNull PreComponentTree EMPTY =
        new PreComponentTree(new InlinePreComponent(Component.empty()));
    private volatile @NotNull TreeState state;

    public PreComponentTree(final @NotNull PreComponent root) {
        this.state = new TreeState(root.deepCopy());
    }

    /**
     * Renders the tree for the given context without {@link Pointered}.
     *
     * @return the rendered component
     */
    public @NotNull Component render() {
        return render(null, RenderScope.EMPTY);
    }

    /**
     * Renders the tree for the given context.
     *
     * @param pointered the context (usually the viewer)
     * @return the rendered component
     */
    public @NotNull Component render(final @Nullable Pointered pointered) {
        return render(pointered, RenderScope.EMPTY);
    }

    public @NotNull Component render(final @Nullable Pointered pointered, final @Nullable RenderScope scope) {
        return render(state, pointered, scope);
    }

    private static @NotNull Component render(final @NotNull TreeState current, final @Nullable Pointered pointered,
        final @Nullable RenderScope scope) {
        if (current.isStatic && (current.scopeIndependent || scope == null || scope.isEmpty())) {
            final Component cached = current.cached;
            if (cached != null) return cached;
            final Component built = current.root.render(pointered, RenderScope.EMPTY);
            current.cached = built;
            return built;
        }
        return current.root.render(pointered, scope != null ? scope : RenderScope.EMPTY);
    }

    public @NotNull Component render(final @Nullable Pointered pointered, final TagResolver @NotNull ... resolvers) {
        final @NotNull TreeState current = state;
        if (current.isStatic && current.scopeIndependent) return render(current, pointered, RenderScope.EMPTY);
        return render(current, pointered, RenderScope.of(resolvers));
    }

    public @NotNull Component render(final @Nullable Pointered pointered,
        final @NotNull ModifierResolver... modifiers) {
        final @NotNull TreeState current = state;
        if (current.isStatic && current.scopeIndependent) return render(current, pointered, RenderScope.EMPTY);
        return render(current, pointered, RenderScope.of(modifiers));
    }

    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RuntimeResolver... resolvers) {
        final @NotNull TreeState current = state;
        if (current.isStatic && current.scopeIndependent) return render(current, pointered, RenderScope.EMPTY);
        return render(current, pointered, RenderScope.from(resolvers));
    }

    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull PreComponent extra) {
        final Component base = render(pointered, RenderScope.EMPTY);
        final Component appended = extra.render(pointered, RenderScope.EMPTY);
        return Component.text().append(base).append(appended).build();
    }

    public @NotNull Component render(final @Nullable Pointered pointered, final PreComponent @NotNull [] extras) {
        final TextComponent.Builder builder = Component.text().append(render(pointered, RenderScope.EMPTY));
        for (final PreComponent extra : extras) builder.append(extra.render(pointered, RenderScope.EMPTY));
        return builder.build();
    }

    public @NotNull Component render(final @Nullable Pointered pointered,
        final @NotNull List<? extends TagResolver> resolvers) {
        final @NotNull TreeState current = state;
        if (current.isStatic && current.scopeIndependent) return render(current, pointered, RenderScope.EMPTY);
        return render(current, pointered, RenderScope.of(resolvers));
    }

    public @NotNull Component render(final @Nullable Pointered pointered, final Placeholder @NotNull ... placeholders) {
        final @NotNull TreeState current = state;
        if (current.isStatic && current.scopeIndependent) return render(current, pointered, RenderScope.EMPTY);
        return render(current, pointered, RenderScope.of(placeholders));
    }

    public boolean isScopeIndependent() {
        return state.scopeIndependent;
    }

    public boolean isStatic() {
        return state.isStatic;
    }

    public @NotNull PreComponentTree copy() {
        return new PreComponentTree(state.root);
    }

    public void inspect(final @NotNull Consumer<@NotNull PreComponentSnapshot> visitor) {
        Objects.requireNonNull(visitor, "visitor");
        inspectDepthFirst(state.root, visitor);
    }

    private static void inspectDepthFirst(final @NotNull PreComponent node,
        final @NotNull Consumer<@NotNull PreComponentSnapshot> visitor) {
        visitor.accept(new PreComponentSnapshot(node.getClass().getSimpleName(), node.getComputedStyle(),
            node instanceof LiteralPreComponent literal ? literal.getText() : null,
            node.getChildren().length, node.isStatic(), node.isScopeIndependent()));
        for (final PreComponent child : node.getChildren()) inspectDepthFirst(child, visitor);
    }

    public void visit(final @NotNull PreComponentVisitor visitor) {
        final @NotNull PreComponent edited = state.root.deepCopy();
        visitDepthFirst(edited, visitor);
        state = new TreeState(edited.deepCopy());
    }

    private void visitDepthFirst(final @NotNull PreComponent node, final @NotNull PreComponentVisitor visitor) {
        visitor.visit(node);
        final PreComponent @NotNull [] childArray = node.getChildren();
        for (final PreComponent child : childArray) visitDepthFirst(child, visitor);
    }

    public @NotNull Component render(final @Nullable Pointered pointered,
        final @NotNull UnaryOperator<PreComponent> modifier) {
        final PreComponent modifiedRoot = modifyDepthFirst(state.root.deepCopy(), modifier);
        return modifiedRoot.render(pointered, RenderScope.EMPTY);
    }

    private @NotNull PreComponent modifyDepthFirst(final @NotNull PreComponent node,
        final @NotNull UnaryOperator<PreComponent> modifier) {
        final PreComponent modified = modifier.apply(node);
        final PreComponent @NotNull [] childArray = modified.getChildren();
        for (int i = 0; i < childArray.length; i++) childArray[i] = modifyDepthFirst(childArray[i], modifier);
        return modified;
    }

    private static final class TreeState {
        private final @NotNull PreComponent root;
        private final boolean isStatic;
        private final boolean scopeIndependent;
        private volatile @Nullable Component cached;

        private TreeState(final @NotNull PreComponent root) {
            this.root = root;
            this.isStatic = root.isStatic();
            this.scopeIndependent = root.isScopeIndependent();
        }
    }
}
