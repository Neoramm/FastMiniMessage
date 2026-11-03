package io.github.neoramm.fastminimessage.v2.model;

import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.v2.template.TagSource;
import io.github.neoramm.fastminimessage.v2.template.TemplateNode;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RenderScope {
    private static final @NotNull Object @NotNull [] NO_PAYLOADS = new Object[0];
    public static final @NotNull RenderScope EMPTY = new RenderScope(PayloadSchema.EMPTY, NO_PAYLOADS, null);
    final @NotNull PayloadSchema schema;
    final @Nullable Object @NotNull [] payloads;
    @Nullable Pointered viewer;

    RenderScope(final @NotNull PayloadSchema schema, final @Nullable Object @NotNull [] payloads,
        final @Nullable Pointered viewer) {
        this.schema = schema;
        this.payloads = payloads;
        this.viewer = viewer;
    }

    public static @NotNull Builder builder(final @NotNull PayloadSchema schema) {
        return new Builder(Objects.requireNonNull(schema, "schema"));
    }

    public static @NotNull Mutable mutable(final @NotNull PayloadSchema schema) {
        return new Mutable(Objects.requireNonNull(schema, "schema"));
    }

    public static @NotNull Mutable mutable() {
        return mutable(PayloadSchema.withDefaults());
    }

    public @NotNull Mutable toMutable() {
        final @NotNull Mutable copy = mutable(schema);
        if (payloads.length != 0) System.arraycopy(payloads, 0, copy.payloads, 0, payloads.length);
        copy.viewer = viewer;
        return copy;
    }

    public @NotNull PayloadSchema schema() {
        return schema;
    }

    public int payloadCount() {
        return payloads.length;
    }

    public <T> @NotNull T payload(final PayloadSchema.@NotNull Key<T> key) {
        schema.requireKey(key);
        return Objects.requireNonNull(key.type().cast(payloads[key.index()]), "payload");
    }

    public @Nullable Pointered viewer() {
        return viewer;
    }

    public @NotNull RenderScope withViewer(final @Nullable Pointered viewer) {
        return this.viewer == viewer ? this : new RenderScope(schema, payloads, viewer);
    }

    public void requireSchema(final @NotNull PayloadSchema expected) {
        Objects.requireNonNull(expected, "expected");
        if (expected.requiredCount() != 0 && schema != expected) {
            throw new IllegalArgumentException("Render scope belongs to another payload schema");
        }
    }

    public <T> @Nullable T payloadOrNull(final PayloadSchema.@NotNull Key<T> key) {
        schema.requireKey(key);
        return key.type().cast(payloads[key.index()]);
    }

    public boolean hasIndexed(final @NotNull String key) {
        final @Nullable TranslationPayload indexed = optional(PayloadSchema.TRANSLATION_ARGUMENTS_KEY);
        return indexed != null && !indexed.arguments().isEmpty()
            && ("arg".equalsIgnoreCase(key) || "argument".equalsIgnoreCase(key));
    }

    public @Nullable Component indexedValue(final @NotNull Arguments arguments) {
        final @Nullable TranslationPayload indexed = optional(PayloadSchema.TRANSLATION_ARGUMENTS_KEY);
        return indexed == null ? null : indexed.resolve(arguments);
    }

    public @Nullable RuntimeTagBridge resolve(final @NotNull String key) {
        Objects.requireNonNull(key, "key");
        if (hasIndexed(key)) return null;
        final @Nullable RuntimeTagBridge runtime = optional(PayloadSchema.RUNTIME_TAGS_KEY);
        final @Nullable RuntimeTagBridge resolved = runtime == null ? null : runtime.resolve(key);
        if (resolved != null) return resolved;
        final @Nullable RuntimeTagBridge adventure = optional(PayloadSchema.ADVENTURE_TAGS_KEY);
        return adventure == null ? null : adventure.resolve(key);
    }

    public boolean isEmpty() {
        if (viewer != null) return false;
        for (int i = 0; i < payloads.length; i++) if (payloads[i] != null) return false;
        return true;
    }

    public @NotNull Component resolveSlot(final TemplateNode.@NotNull Slot slot) {
        final @Nullable RuntimeTagBridge binding = resolve(slot.key());
        final @Nullable Component resolved = resolveBound(slot, binding);
        if (resolved == null) return fallback(slot);
        final @NotNull Component styled = applyStyle(resolved, slot.inheritedStyle());
        if (rendersChildren(binding)) return styled;
        if (slot.children().isEmpty()) return styled;
        return appendChildren(slot, styled);
    }

    public void renderSlotInto(final TemplateNode.@NotNull Slot slot, final TextComponent.@NotNull Builder output) {
        final @Nullable RuntimeTagBridge binding = resolve(slot.key());
        if (binding != null && slot.inheritedStyle().isEmpty()) {
            if (binding.renderInto(bridgeContext(slot), output)) {
                if (!binding.rendersChildren()) {
                    for (int i = 0, size = slot.children().size(); i < size; i++) {
                        slot.children().get(i).renderInto(this, output);
                    }
                }
                return;
            }
            renderFallbackInto(slot, output);
            return;
        }
        final @Nullable Component resolved = resolveBound(slot, binding);
        if (!slot.inheritedStyle().isEmpty() && (resolved == null || !slot.children().isEmpty()
            && !rendersChildren(binding))) {
            final @NotNull Component rendered = resolved == null ? fallback(slot)
                : appendChildren(slot, applyStyle(resolved, slot.inheritedStyle()));
            output.append(rendered);
            return;
        }
        if (resolved == null) {
            renderFallbackInto(slot, output);
            return;
        }
        output.append(applyStyle(resolved, slot.inheritedStyle()));
        if (rendersChildren(binding)) return;
        for (int i = 0, size = slot.children().size(); i < size; i++) slot.children().get(i).renderInto(this, output);
    }

    public static @NotNull Component applyStyle(final @NotNull Component component, final @NotNull Style style) {
        return style.isEmpty() ? component : component.style(style.merge(component.style()));
    }

    public @Nullable Component resolveValue(final @NotNull String key, final @NotNull Arguments args) {
        return resolveValue(key, args, null);
    }

    public @Nullable Component resolveValue(final @NotNull String key, final @NotNull Arguments args,
        final @Nullable MiniMessageCompiler compiler) {
        if (hasIndexed(key)) return indexedValue(args);
        final @Nullable RuntimeTagBridge bridge = resolve(key);
        if (bridge == null) return viewerValue(key);
        return bridge.render(new RuntimeTagContext(key, args.copy(), List.of(), !args.isEmpty(), false, false,
            this, compiler == null ? MiniMessageCompiler.defaults() : compiler));
    }

    private @NotNull Component appendChildren(final TemplateNode.@NotNull Slot slot,
        final @NotNull Component styled) {
        final TextComponent.@NotNull Builder builder = Component.text().style(slot.inheritedStyle())
            .append(styled);
        for (int i = 0, size = slot.children().size(); i < size; i++) slot.children().get(i).renderInto(this, builder);
        return builder.build();
    }

    private void renderFallbackInto(final TemplateNode.@NotNull Slot slot,
        final TextComponent.@NotNull Builder output) {
        output.append(slot.source().opening());
        for (int i = 0, size = slot.children().size(); i < size; i++) slot.children().get(i).renderInto(this, output);
        output.append(slot.source().closing());
    }

    private @Nullable Component resolveBound(final TemplateNode.@NotNull Slot slot,
        final @Nullable RuntimeTagBridge binding) {
        if (binding != null) return binding.render(bridgeContext(slot));
        if (hasIndexed(slot.key())) return indexedValue(slot.args().evaluate(this).rawArguments());
        return viewerValue(slot.key());
    }

    private static boolean rendersChildren(final @Nullable RuntimeTagBridge binding) {
        return binding != null && binding.rendersChildren();
    }

    private <T> @Nullable T optional(final PayloadSchema.@NotNull Key<T> key) {
        return schema.contains(key) ? key.type().cast(payloads[key.index()]) : null;
    }

    private @Nullable Component viewerValue(final @NotNull String key) {
        if (viewer == null || !("player".equalsIgnoreCase(key) || "name".equalsIgnoreCase(key))) return null;
        final @Nullable String name = viewer.get(Identity.NAME).orElse(null);
        return name == null ? null : Component.text(name);
    }

    private @NotNull RuntimeTagContext bridgeContext(final TemplateNode.@NotNull Slot slot) {
        final @NotNull TagSource source = slot.source();
        return new RuntimeTagContext(slot.key(), slot.args().evaluate(this).arguments(), slot.children(),
            source.hasArguments(), source.selfClosing(), source.explicitlyClosed(), this, source.compiler());
    }

    private @NotNull Component fallback(final TemplateNode.@NotNull Slot slot) {
        final TextComponent.@NotNull Builder builder = Component.text().style(slot.inheritedStyle());
        builder.append(slot.source().opening());
        for (int i = 0, size = slot.children().size(); i < size; i++) slot.children().get(i).renderInto(this, builder);
        builder.append(slot.source().closing());
        return builder.build().compact();
    }

    public static final class Builder {
        private final @NotNull PayloadSchema schema;
        private @Nullable Object @Nullable [] payloads;
        private @Nullable Pointered viewer;
        private int assigned;

        private Builder(final @NotNull PayloadSchema schema) {
            this.schema = schema;
            this.payloads = schema.size() == 0 ? NO_PAYLOADS : new Object[schema.size()];
        }

        public <T> @NotNull Builder payload(final PayloadSchema.@NotNull Key<T> key, final @NotNull T value) {
            final @Nullable Object @NotNull [] values = requireOpen();
            schema.requireKey(key);
            key.type().cast(Objects.requireNonNull(value, "value"));
            if (values[key.index()] == null && key.isRequired()) assigned++;
            values[key.index()] = value;
            return this;
        }

        public @NotNull Builder viewer(final @Nullable Pointered viewer) {
            requireOpen();
            this.viewer = viewer;
            return this;
        }

        public @NotNull RenderScope build() {
            final @Nullable Object @NotNull [] values = requireOpen();
            if (assigned != schema.requiredCount()) {
                throw new IllegalStateException("Every required payload must be assigned");
            }
            final @Nullable Pointered target = viewer;
            payloads = null;
            viewer = null;
            if (values.length == 0 && target == null) return EMPTY;
            return new RenderScope(schema, values, target);
        }

        private @Nullable Object @NotNull [] requireOpen() {
            final @Nullable Object @Nullable [] values = payloads;
            if (values == null) throw new IllegalStateException("Render scope builder has already been consumed");
            return values;
        }
    }

    /**
     * A reusable, single-threaded {@link RenderScope} for zero-allocation rendering loops.
     */
    public static final class Mutable extends RenderScope {
        private Mutable(final @NotNull PayloadSchema schema) {
            super(schema, schema.size() == 0 ? NO_PAYLOADS : new Object[schema.size()], null);
        }

        public <T> @NotNull Mutable payload(final PayloadSchema.@NotNull Key<T> key, final @NotNull T value) {
            schema.requireKey(key);
            key.type().cast(Objects.requireNonNull(value, "value"));
            payloads[key.index()] = value;
            return this;
        }

        public <T> @NotNull Mutable payloadOrNull(final PayloadSchema.@NotNull Key<T> key, final @Nullable T value) {
            schema.requireKey(key);
            if (value != null) key.type().cast(value);
            payloads[key.index()] = value;
            return this;
        }

        public @NotNull Mutable viewer(final @Nullable Pointered viewer) {
            this.viewer = viewer;
            return this;
        }

        public @NotNull Mutable clear() {
            if (payloads.length != 0) Arrays.fill(payloads, null);
            this.viewer = null;
            return this;
        }

        public @NotNull Mutable clearPayload(final PayloadSchema.@NotNull Key<?> key) {
            schema.requireKey(key);
            payloads[key.index()] = null;
            return this;
        }

        public @NotNull Mutable copy() {
            final @NotNull Mutable copy = new Mutable(schema);
            if (payloads.length != 0) System.arraycopy(payloads, 0, copy.payloads, 0, payloads.length);
            copy.viewer = viewer;
            return copy;
        }

        public @NotNull RenderScope toImmutable() {
            final @Nullable Object @NotNull [] copy = payloads.length == 0 ? NO_PAYLOADS : payloads.clone();
            return new RenderScope(schema, copy, viewer);
        }
    }
}
