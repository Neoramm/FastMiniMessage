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

import java.util.UUID;

public final class PlayerHeadResolver implements ComponentResolver {
    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        if (args.isEmpty()) return new InlinePreComponent(Component.object(ObjectContents.playerHead().build()));

        final String argument = args.get(0, "");
        TriState outerLayer = TriState.NOT_SET;

        if (args.size() == 1) {
            outerLayer = argumentToTriState(argument);
            if (outerLayer != TriState.NOT_SET) {
                return new InlinePreComponent(Component.object(
                    ObjectContents.playerHead()
                        .hat(outerLayer.toBooleanOrElse(PlayerHeadObjectContents.DEFAULT_HAT))
                        .build()
                ));
            }
        } else {
            outerLayer = argumentToTriState(args.get(1, ""));
        }

        if (args.size() > 2) return new LiteralPreComponent("", inheritedStyle);

        UUID uuid = null;
        try {
            uuid = UUID.fromString(argument);
        } catch (final IllegalArgumentException invalidUuid) {
            uuid = null;
        }

        if (uuid != null) {
            final boolean hat = outerLayer.toBooleanOrElse(PlayerHeadObjectContents.DEFAULT_HAT);
            return new InlinePreComponent(Component.object(
                ObjectContents.playerHead()
                    .id(uuid)
                    .hat(hat)
                    .build()
            ));
        }

        if (argument.contains("/")) {
            Key textureKey;
            try {
                textureKey = Key.key(argument);
            } catch (final IllegalArgumentException invalidTextureKey) {
                textureKey = null;
            }
            if (textureKey != null) {
                final boolean hat = outerLayer.toBooleanOrElse(PlayerHeadObjectContents.DEFAULT_HAT);
                return new InlinePreComponent(Component.object(
                    ObjectContents.playerHead()
                        .texture(textureKey)
                        .hat(hat)
                        .build()
                ));
            }
        }

        final String playerName = argument.trim();
        final boolean hat = outerLayer.toBooleanOrElse(PlayerHeadObjectContents.DEFAULT_HAT);
        return new InlinePreComponent(Component.object(
            ObjectContents.playerHead()
                .name(playerName)
                .hat(hat)
                .build()
        ));
    }

    private @NotNull TriState argumentToTriState(final @NotNull String value) {
        return switch (value.toLowerCase()) {
            case "true", "yes", "on" -> TriState.TRUE;
            case "false", "no", "off" -> TriState.FALSE;
            case "not_set", "default" -> TriState.NOT_SET;
            default -> TriState.NOT_SET;
        };
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return new String[]{"head"};
    }

    @Override
    public boolean isSelfClosing() {
        return true;
    }
}
