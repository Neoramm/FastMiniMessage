package io.github.neoramm.fastminimessage.paper;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class PapiPreComponent extends PreComponent {

    private final @NotNull PapiRenderer renderer;
    private final @NotNull String expression;

    PapiPreComponent(final @NotNull PapiRenderer renderer, final @NotNull String expression,
        final @Nullable Style style) {
        this.renderer = renderer;
        this.expression = expression;
        this.computedStyle = style;
    }

    @Override
    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        final Component value = Component.text(renderer.render(pointered, expression));
        final Style style = computedStyle;
        return style == null || style.isEmpty() ? value : value.style(style);
    }

    @Override
    public boolean isDirectStyleLeaf() {
        return true;
    }

    @Override
    public @NotNull PreComponent deepCopy() {
        return new PapiPreComponent(renderer, expression, computedStyle);
    }
}
