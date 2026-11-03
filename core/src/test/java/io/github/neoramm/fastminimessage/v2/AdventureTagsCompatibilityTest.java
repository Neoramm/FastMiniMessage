package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.compat.AdventureTags;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Modifying;
import net.kyori.adventure.text.minimessage.tag.ParserDirective;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdventureTagsCompatibilityTest {

    private static final @NotNull Pointered NOBODY = new Pointered() {
        @Override
        public @NotNull Pointers pointers() {
            return Pointers.empty();
        }
    };
    private static final @NotNull MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());

    private void assertEquivalent(final @NotNull String input, final @NotNull TagResolver... resolvers) {
        assertEquivalent(NOBODY, input, resolvers);
    }

    private void assertEquivalent(final @NotNull Pointered target, final @NotNull String input,
        final @NotNull TagResolver... resolvers) {
        final Component expected = MINI_MESSAGE.deserialize(input, target, resolvers);
        final Component actual = compiler.compile(input).render(PayloadInputs.runtime(target,
            AdventureTags.of(resolvers)));
        assertEquals(ComponentComparison.expression(expected), ComponentComparison.expression(actual),
            "expression differs for " + input);
    }

    @Test
    void unparsedPlaceholderMatchesMiniMessage() {
        assertEquivalent("<p>", Placeholder.unparsed("p", "Hector"));
        assertEquivalent("<P>", Placeholder.unparsed("p", "Hector"));
    }

    @Test
    void componentPlaceholderMatchesMiniMessage() {
        assertEquivalent("<p>", Placeholder.component("p", Component.text("Hector", NamedTextColor.BLUE)));
    }

    @Test
    void parsedPlaceholderMatchesMiniMessage() {
        assertEquivalent("<p>", Placeholder.parsed("p", "<red><bold>Hector</bold></red>"));
    }

    @Test
    void stylingPlaceholderMatchesMiniMessage() {
        assertEquivalent("<p>hola</p>", Placeholder.styling("p", NamedTextColor.RED));
    }

    @Test
    void customInsertingResolverMatchesMiniMessage() {
        final TagResolver resolver = TagResolver.resolver("greet",
            Tag.inserting(Component.text("Hi ", NamedTextColor.GREEN)));
        assertEquivalent("<greet>world</greet>", resolver);
    }

    @Test
    void customStylingResolverMatchesMiniMessage() {
        assertEquivalent("<focus>text</focus>", TagResolver.resolver("focus", Tag.styling(NamedTextColor.AQUA)));
    }

    @Test
    void selfClosingPlaceholderMatchesMiniMessage() {
        assertEquivalent("<p/>", Placeholder.unparsed("p", "Hector"));
    }

    @Test
    void placeholderInheritsSurroundingStyle() {
        assertEquivalent("<bold><p></bold>", Placeholder.unparsed("p", "Hector"));
        assertEquivalent("<red><p>text</p></red>", Placeholder.styling("p", NamedTextColor.BLUE));
    }

    @Test
    void sameTreeRendersDifferentPlaceholderValues() {
        final PreComponentTree tree = compiler.compile("<p>");
        final Component first = tree.render(PayloadInputs.runtime(NOBODY, AdventureTags.of(Placeholder.unparsed("p",
            "A"))));
        final Component second = tree.render(PayloadInputs.runtime(NOBODY,
            AdventureTags.of(Placeholder.unparsed("p", "B"))));
        final Component firstAgain = tree.render(PayloadInputs.runtime(NOBODY,
            AdventureTags.of(Placeholder.unparsed("p", "A"))));
        assertEquals("A", plainText(first));
        assertEquals("B", plainText(second));
        assertEquals(ComponentComparison.expression(first), ComponentComparison.expression(firstAgain));
        assertNotEquals(ComponentComparison.expression(first), ComponentComparison.expression(second));
    }

    @Test
    void composedPlaceholderResolversMatchMiniMessage() {
        final TagResolver composed = TagResolver.resolver(
            Placeholder.unparsed("x", "A"),
            Placeholder.unparsed("y", "B")
        );
        assertEquivalent("<x> <y>", composed);
        assertEquivalent("<X> <y>", composed);
        assertEquivalent("<x> <y>", TagResolver.resolver(List.of(
            Placeholder.unparsed("x", "A"),
            Placeholder.unparsed("y", "B")
        )));
    }

    @Test
    void builderMapsMatchMiniMessage() {
        assertEquivalent("<x> <y>", TagResolver.builder()
            .tag("x", Tag.inserting(Component.text("A")))
            .tag("y", Tag.inserting(Component.text("B")))
            .build());
    }

    @Test
    void argumentsOnFixedTagsMatchMiniMessage() {
        assertEquivalent("<p:x>", Placeholder.unparsed("p", "P"));
        assertEquivalent("<p: >", Placeholder.unparsed("p", "P"));
        assertEquivalent("<p:x/>", Placeholder.unparsed("p", "P"));
        assertEquivalent("<p:x>", Placeholder.parsed("p", "<red>P</red>"));
        assertEquivalent("<p:x>", Placeholder.styling("p", NamedTextColor.RED));
    }

    @Test
    void composedResolversCanMixPlaceholderKinds() {
        final TagResolver composed = TagResolver.resolver(
            Placeholder.unparsed("x", "A"),
            Placeholder.parsed("y", "<red>B</red>")
        );
        assertEquivalent("<x> <y>", composed);
    }

    @Test
    void selfClosingInsertingKeepsLiteralClosingTag() {
        assertEquivalent("<p>kids</p>", Placeholder.unparsed("p", "P"));
        assertEquivalent("<bold><p>kids</p></bold>", Placeholder.unparsed("p", "P"));
        assertEquivalent("<p></p>", Placeholder.unparsed("p", "P"));
        assertEquivalent("<p>kids</p>", Placeholder.component("p", Component.text("P", NamedTextColor.BLUE)));
        assertEquivalent("<p>kids</p>", TagResolver.resolver("p", Tag.selfClosingInserting(Component.text("P"))));
    }

    @Test
    void parsedPlaceholderWithChildrenKeepsLiteralClosingTag() {
        assertEquivalent("<p/>", Placeholder.parsed("p", "<red>P</red>"));
        assertEquivalent("<p>kids</p>", Placeholder.parsed("p", "<red>P</red>"));
        assertEquivalent("<bold><p>kids</p></bold>", Placeholder.parsed("p", "<red>P</red>"));
        assertEquivalent("<p></p>", Placeholder.parsed("p", "<red>P</red>"));
    }

    @Test
    void insertingValueWithExistingChildrenMatchesMiniMessage() {
        final Component value = Component.text("P", NamedTextColor.BLUE)
            .append(Component.text("C", NamedTextColor.RED));
        assertEquivalent("<x>kids</x>", TagResolver.resolver("x", Tag.inserting(value)));
        assertEquivalent("<x>kids</x>", TagResolver.resolver("x", Tag.selfClosingInserting(value)));
    }

    @Test
    void nestedSelfClosingInsertingMatchesMiniMessage() {
        final TagResolver resolver = TagResolver.resolver(
            Placeholder.unparsed("p", "P"),
            Placeholder.unparsed("q", "Q")
        );
        assertEquivalent("<p><q>x</q></p>", resolver);
    }

    @Test
    void cachingResolverMatchesMiniMessage() {
        final TagResolver caching = TagResolver.caching(
            (TagResolver.WithoutArguments) Placeholder.unparsed("p", "P"));
        assertEquivalent("<p>", caching);
    }

    @Test
    void parsedTextAndNestedTagsMatchMiniMessage() {
        assertEquivalent("pre <p> post", Placeholder.parsed("p", "MIDDLE"));
        assertEquivalent("<p>", Placeholder.parsed("p", "<red>A</red><blue>B</blue>"));
    }

    @Test
    void emptyResolverDoesNothing() {
        assertEquivalent("<p>", TagResolver.empty());
    }

    @Test
    void parserDirectivesKeepLiteralFallback() {
        final TagResolver reset = TagResolver.resolver("myreset", ParserDirective.RESET);
        final Component component = compiler.compile("<myreset>text")
            .render(PayloadInputs.runtime(NOBODY, AdventureTags.of(reset)));
        assertEquals("<myreset>text", plainText(component));
    }

    @Test
    void modifyingTagsAreApplied() {
        final Modifying modifying = (current, depth) -> current.color(NamedTextColor.RED);
        final Component component = compiler.compile("<m>x</m>")
            .render(PayloadInputs.runtime(NOBODY, AdventureTags.of(TagResolver.resolver("m", modifying))));
        assertEquals("x", plainText(component));
        assertEquals(NamedTextColor.RED, component.color());
    }

    @Test
    void nativeTagsTakePriorityOverBridgedResolvers() {
        final TagResolver override = TagResolver.resolver("red",
            Tag.inserting(Component.text("OVERRIDE", NamedTextColor.BLUE)));
        final PreComponentTree tree = compiler.compile("<red>text</red>");
        final Component component = tree.render(PayloadInputs.runtime(NOBODY, AdventureTags.of(override)));
        assertEquals("text", plainText(component));
        for (final String token : ComponentComparison.expression(component)) {
            assertTrue(token.contains(NamedTextColor.RED.asHexString()), token);
        }
    }

    @Test
    void placeholderWithoutResolverKeepsLiteralFallback() {
        final PreComponentTree tree = compiler.compile("<p>");
        assertEquals("<p>", plainText(tree.render(PayloadInputs.viewer(NOBODY))));
    }

    @Test
    void argumentResolversAreResolved() {
        final TagResolver resolver = TagResolver.resolver("upper",
            (args, context) -> Tag.inserting(Component.text(args.pop().value().toUpperCase())));
        assertEquivalent("<upper:hi>", resolver);
    }

    @Test
    void parsedPlaceholderCanUseOwnPlaceholders() {
        final PreComponentTree tree = compiler.compile("<p>");
        final Component component = tree.render(PayloadInputs.runtime(NOBODY,
            io.github.neoramm.fastminimessage.v2.model.Placeholder.ofUnparsed("name", "Hector"),
            AdventureTags.of(Placeholder.parsed("p", "<red><name></red>"))));
        assertEquals("Hector", plainText(component));
        final List<String> expression = ComponentComparison.expression(component);
        for (final String token : expression) assertTrue(token.contains(NamedTextColor.RED.asHexString()), token);
    }

    private static @NotNull String plainText(final @NotNull Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
