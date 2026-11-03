package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.EntityNBTComponent;
import net.kyori.adventure.text.SelectorComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlaceholderDynamismTest {

    private static final @NotNull Pointered NOBODY = new Pointered() {
        @Override
        public @NotNull Pointers pointers() {
            return Pointers.empty();
        }
    };


    private static @NotNull String plainText(final @Nullable Component component) {
        return component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void sameTreeRendersDifferentValuesForTheSameKey() {
        final var layout = PayloadSchema.builder();
        final var xKey = layout.key("x", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("x", (final var scope) -> scope.payload(xKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<bold><red><x></red></bold>");
        final Component first = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("A")));
        final Component second = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("B")));
        final Component repeated = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("A")));
        assertEquals("A", plainText(first));
        assertEquals("B", plainText(second));
        assertEquals("A", plainText(repeated));
        assertEquals(ComponentComparison.expression(first), ComponentComparison.expression(repeated));
        assertNotEquals(ComponentComparison.expression(first), ComponentComparison.expression(second));
    }

    @Test
    void firstRenderIsNotAffectedByLaterRenders() {
        final var layout = PayloadSchema.builder();
        final var xKey = layout.key("x", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("x", (final var scope) -> scope.payload(xKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<red><x></red>");
        final Component first = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("A")));
        tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("B")));
        assertEquals("A", plainText(first));
    }

    @Test
    void sameTreeRendersDifferentTagArguments() {
        final var layout = PayloadSchema.builder();
        final var cKey = layout.key("c", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("c", (final var scope) -> scope.payload(cKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<color:'<c>'>text</color>");
        final Component red = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(cKey, Component.text("red")));
        final Component blue = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(cKey, Component.text("blue")));
        final Component redAgain = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(cKey, Component.text("red")));
        assertEquals(NamedTextColor.RED, red.style().color());
        assertEquals(NamedTextColor.BLUE, blue.style().color());
        assertEquals(NamedTextColor.RED, redAgain.style().color());
        assertEquals("text", plainText(red));
        assertEquals("text", plainText(blue));
    }

    @Test
    void sameTreeRendersDifferentGradientArguments() {
        final var layout = PayloadSchema.builder();
        final var aKey = layout.key("a", Component.class);
        final var bKey = layout.key("b", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("a", (final var scope) -> scope.payload(aKey))
            .registerPlaceholder("b", (final var scope) -> scope.payload(bKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<gradient:'<a>':'<b>'>abc</gradient>");
        final Component first = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(aKey, Component.text("red"))
            .payload(bKey, Component.text("blue")));
        final Component second = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(aKey, Component.text("green"))
            .payload(bKey, Component.text("yellow")));
        final List<String> firstTokens = ComponentComparison.expression(first);
        final List<String> secondTokens = ComponentComparison.expression(second);
        assertTrue(firstTokens.getFirst().contains(NamedTextColor.RED.asHexString()));
        assertTrue(secondTokens.getFirst().contains(NamedTextColor.GREEN.asHexString()));
        assertNotEquals(firstTokens, secondTokens);
    }

    @Test
    void sameTreeRendersDifferentSelectorSeparators() {
        final var layout = PayloadSchema.builder();
        final var xKey = layout.key("x", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("x", (final var scope) -> scope.payload(xKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<selector:@a:'<x>'>");
        final SelectorComponent first = (SelectorComponent) tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("A")));
        final SelectorComponent second = (SelectorComponent) tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("B")));
        assertEquals("A", plainText(first.separator()));
        assertEquals("B", plainText(second.separator()));
    }

    @Test
    void sameTreeRendersDifferentNbtSeparators() {
        final var layout = PayloadSchema.builder();
        final var xKey = layout.key("x", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("x", (final var scope) -> scope.payload(xKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<nbt:entity:@e:Inventory:'<x>'>");
        final EntityNBTComponent first =
            (EntityNBTComponent) tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("A")));
        final EntityNBTComponent second =
            (EntityNBTComponent) tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("B")));
        assertEquals("A", plainText(first.separator()));
        assertEquals("B", plainText(second.separator()));
    }

    @Test
    void everyOccurrenceOfTheSameKeyUpdatesTogether() {
        final var layout = PayloadSchema.builder();
        final var xKey = layout.key("x", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("x", (final var scope) -> scope.payload(xKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<x>-<x>-<x>");
        assertEquals("A-A-A", plainText(tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("A")))));
        assertEquals("B-B-B", plainText(tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("B")))));
    }

    @Test
    void differentKeysResolveIndependently() {
        final var layout = PayloadSchema.builder();
        final var xKey = layout.key("x", Component.class);
        final var yKey = layout.key("y", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("x", (final var scope) -> scope.payload(xKey))
            .registerPlaceholder("y", (final var scope) -> scope.payload(yKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<x> <y>");
        final Component first = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("A"))
            .payload(yKey, Component.text("1")));
        final Component second = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("B"))
            .payload(yKey, Component.text("2")));
        assertEquals("A 1", plainText(first));
        assertEquals("B 2", plainText(second));
    }



    @Test
    void parsedPlaceholderValuesDoNotLeakTheirStyle() {
        final var layout = PayloadSchema.builder();
        final var xKey = layout.key("x", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("x", (final var scope) -> scope.payload(xKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<bold><x></bold>");
        final Component first = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("A", NamedTextColor.RED)));
        final Component second = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("B", NamedTextColor.BLUE)));
        final List<String> firstTokens = ComponentComparison.expression(first);
        final List<String> secondTokens = ComponentComparison.expression(second);
        assertTrue(firstTokens.getFirst().contains(NamedTextColor.RED.asHexString()));
        assertTrue(secondTokens.getFirst().contains(NamedTextColor.BLUE.asHexString()));
        assertTrue(firstTokens.getFirst().contains("bold=true"));
        assertTrue(secondTokens.getFirst().contains("bold=true"));
        assertEquals("A", plainText(first));
        assertEquals("B", plainText(second));
    }
}
