package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public final class HoverPreComponent extends PreComponent {
    private final @NotNull BiFunction<Pointered, RenderScope, HoverEvent<?>> eventFunction;

    public HoverPreComponent(final @NotNull BiFunction<Pointered, RenderScope, HoverEvent<?>> eventFunction,
        final PreComponent @Nullable [] children) {
        this.eventFunction = eventFunction;
        this.children = children;
    }

    @Override
    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        final TextComponent.Builder builder = Component.text()
            .style(computedStyle)
            .hoverEvent(eventFunction.apply(pointered, scope));
        for (final PreComponent child : getChildren()) child.renderInto(pointered, builder, scope);
        return builder.build();
    }

    @Override
    public @NotNull PreComponent deepCopy() {
        final PreComponent @Nullable [] kids = children;
        if (kids == null) return new HoverPreComponent(eventFunction, null).setStyle(computedStyle);
        final PreComponent @NotNull [] copied = new PreComponent[kids.length];
        for (int i = 0; i < kids.length; i++) copied[i] = kids[i].deepCopy();
        return new HoverPreComponent(eventFunction, copied).setStyle(computedStyle);
    }
}
