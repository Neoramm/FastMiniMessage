package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ColorPreComponent extends PreComponent {

    private final @Nullable TextColor color;

    public ColorPreComponent(final @Nullable TextColor color, final PreComponent @NotNull [] children) {
        this.color = color;
        this.children = children;
    }

    @Override
    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        final TextComponent.Builder builder = Component.text();
        final @Nullable Style style = computedStyle;
        if (style != null && !style.isEmpty()) builder.style(style);
        if (color != null) builder.color(color);
        renderChildrenInto(pointered, builder, scope);
        return builder.build();
    }

    @Override
    public boolean isStatic() {
        return childrenAreStatic();
    }

    @Nullable TextColor getColor() {
        return color;
    }

    @Override
    public @NotNull PreComponent deepCopy() {
        return new ColorPreComponent(color, copyChildren()).setStyle(computedStyle);
    }
}
