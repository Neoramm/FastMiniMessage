package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.InlinePreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import net.kyori.adventure.util.TriState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

public final class PlayerHeadResolver implements ComponentResolver {
    private static final String @NotNull [] NAMES = {"head"};
    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        if (args.isEmpty()) {
            return new InlinePreComponent(Component.object(ObjectContents.playerHead().build()))
                .setStyle(inheritedStyle);
        }

        final String argument = args.getFirst("");
        final TriState outerLayer;
        if (args.size() == 1) {
            final TriState single = argumentToTriState(argument);
            if (single != TriState.NOT_SET) {
                final boolean hat = single.toBooleanOrElse(PlayerHeadObjectContents.DEFAULT_HAT);
                return new InlinePreComponent(Component.object(ObjectContents.playerHead().hat(hat).build()))
                    .setStyle(inheritedStyle);
            }
            outerLayer = single;
        } else {
            outerLayer = argumentToTriState(args.get(1, ""));
        }
        if (args.size() > 2) {
            throw new IllegalArgumentException(
                "The head tag only accepts a target and an optional hat boolean");
        }

        final boolean hat = outerLayer.toBooleanOrElse(PlayerHeadObjectContents.DEFAULT_HAT);
        final PlayerHeadObjectContents.Builder head = ObjectContents.playerHead().hat(hat);
        try {
            return new InlinePreComponent(Component.object(head.id(UUID.fromString(argument)).build()))
                .setStyle(inheritedStyle);
        } catch (final IllegalArgumentException notUuid) {
            if (argument.contains("/")) {
                try {
                    return new InlinePreComponent(Component.object(head.texture(Key.key(argument)).build()))
                        .setStyle(inheritedStyle);
                } catch (final IllegalArgumentException notKey) {
                }
            }
            return new InlinePreComponent(Component.object(head.name(argument.trim()).build()))
                .setStyle(inheritedStyle);
        }
    }

    private @NotNull TriState argumentToTriState(final @NotNull String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "yes", "on" -> TriState.TRUE;
            case "false", "no", "off" -> TriState.FALSE;
            case "not_set", "default" -> TriState.NOT_SET;
            default -> TriState.NOT_SET;
        };
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return NAMES;
    }

    @Override
    public boolean isSelfClosing() {
        return true;
    }
}
