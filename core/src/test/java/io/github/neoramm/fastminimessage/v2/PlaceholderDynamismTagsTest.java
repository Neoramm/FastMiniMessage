package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.example.ComponentModifiers;
import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
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

final class PlaceholderDynamismTagsTest {

    private static final @NotNull Pointered NOBODY = new Pointered() {
        @Override
        public @NotNull Pointers pointers() {
            return Pointers.empty();
        }
    };


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
        final var layout = PayloadSchema.builder();
        final var xKey = layout.key("x", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("x", (final var scope) -> scope.payload(xKey));
        final var compiler = new MiniMessageCompiler(registry);
        final String[] opens = {"<gradient:red:blue>", "<rainbow>", "<pride>"};
        final String[] closes = {"</gradient>", "</rainbow>", "</pride>"};
        for (int i = 0; i < opens.length; i++) {
            final PreComponentTree tree = compiler.compile(opens[i] + "<x>" + closes[i]);
            final Component a = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("A")));
            final Component b = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("B")));
            assertEquals("A", plainText(a), opens[i]);
            assertEquals("B", plainText(b), opens[i]);
        }
    }

    @Test
    void hoverBodyAndArgumentPlaceholdersUpdate() {
        final var layout = PayloadSchema.builder();
        final var xKey = layout.key("x", Component.class);
        final var hKey = layout.key("h", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("x", (final var scope) -> scope.payload(xKey))
            .registerPlaceholder("h", (final var scope) -> scope.payload(hKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree body = compiler.compile("<hover:show_text:'hint'><x></hover>");
        assertEquals("A", plainText(body.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("A"))
            .payload(hKey, Component.empty()))));
        assertEquals("B", plainText(body.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("B"))
            .payload(hKey, Component.empty()))));
        assertEquals("hint", plainText(hoverValue(body.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.empty())
            .payload(hKey, Component.empty())))));

        final PreComponentTree argument = compiler.compile("<hover:show_text:'<h>'>text</hover>");
        assertEquals("one", plainText(hoverValue(
            argument.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.empty())
            .payload(hKey, Component.text("one"))))));
        assertEquals("two", plainText(hoverValue(
            argument.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.empty())
            .payload(hKey, Component.text("two"))))));
    }

    @Test
    void clickBodyAndArgumentPlaceholdersUpdate() {
        final var layout = PayloadSchema.builder();
        final var xKey = layout.key("x", Component.class);
        final var cmdKey = layout.key("cmd", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("x", (final var scope) -> scope.payload(xKey))
            .registerPlaceholder("cmd", (final var scope) -> scope.payload(cmdKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree body = compiler.compile("<click:run_command:'/spawn'><x></click>");
        assertEquals("A", plainText(body.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("A"))
            .payload(cmdKey, Component.empty()))));
        assertEquals("/spawn", clickValue(clickEvent(body.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.empty())
            .payload(cmdKey, Component.empty())))));

        final PreComponentTree argument = compiler.compile("<click:run_command:'/<cmd>'>text</click>");
        assertEquals("/home",
            clickValue(clickEvent(argument.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.empty())
            .payload(cmdKey, Component.text("home"))))));
        assertEquals("/back",
            clickValue(clickEvent(argument.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.empty())
            .payload(cmdKey, Component.text("back"))))));
    }

    @Test
    void insertBodyAndArgumentPlaceholdersUpdate() {
        final var layout = PayloadSchema.builder();
        final var xKey = layout.key("x", Component.class);
        final var iKey = layout.key("i", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("x", (final var scope) -> scope.payload(xKey))
            .registerPlaceholder("i", (final var scope) -> scope.payload(iKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree body = compiler.compile("<insert:'shift'><x></insert>");
        assertEquals("shift", body.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("A"))
            .payload(iKey, Component.empty())).insertion());

        final PreComponentTree argument = compiler.compile("<insert:'<i>'>text</insert>");
        assertEquals("a", argument.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.empty())
            .payload(iKey, Component.text("a"))).insertion());
        assertEquals("b", argument.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.empty())
            .payload(iKey, Component.text("b"))).insertion());
    }

    @Test
    void shadowBodyAndArgumentPlaceholdersUpdate() {
        final var layout = PayloadSchema.builder();
        final var xKey = layout.key("x", Component.class);
        final var cKey = layout.key("c", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("x", (final var scope) -> scope.payload(xKey))
            .registerPlaceholder("c", (final var scope) -> scope.payload(cKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree body = compiler.compile("<shadow:#000000aa><x></shadow>");
        final String bodyA = plainText(body.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("A"))
            .payload(cKey, Component.empty())));
        final String bodyB = plainText(body.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("B"))
            .payload(cKey, Component.empty())));
        assertEquals("A", bodyA);
        assertEquals("B", bodyB);

        final PreComponentTree argument = compiler.compile("<shadow:'<c>':0.4>text</shadow>");
        final Component red = argument.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.empty())
            .payload(cKey, Component.text("red")));
        final Component blue = argument.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.empty())
            .payload(cKey, Component.text("blue")));
        assertNotNull(red.style().shadowColor());
        assertNotNull(blue.style().shadowColor());
        assertNotEquals(red.style().shadowColor(), blue.style().shadowColor());
    }

    @Test
    void fontArgumentPlaceholderUpdatesTheFont() {
        final var layout = PayloadSchema.builder();
        final var fKey = layout.key("f", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("f", (final var scope) -> scope.payload(fKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<font:'<f>'>text</font>");
        assertEquals(Key.key("minecraft:default"),
            tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(fKey, Component.text("minecraft:default"))).style().font());
        assertEquals(Key.key("minecraft:uniform"),
            tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(fKey, Component.text("minecraft:uniform"))).style().font());
    }

    @Test
    void keybindArgumentPlaceholderUpdatesTheKey() {
        final var layout = PayloadSchema.builder();
        final var kKey = layout.key("k", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("k", (final var scope) -> scope.payload(kKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<key:'<k>'/>");
        final KeybindComponent jump = (KeybindComponent) tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(kKey, Component.text("key.jump")));
        final KeybindComponent sneak = (KeybindComponent) tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(kKey, Component.text("key.sneak")));
        assertEquals("key.jump", jump.keybind());
        assertEquals("key.sneak", sneak.keybind());
    }

    @Test
    void scoreArgumentPlaceholderUpdatesTheScore() {
        final var layout = PayloadSchema.builder();
        final var mKey = layout.key("m", Component.class);
        final var oKey = layout.key("o", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("m", (final var scope) -> scope.payload(mKey))
            .registerPlaceholder("o", (final var scope) -> scope.payload(oKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<score:'<m>':'<o>'/>");
        final ScoreComponent first = (ScoreComponent) tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(mKey, Component.text("Steve"))
            .payload(oKey, Component.text("kills")));
        final ScoreComponent second = (ScoreComponent) tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(mKey, Component.text("Alex"))
            .payload(oKey, Component.text("deaths")));
        assertEquals("Steve", first.name());
        assertEquals("kills", first.objective());
        assertEquals("Alex", second.name());
        assertEquals("deaths", second.objective());
    }

    @Test
    void spriteArgumentPlaceholderUpdatesTheSprite() {
        final var layout = PayloadSchema.builder();
        final var aKey = layout.key("a", Component.class);
        final var bKey = layout.key("b", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("a", (final var scope) -> scope.payload(aKey))
            .registerPlaceholder("b", (final var scope) -> scope.payload(bKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<sprite:'<a>':'<b>'/>");
        final SpriteObjectContents first =
            (SpriteObjectContents) ((ObjectComponent) tree.render(
                (final var builder) -> builder.viewer(NOBODY)
            .payload(aKey, Component.text("items"))
            .payload(bKey, Component.text("item/diamond")))).contents();
        final SpriteObjectContents second =
            (SpriteObjectContents) ((ObjectComponent) tree.render(
                (final var builder) -> builder.viewer(NOBODY)
            .payload(aKey, Component.text("blocks"))
            .payload(bKey, Component.text("block/stone")))).contents();
        assertEquals(Key.key("minecraft:items"), first.atlas());
        assertEquals(Key.key("minecraft:item/diamond"), first.sprite());
        assertEquals(Key.key("minecraft:blocks"), second.atlas());
        assertEquals(Key.key("minecraft:block/stone"), second.sprite());
    }

    @Test
    void headArgumentPlaceholderUpdatesTheName() {
        final var layout = PayloadSchema.builder();
        final var nKey = layout.key("n", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("n", (final var scope) -> scope.payload(nKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<head:'<n>'/>");
        final PlayerHeadObjectContents first =
            (PlayerHeadObjectContents) ((ObjectComponent) tree.render(
                (final var builder) -> builder.viewer(NOBODY)
            .payload(nKey, Component.text("Steve")))).contents();
        final PlayerHeadObjectContents second =
            (PlayerHeadObjectContents) ((ObjectComponent) tree.render(
                (final var builder) -> builder.viewer(NOBODY)
            .payload(nKey, Component.text("Alex")))).contents();
        assertEquals("Steve", first.name());
        assertEquals("Alex", second.name());
    }

    @Test
    void selectorKeyArgumentPlaceholderUpdatesThePattern() {
        final var layout = PayloadSchema.builder();
        final var kKey = layout.key("k", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("k", (final var scope) -> scope.payload(kKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<selector:'<k>'/>");
        final SelectorComponent all = (SelectorComponent) tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(kKey, Component.text("@a")));
        final SelectorComponent entities = (SelectorComponent) tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(kKey, Component.text("@e")));
        assertEquals("@a", all.pattern());
        assertEquals("@e", entities.pattern());
    }

    @Test
    void nbtSelectorArgumentPlaceholderUpdatesTheSelector() {
        final var layout = PayloadSchema.builder();
        final var sKey = layout.key("s", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("s", (final var scope) -> scope.payload(sKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<nbt:entity:'<s>':Inventory/>");
        final EntityNBTComponent all =
            (EntityNBTComponent) tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(sKey, Component.text("@a")));
        final EntityNBTComponent entities =
            (EntityNBTComponent) tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(sKey, Component.text("@e")));
        assertEquals("@a", all.selector());
        assertEquals("@e", entities.selector());
    }

    @Test
    void langArgumentPlaceholderUpdatesTheArguments() {
        final var layout = PayloadSchema.builder();
        final var aKey = layout.key("a", Component.class);
        final var msgKey = layout.key("msg", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("a", (final var scope) -> scope.payload(aKey))
            .registerPlaceholder("msg", (final var scope) -> scope.payload(msgKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<lang:chat.type.text:'<a>':'<msg>'/>");
        final TranslatableComponent first =
            (TranslatableComponent) tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(aKey, Component.text("Steve"))
            .payload(msgKey, Component.text("hello")));
        final TranslatableComponent second =
            (TranslatableComponent) tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(aKey, Component.text("Alex"))
            .payload(msgKey, Component.text("bye")));
        final List<TranslationArgument> firstArgs = first.arguments();
        final List<TranslationArgument> secondArgs = second.arguments();
        assertEquals("Steve", plainText(firstArgs.getFirst().asComponent()));
        assertEquals("hello", plainText(firstArgs.get(1).asComponent()));
        assertEquals("Alex", plainText(secondArgs.getFirst().asComponent()));
        assertEquals("bye", plainText(secondArgs.get(1).asComponent()));
    }

    @Test
    void transitionArgumentPlaceholderUpdatesTheColour() {
        final var layout = PayloadSchema.builder();
        final var aKey = layout.key("a", Component.class);
        final var bKey = layout.key("b", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("a", (final var scope) -> scope.payload(aKey))
            .registerPlaceholder("b", (final var scope) -> scope.payload(bKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<transition:'<a>':'<b>'/>");
        final Component red = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(aKey, Component.text("red"))
            .payload(bKey, Component.text("blue")));
        final Component green = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(aKey, Component.text("green"))
            .payload(bKey, Component.text("yellow")));
        assertEquals(NamedTextColor.RED, red.style().color());
        assertEquals(NamedTextColor.GREEN, green.style().color());
    }

    @Test
    void modifierBodyPlaceholderKeepsWorking() {
        final var layout = PayloadSchema.builder();
        final var xKey = layout.key("x", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("x", (final var scope) -> scope.payload(xKey));
        final var compiler = new MiniMessageCompiler(registry);
        registry.registerModifier("shout", ComponentModifiers.uppercase());
        final PreComponentTree tree = compiler.compile("<shout><x></shout>");
        assertEquals("HELLO", plainText(tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("hello")))));
        assertEquals("WORLD", plainText(tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("world")))));
    }

    @Test
    void unknownTagBodyPlaceholderKeepsWorking() {
        final var layout = PayloadSchema.builder();
        final var xKey = layout.key("x", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("x", (final var scope) -> scope.payload(xKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<unknown><x></unknown>");
        final String first = plainText(tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("A"))));
        final String second = plainText(tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("B"))));
        assertTrue(first.contains("A"));
        assertTrue(second.contains("B"));
        assertNotEquals(first, second);
    }

    @Test
    void combinedMessageUpdatesEveryDynamicTag() {
        final var layout = PayloadSchema.builder();
        final var gKey = layout.key("g", Component.class);
        final var hKey = layout.key("h", Component.class);
        final var cKey = layout.key("c", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("g", (final var scope) -> scope.payload(gKey))
            .registerPlaceholder("h", (final var scope) -> scope.payload(hKey))
            .registerPlaceholder("c", (final var scope) -> scope.payload(cKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile(
            "<gradient:red:blue><g></gradient> <hover:show_text:'<h>'>"
                + "<click:run_command:'/<c>'>click</click></hover>");
        final Component first = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(gKey, Component.text("G"))
            .payload(hKey, Component.text("H1"))
            .payload(cKey, Component.text("one")));
        final Component second = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(gKey, Component.text("GG"))
            .payload(hKey, Component.text("H2"))
            .payload(cKey, Component.text("two")));
        assertEquals("G click", plainText(first));
        assertEquals("GG click", plainText(second));
        assertNotEquals(ComponentComparison.expression(first), ComponentComparison.expression(second));
    }

    @Test
    void hoverSelfClosingPlaceholderUsesTheCompiledTree() {
        final var layout = PayloadSchema.builder();
        final var xKey = layout.key("x", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("x", (final var scope) -> scope.payload(xKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<hover:show_text:'<x/>'>text</hover>");
        final Component red = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("A", NamedTextColor.RED)));
        final Component blue = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("B", NamedTextColor.BLUE)));
        final Component redHover = hoverValue(red);
        final Component blueHover = hoverValue(blue);
        assertEquals("A", plainText(redHover));
        assertEquals("B", plainText(blueHover));
        assertEquals(NamedTextColor.RED, redHover.style().color());
        assertEquals(NamedTextColor.BLUE, blueHover.style().color());
    }

    @Test
    void langSelfClosingPlaceholderKeepsParsedStyle() {
        final var layout = PayloadSchema.builder();
        final var xKey = layout.key("x", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("x", (final var scope) -> scope.payload(xKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<lang:chat.type.text:'<x/>'/>");
        final TranslatableComponent red =
            (TranslatableComponent) tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("A", NamedTextColor.RED)));
        final TranslatableComponent blue =
            (TranslatableComponent) tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("B", NamedTextColor.BLUE)));
        final Component redArg = red.arguments().getFirst().asComponent();
        final Component blueArg = blue.arguments().getFirst().asComponent();
        assertEquals("A", plainText(redArg));
        assertEquals("B", plainText(blueArg));
        assertEquals(NamedTextColor.RED, redArg.style().color());
        assertEquals(NamedTextColor.BLUE, blueArg.style().color());
    }

    @Test
    void gradientWithParsedPlaceholderKeepsItsColourAcrossRenders() {
        final var layout = PayloadSchema.builder();
        final var xKey = layout.key("x", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("x", (final var scope) -> scope.payload(xKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree tree = compiler.compile("<gradient:red:blue><x></gradient>");
        final Component parsed = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("A", NamedTextColor.BLUE)));
        final Component unparsed = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("B")));
        final Component parsedAgain = tree.render((final var builder) -> builder.viewer(NOBODY)
            .payload(xKey, Component.text("C", NamedTextColor.BLUE)));
        final List<String> parsedTokens = ComponentComparison.expression(parsed);
        final List<String> unparsedTokens = ComponentComparison.expression(unparsed);
        final List<String> parsedAgainTokens = ComponentComparison.expression(parsedAgain);
        assertTrue(parsedTokens.getFirst().contains(NamedTextColor.BLUE.asHexString()));
        assertTrue(unparsedTokens.getFirst().contains(NamedTextColor.RED.asHexString()));
        assertTrue(parsedAgainTokens.getFirst().contains(NamedTextColor.BLUE.asHexString()));
    }

    @Test
    void selfClosingPlaceholderIsNotReportedAsStatic() {
        final var layout = PayloadSchema.builder();
        final var pKey = layout.key("p", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("p", (final var scope) -> scope.payload(pKey));
        final var compiler = new MiniMessageCompiler(registry);
        assertFalse(compiler.compile("<gradient:red:blue><p/></gradient>").isStatic());
        assertFalse(compiler.compile("<selector:@a:'<p/>'>").isStatic());
        assertFalse(compiler.compile("<nbt:entity:@e:Inventory:'<p/>'>").isStatic());
    }

    @Test
    void storedTreesWithSelfClosingPlaceholderRenderDynamically() {
        final var layout = PayloadSchema.builder();
        final var pKey = layout.key("p", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("p", (final var scope) -> scope.payload(pKey));
        final var compiler = new MiniMessageCompiler(registry);
        final PreComponentTree selector = compiler.compile("<selector:@a:'<p/>'>");
        final SelectorComponent selectorA =
            (SelectorComponent) selector.render((final var builder) -> builder.viewer(NOBODY)
            .payload(pKey, Component.text("A")));
        final SelectorComponent selectorB =
            (SelectorComponent) selector.render((final var builder) -> builder.viewer(NOBODY)
            .payload(pKey, Component.text("B")));
        assertEquals("A", plainText(selectorA.separator()));
        assertEquals("B", plainText(selectorB.separator()));

        final PreComponentTree gradient = compiler.compile("<gradient:red:blue><p/></gradient>");
        assertEquals("A", plainText(gradient.render((final var builder) -> builder.viewer(NOBODY)
            .payload(pKey, Component.text("A")))));
        assertEquals("", plainText(gradient.render((final var builder) -> builder.viewer(NOBODY)
            .payload(pKey, Component.empty()))));
        assertEquals("B", plainText(gradient.render((final var builder) -> builder.viewer(NOBODY)
            .payload(pKey, Component.text("B")))));
    }
}
