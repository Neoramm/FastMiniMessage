package io.github.neoramm.fastminimessage.model;

import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Base type of every node in a precompiled component tree.
 */
public abstract class PreComponent {

    public static final PreComponent @NotNull [] EMPTY_CHILDREN = new PreComponent[0];

    protected @UnknownNullability PreComponent[] children;
    protected @Nullable Style computedStyle = Style.empty();

    public @NotNull Component render(final @Nullable Pointered pointered) {
        return render(pointered, RenderScope.EMPTY);
    }

    public abstract @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope);

    /**
     * Appends this component's rendered form to an existing builder.
     *
     * <p>Containers whose style is already on the builder can forward the call to their children,
     * avoiding one intermediate component per nesting level.</p>
     */
    public void renderInto(final @Nullable Pointered pointered, final @NotNull TextComponent.Builder builder) {
        renderInto(pointered, builder, RenderScope.EMPTY);
    }

    public void renderInto(final @Nullable Pointered pointered, final @NotNull TextComponent.Builder builder,
        final @NotNull RenderScope scope) {
        builder.append(render(pointered, scope));
    }

    public abstract @NotNull PreComponent deepCopy();

    /**
     * Indicates whether rendering this component ignores the {@link Pointered} argument,
     * which allows the result to be cached across renders.
     */
    public boolean isStatic() {
        return false;
    }

    public boolean isDirectStyleLeaf() {
        return false;
    }

    protected final boolean childrenAreStatic() {
        final PreComponent @Nullable [] kids = children;
        if (kids == null) return true;
        for (final PreComponent child : kids) if (!child.isStatic()) return false;
        return true;
    }

    protected final void renderChildrenInto(final @Nullable Pointered pointered,
        final @NotNull TextComponent.Builder builder, final @NotNull RenderScope scope) {
        final PreComponent @Nullable [] kids = children;
        if (kids != null) for (final PreComponent child : kids) child.renderInto(pointered, builder, scope);
    }

    protected final PreComponent @Nullable [] copyChildren() {
        final PreComponent @Nullable [] kids = children;
        if (kids == null) return null;
        if (kids.length == 0) return EMPTY_CHILDREN;
        final PreComponent @NotNull [] copied = new PreComponent[kids.length];
        for (int i = 0; i < kids.length; i++) copied[i] = kids[i].deepCopy();
        return copied;
    }

    protected final PreComponent @NotNull [] copyNonNullChildren() {
        final PreComponent @Nullable [] copied = copyChildren();
        return copied != null ? copied : EMPTY_CHILDREN;
    }

    public @NotNull PreComponent setChildren(final @Nullable PreComponent[] children) {
        this.children = children;
        return this;
    }

    public @NotNull PreComponent setStyle(final @Nullable Style style) {
        this.computedStyle = style;
        return this;
    }

    public @NotNull PreComponent prependChild(final @NotNull PreComponent child) {
        if (children == null) children = new PreComponent[]{child};
        else {
            final PreComponent @NotNull [] next = new PreComponent[children.length + 1];
            next[0] = child;
            System.arraycopy(children, 0, next, 1, children.length);
            children = next;
        }
        return this;
    }

    public @NotNull PreComponent appendChild(final @NotNull PreComponent child) {
        if (children == null) children = new PreComponent[]{child};
        else {
            final PreComponent @NotNull [] next = new PreComponent[children.length + 1];
            System.arraycopy(children, 0, next, 0, children.length);
            next[children.length] = child;
            children = next;
        }
        return this;
    }

    public @NotNull PreComponent replaceChild(final int index, final @NotNull PreComponent replacement) {
        if (children != null && index >= 0 && index < children.length) children[index] = replacement;
        return this;
    }

    public PreComponent @NotNull [] getChildren() {
        return children != null ? children : EMPTY_CHILDREN;
    }

    public @NotNull Style getComputedStyle() {
        return computedStyle != null ? computedStyle : Style.empty();
    }
}
