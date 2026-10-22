package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.component.GroupPreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Compares MiniMessage and precompiler output at two levels: the expression that a client would
 * actually display, and the shape of the tree that produces it.
 */
final class ComponentComparison {

    private ComponentComparison() {
    }

    /**
     * Flattens a component tree into one token per visible code point with its effective style.
     * Trees with different shapes but the same tokens render identically.
     */
    static @NotNull List<String> expression(final @NotNull Component component) {
        final List<String> out = new ArrayList<>();
        appendExpression(component, Style.empty(), out);
        return out;
    }

    /**
     * Describes the component hierarchy, including own and inherited style at each node.
     */
    static @NotNull List<String> componentTree(final @NotNull Component component) {
        final List<String> out = new ArrayList<>();
        appendComponentTree(component, Style.empty(), 0, out);
        return out;
    }

    /**
     * Describes the precompiler hierarchy before rendering.
     */
    static @NotNull List<String> preCompilerTree(final @NotNull PreComponentTree tree) {
        final List<String> out = new ArrayList<>();
        tree.visit(node -> out.add(describe(node)));
        return out;
    }

    static @NotNull String plainText(final @NotNull Component component) {
        final StringBuilder out = new StringBuilder();
        appendPlainText(component, out);
        return out.toString();
    }

    /**
     * Number of nodes in the precompiler tree, useful to confirm that flattening actually removed
     * style wrappers instead of just moving them around.
     */
    static int preCompilerNodeCount(final @NotNull PreComponentTree tree) {
        final int[] count = {0};
        tree.visit(node -> count[0]++);
        return count[0];
    }

    static @NotNull String describe(final @NotNull PreComponent node) {
        final StringBuilder out = new StringBuilder(node.getClass().getSimpleName());
        if (node instanceof LiteralPreComponent literal) out.append(" \"").append(literal.getText()).append('"');
        if (node instanceof GroupPreComponent) out.append(" children=").append(node.getChildren().length);
        out.append(" style=").append(node.getComputedStyle());
        return out.toString();
    }

    static @NotNull String difference(final @NotNull String source, final @NotNull List<String> expected,
        final @NotNull List<String> actual) {
        final int size = Math.min(expected.size(), actual.size());
        int index = 0;
        while (index < size && expected.get(index).equals(actual.get(index))) index++;
        final String expectedToken = index < expected.size() ? expected.get(index) : "<end>";
        final String actualToken = index < actual.size() ? actual.get(index) : "<end>";
        return "expression differs for: " + source
            + "\n  first difference at " + index
            + "\n  miniMessage: " + expectedToken
            + "\n  precompiler: " + actualToken
            + "\n  sizes: " + expected.size() + " vs " + actual.size();
    }

    private static void appendExpression(final @NotNull Component component, final @NotNull Style inherited,
        final @NotNull List<String> out) {
        final Style style = inherited.merge(component.style());
        if (component instanceof TextComponent text) {
            final String content = text.content();
            int index = 0;
            while (index < content.length()) {
                final int codePoint = content.codePointAt(index);
                index += Character.charCount(codePoint);
                out.add("'" + new String(Character.toChars(codePoint)) + "'" + styleSignature(style));
            }
        } else if (component instanceof TranslatableComponent translatable) {
            final StringBuilder args = new StringBuilder();
            for (final TranslationArgument argument : translatable.arguments()) {
                args.append(expression(argument.asComponent())).append(';');
            }
            out.add("<translatable:" + translatable.key() + ':' + translatable.fallback() + ':' + args + '>'
                + styleSignature(style));
        } else if (component instanceof KeybindComponent keybind) {
            out.add("<keybind:" + keybind.keybind() + '>' + styleSignature(style));
        } else if (component instanceof ObjectComponent object) {
            out.add("<object:" + object.getClass().getSimpleName() + ':' + object.contents() + '>'
                + styleSignature(style));
        } else {
            out.add("<" + component.getClass().getSimpleName() + '>' + styleSignature(style));
        }
        for (final Component child : component.children()) appendExpression(child, style, out);
    }

    private static void appendComponentTree(final @NotNull Component component, final @NotNull Style inherited,
        final int depth, final @NotNull List<String> out) {
        final Style effective = inherited.merge(component.style());
        out.add("  ".repeat(depth) + label(component)
            + " own=" + component.style()
            + " effective=" + effective);
        if (component instanceof TranslatableComponent translatable) {
            for (final TranslationArgument argument : translatable.arguments()) {
                out.add("  ".repeat(depth + 1) + "arg:");
                appendComponentTree(argument.asComponent(), effective, depth + 2, out);
            }
        }
        if (component.style().hoverEvent() instanceof HoverEvent<?> hover
            && hover.action() == HoverEvent.Action.SHOW_TEXT && hover.value() instanceof Component hovered) {
            out.add("  ".repeat(depth + 1) + "hover:");
            appendComponentTree(hovered, Style.empty(), depth + 2, out);
        }
        for (final Component child : component.children()) appendComponentTree(child, effective, depth + 1, out);
    }

    private static @NotNull String label(final @NotNull Component component) {
        if (component instanceof TextComponent text) return "text \"" + text.content() + '"';
        if (component instanceof TranslatableComponent translatable) return "translatable " + translatable.key();
        if (component instanceof KeybindComponent keybind) return "keybind " + keybind.keybind();
        if (component instanceof ObjectComponent object) {
            return "object " + object.getClass().getSimpleName() + ' ' + object.contents();
        }
        return component.getClass().getSimpleName();
    }

    private static void appendPlainText(final @NotNull Component component, final @NotNull StringBuilder out) {
        if (component instanceof TextComponent text) out.append(text.content());
        for (final Component child : component.children()) appendPlainText(child, out);
    }

    private static @NotNull String styleSignature(final @NotNull Style style) {
        final StringBuilder out = new StringBuilder(96);
        out.append(" color=").append(color(style.color()));
        for (final TextDecoration decoration : TextDecoration.values()) {
            out.append(' ').append(decoration).append('=').append(style.decoration(decoration));
        }
        out.append(" insert=").append(style.insertion());
        out.append(" click=").append(click(style.clickEvent()));
        out.append(" hover=").append(hover(style.hoverEvent()));
        out.append(" font=").append(style.font());
        out.append(" shadow=").append(shadow(style.shadowColor()));
        return out.toString();
    }

    private static @NotNull String color(final @Nullable TextColor color) {
        return color == null ? "-" : color.asHexString();
    }

    private static @NotNull String shadow(final @Nullable ShadowColor color) {
        return color == null ? "-" : color.asHexString();
    }

    private static @NotNull String click(final @Nullable ClickEvent event) {
        if (event == null) return "-";
        final ClickEvent.Payload payload = event.payload();
        final String value;
        if (payload instanceof ClickEvent.Payload.Text text) value = text.value();
        else if (payload instanceof ClickEvent.Payload.Int integer) value = Integer.toString(integer.integer());
        else value = String.valueOf(payload);
        return event.action().name() + ':' + value;
    }

    private static @NotNull String hover(final @Nullable HoverEvent<?> event) {
        if (event == null) return "-";
        final String action = String.valueOf(event.action());
        if (event.action() == HoverEvent.Action.SHOW_TEXT) return action + ':' + expression((Component) event.value());
        if (event.action() == HoverEvent.Action.SHOW_ITEM) {
            final HoverEvent.ShowItem item = (HoverEvent.ShowItem) event.value();
            return action + ':' + item.item() + ':' + item.count() + ':' + item.dataComponents();
        }
        if (event.action() == HoverEvent.Action.SHOW_ENTITY) {
            final HoverEvent.ShowEntity entity = (HoverEvent.ShowEntity) event.value();
            return action + ':' + entity.type() + ':' + entity.id() + ':'
                + (entity.name() == null ? "-" : expression(entity.name()));
        }
        return action + ':' + event.value();
    }
}
