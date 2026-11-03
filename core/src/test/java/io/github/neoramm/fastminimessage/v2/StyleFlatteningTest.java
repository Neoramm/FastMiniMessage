package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
import io.github.neoramm.fastminimessage.v2.tag.TagSpec;
import io.github.neoramm.fastminimessage.v2.template.TemplateNode;
import io.github.neoramm.fastminimessage.v2.template.ValueSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

final class StyleFlatteningTest {

    private static final @NotNull Pointered NOBODY = new Pointered() {
        @Override
        public @NotNull Pointers pointers() {
            return Pointers.empty();
        }
    };

    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());

    @Test
    void styleWrappersArePushedIntoLiteralLeaves() {
        final PreComponentTree tree = compiler.compile(
            "<bold><red>a</red> <click:run_command:'/x'>b</click></bold>");
        assertInstanceOf(TemplateNode.Static.class, tree.root());
        final List<TextComponent> literals = literalsOf(tree);
        assertEquals(3, literals.size());
        assertEquals("a", literals.getFirst().content());
        assertEquals(NamedTextColor.RED, literals.getFirst().style().color());
        assertEquals(TextDecoration.State.TRUE,
            literals.getFirst().style().decoration(TextDecoration.BOLD));
        assertEquals(" ", literals.get(1).content());
        assertEquals("b", literals.get(2).content());
        assertEquals(ClickEvent.runCommand("/x"), literals.get(2).style().clickEvent());
        assertEquals(TextDecoration.State.TRUE,
            literals.get(2).style().decoration(TextDecoration.BOLD));
    }

    @Test
    void adjacentLiteralsWithTheSameStyleAreMerged() {
        final PreComponentTree tree = compiler.compile("<red>a</red><red>b</red><red>c</red>");
        final List<TextComponent> literals = literalsOf(tree);
        assertEquals(1, literals.size());
        assertEquals("abc", literals.getFirst().content());
        assertEquals(NamedTextColor.RED, literals.getFirst().style().color());
    }

    @Test
    void adjacentLiteralsWithTheSameClickEventAreMerged() {
        final PreComponentTree tree = compiler.compile(
            "<click:run_command:'/x'>a</click><click:run_command:'/x'>b</click>");
        final List<TextComponent> literals = literalsOf(tree);
        assertEquals(1, literals.size());
        assertEquals("ab", literals.getFirst().content());
        assertEquals(ClickEvent.runCommand("/x"), literals.getFirst().style().clickEvent());
    }

    @Test
    void literalsWithDifferentStylesOrInBetweenTextAreNotMerged() {
        final PreComponentTree tree = compiler.compile("<red>a</red><blue>b</blue>");
        final List<TextComponent> literals = literalsOf(tree);
        assertEquals(2, literals.size());
        assertEquals(NamedTextColor.RED, literals.getFirst().style().color());
        assertEquals(NamedTextColor.BLUE, literals.get(1).style().color());

        final List<TextComponent> separated =
            literalsOf(identity(compiler.compile("<red>a</red>b<red>c</red>")));
        assertEquals(3, separated.size());
        assertEquals(NamedTextColor.RED, separated.getFirst().style().color());
        assertEquals(Style.empty(), separated.get(1).style());
        assertEquals(NamedTextColor.RED, separated.get(2).style().color());
    }

    @Test
    void negatedDecorationsStayOnTheirLiteral() {
        final List<TextComponent> literals = literalsOf(
            identity(compiler.compile("<bold>a<!bold>b</!bold>c</bold>")));
        assertEquals(3, literals.size());
        assertEquals(TextDecoration.State.TRUE,
            literals.getFirst().style().decoration(TextDecoration.BOLD));
        assertEquals(TextDecoration.State.FALSE,
            literals.get(1).style().decoration(TextDecoration.BOLD));
        assertEquals(TextDecoration.State.TRUE,
            literals.get(2).style().decoration(TextDecoration.BOLD));
    }

    @Test
    void resetClosesOpenTags() {
        final List<TextComponent> literals = literalsOf(identity(compiler.compile("<bold><red>a<reset>b")));
        assertEquals(2, literals.size());
        assertEquals(NamedTextColor.RED, literals.getFirst().style().color());
        assertEquals(TextDecoration.State.TRUE,
            literals.getFirst().style().decoration(TextDecoration.BOLD));
        assertEquals(Style.empty(), literals.get(1).style());
    }

    @Test
    void gradientsAndHoversAbsorbTheStyleOfTheirAncestors() {
        final var tree = compiler.compile(
            "<red><gradient:red:blue>abc</gradient><hover:show_text:'h'>x</hover></red>");
        assertInstanceOf(TemplateNode.Static.class, tree.root());
        final var chars = flatten(tree.render());
        assertEquals(NamedTextColor.RED, chars.getFirst().style().color());
        assertEquals(NamedTextColor.BLUE, chars.get(2).style().color());
        assertEquals(NamedTextColor.RED, chars.getLast().style().color());
        assertNotNull(chars.getLast().style().hoverEvent());
    }

    @Test
    void unknownDynamicComponentsKeepTheInheritedStyle() {
        final var registry = ResolverRegistry.withDefaults().register(new TagSpec.LeafRule(Set.of("dyn"),
            (final var args, final var context) -> new ValueSource.Computed<>((final var render) -> Component.text("Z"))));
        final var tree = new MiniMessageCompiler(registry).compile("<bold><red><dyn/></red></bold>");
        assertInstanceOf(TemplateNode.DynamicLeaf.class, tree.root());
        final var chars = flatten(tree.render(PayloadInputs.viewer(NOBODY)));
        assertEquals(1, chars.size());
        assertEquals(NamedTextColor.RED, chars.getFirst().style().color());
        assertEquals(TextDecoration.State.TRUE, chars.getFirst().style().decoration(TextDecoration.BOLD));
    }

    @Test
    void unparsedPlaceholdersKeepTheInheritedStyle() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> pKey = layout.key("p", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("p", (final var scope) -> Component.text(scope.payload(pKey)));
        final PreComponentTree tree = new MiniMessageCompiler(registry).compile("<bold><red><p/></red></bold>");
        final Component rendered = tree.render((final var b) -> b.viewer(NOBODY).payload(pKey, "X"));
        final List<StyledChar> chars = flatten(rendered);
        assertEquals(1, chars.size());
        assertEquals('X', chars.getFirst().value());
        assertEquals(NamedTextColor.RED, chars.getFirst().style().color());
        assertEquals(TextDecoration.State.TRUE,
            chars.getFirst().style().decoration(TextDecoration.BOLD));
    }

    @Test
    void unparsedFallbacksKeepTheInheritedStyle() {
        final PreComponentTree tree = compiler.compile("<bold><red><unknown></red></bold>");
        final List<StyledChar> chars = flatten(tree.render(PayloadInputs.viewer(NOBODY)));
        assertEquals("<unknown>".length(), chars.size());
        for (final StyledChar styled : chars) {
            assertEquals(NamedTextColor.RED, styled.style().color());
            assertEquals(TextDecoration.State.TRUE, styled.style().decoration(TextDecoration.BOLD));
        }
    }

    @Test
    void unknownColorsRemainLiteral() {
        assertEquals("<color:nope>a</color>",
            ComponentComparison.plainText(
                compiler.compile("<color:nope>a</color>").render(PayloadInputs.viewer(NOBODY))));
        assertEquals("<color>a</color>",
            ComponentComparison.plainText(compiler.compile("<color>a</color>").render(PayloadInputs.viewer(NOBODY))));
    }

    @Test
    void emptyAndMalformedInputsCompileAndRender() {
        assertDoesNotThrow(() -> compiler.compile("").render(PayloadInputs.viewer(NOBODY)));
        assertDoesNotThrow(() -> compiler.compile("<bold>").render(PayloadInputs.viewer(NOBODY)));
        assertDoesNotThrow(() -> compiler.compile("</bold>").render(PayloadInputs.viewer(NOBODY)));
        assertDoesNotThrow(() -> compiler.compile("<red>a").render(PayloadInputs.viewer(NOBODY)));
        assertDoesNotThrow(() -> compiler.compile("<color:nope>a</color>").render(PayloadInputs.viewer(NOBODY)));
        assertDoesNotThrow(() -> compiler.compile("<sprite/>").render(PayloadInputs.viewer(NOBODY)));
        assertDoesNotThrow(() -> compiler.compile("<hover:show_text:'x'/>").render(PayloadInputs.viewer(NOBODY)));
        assertDoesNotThrow(() -> compiler.compile("<click/>").render(PayloadInputs.viewer(NOBODY)));
    }

    private static @NotNull PreComponentTree identity(final @NotNull PreComponentTree tree) {
        return tree;
    }

    private static @NotNull List<TextComponent> literalsOf(final @NotNull PreComponentTree tree) {
        final List<TextComponent> literals = new ArrayList<>();
        collect(tree.render(), Style.empty(), literals);
        return literals;
    }

    private static void collect(final @NotNull Component component, final @NotNull Style inherited,
        final @NotNull List<TextComponent> literals) {
        final var style = inherited.merge(component.style());
        if (component instanceof final TextComponent text && !text.content().isEmpty()) {
            literals.add(Component.text(text.content(), style));
        }
        for (final var child : component.children()) collect(child, style, literals);
    }

    private static @NotNull List<StyledChar> flatten(final @NotNull Component component) {
        final List<StyledChar> chars = new ArrayList<>();
        flatten(component, Style.empty(), chars);
        return chars;
    }

    private static void flatten(final @NotNull Component component, final @NotNull Style inherited,
        final @NotNull List<StyledChar> chars) {
        final Style merged = inherited.merge(component.style());
        if (component instanceof TextComponent text) {
            for (int i = 0; i < text.content().length(); i++) {
                chars.add(new StyledChar(text.content().charAt(i), merged));
            }
        }
        for (final Component child : component.children()) flatten(child, merged, chars);
    }

    private record StyledChar(char value, @NotNull Style style) {
    }

}
