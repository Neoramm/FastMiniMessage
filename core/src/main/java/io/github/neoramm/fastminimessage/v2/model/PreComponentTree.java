package io.github.neoramm.fastminimessage.v2.model;

import io.github.neoramm.fastminimessage.v2.template.TemplateNode;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public final class PreComponentTree {
    public static final @NotNull PreComponentTree EMPTY =
        new PreComponentTree(new TemplateNode.Static(Component.empty()), PayloadSchema.EMPTY);
    private final @NotNull TemplateNode root;
    private final @NotNull PayloadSchema payloadSchema;
    private final @NotNull RenderScope defaultScope;

    public PreComponentTree(final @NotNull TemplateNode root) {
        this(root, PayloadSchema.withDefaults());
    }

    public PreComponentTree(final @NotNull TemplateNode root, final @NotNull PayloadSchema payloadSchema) {
        this.root = Objects.requireNonNull(root, "root");
        this.payloadSchema = Objects.requireNonNull(payloadSchema, "payloadSchema");
        this.defaultScope = payloadSchema.requiredCount() == 0 ? RenderScope.builder(payloadSchema).build()
            : RenderScope.EMPTY;
    }

    public @NotNull TemplateNode root() {
        return root;
    }

    public @NotNull Component render() {
        if (root instanceof TemplateNode.Static(Component component)) return component;
        return render(defaultScope);
    }

    public int payloadCount() {
        return payloadSchema.size();
    }

    public RenderScope.@NotNull Builder scopeBuilder() {
        return RenderScope.builder(payloadSchema);
    }

    public RenderScope.@NotNull Mutable mutableScope() {
        return RenderScope.mutable(payloadSchema);
    }

    public @NotNull Component render(final @NotNull Consumer<RenderScope.@NotNull Builder> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        final RenderScope.@NotNull Builder builder = scopeBuilder();
        consumer.accept(builder);
        if (root instanceof TemplateNode.Static(Component component)) return component;
        return render(builder.build());
    }

    public @NotNull Component render(final @NotNull RenderScope scope) {
        Objects.requireNonNull(scope, "scope");
        if (root instanceof TemplateNode.Static(Component component)) return component;
        scope.requireSchema(payloadSchema);
        return root.render(scope);
    }

    public boolean isStatic() {
        return root instanceof TemplateNode.Static;
    }

    public boolean isScopeIndependent() {
        return isStatic();
    }

    public @NotNull PreComponentTree copy() {
        return this;
    }

    public void inspect(final @NotNull Consumer<@NotNull TemplateNode> visitor) {
        Objects.requireNonNull(visitor, "visitor");
        inspect(root, visitor);
    }

    public @NotNull PreComponentTree map(final @NotNull UnaryOperator<@NotNull TemplateNode> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new PreComponentTree(map(root, mapper), payloadSchema);
    }

    private static void inspect(final @NotNull TemplateNode node,
        final @NotNull Consumer<@NotNull TemplateNode> visitor) {
        visitor.accept(node);
        final @NotNull List<@NotNull TemplateNode> children = children(node);
        for (final @NotNull TemplateNode child : children) inspect(child, visitor);
    }

    private static @NotNull TemplateNode map(final @NotNull TemplateNode node,
        final @NotNull UnaryOperator<@NotNull TemplateNode> mapper) {
        final @NotNull List<@NotNull TemplateNode> children = children(node);
        final @NotNull List<@NotNull TemplateNode> mapped = children.stream()
            .map((final var child) -> map(child, mapper)).toList();
        final @NotNull TemplateNode updated = switch (node) {
            case TemplateNode.DynamicTransform transform ->
                new TemplateNode.DynamicTransform(transform.baseStyle(), mapped, transform.transformer());
            case TemplateNode.Slot slot ->
                new TemplateNode.Slot(slot.key(), slot.args(), slot.inheritedStyle(), mapped, slot.source());
            case TemplateNode.RegisteredPlaceholder placeholder ->
                new TemplateNode.RegisteredPlaceholder(placeholder.key(), placeholder.inheritedStyle(), mapped,
                    placeholder.renderer());
            default -> node;
        };
        return Objects.requireNonNull(mapper.apply(updated), "mapped node");
    }

    private static @NotNull List<@NotNull TemplateNode> children(final @NotNull TemplateNode node) {
        return switch (node) {
            case TemplateNode.DynamicTransform transform -> transform.children();
            case TemplateNode.Slot slot -> slot.children();
            case TemplateNode.RegisteredPlaceholder placeholder -> placeholder.children();
            default -> List.of();
        };
    }
}
