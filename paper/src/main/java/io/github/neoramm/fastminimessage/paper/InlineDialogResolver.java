package io.github.neoramm.fastminimessage.paper;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.ClickPreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.dialog.DialogLike;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

final class InlineDialogResolver implements ComponentResolver {

    private final @NotNull DialogFactory factory;

    InlineDialogResolver(final @NotNull DialogFactory factory) {
        this.factory = factory;
    }

    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        if (children.length == 0) return new LiteralPreComponent("", inheritedStyle);
        final Component title = render(compiler, Objects.requireNonNullElse(args.get(0, "Dialog"), "Dialog"));
        final String bodyRaw = Objects.requireNonNullElse(args.get(1, ""), "");
        final @Nullable Component body = bodyRaw.isEmpty() ? null : render(compiler, bodyRaw);
        final Component button = render(compiler, Objects.requireNonNullElse(args.get(2, "OK"), "OK"));
        final DialogLike dialog = factory.create(new InlineDialog(title, body, button));
        return new ClickPreComponent(ClickEvent.showDialog(dialog), children);
    }

    private static @NotNull Component render(final @NotNull MiniMessageCompiler compiler, final @NotNull String input) {
        return compiler.compile(input).render(null);
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return new String[]{"show_dialog"};
    }
}
