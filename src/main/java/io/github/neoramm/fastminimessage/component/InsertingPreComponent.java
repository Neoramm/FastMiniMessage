package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InsertingPreComponent extends PreComponent {
    private final @Nullable String insertion;

    public InsertingPreComponent(final @Nullable String insertion, final PreComponent @Nullable [] children) {
        this.insertion = insertion;
        this.children = children;
    }

    @Override
    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        final TextComponent.Builder builder = Component.text();
        final @Nullable Style style = computedStyle;
        if (style != null && !style.isEmpty()) builder.style(style);
        builder.insertion(insertion);
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

    @Nullable String getInsertion() {
        return insertion;
    }

    @Override
    public @NotNull PreComponent deepCopy() {
        final PreComponent @Nullable [] kids = children;
        if (kids == null) return new InsertingPreComponent(insertion, null).setStyle(computedStyle);
        final PreComponent @NotNull [] copied = new PreComponent[kids.length];
        for (int i = 0; i < kids.length; i++) copied[i] = kids[i].deepCopy();
        return new InsertingPreComponent(insertion, copied).setStyle(computedStyle);
    }
}
