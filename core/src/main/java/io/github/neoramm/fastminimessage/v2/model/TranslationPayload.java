package io.github.neoramm.fastminimessage.v2.model;

import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslationArgument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record TranslationPayload(@NotNull List<@NotNull TranslationArgument> arguments) {
    public TranslationPayload {
        arguments = List.copyOf(arguments);
    }

    public @Nullable Component resolve(final @NotNull Arguments args) {
        Objects.requireNonNull(args, "args");
        final int index = args.size() == 1 ? args.getInt(0, -1) : -1;
        return index >= 0 && index < arguments.size() ? arguments.get(index).asComponent() : null;
    }
}
