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

    public SelectorPreComponent(final @NotNull String key, final @Nullable PreComponentTree separator) {
        this.key = key;
        this.separator = separator;
    }

    @Override
    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        final @Nullable PreComponentTree separator = this.separator;
        final Component component = separator == null
            ? Component.selector(key)
            : Component.selector(key, separator.render(pointered, scope));
        final Style style = computedStyle != null ? computedStyle : Style.empty();
        if (style.isEmpty()) return component;
        return component.style(style.merge(component.style()));
    }

    @Override
    public boolean isStatic() {
        final @Nullable PreComponentTree separator = this.separator;
        return separator == null || separator.isStatic();
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
