package io.github.neoramm.fastminimessage.model;

import io.github.neoramm.fastminimessage.api.ComponentModifier;
import io.github.neoramm.fastminimessage.resolver.ModifierResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class RenderScope {

    public static final @NotNull RenderScope EMPTY = new RenderScope(null);

    private final TagResolver @Nullable [] resolvers;

    private RenderScope(final TagResolver @Nullable [] resolvers) {
        this.resolvers = resolvers;
    }

    public static @NotNull RenderScope from(final @Nullable RuntimeResolver... resolvers) {
        if (resolvers == null || resolvers.length == 0) return EMPTY;
        int nonNullCount = 0;
        for (final RuntimeResolver r : resolvers) if (r != null) nonNullCount++;
        if (nonNullCount == 0) return EMPTY;
        final TagResolver @NotNull [] arr = new TagResolver[nonNullCount];
        int idx = 0;
        for (final RuntimeResolver r : resolvers) {
            if (r == null) continue;
            if (r instanceof final TagResolver tr) arr[idx++] = tr;
            else if (r instanceof final RuntimeTagResolver rtr) arr[idx++] = TagResolver.custom(rtr);
            else throw new IllegalArgumentException("Unknown resolver: " + r);
        }
        return new RenderScope(arr);
    }

    public static @NotNull RenderScope of(final TagResolver @Nullable ... resolvers) {
        if (resolvers == null || resolvers.length == 0) return EMPTY;
        return new RenderScope(resolvers);
    }

    public static @NotNull RenderScope of(final @Nullable List<? extends TagResolver> resolvers) {
        if (resolvers == null || resolvers.isEmpty()) return EMPTY;
        return new RenderScope(resolvers.toArray(TagResolver[]::new));
    }

    public static @NotNull RenderScope of(final Placeholder @Nullable ... placeholders) {
        if (placeholders == null || placeholders.length == 0) return EMPTY;
        return new RenderScope(placeholders);
    }

    public static @NotNull RenderScope of(final ModifierResolver @Nullable ... modifiers) {
        if (modifiers == null || modifiers.length == 0) return EMPTY;
        return new RenderScope(modifiers);
    }

    public static @NotNull RenderScope ofTags(final RuntimeTagResolver @Nullable ... tagResolvers) {
        if (tagResolvers == null || tagResolvers.length == 0) return EMPTY;
        final TagResolver @NotNull [] arr = new TagResolver[tagResolvers.length];
        for (int i = 0; i < tagResolvers.length; i++) arr[i] = TagResolver.custom(tagResolvers[i]);
        return new RenderScope(arr);
    }

    public static @NotNull RenderScope of(final Placeholder @Nullable [] placeholders,
        final ModifierResolver @Nullable [] modifiers) {
        return of(placeholders, modifiers, null);
    }

    public static @NotNull RenderScope of(final Placeholder @Nullable [] placeholders,
        final ModifierResolver @Nullable [] modifiers, final RuntimeTagResolver @Nullable [] tagResolvers) {
        final int pLen = placeholders != null ? placeholders.length : 0;
        final int mLen = modifiers != null ? modifiers.length : 0;
        final int tLen = tagResolvers != null ? tagResolvers.length : 0;
        final int total = pLen + mLen + tLen;
        if (total == 0) return EMPTY;
        final TagResolver @NotNull [] arr = new TagResolver[total];
        int idx = 0;
        if (pLen > 0) for (final Placeholder p : placeholders) arr[idx++] = p;
        if (mLen > 0) for (final ModifierResolver m : modifiers) arr[idx++] = m;
        if (tLen > 0) for (final RuntimeTagResolver t : tagResolvers) arr[idx++] = TagResolver.custom(t);
        return new RenderScope(arr);
    }

    public static @NotNull RenderScope of(final @Nullable List<Placeholder> placeholders,
        final @Nullable List<ModifierResolver> modifiers) {
        return of(placeholders, modifiers, null);
    }

    public static @NotNull RenderScope of(final @Nullable List<Placeholder> placeholders,
        final @Nullable List<ModifierResolver> modifiers, final @Nullable List<RuntimeTagResolver> tagResolvers) {
        final int pLen = placeholders != null ? placeholders.size() : 0;
        final int mLen = modifiers != null ? modifiers.size() : 0;
        final int tLen = tagResolvers != null ? tagResolvers.size() : 0;
        final int total = pLen + mLen + tLen;
        if (total == 0) return EMPTY;
        final TagResolver @NotNull [] arr = new TagResolver[total];
        int idx = 0;
        if (pLen > 0) for (final Placeholder p : placeholders) arr[idx++] = p;
        if (mLen > 0) for (final ModifierResolver m : modifiers) arr[idx++] = m;
        if (tLen > 0) for (final RuntimeTagResolver t : tagResolvers) arr[idx++] = TagResolver.custom(t);
        return new RenderScope(arr);
    }

    public @Nullable TagResolver resolveTag(final @NotNull String name) {
        final TagResolver @Nullable [] arr = resolvers;
        if (arr == null) return null;
        for (final TagResolver resolver : arr) {
            if (resolver == null) continue;
            final String resName = resolver.name();
            if (name.equalsIgnoreCase(resName)) return resolver;
            final RuntimeTagResolver custom = resolver.customResolver();
            if (custom != null && custom.has(name)) return resolver;
        }
        return null;
    }

    public @Nullable PreComponent resolvePlaceholder(final @NotNull String key) {
        final TagResolver resolver = resolveTag(key);
        return resolver != null ? resolver.preComponent() : null;
    }

    public @Nullable ComponentModifier resolveModifier(final @NotNull String name) {
        final TagResolver resolver = resolveTag(name);
        return resolver != null ? resolver.modifier() : null;
    }

    public boolean hasTagResolvers() {
        return resolvers != null;
    }

    public @Nullable PreComponent resolve(final @NotNull String key) {
        return resolvePlaceholder(key);
    }

    public boolean isEmpty() {
        return resolvers == null || resolvers.length == 0;
    }
}
