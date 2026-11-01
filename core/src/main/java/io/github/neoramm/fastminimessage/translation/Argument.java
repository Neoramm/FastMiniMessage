package io.github.neoramm.fastminimessage.translation;

import io.github.neoramm.fastminimessage.model.TagResolver;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.TranslationArgumentLike;
import net.kyori.adventure.text.VirtualComponentRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public final class Argument {
    private Argument() {
    }

    public static @NotNull ComponentLike component(final @NotNull String name, final @NotNull ComponentLike value) {
        return argument(name, TranslationArgument.component(Objects.requireNonNull(value, "value")));
    }

    public static @NotNull ComponentLike string(final @NotNull String name, final @NotNull String value) {
        return component(name, Component.text(Objects.requireNonNull(value, "value")));
    }

    public static @NotNull ComponentLike bool(final @NotNull String name, final boolean value) {
        return argument(name, TranslationArgument.bool(value));
    }

    public static @NotNull ComponentLike numeric(final @NotNull String name, final @NotNull Number value) {
        return argument(name, TranslationArgument.numeric(Objects.requireNonNull(value, "value")));
    }

    public static @NotNull ComponentLike argument(final @NotNull String name,
        final @NotNull TranslationArgumentLike value) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        if (name.isEmpty()) throw new IllegalArgumentException("Empty argument name");
        for (int i = 0; i < name.length(); i++) {
            final char character = name.charAt(i);
            if (character != '_' && character != '-' && (character < 'a' || character > 'z')
                && (character < '0' || character > '9')) {
                throw new IllegalArgumentException("Invalid argument name: " + name);
            }
        }
        return Component.virtual(Void.class, new Named(name, value.asTranslationArgument().asComponent()));
    }

    public static @NotNull ComponentLike tagResolver(final TagResolver @NotNull ... resolvers) {
        Objects.requireNonNull(resolvers, "resolvers");
        return Component.virtual(Void.class, new Resolvers(List.of(resolvers)));
    }

    public static @NotNull ComponentLike target(final @NotNull Pointered target) {
        return Component.virtual(Void.class, new Target(Objects.requireNonNull(target, "target")));
    }

    record Named(@NotNull String name, @NotNull Component value) implements VirtualComponentRenderer<@Nullable Void> {
        @Override
        public @NotNull Component apply(final @Nullable Void context) {
            return value;
        }
    }

    record Resolvers(@NotNull List<@NotNull TagResolver> values) implements VirtualComponentRenderer<@Nullable Void> {
        @Override
        public @NotNull Component apply(final @Nullable Void context) {
            return Component.empty();
        }
    }

    record Target(@NotNull Pointered value) implements VirtualComponentRenderer<@Nullable Void> {
        @Override
        public @NotNull Component apply(final @Nullable Void context) {
            return Component.empty();
        }
    }
}
