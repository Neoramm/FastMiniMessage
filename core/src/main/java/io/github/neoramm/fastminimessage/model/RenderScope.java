package io.github.neoramm.fastminimessage.model;

import io.github.neoramm.fastminimessage.api.ComponentModifier;
import io.github.neoramm.fastminimessage.resolver.ModifierResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class RenderScope {

    public static final @NotNull RenderScope EMPTY = new RenderScope(null, null, null);

    private final Placeholder @Nullable [] placeholders;
    private final ModifierResolver @Nullable [] modifiers;
    private final RuntimeTagResolver @Nullable [] tagResolvers;

    private RenderScope(final Placeholder @Nullable [] placeholders, final ModifierResolver @Nullable [] modifiers,
        final RuntimeTagResolver @Nullable [] tagResolvers) {
        this.placeholders = placeholders;
        this.modifiers = modifiers;
        this.tagResolvers = tagResolvers;
    }

    public static @NotNull RenderScope of(final Placeholder @Nullable ... placeholders) {
        return of(placeholders, null, null);
    }

    public static @NotNull RenderScope of(final @Nullable List<Placeholder> placeholders) {
        return of(placeholders == null ? null : placeholders.toArray(Placeholder[]::new), null, null);
    }

    public static @NotNull RenderScope of(final ModifierResolver @Nullable ... modifiers) {
        return of(null, modifiers, null);
    }

    public static @NotNull RenderScope ofTags(final RuntimeTagResolver @Nullable ... tagResolvers) {
        return of(null, null, tagResolvers);
    }

    public static @NotNull RenderScope of(final Placeholder @Nullable [] placeholders,
        final ModifierResolver @Nullable [] modifiers) {
        return of(placeholders, modifiers, null);
    }

    public static @NotNull RenderScope of(final Placeholder @Nullable [] placeholders,
        final ModifierResolver @Nullable [] modifiers, final RuntimeTagResolver @Nullable [] tagResolvers) {
        if (isEmpty(placeholders, modifiers, tagResolvers)) return EMPTY;
        return new RenderScope(
            placeholders == null || placeholders.length == 0 ? null : placeholders.clone(),
            modifiers == null || modifiers.length == 0 ? null : modifiers.clone(),
            tagResolvers == null || tagResolvers.length == 0 ? null : tagResolvers.clone()
        );
    }

    public static @NotNull RenderScope of(final @Nullable List<Placeholder> placeholders,
        final @Nullable List<ModifierResolver> modifiers) {
        return of(placeholders, modifiers, null);
    }

    public static @NotNull RenderScope of(final @Nullable List<Placeholder> placeholders,
        final @Nullable List<ModifierResolver> modifiers, final @Nullable List<RuntimeTagResolver> tagResolvers) {
        return of(
            placeholders == null ? null : placeholders.toArray(Placeholder[]::new),
            modifiers == null ? null : modifiers.toArray(ModifierResolver[]::new),
            tagResolvers == null ? null : tagResolvers.toArray(RuntimeTagResolver[]::new)
        );
    }

    public @Nullable PreComponent resolvePlaceholder(final @NotNull String key) {
        final Placeholder @Nullable [] arr = placeholders;
        if (arr == null) return null;
        for (final Placeholder placeholder : arr) {
            if (key.equals(placeholder.key())) return placeholder.preComponent();
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

    public boolean hasTagResolvers() {
        return tagResolvers != null;
    }

    public @Nullable RuntimeTagResolver resolveTag(final @NotNull String name) {
        final RuntimeTagResolver @Nullable [] arr = tagResolvers;
        if (arr == null) return null;
        for (final RuntimeTagResolver resolver : arr) {
            if (resolver.has(name)) return resolver;
        }
        return null;
    }

    public @Nullable PreComponent resolve(final @NotNull String key) {
        return resolvePlaceholder(key);
    }

    public boolean isEmpty() {
        return placeholders == null && modifiers == null && tagResolvers == null;
    }

    private static boolean isEmpty(final Placeholder @Nullable [] placeholders,
        final ModifierResolver @Nullable [] modifiers, final RuntimeTagResolver @Nullable [] tagResolvers) {
        return (placeholders == null || placeholders.length == 0)
            && (modifiers == null || modifiers.length == 0)
            && (tagResolvers == null || tagResolvers.length == 0);
    }
}
