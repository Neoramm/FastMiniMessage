package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ClickPreComponent<T extends ClickEvent.Payload> extends PreComponent {
    private final @Nullable ClickEvent<T> event;

    public ClickPreComponent(final @Nullable ClickEvent<T> event, final PreComponent @Nullable [] children) {
        this.event = event;
        this.children = children;
    }

    @Override
    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        final TextComponent.Builder builder = Component.text();
        final @Nullable Style style = computedStyle;
        if (style != null && !style.isEmpty()) builder.style(style);
        builder.clickEvent(event);
        renderChildrenInto(pointered, builder, scope);
        return builder.build();
    }

    @Override
    public boolean isStatic() {
        return childrenAreStatic();
    }

    @Nullable ClickEvent<T> getEvent() {
        return event;
    }

    @Override
    public @NotNull PreComponent deepCopy() {
        return new ClickPreComponent<>(event, copyChildren()).setStyle(computedStyle);
    }
}
