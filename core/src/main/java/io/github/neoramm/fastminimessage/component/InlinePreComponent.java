package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InlinePreComponent extends PreComponent {
    private final @NotNull Component component;
    private volatile @Nullable Component styled;

    public InlinePreComponent(final @NotNull Component component) {
        this.component = component;
    }

    @Override
    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        final Style style = computedStyle;
        if (style == null || style.isEmpty()) return component;
        Component cached = styled;
        if (cached == null) {
            cached = component.style(style.merge(component.style()));
            styled = cached;
        }
        return cached;
    }

    @Override
    public @NotNull PreComponent setStyle(final @Nullable Style style) {
        styled = null;
        return super.setStyle(style);
    }

    @Override
    public boolean isStatic() {
        return true;
    }

    @Override
    public @NotNull PreComponent deepCopy() {
        return new InlinePreComponent(component).setStyle(computedStyle);
    }
}
