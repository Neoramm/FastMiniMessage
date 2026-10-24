package io.github.neoramm.fastminimessage.model;

import io.github.neoramm.fastminimessage.api.ComponentModifier;
import io.github.neoramm.fastminimessage.resolver.ModifierResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class RenderScope {

    public static final @NotNull RenderScope EMPTY = new RenderScope(null, null);

    private final Placeholder @Nullable [] placeholders;
    private final ModifierResolver @Nullable [] modifiers;

    private RenderScope(final Placeholder @Nullable [] placeholders, final ModifierResolver @Nullable [] modifiers) {
        this.placeholders = placeholders;
        this.modifiers = modifiers;
    }

    public static @NotNull RenderScope of(final Placeholder @Nullable ... placeholders) {
        return of(placeholders, null);
    }

    public static @NotNull RenderScope of(final @Nullable List<Placeholder> placeholders) {
        return of(placeholders == null ? null : placeholders.toArray(Placeholder[]::new), null);
    }

    public static @NotNull RenderScope of(final ModifierResolver @Nullable ... modifiers) {
        return of(null, modifiers);
    }

    public static @NotNull RenderScope of(final Placeholder @Nullable [] placeholders,
        final ModifierResolver @Nullable [] modifiers) {
        if ((placeholders == null || placeholders.length == 0) && (modifiers == null || modifiers.length == 0)) {
            return EMPTY;
        }
        return new RenderScope(
            placeholders == null || placeholders.length == 0 ? null : placeholders.clone(),
            modifiers == null || modifiers.length == 0 ? null : modifiers.clone()
        );
    }

    public static @NotNull RenderScope of(final @Nullable List<Placeholder> placeholders,
        final @Nullable List<ModifierResolver> modifiers) {
        return of(
            placeholders == null ? null : placeholders.toArray(Placeholder[]::new),
            modifiers == null ? null : modifiers.toArray(ModifierResolver[]::new)
        );
    }

    public @Nullable PreComponent resolvePlaceholder(final @NotNull String key) {
        final Placeholder @Nullable [] arr = placeholders;
        if (arr == null) return null;
        for (final Placeholder placeholder : arr) {
            if (key.equals(placeholder.getKey())) return placeholder.getPreComponent();
        }
        return null;
    }

    public @Nullable ComponentModifier resolveModifier(final @NotNull String name) {
        final ModifierResolver @Nullable [] arr = modifiers;
        if (arr == null) return null;
        for (final ModifierResolver resolver : arr) {
            for (final String tagName : resolver.names()) {
                if (name.equals(tagName)) return resolver.modifier();
            }
        }
        return null;
    }

    public @Nullable PreComponent resolve(final @NotNull String key) {
        return resolvePlaceholder(key);
    }

    public boolean isEmpty() {
        return (placeholders == null || placeholders.length == 0) && (modifiers == null || modifiers.length == 0);
    }
}
