package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Pushes inherited styles down to the components that actually render text, so the render path
 * does not need one wrapper component per nested tag.
 *
 * <p>The input tree is normalized in place where possible: callers must not reuse it after
 * flattening.</p>
 */
public final class StyleFlattener {

    private StyleFlattener() {
    }

    /**
     * Flattens the given tree in place-friendly fashion and returns the normalized root.
     *
     * @param root the root to flatten
     * @return the flattened root
     */
    public static @NotNull PreComponent flatten(final @NotNull PreComponent root) {
        final List<PreComponent> flattened = new ArrayList<>();
        normalizeInto(root, Style.empty(), flattened);
        return groupOf(flattened);
    }

    private static void normalizeInto(final @NotNull PreComponent node, final @NotNull Style inherited,
        final @NotNull List<PreComponent> out) {
        if (node instanceof LiteralPreComponent literal) {
            appendLiteral(literal, merge(inherited, literal.getComputedStyle()), out);
            return;
        }
        if (node instanceof GroupPreComponent group) {
            final Style childStyle = merge(inherited, group.getComputedStyle());
            for (final PreComponent child : group.getChildren()) normalizeInto(child, childStyle, out);
            return;
        }
        if (node instanceof ColorPreComponent color) {
            final Style childStyle = merge(inherited, color.getComputedStyle());
            final TextColor colorValue = color.getColor();
            final Style colored = colorValue != null ? childStyle.color(colorValue) : childStyle;
            for (final PreComponent child : color.getChildren()) normalizeInto(child, colored, out);
            return;
        }
        if (node instanceof ClickPreComponent click) {
            final Style base = merge(inherited, click.getComputedStyle());
            final ClickEvent event = click.getEvent();
            final Style childStyle = event != null ? base.clickEvent(event) : base;
            for (final PreComponent child : click.getChildren()) normalizeInto(child, childStyle, out);
            return;
        }
        if (node instanceof InsertingPreComponent insert) {
            final Style base = merge(inherited, insert.getComputedStyle());
            final String insertion = insert.getInsertion();
            final Style childStyle = insertion != null ? base.insertion(insertion) : base;
            for (final PreComponent child : insert.getChildren()) normalizeInto(child, childStyle, out);
            return;
        }
        if (node instanceof ShadowPreComponent shadow) {
            final Style base = merge(inherited, shadow.getComputedStyle());
            final ShadowColor color = shadow.getShadowColor();
            final Style childStyle = color != null ? base.shadowColor(color) : base;
            for (final PreComponent child : shadow.getChildren()) normalizeInto(child, childStyle, out);
            return;
        }
        if (node instanceof GradientPreComponent gradient) {
            gradient.setStyle(merge(inherited, gradient.getComputedStyle()));
            gradient.setChildren(normalizeChildren(gradient.getChildren(), Style.empty()));
            out.add(gradient);
            return;
        }
        if (node instanceof HoverPreComponent hover) {
            hover.setStyle(merge(inherited, hover.getComputedStyle()));
            hover.setChildren(normalizeChildren(hover.getChildren(), Style.empty()));
            out.add(hover);
            return;
        }
        if (node instanceof ModifiedPreComponent modified) {
            modified.setStyle(merge(inherited, modified.getComputedStyle()));
            modified.setChildren(normalizeChildren(modified.getChildren(), Style.empty()));
            out.add(modified);
            return;
        }
        final Style style = merge(inherited, node.getComputedStyle());
        if (node instanceof InlinePreComponent || node instanceof UnparsedPreComponent
            || node instanceof KeybindPreComponent || node instanceof TranslatablePreComponent
            || node instanceof StaticComponentPreComponent) {
            node.setStyle(style);
            out.add(node);
            return;
        }
        if (style.isEmpty()) {
            out.add(node);
            return;
        }
        out.add(new GroupPreComponent(new PreComponent[]{node}).setStyle(style));
    }

    private static @NotNull Style merge(final @NotNull Style inherited, final @NotNull Style own) {
        if (inherited.isEmpty()) return own;
        if (own.isEmpty()) return inherited;
        return inherited.merge(own);
    }

    private static PreComponent @NotNull [] normalizeChildren(final PreComponent @NotNull [] children,
        final @NotNull Style inherited) {
        final List<PreComponent> out = new ArrayList<>(children.length);
        for (final PreComponent child : children) normalizeInto(child, inherited, out);
        return out.toArray(new PreComponent[0]);
    }

    private static void appendLiteral(final @NotNull LiteralPreComponent literal, final @NotNull Style style,
        final @NotNull List<PreComponent> out) {
        if (!out.isEmpty() && out.getLast() instanceof LiteralPreComponent previous
            && previous.getChildren().length == 0 && literal.getChildren().length == 0
            && previous.getComputedStyle().equals(style)) {
            previous.setText(previous.getText() + literal.getText());
            return;
        }
        if (!literal.getComputedStyle().equals(style)) literal.setStyle(style);
        out.add(literal);
    }

    private static @NotNull PreComponent groupOf(final @NotNull List<PreComponent> components) {
        if (components.isEmpty()) return new LiteralPreComponent("", Style.empty());
        if (components.size() == 1) return components.getFirst();
        return new GroupPreComponent(components.toArray(new PreComponent[0]));
    }
}
