package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GroupPreComponent extends PreComponent {

    private volatile @Nullable Component cached;

    public GroupPreComponent(final PreComponent @Nullable [] children) {
        this.children = children;
    }

    @Override
    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        if (isStatic() && (scope.isEmpty() || isScopeIndependent())) {
            final Component current = cached;
            if (current != null) return current;
            final Component built = renderUncached(pointered, scope);
            cached = built;
            return built;
        }
        return renderUncached(pointered, scope);
    }

    private @NotNull Component renderUncached(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        final PreComponent @Nullable [] kids = children;
        if (kids == null || kids.length == 0) {
            return Component.empty().style(computedStyle != null ? computedStyle : Style.empty());
        }
        final @Nullable Style style = computedStyle;
        if (kids.length == 1) {
            final Component child = kids[0].render(pointered, scope);
            if (style == null || style.isEmpty()) return child;
            return Component.text().style(style).append(child).build();
        }
        final TextComponent.Builder builder = Component.text();
        if (style != null && !style.isEmpty()) builder.style(style);
        renderChildrenInto(pointered, builder, scope);
        return builder.build();
    }

    @Override
    public void renderInto(final @Nullable Pointered pointered, final @NotNull TextComponent.Builder builder,
        final @NotNull RenderScope scope) {
        final @Nullable Style style = computedStyle;
        if (style != null && !style.isEmpty()) {
            builder.append(render(pointered, scope));
            return;
        }
        if (isStatic() && (scope.isEmpty() || isScopeIndependent())) {
            final Component current = cached;
            if (current != null) {
                builder.append(current);
                return;
            }
        }
        renderChildrenInto(pointered, builder, scope);
    }

    @Override
    public @NotNull PreComponent setStyle(final @Nullable Style style) {
        cached = null;
        return super.setStyle(style);
    }

    @Override
    public @NotNull PreComponent setChildren(final @Nullable PreComponent[] children) {
        cached = null;
        return super.setChildren(children);
    }

    @Override
    public boolean isScopeIndependent() {
        return childrenAreScopeIndependent();
    }

    @Override
    public boolean isStatic() {
        return childrenAreStatic();
    }

    @Override
    public @NotNull PreComponent deepCopy() {
        return new GroupPreComponent(copyChildren()).setStyle(computedStyle);
    }
}
