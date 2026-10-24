package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.compat.MiniMessageTags;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.pointer.Pointer;
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

final class MiniMessageTagsTest {

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
        final Component actual = compiler.compile(input).render(target, MiniMessageTags.of(resolvers));
        assertEquals(ComponentComparison.expression(expected), ComponentComparison.expression(actual),
            "expression differs for " + input);
    }

    private void assertDelegatedEquivalent(final @NotNull String input, final @NotNull TagResolver... resolvers) {
        assertDelegatedEquivalent(NOBODY, input, resolvers);
    }

    private void assertDelegatedEquivalent(final @NotNull Pointered target, final @NotNull String input,
        final @NotNull TagResolver... resolvers) {
        final Component expected = MINI_MESSAGE.deserialize(input, target, resolvers);
        final Component actual = compiler.compile(input)
            .render(target, MiniMessageTags.of(MINI_MESSAGE, resolvers));
        assertEquals(ComponentComparison.expression(expected), ComponentComparison.expression(actual),
            "delegated expression differs for " + input);
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
        final Component first = tree.render(NOBODY, MiniMessageTags.of(Placeholder.unparsed("p", "A")));
        final Component second = tree.render(NOBODY, MiniMessageTags.of(Placeholder.unparsed("p", "B")));
        final Component firstAgain = tree.render(NOBODY, MiniMessageTags.of(Placeholder.unparsed("p", "A")));
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
        assertEquivalent("<p:>", Placeholder.unparsed("p", "P"));
        assertEquivalent("<p:>", Placeholder.parsed("p", "<red>P</red>"));
        assertEquivalent("<p:>", Placeholder.styling("p", NamedTextColor.RED));
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
    void delegatedFixedPlaceholderMatchesMiniMessage() {
        assertDelegatedEquivalent("<p>", Placeholder.unparsed("p", "P"));
        assertDelegatedEquivalent("<p>kids</p>", Placeholder.unparsed("p", "P"));
    }

    @Test
    void delegatedArgumentResolverMatchesMiniMessage() {
        final TagResolver upper = TagResolver.resolver("upper",
            (args, context) -> Tag.inserting(Component.text(args.pop().value().toUpperCase())));
        assertDelegatedEquivalent("<upper:hi>kids</upper>", upper);
        assertDelegatedEquivalent("<upper:hi/>", upper);
        assertDelegatedEquivalent("<upper:hi>", upper);
    }

    @Test
    void delegatedUsesMiniMessageInstanceAndResolver() {
        final TagResolver upper = TagResolver.resolver("upper",
            (args, context) -> Tag.inserting(Component.text(args.pop().value().toUpperCase())));
        final MiniMessage custom = MiniMessage.builder()
            .tags(TagResolver.builder().resolver(upper).build())
            .build();
        final Component expected = custom.deserialize("<upper:hi>kids</upper>", upper);
        final Component actual = compiler.compile("<upper:hi>kids</upper>")
            .render(NOBODY, MiniMessageTags.of(custom, upper));
        assertEquals(ComponentComparison.expression(expected), ComponentComparison.expression(actual));
    }

    @Test
    void delegatedArgumentResolverCanProduceAllTagKinds() {
        assertDelegatedEquivalent("<sty>x</sty>",
            TagResolver.resolver("sty", (args, context) -> Tag.styling(NamedTextColor.RED)));
        assertDelegatedEquivalent("<ins>x</ins>",
            TagResolver.resolver("ins", (args, context) -> Tag.inserting(Component.text("I"))));
        assertDelegatedEquivalent("<sci/>",
            TagResolver.resolver("sci", (args, context) -> Tag.selfClosingInserting(Component.text("S"))));
        assertDelegatedEquivalent("<pre>",
            TagResolver.resolver("pre", (args, context) -> Tag.preProcessParsed("<red>P</red>")));
    }

    @Test
    void delegatedModifyingMatchesMiniMessage() {
        final Modifying modifying = (current, depth) ->
            current.color(depth == 0 ? NamedTextColor.RED : NamedTextColor.BLUE);
        assertDelegatedEquivalent("<m>kids</m>", TagResolver.resolver("m", modifying));
    }

    @Test
    void delegatedResolverSeesTarget() {
        final Pointer<String> name = Pointer.pointer(String.class, Key.key("fastminimessage", "name"));
        final Pointered target = new Pointered() {
            @Override
            public @NotNull Pointers pointers() {
                return Pointers.builder().withStatic(name, "Hector").build();
            }
        };
        final TagResolver who = TagResolver.resolver("who",
            (args, context) -> Tag.inserting(Component.text(context.targetOrThrow().getOrDefault(name, "?"))));
        assertDelegatedEquivalent(target, "<who>", who);
        assertDelegatedEquivalent(target, "<who:x>", who);
    }

    @Test
    void parserDirectivesKeepLiteralFallback() {
        final TagResolver reset = TagResolver.resolver("myreset", ParserDirective.RESET);
        final Component component = compiler.compile("<myreset>text")
            .render(NOBODY, MiniMessageTags.of(MINI_MESSAGE, reset));
        assertEquals("<myreset>text", plainText(component));
    }

    @Test
    void modifyingTagsKeepLiteralFallback() {
        final Modifying modifying = (current, depth) -> current.color(NamedTextColor.RED);
        final Component component = compiler.compile("<m>x</m>")
            .render(NOBODY, MiniMessageTags.of(TagResolver.resolver("m", modifying)));
        assertEquals("<m>x</m>", plainText(component));
    }

    @Test
    void nativeTagsTakePriorityOverBridgedResolvers() {
        final TagResolver override = TagResolver.resolver("red",
            Tag.inserting(Component.text("OVERRIDE", NamedTextColor.BLUE)));
        final PreComponentTree tree = compiler.compile("<red>text</red>");
        final Component component = tree.render(NOBODY, MiniMessageTags.of(override));
        assertEquals("text", plainText(component));
        for (final String token : ComponentComparison.expression(component)) {
            assertTrue(token.contains(NamedTextColor.RED.asHexString()), token);
        }
    }

    @Test
    void placeholderWithoutResolverKeepsLiteralFallback() {
        final PreComponentTree tree = compiler.compile("<p>");
        assertEquals("<p>", plainText(tree.render(NOBODY)));
    }

    @Test
    void unsupportedArgumentResolverKeepsLiteralFallback() {
        final TagResolver resolver = TagResolver.resolver("upper",
            (args, context) -> Tag.inserting(Component.text(args.pop().value().toUpperCase())));
        assertEquals("<upper:hi>", plainText(compiler.compile("<upper:hi>").render(NOBODY, MiniMessageTags.of(resolver))));
    }

    @Test
    void parsedPlaceholderCanUseOwnPlaceholders() {
        final PreComponentTree tree = compiler.compile("<p>");
        final Component component = tree.render(NOBODY,
            io.github.neoramm.fastminimessage.model.Placeholder.ofUnparsed("name", "Hector"),
            MiniMessageTags.of(Placeholder.parsed("p", "<red><name></red>")));
        assertEquals("Hector", plainText(component));
        final List<String> expression = ComponentComparison.expression(component);
        for (final String token : expression) assertTrue(token.contains(NamedTextColor.RED.asHexString()), token);
    }

    private static @NotNull String plainText(final @NotNull Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
