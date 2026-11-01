package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public final class HoverPreComponent extends PreComponent {
    private final @Nullable HoverEvent<?> staticEvent;
    private final @Nullable BiFunction<Pointered, RenderScope, HoverEvent<?>> eventFunction;

    public HoverPreComponent(final @NotNull HoverEvent<?> staticEvent, final PreComponent @Nullable [] children) {
        this.staticEvent = staticEvent;
        this.eventFunction = null;
        this.children = children;
    }

    public HoverPreComponent(final @NotNull BiFunction<Pointered, RenderScope, HoverEvent<?>> eventFunction,
        final PreComponent @Nullable [] children) {
        this.staticEvent = null;
        this.eventFunction = eventFunction;
        this.children = children;
    }

    @Override
    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        final TextComponent.Builder builder = Component.text();
        final @Nullable Style style = computedStyle;
        if (style != null && !style.isEmpty()) builder.style(style);
        final HoverEvent<?> event = staticEvent != null
            ? staticEvent
            : (eventFunction != null ? eventFunction.apply(pointered, scope) : null);
        if (event != null) builder.hoverEvent(event);
        renderChildrenInto(pointered, builder, scope);
        return builder.build();
    }

    @Override
    public boolean isScopeIndependent() {
        return staticEvent != null && childrenAreScopeIndependent();
    }

    @Override
    public boolean isStatic() {
        return staticEvent != null && childrenAreStatic();
    }

    @Override
    public @NotNull PreComponent deepCopy() {
        final HoverPreComponent copy = staticEvent != null
            ? new HoverPreComponent(staticEvent, copyChildren())
            : new HoverPreComponent(eventFunction != null ? eventFunction : (p, s) -> null, copyChildren());
        return copy.setStyle(computedStyle);
    }
}
