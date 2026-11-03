package io.github.neoramm.fastminimessage.v2.template;

import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;

public final class FoldPass {
    static final @NotNull TreeTransformer IDENTITY =
        (final var children, final var style, final var context) -> RenderScope.applyStyle(children, style);

    private FoldPass() {
    }

    public static @NotNull TemplateNode fold(final @NotNull List<@NotNull TemplateNode> nodes) {
        Objects.requireNonNull(nodes, "nodes");
        if (nodes.isEmpty()) return new TemplateNode.Static(Component.empty());
        if (nodes.size() == 1) return nodes.getFirst();
        final @NotNull List<@NotNull TemplateNode> folded = new ArrayList<>(nodes.size());
        final @NotNull List<@NotNull Component> constants = new ArrayList<>(nodes.size());
        for (final @NotNull TemplateNode node : nodes) {
            if (node instanceof final TemplateNode.Static fixed) {
                constants.add(fixed.component());
            } else {
                if (!constants.isEmpty()) {
                    folded.add(new TemplateNode.Static(merge(constants)));
                    constants.clear();
                }
                folded.add(node);
            }
        }
        if (!constants.isEmpty()) folded.add(new TemplateNode.Static(merge(constants)));
        return folded.size() == 1 ? folded.getFirst()
            : new TemplateNode.DynamicTransform(Style.empty(), folded, IDENTITY);
    }

    public static @NotNull TemplateNode transform(final @NotNull Style style,
        final @NotNull List<@NotNull TemplateNode> children,
        final @NotNull ValueSource<@NotNull TreeTransformer> source) {
        final @NotNull TemplateNode folded = fold(children);
        if (folded instanceof final TemplateNode.Static fixed && source.isConstant()) {
            return new TemplateNode.Static(source.evaluate(RenderScope.EMPTY)
                .transform(fixed.component(), style, RenderScope.EMPTY));
        }
        return new TemplateNode.DynamicTransform(style, children(folded),
            (final var content, final var inherited, final var context) ->
                source.evaluate(context).transform(content, inherited, context));
    }

    public static @NotNull List<@NotNull TemplateNode> children(final @NotNull TemplateNode node) {
        if (node instanceof final TemplateNode.DynamicTransform group
            && group.transformer() == IDENTITY && group.baseStyle().isEmpty()) return group.children();
        return List.of(node);
    }

    public static @NotNull Component render(final @NotNull List<@NotNull TemplateNode> nodes,
        final @NotNull RenderScope context) {
        if (nodes.isEmpty()) return Component.empty();
        if (nodes.size() == 1) return nodes.getFirst().render(context);
        final TextComponent.@NotNull Builder builder = Component.text();
        for (int i = 0, size = nodes.size(); i < size; i++) nodes.get(i).renderInto(context, builder);
        return builder.build();
    }

    /**
     * Compacts the static components of an already-folded tree exactly once.
     */
    public static @NotNull TemplateNode compactTree(final @NotNull TemplateNode node) {
        return switch (node) {
            case TemplateNode.Static fixed -> new TemplateNode.Static(compact(fixed.component()));
            case TemplateNode.DynamicTransform transform ->
                new TemplateNode.DynamicTransform(transform.baseStyle(), compactChildren(transform.children()),
                    transform.transformer());
            case TemplateNode.Slot slot ->
                new TemplateNode.Slot(slot.key(), slot.args(), slot.inheritedStyle(), compactChildren(slot.children()),
                    slot.source());
            case TemplateNode.DynamicLeaf leaf -> leaf;
            case TemplateNode.RegisteredPlaceholder placeholder ->
                new TemplateNode.RegisteredPlaceholder(placeholder.key(), placeholder.inheritedStyle(),
                    compactChildren(placeholder.children()), placeholder.renderer());
        };
    }

    private static @NotNull List<@NotNull TemplateNode> compactChildren(
        final @NotNull List<@NotNull TemplateNode> nodes) {
        final @NotNull List<@NotNull TemplateNode> compacted = new ArrayList<>(nodes.size());
        for (final @NotNull TemplateNode node : nodes) compacted.add(compactTree(node));
        return compacted;
    }

    private static @NotNull Component merge(final @NotNull List<@NotNull Component> constants) {
        if (constants.size() == 1) return constants.getFirst();
        final @NotNull List<@NotNull Component> merged = new ArrayList<>(constants.size());
        int index = 0;
        while (index < constants.size()) {
            final @NotNull Component current = constants.get(index);
            if (current instanceof final TextComponent text && text.children().isEmpty()) {
                final @NotNull StringBuilder content = new StringBuilder(text.content());
                final @NotNull Style style = text.style();
                int next = index + 1;
                while (next < constants.size()) {
                    final @NotNull Component candidate = constants.get(next);
                    if (candidate instanceof final TextComponent textNext && textNext.children().isEmpty()
                        && textNext.style().equals(style)) {
                        content.append(textNext.content());
                        next++;
                    } else break;
                }
                merged.add(Component.text(content.toString(), style));
                index = next;
            } else {
                merged.add(current);
                index++;
            }
        }
        if (merged.size() == 1) return merged.getFirst();
        final TextComponent.@NotNull Builder builder = Component.text();
        for (final @NotNull Component component : merged) builder.append(component);
        return builder.build();
    }

    private static @NotNull Component compact(final @NotNull Component component) {
        final @NotNull Component compacted = component.compact();
        if (compacted instanceof final TextComponent text && text.content().isEmpty()
            && text.style().isEmpty() && text.children().size() == 1) return text.children().getFirst();
        return compacted;
    }
}
