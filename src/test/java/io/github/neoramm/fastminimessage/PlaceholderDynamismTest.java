package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.model.Placeholder;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
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

    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());

    private static @NotNull String plainText(final @Nullable Component component) {
        return component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void sameTreeRendersDifferentValuesForTheSameKey() {
        final PreComponentTree tree = compiler.compile("<bold><red><x></red></bold>");
        final Component first = tree.render(NOBODY, Placeholder.ofUnparsed("x", "A"));
        final Component second = tree.render(NOBODY, Placeholder.ofUnparsed("x", "B"));
        final Component repeated = tree.render(NOBODY, Placeholder.ofUnparsed("x", "A"));
        assertEquals("A", plainText(first));
        assertEquals("B", plainText(second));
        assertEquals("A", plainText(repeated));
        assertEquals(ComponentComparison.expression(first), ComponentComparison.expression(repeated));
        assertNotEquals(ComponentComparison.expression(first), ComponentComparison.expression(second));
    }

    @Test
    void firstRenderIsNotAffectedByLaterRenders() {
        final PreComponentTree tree = compiler.compile("<red><x></red>");
        final Component first = tree.render(NOBODY, Placeholder.ofUnparsed("x", "A"));
        tree.render(NOBODY, Placeholder.ofUnparsed("x", "B"));
        assertEquals("A", plainText(first));
    }

    @Test
    void sameTreeRendersDifferentTagArguments() {
        final PreComponentTree tree = compiler.compile("<color:'<c>'>text</color>");
        final Component red = tree.render(NOBODY, Placeholder.ofUnparsed("c", "red"));
        final Component blue = tree.render(NOBODY, Placeholder.ofUnparsed("c", "blue"));
        final Component redAgain = tree.render(NOBODY, Placeholder.ofUnparsed("c", "red"));
        assertEquals(NamedTextColor.RED, red.style().color());
        assertEquals(NamedTextColor.BLUE, blue.style().color());
        assertEquals(NamedTextColor.RED, redAgain.style().color());
        assertEquals("text", plainText(red));
        assertEquals("text", plainText(blue));
    }

    @Test
    void sameTreeRendersDifferentGradientArguments() {
        final PreComponentTree tree = compiler.compile("<gradient:'<a>':'<b>'>abc</gradient>");
        final Component first = tree.render(NOBODY,
            Placeholder.ofUnparsed("a", "red"), Placeholder.ofUnparsed("b", "blue"));
        final Component second = tree.render(NOBODY,
            Placeholder.ofUnparsed("a", "green"), Placeholder.ofUnparsed("b", "yellow"));
        final List<String> firstTokens = ComponentComparison.expression(first);
        final List<String> secondTokens = ComponentComparison.expression(second);
        assertTrue(firstTokens.getFirst().contains(NamedTextColor.RED.asHexString()));
        assertTrue(secondTokens.getFirst().contains(NamedTextColor.GREEN.asHexString()));
        assertNotEquals(firstTokens, secondTokens);
    }

    @Test
    void sameTreeRendersDifferentSelectorSeparators() {
        final PreComponentTree tree = compiler.compile("<selector:@a:'<x>'>");
        final SelectorComponent first = (SelectorComponent) tree.render(NOBODY, Placeholder.ofUnparsed("x", "A"));
        final SelectorComponent second = (SelectorComponent) tree.render(NOBODY, Placeholder.ofUnparsed("x", "B"));
        assertEquals("A", plainText(first.separator()));
        assertEquals("B", plainText(second.separator()));
    }

    @Test
    void sameTreeRendersDifferentNbtSeparators() {
        final PreComponentTree tree = compiler.compile("<nbt:entity:@e:Inventory:'<x>'>");
        final EntityNBTComponent first =
            (EntityNBTComponent) tree.render(NOBODY, Placeholder.ofUnparsed("x", "A"));
        final EntityNBTComponent second =
            (EntityNBTComponent) tree.render(NOBODY, Placeholder.ofUnparsed("x", "B"));
        assertEquals("A", plainText(first.separator()));
        assertEquals("B", plainText(second.separator()));
    }

    @Test
    void everyOccurrenceOfTheSameKeyUpdatesTogether() {
        final PreComponentTree tree = compiler.compile("<x>-<x>-<x>");
        assertEquals("A-A-A", plainText(tree.render(NOBODY, Placeholder.ofUnparsed("x", "A"))));
        assertEquals("B-B-B", plainText(tree.render(NOBODY, Placeholder.ofUnparsed("x", "B"))));
    }

    @Test
    void differentKeysResolveIndependently() {
        final PreComponentTree tree = compiler.compile("<x> <y>");
        final Component first = tree.render(NOBODY,
            Placeholder.ofUnparsed("x", "A"), Placeholder.ofUnparsed("y", "1"));
        final Component second = tree.render(NOBODY,
            Placeholder.ofUnparsed("x", "B"), Placeholder.ofUnparsed("y", "2"));
        assertEquals("A 1", plainText(first));
        assertEquals("B 2", plainText(second));
    }

    @Test
    void fallbackBetweenDynamicRendersDoesNotStick() {
        final PreComponentTree tree = compiler.compile("<red><x></red>");
        assertEquals("A", plainText(tree.render(NOBODY, Placeholder.ofUnparsed("x", "A"))));
        assertEquals("<x>", plainText(tree.render(NOBODY)));
        assertEquals("B", plainText(tree.render(NOBODY, Placeholder.ofUnparsed("x", "B"))));
    }

    @Test
    void parsedPlaceholderValuesDoNotLeakTheirStyle() {
        final PreComponentTree tree = compiler.compile("<bold><x></bold>");
        final Component first = tree.render(NOBODY,
            Placeholder.ofParsed("x", Component.text("A", NamedTextColor.RED)));
        final Component second = tree.render(NOBODY,
            Placeholder.ofParsed("x", Component.text("B", NamedTextColor.BLUE)));
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
