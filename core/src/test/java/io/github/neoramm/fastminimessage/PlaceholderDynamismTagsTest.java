package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.example.ComponentModifiers;
import io.github.neoramm.fastminimessage.model.Placeholder;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.EntityNBTComponent;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.ScoreComponent;
import net.kyori.adventure.text.SelectorComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import net.kyori.adventure.text.object.SpriteObjectContents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that one compiled tree honours new placeholder values across tag contexts,
 * including argument, body and separator positions.
 */
final class PlaceholderDynamismTagsTest {

    private static final @NotNull Pointered NOBODY = new Pointered() {
        @Override
        public @NotNull Pointers pointers() {
            return Pointers.empty();
        }
    };

    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());

    private static @NotNull String plainText(final @Nullable Component component) {
        return component == null ? "" : ComponentComparison.plainText(component);
    }

    private static @NotNull ClickEvent clickEvent(final @NotNull Component component) {
        final @Nullable ClickEvent event = component.clickEvent();
        assertNotNull(event, "click event");
        return event;
    }

    private static @NotNull String clickValue(final @NotNull ClickEvent event) {
        final ClickEvent.Payload payload = event.payload();
        return payload instanceof final ClickEvent.Payload.Text text ? text.value() : String.valueOf(payload);
    }

    private static @NotNull Component hoverValue(final @NotNull Component component) {
        final @Nullable HoverEvent<?> event = component.hoverEvent();
        assertNotNull(event, "hover event");
        return (Component) event.value();
    }

    @Test
    void gradientAndRainbowBodyPlaceholdersRecompute() {
        final String[] opens = {"<gradient:red:blue>", "<rainbow>", "<pride>"};
        final String[] closes = {"</gradient>", "</rainbow>", "</pride>"};
        for (int i = 0; i < opens.length; i++) {
            final PreComponentTree tree = compiler.compile(opens[i] + "<x>" + closes[i]);
            final Component a = tree.render(NOBODY, Placeholder.ofUnparsed("x", "A"));
            final Component b = tree.render(NOBODY, Placeholder.ofUnparsed("x", "B"));
            assertEquals("A", plainText(a), opens[i]);
            assertEquals("B", plainText(b), opens[i]);
        }
    }

    @Test
    void hoverBodyAndArgumentPlaceholdersUpdate() {
        final PreComponentTree body = compiler.compile("<hover:show_text:'hint'><x></hover>");
        assertEquals("A", plainText(body.render(NOBODY, Placeholder.ofUnparsed("x", "A"))));
        assertEquals("B", plainText(body.render(NOBODY, Placeholder.ofUnparsed("x", "B"))));
        assertEquals("hint", plainText(hoverValue(body.render(NOBODY))));

        final PreComponentTree argument = compiler.compile("<hover:show_text:'<h>'>text</hover>");
        assertEquals("one", plainText(hoverValue(
            argument.render(NOBODY, Placeholder.ofUnparsed("h", "one")))));
        assertEquals("two", plainText(hoverValue(
            argument.render(NOBODY, Placeholder.ofUnparsed("h", "two")))));
    }

    @Test
    void clickBodyAndArgumentPlaceholdersUpdate() {
        final PreComponentTree body = compiler.compile("<click:run_command:'/spawn'><x></click>");
        assertEquals("A", plainText(body.render(NOBODY, Placeholder.ofUnparsed("x", "A"))));
        assertEquals("/spawn", clickValue(clickEvent(body.render(NOBODY))));

        final PreComponentTree argument = compiler.compile("<click:run_command:'/<cmd>'>text</click>");
        assertEquals("/home",
            clickValue(clickEvent(argument.render(NOBODY, Placeholder.ofUnparsed("cmd", "home")))));
        assertEquals("/back",
            clickValue(clickEvent(argument.render(NOBODY, Placeholder.ofUnparsed("cmd", "back")))));
    }

    @Test
    void insertBodyAndArgumentPlaceholdersUpdate() {
        final PreComponentTree body = compiler.compile("<insert:'shift'><x></insert>");
        assertEquals("shift", body.render(NOBODY, Placeholder.ofUnparsed("x", "A")).insertion());

        final PreComponentTree argument = compiler.compile("<insert:'<i>'>text</insert>");
        assertEquals("a", argument.render(NOBODY, Placeholder.ofUnparsed("i", "a")).insertion());
        assertEquals("b", argument.render(NOBODY, Placeholder.ofUnparsed("i", "b")).insertion());
    }

    @Test
    void shadowBodyAndArgumentPlaceholdersUpdate() {
        final PreComponentTree body = compiler.compile("<shadow:#000000aa><x></shadow>");
        final String bodyA = plainText(body.render(NOBODY, Placeholder.ofUnparsed("x", "A")));
        final String bodyB = plainText(body.render(NOBODY, Placeholder.ofUnparsed("x", "B")));
        assertEquals("A", bodyA);
        assertEquals("B", bodyB);

        final PreComponentTree argument = compiler.compile("<shadow:'<c>':0.4>text</shadow>");
        final Component red = argument.render(NOBODY, Placeholder.ofUnparsed("c", "red"));
        final Component blue = argument.render(NOBODY, Placeholder.ofUnparsed("c", "blue"));
        assertNotNull(red.style().shadowColor());
        assertNotNull(blue.style().shadowColor());
        assertNotEquals(red.style().shadowColor(), blue.style().shadowColor());
    }

    @Test
    void fontArgumentPlaceholderUpdatesTheFont() {
        final PreComponentTree tree = compiler.compile("<font:'<f>'>text</font>");
        assertEquals(Key.key("minecraft:default"),
            tree.render(NOBODY, Placeholder.ofUnparsed("f", "minecraft:default")).style().font());
        assertEquals(Key.key("minecraft:uniform"),
            tree.render(NOBODY, Placeholder.ofUnparsed("f", "minecraft:uniform")).style().font());
    }

    @Test
    void keybindArgumentPlaceholderUpdatesTheKey() {
        final PreComponentTree tree = compiler.compile("<key:'<k>'/>");
        final KeybindComponent jump = (KeybindComponent) tree.render(NOBODY, Placeholder.ofUnparsed("k", "key.jump"));
        final KeybindComponent sneak = (KeybindComponent) tree.render(NOBODY, Placeholder.ofUnparsed("k", "key.sneak"));
        assertEquals("key.jump", jump.keybind());
        assertEquals("key.sneak", sneak.keybind());
    }

    @Test
    void scoreArgumentPlaceholderUpdatesTheScore() {
        final PreComponentTree tree = compiler.compile("<score:'<m>':'<o>'/>");
        final ScoreComponent first = (ScoreComponent) tree.render(NOBODY,
            Placeholder.ofUnparsed("m", "Steve"), Placeholder.ofUnparsed("o", "kills"));
        final ScoreComponent second = (ScoreComponent) tree.render(NOBODY,
            Placeholder.ofUnparsed("m", "Alex"), Placeholder.ofUnparsed("o", "deaths"));
        assertEquals("Steve", first.name());
        assertEquals("kills", first.objective());
        assertEquals("Alex", second.name());
        assertEquals("deaths", second.objective());
    }

    @Test
    void spriteArgumentPlaceholderUpdatesTheSprite() {
        final PreComponentTree tree = compiler.compile("<sprite:'<a>':'<b>'/>");
        final SpriteObjectContents first = (SpriteObjectContents) ((ObjectComponent) tree.render(NOBODY,
            Placeholder.ofUnparsed("a", "items"), Placeholder.ofUnparsed("b", "item/diamond"))).contents();
        final SpriteObjectContents second = (SpriteObjectContents) ((ObjectComponent) tree.render(NOBODY,
            Placeholder.ofUnparsed("a", "blocks"), Placeholder.ofUnparsed("b", "block/stone"))).contents();
        assertEquals(Key.key("minecraft:items"), first.atlas());
        assertEquals(Key.key("minecraft:item/diamond"), first.sprite());
        assertEquals(Key.key("minecraft:blocks"), second.atlas());
        assertEquals(Key.key("minecraft:block/stone"), second.sprite());
    }

    @Test
    void headArgumentPlaceholderUpdatesTheName() {
        final PreComponentTree tree = compiler.compile("<head:'<n>'/>");
        final PlayerHeadObjectContents first = (PlayerHeadObjectContents) ((ObjectComponent) tree.render(NOBODY,
            Placeholder.ofUnparsed("n", "Steve"))).contents();
        final PlayerHeadObjectContents second = (PlayerHeadObjectContents) ((ObjectComponent) tree.render(NOBODY,
            Placeholder.ofUnparsed("n", "Alex"))).contents();
        assertEquals("Steve", first.name());
        assertEquals("Alex", second.name());
    }

    @Test
    void selectorKeyArgumentPlaceholderUpdatesThePattern() {
        final PreComponentTree tree = compiler.compile("<selector:'<k>'/>");
        final SelectorComponent all = (SelectorComponent) tree.render(NOBODY, Placeholder.ofUnparsed("k", "@a"));
        final SelectorComponent entities = (SelectorComponent) tree.render(NOBODY, Placeholder.ofUnparsed("k", "@e"));
        assertEquals("@a", all.pattern());
        assertEquals("@e", entities.pattern());
    }

    @Test
    void nbtSelectorArgumentPlaceholderUpdatesTheSelector() {
        final PreComponentTree tree = compiler.compile("<nbt:entity:'<s>':Inventory/>");
        final EntityNBTComponent all =
            (EntityNBTComponent) tree.render(NOBODY, Placeholder.ofUnparsed("s", "@a"));
        final EntityNBTComponent entities =
            (EntityNBTComponent) tree.render(NOBODY, Placeholder.ofUnparsed("s", "@e"));
        assertEquals("@a", all.selector());
        assertEquals("@e", entities.selector());
    }

    @Test
    void langArgumentPlaceholderUpdatesTheArguments() {
        final PreComponentTree tree = compiler.compile("<lang:chat.type.text:'<a>':'<b>'/>");
        final TranslatableComponent first = (TranslatableComponent) tree.render(NOBODY,
            Placeholder.ofUnparsed("a", "Steve"), Placeholder.ofUnparsed("b", "hello"));
        final TranslatableComponent second = (TranslatableComponent) tree.render(NOBODY,
            Placeholder.ofUnparsed("a", "Alex"), Placeholder.ofUnparsed("b", "bye"));
        final List<TranslationArgument> firstArgs = first.arguments();
        final List<TranslationArgument> secondArgs = second.arguments();
        assertEquals("Steve", plainText(firstArgs.getFirst().asComponent()));
        assertEquals("hello", plainText(firstArgs.get(1).asComponent()));
        assertEquals("Alex", plainText(secondArgs.getFirst().asComponent()));
        assertEquals("bye", plainText(secondArgs.get(1).asComponent()));
    }

    @Test
    void transitionArgumentPlaceholderUpdatesTheColour() {
        final PreComponentTree tree = compiler.compile("<transition:'<a>':'<b>'/>");
        final Component red = tree.render(NOBODY,
            Placeholder.ofUnparsed("a", "red"), Placeholder.ofUnparsed("b", "blue"));
        final Component green = tree.render(NOBODY,
            Placeholder.ofUnparsed("a", "green"), Placeholder.ofUnparsed("b", "yellow"));
        assertEquals(NamedTextColor.RED, red.style().color());
        assertEquals(NamedTextColor.GREEN, green.style().color());
    }

    @Test
    void modifierBodyPlaceholderKeepsWorking() {
        compiler.registerModifier("shout", ComponentModifiers.uppercase());
        final PreComponentTree tree = compiler.compile("<shout><x></shout>");
        assertEquals("HELLO", plainText(tree.render(NOBODY, Placeholder.ofUnparsed("x", "hello"))));
        assertEquals("WORLD", plainText(tree.render(NOBODY, Placeholder.ofUnparsed("x", "world"))));
    }

    @Test
    void unknownTagBodyPlaceholderKeepsWorking() {
        final PreComponentTree tree = compiler.compile("<unknown><x></unknown>");
        final String first = plainText(tree.render(NOBODY, Placeholder.ofUnparsed("x", "A")));
        final String second = plainText(tree.render(NOBODY, Placeholder.ofUnparsed("x", "B")));
        assertTrue(first.contains("A"));
        assertTrue(second.contains("B"));
        assertNotEquals(first, second);
    }

    @Test
    void combinedMessageUpdatesEveryDynamicTag() {
        final PreComponentTree tree = compiler.compile(
            "<gradient:red:blue><g></gradient> <hover:show_text:'<h>'>"
                + "<click:run_command:'/<c>'>click</click></hover>");
        final Component first = tree.render(NOBODY,
            Placeholder.ofUnparsed("g", "G"), Placeholder.ofUnparsed("h", "H1"),
            Placeholder.ofUnparsed("c", "one"));
        final Component second = tree.render(NOBODY,
            Placeholder.ofUnparsed("g", "GG"), Placeholder.ofUnparsed("h", "H2"),
            Placeholder.ofUnparsed("c", "two"));
        assertEquals("G click", plainText(first));
        assertEquals("GG click", plainText(second));
        assertNotEquals(ComponentComparison.expression(first), ComponentComparison.expression(second));
    }

    @Test
    void hoverSelfClosingPlaceholderUsesTheCompiledTree() {
        final PreComponentTree tree = compiler.compile("<hover:show_text:'<x/>'>text</hover>");
        final Component red = tree.render(NOBODY,
            Placeholder.ofParsed("x", Component.text("A", NamedTextColor.RED)));
        final Component blue = tree.render(NOBODY,
            Placeholder.ofParsed("x", Component.text("B", NamedTextColor.BLUE)));
        final Component redHover = hoverValue(red);
        final Component blueHover = hoverValue(blue);
        assertEquals("A", plainText(redHover));
        assertEquals("B", plainText(blueHover));
        assertEquals(NamedTextColor.RED, redHover.style().color());
        assertEquals(NamedTextColor.BLUE, blueHover.style().color());
    }

    @Test
    void langSelfClosingPlaceholderKeepsParsedStyle() {
        final PreComponentTree tree = compiler.compile("<lang:chat.type.text:'<x/>'/>");
        final TranslatableComponent red = (TranslatableComponent) tree.render(NOBODY,
            Placeholder.ofParsed("x", Component.text("A", NamedTextColor.RED)));
        final TranslatableComponent blue = (TranslatableComponent) tree.render(NOBODY,
            Placeholder.ofParsed("x", Component.text("B", NamedTextColor.BLUE)));
        final Component redArg = red.arguments().getFirst().asComponent();
        final Component blueArg = blue.arguments().getFirst().asComponent();
        assertEquals("A", plainText(redArg));
        assertEquals("B", plainText(blueArg));
        assertEquals(NamedTextColor.RED, redArg.style().color());
        assertEquals(NamedTextColor.BLUE, blueArg.style().color());
    }

    @Test
    void gradientWithParsedPlaceholderKeepsItsColourAcrossRenders() {
        final PreComponentTree tree = compiler.compile("<gradient:red:blue><x></gradient>");
        final Component parsed = tree.render(NOBODY,
            Placeholder.ofParsed("x", Component.text("A", NamedTextColor.BLUE)));
        final Component unparsed = tree.render(NOBODY, Placeholder.ofUnparsed("x", "B"));
        final Component parsedAgain = tree.render(NOBODY,
            Placeholder.ofParsed("x", Component.text("C", NamedTextColor.BLUE)));
        final List<String> parsedTokens = ComponentComparison.expression(parsed);
        final List<String> unparsedTokens = ComponentComparison.expression(unparsed);
        final List<String> parsedAgainTokens = ComponentComparison.expression(parsedAgain);
        assertTrue(parsedTokens.getFirst().contains(NamedTextColor.BLUE.asHexString()));
        assertTrue(unparsedTokens.getFirst().contains(NamedTextColor.RED.asHexString()));
        assertTrue(parsedAgainTokens.getFirst().contains(NamedTextColor.BLUE.asHexString()));
    }

    @Test
    void selfClosingPlaceholderIsNotReportedAsStatic() {
        assertFalse(compiler.compile("<gradient:red:blue><p/></gradient>").isStatic());
        assertFalse(compiler.compile("<selector:@a:'<p/>'>").isStatic());
        assertFalse(compiler.compile("<nbt:entity:@e:Inventory:'<p/>'>").isStatic());
    }

    @Test
    void storedTreesWithSelfClosingPlaceholderRenderDynamically() {
        final PreComponentTree selector = compiler.compile("<selector:@a:'<p/>'>");
        final SelectorComponent selectorA =
            (SelectorComponent) selector.render(NOBODY, Placeholder.ofUnparsed("p", "A"));
        final SelectorComponent selectorB =
            (SelectorComponent) selector.render(NOBODY, Placeholder.ofUnparsed("p", "B"));
        assertEquals("A", plainText(selectorA.separator()));
        assertEquals("B", plainText(selectorB.separator()));

        final PreComponentTree gradient = compiler.compile("<gradient:red:blue><p/></gradient>");
        assertEquals("A", plainText(gradient.render(NOBODY, Placeholder.ofUnparsed("p", "A"))));
        assertEquals("<p/>", plainText(gradient.render(NOBODY)));
        assertEquals("B", plainText(gradient.render(NOBODY, Placeholder.ofUnparsed("p", "B"))));
    }
}
