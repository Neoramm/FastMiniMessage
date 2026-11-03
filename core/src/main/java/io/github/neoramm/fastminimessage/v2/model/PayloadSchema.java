package io.github.neoramm.fastminimessage.v2.model;

import io.github.neoramm.fastminimessage.v2.compat.AdventureTags;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class PayloadSchema {
    public static final @NotNull Key<@NotNull AdventureTags> ADVENTURE_TAGS_KEY =
        new Key<>(0, "adventure_tags", AdventureTags.class, false);
    public static final @NotNull Key<@NotNull RuntimeTagBridge> RUNTIME_TAGS_KEY =
        new Key<>(1, "runtime_tags", RuntimeTagBridge.class, false);
    public static final @NotNull Key<@NotNull TranslationPayload> TRANSLATION_ARGUMENTS_KEY =
        new Key<>(2, "translation_arguments", TranslationPayload.class, false);
    public static final @NotNull PayloadSchema EMPTY = new PayloadSchema(List.of());
    private static final @NotNull PayloadSchema DEFAULTS = new PayloadSchema(
        List.of(ADVENTURE_TAGS_KEY, RUNTIME_TAGS_KEY, TRANSLATION_ARGUMENTS_KEY));
    private final @NotNull Key<?> @NotNull [] keys;
    private final int requiredCount;

    private PayloadSchema(final @NotNull List<@NotNull Key<?>> keys) {
        this.keys = keys.toArray(Key<?>[]::new);
        int required = 0;
        for (final @NotNull Key<?> key : keys) if (key.required) required++;
        this.requiredCount = required;
    }

    public static @NotNull Builder builder() {
        return new Builder(EMPTY);
    }

    public static @NotNull Builder builderWithDefaults() {
        return new Builder(DEFAULTS);
    }

    public static @NotNull PayloadSchema withDefaults() {
        return DEFAULTS;
    }

    public int size() {
        return keys.length;
    }

    public int requiredCount() {
        return requiredCount;
    }

    public boolean contains(final @NotNull Key<?> key) {
        Objects.requireNonNull(key, "key");
        return key.index < keys.length && keys[key.index] == key;
    }

    public void requireKey(final @NotNull Key<?> key) {
        if (!contains(key)) throw new IllegalArgumentException("Payload key belongs to another schema");
    }

    public static final class Key<T> {
        private final int index;
        private final @NotNull String name;
        private final @NotNull Class<T> type;
        private final boolean required;

        private Key(final int index, final @NotNull String name, final @NotNull Class<T> type,
            final boolean required) {
            this.index = index;
            this.name = name;
            this.type = type;
            this.required = required;
        }

        public int index() {
            return index;
        }

        public @NotNull String name() {
            return name;
        }

        public @NotNull Class<T> type() {
            return type;
        }

        public boolean isRequired() {
            return required;
        }
    }

    public static final class Builder {
        private final @NotNull List<@NotNull Key<?>> ordered = new ArrayList<>();
        private final @NotNull Map<@NotNull String, @NotNull Key<?>> keys = new HashMap<>();
        private boolean built;

        private Builder(final @NotNull PayloadSchema defaults) {
            for (final @NotNull Key<?> key : defaults.keys) {
                ordered.add(key);
                keys.put(key.name, key);
            }
        }

        public <T> @NotNull Key<T> key(final @NotNull String name, final @NotNull Class<T> type) {
            return key(name, type, true);
        }

        public <T> @NotNull Key<T> optionalKey(final @NotNull String name, final @NotNull Class<T> type) {
            return key(name, type, false);
        }

        public <T> @NotNull Key<T> key(final @NotNull String name, final @NotNull Class<T> type,
            final boolean required) {
            requireOpen();
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(type, "type");
            if (name.isBlank()) throw new IllegalArgumentException("Payload name must not be blank");
            if (type.isPrimitive()) throw new IllegalArgumentException("Payload type must be a reference type");
            if (keys.containsKey(name)) throw new IllegalArgumentException("Duplicate payload name: " + name);
            final @NotNull Key<T> key = new Key<>(ordered.size(), name, type, required);
            keys.put(name, key);
            ordered.add(key);
            return key;
        }

        public @NotNull PayloadSchema build() {
            requireOpen();
            built = true;
            if (ordered.isEmpty()) return EMPTY;
            if (ordered.size() == DEFAULTS.size() && ordered.getFirst() == ADVENTURE_TAGS_KEY) return DEFAULTS;
            return new PayloadSchema(ordered);
        }

        private void requireOpen() {
            if (built) throw new IllegalStateException("Payload schema has already been built");
        }
    }
}
