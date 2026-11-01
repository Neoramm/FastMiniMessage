package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wraps an immutable Adventure component whose rendering applies the inherited style.
 */
public final class StaticComponentPreComponent extends PreComponent {
    private final @NotNull Component component;
    private volatile @Nullable Component cached;

    public StaticComponentPreComponent(final @NotNull Component component) {
        this.component = component;
    }

    @Override
    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        final Component current = cached;
        if (current != null) return current;
        final Style style = computedStyle;
        final Component built = (style == null || style.isEmpty())
            ? component
            : component.style(style.merge(component.style()));
        cached = built;
        return built;
    }

    @Override
    public boolean isScopeIndependent() {
        return true;
    }

    @Override
    public boolean isStatic() {
        return true;
    }

    @Override
    public boolean isDirectStyleLeaf() {
        return true;
    }

    @Override
    public @NotNull PreComponent setStyle(final @Nullable Style style) {
        cached = null;
        return super.setStyle(style);
    }

    @Override
    public @NotNull PreComponent deepCopy() {
        final StaticComponentPreComponent copy = new StaticComponentPreComponent(component);
        copy.setStyle(computedStyle != null ? computedStyle : Style.empty());
        return copy;
    }
}
