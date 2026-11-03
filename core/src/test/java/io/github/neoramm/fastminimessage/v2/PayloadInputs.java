package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.api.ComponentModifier;
import io.github.neoramm.fastminimessage.v2.compat.AdventureTags;
import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import io.github.neoramm.fastminimessage.v2.model.RuntimeTagBridge;
import io.github.neoramm.fastminimessage.v2.model.TagResolver;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PayloadInputs {
    private PayloadInputs() {
    }

    public static @NotNull Consumer<RenderScope.@NotNull Builder> runtime(final @Nullable Pointered viewer,
        final @NotNull RuntimeTagBridge @NotNull ... bridges) {
        if (bridges.length == 1 && bridges[0] instanceof final AdventureTags adventure) {
            return (final var builder) -> builder.viewer(viewer).payload(PayloadSchema.ADVENTURE_TAGS_KEY, adventure);
        }
        final @NotNull RuntimeTagBridge bridge = combine(bridges);
        return (final var builder) -> builder.viewer(viewer).payload(PayloadSchema.RUNTIME_TAGS_KEY, bridge);
    }

    public static @NotNull Consumer<RenderScope.@NotNull Builder> runtime(final @Nullable Pointered viewer,
        final @NotNull List<? extends @NotNull RuntimeTagBridge> bridges) {
        final @NotNull RuntimeTagBridge bridge = TagResolver.combine(bridges);
        return (final var builder) -> builder.viewer(viewer).payload(PayloadSchema.RUNTIME_TAGS_KEY, bridge);
    }

    public static @NotNull Consumer<RenderScope.@NotNull Builder> viewer(final @Nullable Pointered viewer) {
        return (final var builder) -> builder.viewer(viewer);
    }

    public static @NotNull RenderScope scope(final @NotNull RuntimeTagBridge @NotNull ... bridges) {
        return RenderScope.builder(PayloadSchema.withDefaults())
            .payload(PayloadSchema.RUNTIME_TAGS_KEY, combine(bridges)).build();
    }

    public static @NotNull RenderScope scope(final @NotNull List<? extends @NotNull RuntimeTagBridge> bridges) {
        return RenderScope.builder(PayloadSchema.withDefaults())
            .payload(PayloadSchema.RUNTIME_TAGS_KEY, TagResolver.combine(bridges)).build();
    }

    public static @NotNull RuntimeTagBridge combine(final @NotNull RuntimeTagBridge @NotNull ... bridges) {
        return TagResolver.combine(bridges);
    }

    public static @NotNull RuntimeTagBridge value(final @NotNull String key, final @NotNull Component value) {
        return TagResolver.inserting(key, value);
    }

    public static @NotNull RuntimeTagBridge modifier(final @NotNull String key,
        final @NotNull ComponentModifier modifier) {
        return TagResolver.modifying(key, modifier);
    }

    public static @NotNull RuntimeTagBridge dynamic(final @NotNull String key,
        final RuntimeTagBridge.@NotNull Renderer renderer) {
        return TagResolver.dynamic(key, renderer);
    }

    public static @NotNull RuntimeTagBridge inserting(final @NotNull String key, final @NotNull String value) {
        return TagResolver.inserting(key, value);
    }

    public static @NotNull RuntimeTagBridge resolver(final @NotNull String key, final @NotNull Component value) {
        return TagResolver.inserting(key, value);
    }

    public static @NotNull RuntimeTagBridge modifying(final @NotNull String key,
        final @NotNull ComponentModifier modifier) {
        return TagResolver.modifying(key, modifier);
    }
}
