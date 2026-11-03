package io.github.neoramm.fastminimessage.v2.model;

import io.github.neoramm.fastminimessage.v2.api.ComponentModifier;
import io.github.neoramm.fastminimessage.v2.template.FoldPass;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TagResolver {
    private static final @NotNull RuntimeTagBridge EMPTY = new Combined(new RuntimeTagBridge[0]);

    private TagResolver() {
    }

    public static @NotNull RuntimeTagBridge inserting(final @NotNull String key, final @NotNull Component value) {
        Objects.requireNonNull(value, "value");
        return new Named(key, (final var context) -> value, false);
    }

    public static @NotNull RuntimeTagBridge inserting(final @NotNull String key, final @NotNull String value) {
        return inserting(key, Component.text(value));
    }

    public static @NotNull RuntimeTagBridge modifying(final @NotNull String key,
        final @NotNull ComponentModifier modifier) {
        Objects.requireNonNull(modifier, "modifier");
        return new Named(key, (final var context) -> modifier.modify(
            FoldPass.render(context.children(), context.scope()), context.scope()), true);
    }

    public static @NotNull RuntimeTagBridge dynamic(final @NotNull String key,
        final RuntimeTagBridge.@NotNull Renderer renderer) {
        Objects.requireNonNull(renderer, "renderer");
        return new Named(key, (final var context) -> renderer.render(context.arguments(), context.scope()), false);
    }

    public static @NotNull RuntimeTagBridge dynamic(final @NotNull String key,
        final @NotNull Function<@NotNull Arguments, @NotNull Component> renderer) {
        Objects.requireNonNull(renderer, "renderer");
        return dynamic(key, (final var args, final var viewer) -> renderer.apply(args));
    }

    public static @NotNull RuntimeTagBridge resolver(final @NotNull String key, final @NotNull Component value) {
        return inserting(key, value);
    }

    public static @NotNull RuntimeTagBridge resolver(final @NotNull String key, final @NotNull String value) {
        return inserting(key, value);
    }

    public static @NotNull RuntimeTagBridge resolver(final @NotNull String key,
        final @NotNull ComponentModifier modifier) {
        return modifying(key, modifier);
    }

    public static @NotNull RuntimeTagBridge custom(final @NotNull RuntimeTagBridge bridge) {
        return Objects.requireNonNull(bridge, "bridge");
    }

    public static @NotNull RuntimeTagBridge combine(final @NotNull RuntimeTagBridge @NotNull ... bridges) {
        Objects.requireNonNull(bridges, "bridges");
        if (bridges.length == 0) return EMPTY;
        if (bridges.length == 1) return Objects.requireNonNull(bridges[0], "bridge");
        final @NotNull RuntimeTagBridge @NotNull [] owned = bridges.clone();
        for (final @NotNull RuntimeTagBridge bridge : owned) Objects.requireNonNull(bridge, "bridge");
        return new Combined(owned);
    }

    public static @NotNull RuntimeTagBridge combine(
        final @NotNull List<? extends @NotNull RuntimeTagBridge> bridges) {
        Objects.requireNonNull(bridges, "bridges");
        return combine(bridges.toArray(RuntimeTagBridge[]::new));
    }

    private record Named(@NotNull String key,
        @NotNull Function<@NotNull RuntimeTagContext, @Nullable Component> renderer,
        boolean rendersChildren) implements RuntimeTagBridge {
        private Named {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(renderer, "renderer");
        }

        @Override
        public boolean has(final @NotNull String name) {
            return key.equals(name) || key.equalsIgnoreCase(name);
        }

        @Override
        public @Nullable Component render(final @NotNull RuntimeTagContext context) {
            return renderer.apply(context);
        }
    }

    private record Combined(@NotNull RuntimeTagBridge @NotNull [] bridges) implements RuntimeTagBridge {
        @Override
        public boolean has(final @NotNull String name) {
            return resolve(name) != null;
        }

        @Override
        public @Nullable RuntimeTagBridge resolve(final @NotNull String name) {
            for (int i = 0; i < bridges.length; i++) {
                final @Nullable RuntimeTagBridge bridge = bridges[i].resolve(name);
                if (bridge != null) return bridge;
            }
            return null;
        }

        @Override
        public @Nullable Component render(final @NotNull RuntimeTagContext context) {
            final @Nullable RuntimeTagBridge bridge = resolve(context.name());
            return bridge == null ? null : bridge.render(context);
        }
    }
}
