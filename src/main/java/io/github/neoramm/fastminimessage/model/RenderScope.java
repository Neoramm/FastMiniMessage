package io.github.neoramm.fastminimessage.model;

import io.github.neoramm.fastminimessage.resolver.ModifierResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class RenderScope {

  public static final RenderScope EMPTY = new RenderScope(null, null);

  private final Placeholder[] placeholders;
  private final ModifierResolver[] modifiers;

  private RenderScope(final @Nullable Placeholder[] placeholders, final @Nullable ModifierResolver[] modifiers) {
    this.placeholders = placeholders;
    this.modifiers = modifiers;
  }

  public static RenderScope of(final @Nullable Placeholder... placeholders) {
    return of(placeholders, null);
  }

  public static RenderScope of(final @Nullable List<Placeholder> placeholders) {
    return of(placeholders == null ? null : placeholders.toArray(Placeholder[]::new), null);
  }

  public static RenderScope of(final @Nullable ModifierResolver... modifiers) {
    return of(null, modifiers);
  }

  public static RenderScope of(final @Nullable Placeholder[] placeholders, final @Nullable ModifierResolver[] modifiers) {
    if ((placeholders == null || placeholders.length == 0) && (modifiers == null || modifiers.length == 0)) return EMPTY;
    return new RenderScope(
      placeholders == null || placeholders.length == 0 ? null : placeholders.clone(),
      modifiers == null || modifiers.length == 0 ? null : modifiers.clone()
    );
  }

  public static RenderScope of(final @Nullable List<Placeholder> placeholders, final @Nullable List<ModifierResolver> modifiers) {
    return of(
      placeholders == null ? null : placeholders.toArray(Placeholder[]::new),
      modifiers == null ? null : modifiers.toArray(ModifierResolver[]::new)
    );
  }

  public @Nullable PreComponent resolvePlaceholder(final @NotNull String key) {
    final Placeholder[] arr = placeholders;
    if (arr == null) return null;
    for (final Placeholder placeholder : arr) {
      if (placeholder.getKey().equals(key)) return placeholder.getPreComponent();
    }
    return null;
  }

  public @Nullable io.github.neoramm.fastminimessage.api.ComponentModifier resolveModifier(final @NotNull String name) {
    final ModifierResolver[] arr = modifiers;
    if (arr == null) return null;
    for (final ModifierResolver resolver : arr) {
      for (final String n : resolver.names()) {
        if (n.equals(name)) return resolver.modifier();
      }
    }
    return null;
  }

  // backward compat
  public @Nullable PreComponent resolve(final @NotNull String key) {
    return resolvePlaceholder(key);
  }

  public boolean isEmpty() {
    return (placeholders == null || placeholders.length == 0) && (modifiers == null || modifiers.length == 0);
  }
}