package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ShadowPreComponent extends PreComponent {

    private final @NotNull ShadowColor color;

    public ShadowPreComponent(final @NotNull ShadowColor color, final PreComponent @NotNull [] children) {
        this.color = color;
        this.children = children;
    }

    @Override
    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        final TextComponent.Builder builder = Component.text();
        final @Nullable Style style = computedStyle;
        if (style != null && !style.isEmpty()) builder.style(style);
        builder.shadowColor(color);
        final PreComponent @Nullable [] kids = children;
        if (kids != null) {
            for (final PreComponent child : kids) child.renderInto(pointered, builder, scope);
        }
        return builder.build();
    }

    @Override
    public boolean isStatic() {
        return childrenAreStatic();
    }

    @Nullable ShadowColor getShadowColor() {
        return color;
    }

    @Override
    public @NotNull PreComponent deepCopy() {
        final PreComponent @Nullable [] kids = children;
        if (kids == null) return new ShadowPreComponent(color, new PreComponent[0]).setStyle(computedStyle);
        final PreComponent @NotNull [] copied = new PreComponent[kids.length];
        for (int i = 0; i < kids.length; i++) copied[i] = kids[i].deepCopy();
        return new ShadowPreComponent(color, copied).setStyle(computedStyle);
    }
}
