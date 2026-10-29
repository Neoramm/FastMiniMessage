package io.github.neoramm.fastminimessage.paper;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.HoverPreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

final class ItemHoverResolver implements ComponentResolver {
    private static final String @NotNull [] NAMES = {"item_hover"};

    private final @NotNull ItemHoverFactory factory;

    ItemHoverResolver(final @NotNull ItemHoverFactory factory) {
        this.factory = factory;
    }

    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        if (children.length == 0) return new LiteralPreComponent("", inheritedStyle);
        final String reference = Objects.requireNonNullElse(args.getFirst(""), "");
        final HoverEvent<?> event = factory.create(reference, args.getInt(1, 1));
        return new HoverPreComponent(
            (final @Nullable Pointered pointered, final @NotNull RenderScope scope) -> event, children
        ).setStyle(inheritedStyle);
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return NAMES;
    }
}
