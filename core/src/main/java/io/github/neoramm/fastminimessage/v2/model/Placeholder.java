package io.github.neoramm.fastminimessage.v2.model;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public final class Placeholder {
    private Placeholder() {
    }

    public static @NotNull RuntimeTagBridge ofUnparsed(final @NotNull String key, final @NotNull String value) {
        return TagResolver.inserting(key, value);
    }

    public static @NotNull RuntimeTagBridge ofParsed(final @NotNull String key, final @NotNull Component value) {
        return TagResolver.inserting(key, value);
    }

    public static @NotNull RuntimeTagBridge of(final @NotNull String key, final @NotNull Component value) {
        return ofParsed(key, value);
    }
}
