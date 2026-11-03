package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.api.ComponentModifier;
import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import io.github.neoramm.fastminimessage.v2.tag.StandardLeafTags;
import io.github.neoramm.fastminimessage.v2.tag.StandardStyleTags;
import io.github.neoramm.fastminimessage.v2.tag.StandardTransformTags;
import io.github.neoramm.fastminimessage.v2.tag.TagSpec;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ResolverRegistry {
    private final @NotNull Map<@NotNull String, @NotNull TagSpec> tags = new ConcurrentHashMap<>();
    private final @NotNull PayloadSchema payloadSchema;

    private ResolverRegistry(final @NotNull PayloadSchema payloadSchema) {
        this.payloadSchema = payloadSchema;
    }

    public static @NotNull ResolverRegistry create() {
        return create(PayloadSchema.EMPTY);
    }

    public static @NotNull ResolverRegistry create(final @NotNull PayloadSchema payloadSchema) {
        return new ResolverRegistry(Objects.requireNonNull(payloadSchema, "payloadSchema"));
    }

    public static @NotNull ResolverRegistry withDefaults() {
        return withDefaults(PayloadSchema.withDefaults());
    }

    public static @NotNull ResolverRegistry withDefaults(final @NotNull PayloadSchema payloadSchema) {
        final @NotNull ResolverRegistry registry = create(payloadSchema);
        StandardStyleTags.register(registry);
        StandardLeafTags.register(registry);
        StandardTransformTags.register(registry);
        return registry;
    }

    public @NotNull PayloadSchema payloadSchema() {
        return payloadSchema;
    }

    public @NotNull ResolverRegistry registerPlaceholder(final @NotNull String name,
        final @NotNull Function<@NotNull RenderScope, @NotNull Component> renderer) {
        return register(new TagSpec.PlaceholderRule(Set.of(Objects.requireNonNull(name, "name")), renderer));
    }

    public @NotNull ResolverRegistry registerModifier(final @NotNull String name,
        final @NotNull ComponentModifier modifier) {
        return register(new TagSpec.ModifierRule(Set.of(Objects.requireNonNull(name, "name")), modifier));
    }

    public @NotNull ResolverRegistry register(final @NotNull TagSpec spec) {
        Objects.requireNonNull(spec, "spec");
        for (final @NotNull String name : spec.names()) tags.put(name.toLowerCase(Locale.ROOT), spec);
        return this;
    }

    public @NotNull ResolverRegistry registerAll(final @NotNull Collection<@NotNull TagSpec> specs) {
        Objects.requireNonNull(specs, "specs");
        for (final @NotNull TagSpec spec : specs) register(spec);
        return this;
    }

    public @NotNull ResolverRegistry unregister(final @NotNull String name) {
        tags.remove(Objects.requireNonNull(name, "name").toLowerCase(Locale.ROOT));
        return this;
    }

    public @Nullable TagSpec resolve(final @NotNull String name) {
        Objects.requireNonNull(name, "name");
        final @Nullable TagSpec direct = tags.get(name);
        if (direct != null) return direct;
        for (int i = 0, len = name.length(); i < len; i++) {
            final char c = name.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                final @Nullable TagSpec spec = tags.get(name.toLowerCase(Locale.ROOT));
                return spec != null ? spec : name.startsWith("#") ? tags.get("color") : null;
            }
        }
        return name.startsWith("#") ? tags.get("color") : null;
    }
}
