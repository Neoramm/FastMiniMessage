package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SelectorPreComponent extends PreComponent {
    private final @NotNull String key;
    private final @Nullable PreComponentTree separator;
    private volatile @Nullable Component cachedComponent;

    public SelectorPreComponent(final @NotNull String key, final @Nullable PreComponentTree separator) {
        this.key = key;
        this.separator = separator;
    }

    @Override
    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        final @Nullable Component cached = this.cachedComponent;
        if (cached != null && (scope.isEmpty() || isScopeIndependent())) return cached;
        final @Nullable PreComponentTree separator = this.separator;
        final Component component = separator == null
            ? Component.selector(key)
            : Component.selector(key, separator.render(pointered, scope));
        final Style style = computedStyle != null ? computedStyle : Style.empty();
        final Component result = style.isEmpty() ? component : component.style(style.merge(component.style()));
        if (isStatic() && (scope.isEmpty() || isScopeIndependent())) this.cachedComponent = result;
        return result;
    }

    @Override
    public @NotNull PreComponent setStyle(final @Nullable Style style) {
        super.setStyle(style);
        this.cachedComponent = null;
        return this;
    }

    @Override
    public boolean isScopeIndependent() {
        return separator == null || separator.isScopeIndependent();
    }

    @Override
    public boolean isStatic() {
        final @Nullable PreComponentTree separator = this.separator;
        return separator == null || separator.isStatic();
    }

    @Override
    public boolean isDirectStyleLeaf() {
        return true;
    }

    @Override
    public @NotNull PreComponent deepCopy() {
        final @Nullable PreComponentTree separator = this.separator;
        final SelectorPreComponent copy = new SelectorPreComponent(
            key, separator != null ? separator.copy() : null);
        copy.setStyle(computedStyle);
        return copy;
    }
}
