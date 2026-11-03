package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
import io.github.neoramm.fastminimessage.v2.template.TemplateNode;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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
            final Component rendered = tree.render(PayloadInputs.viewer(NOBODY));
            final List<String> expected = ComponentComparison.expression(reference);
            final List<String> actual = ComponentComparison.expression(rendered);
            if (!expected.equals(actual)) failures.add(ComponentComparison.difference(sample, expected, actual));
        }
        assertEquals(List.of(), failures, "expression differences");
    }

    @Test
    void noStyleWrapperSurvivesFlattening() {
        for (final String sample : SAMPLES) {
            assertInstanceOf(TemplateNode.Static.class, compiler.compile(sample).root(), sample);
        }
    }

    @Test
    void adjacentLiteralsAreMergedAtEveryLevel() {
        for (final String sample : SAMPLES) assertMerged(compiler.compile(sample), sample);
    }

    @Test
    void styledGroupsOnlyWrapOpaqueComponents() {
        for (final String sample : SAMPLES) {
            assertInstanceOf(TemplateNode.Static.class, compiler.compile(sample).root(), sample);
        }
    }

    @Test
    void staticGradientAndHoverFoldTogether() {
        final var tree = compiler.compile(
            "<red><gradient:red:blue>a<hover:show_text:'h'>b</hover></gradient></red>");
        assertInstanceOf(TemplateNode.Static.class, tree.root());
        assertEquals(1, ComponentComparison.preCompilerNodeCount(tree));
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
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> pKey = layout.key("p", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("p", (final var scope) -> scope.payload(pKey));
        final @NotNull MiniMessageCompiler placeholderCompiler = new MiniMessageCompiler(registry);
        final List<String> failures = new ArrayList<>();
        for (final String[] sample : cases) {
            final PreComponentTree tree = placeholderCompiler.compile(sample[0]);
            final Component actual = tree.render((final var builder) ->
                builder.viewer(NOBODY).payload(pKey, Component.text(sample[1])));
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
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> pKey = layout.key("p", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("p", (final var scope) -> scope.payload(pKey));
        final PreComponentTree tree = new MiniMessageCompiler(registry).compile("<bold><red><p></red></bold>");
        final Component actual = tree.render((final var builder) ->
            builder.viewer(NOBODY).payload(pKey, Component.text("X", NamedTextColor.BLUE)));
        final Component expected = miniMessage.deserialize("<bold><red><blue>X</blue></red></bold>");
        assertEquals(ComponentComparison.expression(expected), ComponentComparison.expression(actual));
    }

    @Test
    void placeholderRendersDoNotLeakIntoFollowingRenders() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> pKey = layout.key("p", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("p", (final var scope) -> scope.payload(pKey));
        final PreComponentTree tree = new MiniMessageCompiler(registry).compile("<bold><red><p></red></bold>");
        final String first = ComponentComparison.plainText(tree.render((final var builder) ->
            builder.viewer(NOBODY).payload(pKey, Component.text("A"))));
        final String second = ComponentComparison.plainText(tree.render((final var builder) ->
            builder.viewer(NOBODY).payload(pKey, Component.text("B"))));
        assertEquals("A", first);
        assertEquals("B", second);
    }

    @Test
    void repeatedRendersKeepTheSameExpression() {
        for (final String sample : SAMPLES) {
            final PreComponentTree tree = compiler.compile(sample);
            final List<String> first = ComponentComparison.expression(tree.render(PayloadInputs.viewer(NOBODY)));
            final List<String> second = ComponentComparison.expression(tree.render(PayloadInputs.viewer(NOBODY)));
            assertEquals(first, second, () -> "render is not stable for " + sample);
        }
    }

    private static void assertMerged(final @NotNull PreComponentTree tree, final @NotNull String sample) {
        assertEquals(tree.render().compact(), tree.render(), sample);
    }
}
