package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LiteralPreComponent extends PreComponent {
    private @NotNull String text;
    private volatile @Nullable Component cached;

    public LiteralPreComponent(final @NotNull String text, final @Nullable Style style) {
        this.text = text;
        this.computedStyle = style;
    }

    @Override
    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        final Component current = cached;
        if (current != null) return current;
        final Style style = computedStyle;
        final Component built = (style == null || style.isEmpty())
            ? Component.text(text)
            : Component.text(text, style);
        cached = built;
        return built;
    }

    @Override
    public boolean isStatic() {
        return true;
    }

    @Override
    public @NotNull PreComponent setStyle(final @Nullable Style style) {
        cached = null;
        return super.setStyle(style);
    }

    @Override
    public @NotNull PreComponent deepCopy() {
        final LiteralPreComponent copy = new LiteralPreComponent(text, computedStyle);
        copy.children = copyChildren();
        return copy;
    }

    public @NotNull String getText() {
        return text;
    }

    public void setText(final @NotNull String text) {
        this.text = text;
        this.cached = null;
    }
}
