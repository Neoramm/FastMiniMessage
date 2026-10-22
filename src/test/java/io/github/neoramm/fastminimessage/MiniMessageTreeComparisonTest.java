package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.component.ClickPreComponent;
import io.github.neoramm.fastminimessage.component.ColorPreComponent;
import io.github.neoramm.fastminimessage.component.GradientPreComponent;
import io.github.neoramm.fastminimessage.component.GroupPreComponent;
import io.github.neoramm.fastminimessage.component.HoverPreComponent;
import io.github.neoramm.fastminimessage.component.InsertingPreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.component.ShadowPreComponent;
import io.github.neoramm.fastminimessage.component.UnparsedPreComponent;
import io.github.neoramm.fastminimessage.model.Placeholder;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compares the tree produced by the precompiler with MiniMessage's tree. Both must yield the same
 * expression when rendered, even though the precompiler deliberately flattens style wrappers.
 */
final class MiniMessageTreeComparisonTest {

    private static final @NotNull Pointered NOBODY = new Pointered() {
        @Override
        public @NotNull Pointers pointers() {
            return Pointers.empty();
        }
    };

    private static final @NotNull String @NotNull [] SAMPLES = {
        "plain",
        "<red>a</red><blue>b</blue>",
        "<red>a<blue>b</blue>c</red>",
        "<bold>a <red>b</red> c</bold>",
        "<bold><italic><underlined><strikethrough><obfuscated>a</obfuscated>"
            + "</strikethrough></underlined></italic></bold>",
        "<bold>a<!bold>b</!bold>c</bold>",
        "<red>a<reset>b</reset>",
        "<bold><red>a<reset>b</reset>",
        "<reset>abc",
        "<red>a</red><reset>b<bold>c</bold>",
        "<red>a</red>b<red>c</red>",
        "<red>a</red><red>b</red>",
        "<bold><red><click:run_command:'/spawn'>x</click></red></bold>",
        "<red><click:run_command:'/spawn'>a <bold>b</bold></click></red>",
        "<insert:'x'>a <underlined>b</underlined></insert>",
        "<shadow:red:0.4>a <bold>b</bold></shadow>",
        "<hover:show_text:'<gray>hint <bold>x</bold></gray>'>a <red>b</red></hover>",
        "<red><hover:show_text:'hint'>a <bold>b</bold></hover></red>",
        "<click:run_command:'/x'><hover:show_text:'h'>a <red>b</red></hover></click>",
        "<hover:show_text:'h'><click:run_command:'/x'>a</click></hover>",
        "<hover:show_item:'minecraft:diamond':1>a</hover>",
        "<red><hover:show_item:'minecraft:diamond':1>a</hover></red>",
        "<hover:show_entity:'minecraft:zombie':00000000-0000-0000-0000-000000000000:'Zombie'>a</hover>",
        "<key:key.jump/>",
        "<red><key:key.jump/></red>",
        "<lang:chat.type.text:Steve:hello/>",
        "<red><lang:chat.type.text:Steve:hello/></red>",
        "<sprite:items:item/diamond/>",
        "<red><sprite:items:item/diamond/></red>",
        "<head:Steve/>",
        "<gold><head:Steve/></gold>",
        "<gradient:red:blue>abc</gradient>",
        "<bold><gradient:red:blue>abc</gradient></bold>",
        "<gradient:red:blue><bold>a</bold>b</gradient>",
        "<gradient:red:blue>a <red>b</red> c</gradient>",
        "<red><gradient:red:blue>abc</gradient></red>",
        "<gradient:red:blue><gradient:green:yellow>abc</gradient>def</gradient>",
        "<gradient:red:blue><rainbow>abc</rainbow>def</gradient>",
        "<rainbow>abc</rainbow>",
        "<rainbow:5>abc</rainbow>",
        "<rainbow:0>abc</rainbow>",
        "<rainbow:!5>abc</rainbow>",
        "<click:run_command:'/x'><gradient:red:blue>abc</gradient></click>",
        "<gradient:red:blue><click:run_command:'/x'>abc</click></gradient>",
        "<newline/>",
        "<bold>a<br/>b</bold>",
        "<red>😀a</red>",
        "<gradient:red:blue>😀a</gradient>",
        "",
        "<bold>",
        "<red>a",
    };

    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
    private final @NotNull MiniMessage miniMessage = MiniMessage.builder().strict(false).build();

    @Test
    void precompilerTreeRendersTheSameExpressionAsMiniMessage() {
        final List<String> failures = new ArrayList<>();
        for (final String sample : SAMPLES) {
            final Component reference = miniMessage.deserialize(sample);
            final PreComponentTree tree = compiler.compile(sample);
            final Component rendered = tree.render(NOBODY);
            final List<String> expected = ComponentComparison.expression(reference);
            final List<String> actual = ComponentComparison.expression(rendered);
            if (!expected.equals(actual)) failures.add(ComponentComparison.difference(sample, expected, actual));
        }
        assertEquals(List.of(), failures, "expression differences");
    }

    @Test
    void noStyleWrapperSurvivesFlattening() {
        for (final String sample : SAMPLES) {
            final List<PreComponent> nodes = nodesOf(compiler.compile(sample));
            for (final PreComponent node : nodes) {
                assertFalse(node instanceof ColorPreComponent, () -> "color wrapper survived in " + sample);
                assertFalse(node instanceof ClickPreComponent, () -> "click wrapper survived in " + sample);
                assertFalse(node instanceof InsertingPreComponent, () -> "insert wrapper survived in " + sample);
                assertFalse(node instanceof ShadowPreComponent, () -> "shadow wrapper survived in " + sample);
            }
        }
    }

    @Test
    void adjacentLiteralsAreMergedAtEveryLevel() {
        for (final String sample : SAMPLES) assertMerged(compiler.compile(sample), sample);
    }

    @Test
    void styledGroupsOnlyWrapOpaqueComponents() {
        for (final String sample : SAMPLES) {
            for (final PreComponent node : nodesOf(compiler.compile(sample))) {
                if (!(node instanceof GroupPreComponent group) || group.getComputedStyle().isEmpty()) continue;
                assertTrue(hasOpaqueChild(group),
                    () -> "styled group without an opaque child survived in " + sample + ": "
                        + node.getComputedStyle());
            }
        }
    }

    @Test
    void gradientAndHoverRemainTheOnlyStyleBoundaries() {
        final List<PreComponent> nodes = nodesOf(compiler.compile(
            "<red><gradient:red:blue>a<hover:show_text:'h'>b</hover></gradient></red>"));
        assertTrue(nodes.stream().anyMatch(GradientPreComponent.class::isInstance));
        assertTrue(nodes.stream().anyMatch(HoverPreComponent.class::isInstance));
        assertFalse(nodes.stream().anyMatch(node ->
            node instanceof GroupPreComponent group && !group.getComputedStyle().isEmpty()));
    }

    @Test
    void placeholdersRenderLikeTheirMiniMessageSubstitution() {
        final String[][] cases = {
            {"<p>", "X"},
            {"<bold><red><p></red></bold>", "X"},
            {"<red>a<p>c</red>", "b"},
            {"<gradient:red:blue><p></gradient>", "abc"},
            {"<gradient:red:blue>a<p>c</gradient>", "b"},
            {"<hover:show_text:'h'><p></hover>", "X"},
            {"<click:run_command:'/x'><p></click>", "X"},
            {"<insert:'x'><bold><p></bold></insert>", "X"},
            {"<shadow:red:0.4><p></shadow>", "X"},
        };
        final List<String> failures = new ArrayList<>();
        for (final String[] sample : cases) {
            final PreComponentTree tree = compiler.compile(sample[0]);
            final Component actual = tree.render(NOBODY, Placeholder.ofUnparsed("p", sample[1]));
            final Component expected = miniMessage.deserialize(sample[0].replace("<p>", sample[1]));
            final List<String> expectedExpression = ComponentComparison.expression(expected);
            final List<String> actualExpression = ComponentComparison.expression(actual);
            if (!expectedExpression.equals(actualExpression)) {
                failures.add(ComponentComparison.difference(sample[0], expectedExpression, actualExpression));
            }
        }
        assertEquals(List.of(), failures, "placeholder expression differences");
    }

    @Test
    void parsedPlaceholdersLetTheirOwnStyleWin() {
        final PreComponentTree tree = compiler.compile("<bold><red><p></red></bold>");
        final Component actual = tree.render(NOBODY,
            Placeholder.ofParsed("p", Component.text("X", NamedTextColor.BLUE)));
        final Component expected = miniMessage.deserialize("<bold><red><blue>X</blue></red></bold>");
        assertEquals(ComponentComparison.expression(expected), ComponentComparison.expression(actual));
    }

    @Test
    void placeholderRendersDoNotLeakIntoFollowingRenders() {
        final PreComponentTree tree = compiler.compile("<bold><red><p></red></bold>");
        final String first = ComponentComparison.plainText(tree.render(NOBODY, Placeholder.ofUnparsed("p", "A")));
        final String second = ComponentComparison.plainText(tree.render(NOBODY, Placeholder.ofUnparsed("p", "B")));
        final String fallback = ComponentComparison.plainText(tree.render(NOBODY));
        assertEquals("A", first);
        assertEquals("B", second);
        assertEquals("<p>", fallback);
    }

    @Test
    void repeatedRendersKeepTheSameExpression() {
        for (final String sample : SAMPLES) {
            final PreComponentTree tree = compiler.compile(sample);
            final List<String> first = ComponentComparison.expression(tree.render(NOBODY));
            final List<String> second = ComponentComparison.expression(tree.render(NOBODY));
            assertEquals(first, second, () -> "render is not stable for " + sample);
        }
    }

    private static void assertMerged(final @NotNull PreComponentTree tree, final @NotNull String sample) {
        tree.visit(node -> {
            final PreComponent[] children = node.getChildren();
            for (int i = 1; i < children.length; i++) {
                if (children[i - 1] instanceof LiteralPreComponent previous
                    && children[i] instanceof LiteralPreComponent current) {
                    assertFalse(previous.getComputedStyle().equals(current.getComputedStyle()),
                        () -> "adjacent literals with equal style survived in " + sample
                            + ": \"" + previous.getText() + "\" and \"" + current.getText() + '"');
                }
            }
            for (final PreComponent child : children) {
                if (child instanceof LiteralPreComponent literal) {
                    assertEquals(0, literal.getChildren().length,
                        () -> "literal with children survived in " + sample);
                }
            }
        });
    }

    private static boolean hasOpaqueChild(final @NotNull GroupPreComponent group) {
        for (final PreComponent child : group.getChildren()) {
            if (child instanceof UnparsedPreComponent) return true;
            if (!(child instanceof LiteralPreComponent) && !(child instanceof GroupPreComponent)) return true;
            if (child instanceof GroupPreComponent nested && !nested.getComputedStyle().isEmpty()
                && hasOpaqueChild(nested)) return true;
        }
        return false;
    }

    private static @NotNull List<PreComponent> nodesOf(final @NotNull PreComponentTree tree) {
        final List<PreComponent> nodes = new ArrayList<>();
        tree.visit(nodes::add);
        return nodes;
    }
}
