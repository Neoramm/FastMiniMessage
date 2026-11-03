package io.github.neoramm.fastminimessage.v2.translation;

import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import java.util.Objects;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.TranslationArgumentLike;
import net.kyori.adventure.text.VirtualComponentRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            if (character != '_' && character != '-' && character != '.' && (character < 'a' || character > 'z')
                && (character < 'A' || character > 'Z') && (character < '0' || character > '9')) {
                throw new IllegalArgumentException("Invalid argument name: " + name);
            }
        }
        return Component.virtual(Void.class, new Named(name, value.asTranslationArgument().asComponent()));
    }

    public static <T> @NotNull ComponentLike payload(final PayloadSchema.@NotNull Key<T> key,
        final @NotNull T value) {
        Objects.requireNonNull(key, "key");
        key.type().cast(Objects.requireNonNull(value, "value"));
        return Component.virtual(Void.class, new Payload<>(key, value));
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

    record Payload<T>(PayloadSchema.@NotNull Key<T> key, @NotNull T value)
        implements VirtualComponentRenderer<@Nullable Void> {
        void applyTo(final RenderScope.@NotNull Builder builder) {
            builder.payload(key, value);
        }

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
