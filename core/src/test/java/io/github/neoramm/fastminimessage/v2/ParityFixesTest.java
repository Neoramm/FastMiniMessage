package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.example.ComponentModifiers;
import io.github.neoramm.fastminimessage.v2.model.Arguments;
import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import io.github.neoramm.fastminimessage.v2.tag.CompileContext;
import io.github.neoramm.fastminimessage.v2.tag.TagArgs;
import io.github.neoramm.fastminimessage.v2.tag.TagSpec;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ParityFixesTest {

    private static final @NotNull Pointered NOBODY = new Pointered() {
        @Override
        public @NotNull Pointers pointers() {
            return Pointers.empty();
        }
    };

    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
    private final @NotNull MiniMessage miniMessage = MiniMessage.builder().strict(false).build();

    @Test
    void langOrSingleArgTest() {
        final Component cLangEmpty = miniMessage.deserialize("<lang>");
        final Component fastLangEmpty = compiler.compile("<lang>").render();
        assertEquals(plain(cLangEmpty), plain(fastLangEmpty));

        final Component cLangOr1 = miniMessage.deserialize("<lang_or:my.key>");
        final Component fastLangOr1 = compiler.compile("<lang_or:my.key>").render();
        assertEquals(plain(cLangOr1), plain(fastLangOr1));
    }

    @Test
    void insertNoArgsTest() {
        final Component c = miniMessage.deserialize("<insert>hello</insert>");
        final Component fast = compiler.compile("<insert>hello</insert>").render();
        assertEquals(plain(c), plain(fast));
    }

    @Test
    void shadowNoArgsTest() {
        final Component c = miniMessage.deserialize("<shadow>hello</shadow>");
        final Component fast = compiler.compile("<shadow>hello</shadow>").render();
        assertEquals(plain(c), plain(fast));
    }

    @Test
    void playerHeadAdventureTest() {
        final Component c1 = miniMessage.deserialize("<head:steve>");
        final Component fast1 = compiler.compile("<head:steve>").render();
        assertEquals(c1, fast1);

        final Component cExtra = miniMessage.deserialize("<head:steve:true:extra>");
        final Component fastExtra = compiler.compile("<head:steve:true:extra>").render();
        assertEquals(plain(cExtra), plain(fastExtra));
    }

    @Test
    void spriteAdventureTest() {
        final Component c1 = miniMessage.deserialize("<sprite:item/diamond_pickaxe>");
        final Component fast1 = compiler.compile("<sprite:item/diamond_pickaxe>").render();
        assertEquals(c1, fast1);

        final Component c2 = miniMessage.deserialize("<sprite:items:item/diamond_pickaxe>");
        final Component fast2 = compiler.compile("<sprite:items:item/diamond_pickaxe>").render();
        assertEquals(c2, fast2);

        final Component fastExtra = compiler.compile("<sprite:a:b:c>").render();
        assertEquals("<sprite:a:b:c>", plain(fastExtra));
    }

    @Test
    void quoteWithGreaterSign() {
        final Component c1 = miniMessage.deserialize("<hover:show_text:'a > b'>text</hover>");
        final Component fast1 = compiler.compile("<hover:show_text:'a > b'>text</hover>").render();
        assertEquals(plain(c1), plain(fast1));
        assertEquals(c1.hoverEvent(), fast1.hoverEvent());

        final Component c2 = miniMessage.deserialize("<hover:show_text:'unclosed>text</hover>");
        final Component fast2 = compiler.compile("<hover:show_text:'unclosed>text</hover>").render();
        assertEquals(plain(c2), plain(fast2));
        assertEquals(c2.hoverEvent(), fast2.hoverEvent());

        final Component c3 = miniMessage.deserialize("<click:run_command:'unclosed>test</click>");
        final Component fast3 = compiler.compile("<click:run_command:'unclosed>test</click>").render();
        assertEquals(plain(c3), plain(fast3));
        assertEquals(c3.clickEvent(), fast3.clickEvent());
    }

    @Test
    void negativeGradientPhaseMatchesMiniMessage() {
        final String[] samples = {
            "<gradient:red:blue:-0.5>abc</gradient>",
            "<gradient:#ff0000:#00ff00:#0000ff:-0.2>test text long gradient</gradient>",
            "<gradient:red:blue:-1.0>xyz</gradient>",
            "<gradient:red:blue:1.0>xyz</gradient>"
        };
        for (final String sample : samples) {
            final Component expected = miniMessage.deserialize(sample);
            final Component actual = compiler.compile(sample).render(PayloadInputs.viewer(NOBODY));
            assertEquals(ComponentComparison.expression(expected), ComponentComparison.expression(actual), sample);
        }
    }

    @Test
    void escapedQuotesInArguments() {
        final Arguments singleQuote = Arguments.parse("'It\\'s working'");
        assertEquals(1, singleQuote.size());
        assertEquals("It's working", singleQuote.getFirst());

        final Arguments doubleQuote = Arguments.parse("\"Double \\\"quote\\\" test\"");
        assertEquals(1, doubleQuote.size());
        assertEquals("Double \"quote\" test", doubleQuote.getFirst());

        final String hoverSample = "<hover:show_text:'It\\'s working'>test</hover>";
        final Component expected = miniMessage.deserialize(hoverSample);
        final Component actual = compiler.compile(hoverSample).render(PayloadInputs.viewer(NOBODY));
        assertEquals(ComponentComparison.expression(expected), ComponentComparison.expression(actual));
    }

    @Test
    void emptyArgumentsHandling() {
        final Arguments emptySingleQuote = Arguments.parse("''");
        assertEquals(1, emptySingleQuote.size());
        assertEquals("", emptySingleQuote.getFirst());

        final Arguments emptyDoubleQuote = Arguments.parse("\"\"");
        assertEquals(1, emptyDoubleQuote.size());
        assertEquals("", emptyDoubleQuote.getFirst());

        final Arguments trailingColon = Arguments.parse("a:");
        assertEquals(2, trailingColon.size());
        assertEquals("a", trailingColon.getFirst());
        assertEquals("", trailingColon.get(1));

        final Arguments leadingColon = Arguments.parse(":b");
        assertEquals(2, leadingColon.size());
        assertEquals("", leadingColon.getFirst());
        assertEquals("b", leadingColon.get(1));
    }

    @Test
    void argumentsCursorThrowsNoSuchElementException() {
        final Arguments.Cursor cursor = Arguments.parse("a").cursor();
        assertEquals("a", cursor.next());
        assertThrows(NoSuchElementException.class, cursor::next);
    }

    @Test
    void negatedItalicAliases() {
        final String[] samples = {
            "<!italic>text</!italic>",
            "<!i>text</!i>",
            "<!em>text</!em>"
        };
        for (final String sample : samples) {
            final Component expected = miniMessage.deserialize(sample);
            final Component actual = compiler.compile(sample).render(PayloadInputs.viewer(NOBODY));
            assertEquals(ComponentComparison.expression(expected), ComponentComparison.expression(actual), sample);
        }
    }

    @Test
    void placeholdersWithSpecialCharactersInTagArguments() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> playerKey = layout.optionalKey("player.name", String.class);
        final PayloadSchema.@NotNull Key<@NotNull String> themeKey = layout.optionalKey("theme-color", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("player.name", (final var scope) -> Component.text(scope.payload(playerKey)))
            .registerPlaceholder("theme-color", (final var scope) -> Component.text(scope.payload(themeKey)));
        final @NotNull MiniMessageCompiler comp = new MiniMessageCompiler(registry);
        final PreComponentTree dotTree = comp.compile("<color:'<player.name>'>text</color>");
        final Component dotRendered = dotTree.render((final var b) -> b.viewer(NOBODY).payload(playerKey, "red"));
        assertEquals(NamedTextColor.RED, dotRendered.style().color());

        final PreComponentTree hyphenTree = comp.compile("<color:'<theme-color>'>text</color>");
        final Component hyphenRendered = hyphenTree.render((final var b) -> b.viewer(NOBODY).payload(themeKey, "#00ff00"));
        final TextColor color = hyphenRendered.style().color();
        assertNotNull(color);
        assertEquals("#00FF00", color.asHexString().toUpperCase());
    }

    @Test
    void modifierIsolationAcrossRenders() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Boolean> shoutKey = layout.key("shout", Boolean.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerModifier("shout", (final var content, final var scope) -> {
                final Boolean enabled = scope.payload(shoutKey);
                return Boolean.TRUE.equals(enabled) ? ComponentModifiers.uppercase().modify(content, scope) : content;
            });
        final @NotNull PreComponentTree tree = new MiniMessageCompiler(registry).compile("<shout>hello</shout>");
        final @NotNull Component rendered1 = tree.render((final var b) -> b.viewer(NOBODY).payload(shoutKey, true));
        assertEquals("HELLO", plain(rendered1));

        final @NotNull Component rendered2 = tree.render((final var b) -> b.viewer(NOBODY).payload(shoutKey, false));
        assertEquals("hello", plain(rendered2));
    }

    @Test
    void translatableFallbackBoundsSafety() {
        final PreComponentTree singleArg = compiler.compile("<lang_or:test.key>");
        final Component comp1 = singleArg.render(PayloadInputs.viewer(NOBODY));
        assertNotNull(comp1);

        final PreComponentTree twoArgs = compiler.compile("<lang_or:test.key:fallback>");
        final Component comp2 = twoArgs.render(PayloadInputs.viewer(NOBODY));
        assertNotNull(comp2);
        if (comp2 instanceof final TranslatableComponent translatable) {
            assertEquals("test.key", translatable.key());
            assertEquals("fallback", translatable.fallback());
            assertEquals(0, translatable.arguments().size());
        }

        final PreComponentTree threeArgs = compiler.compile("<lang_or:test.key:fallback:arg1:arg2>");
        final Component comp3 = threeArgs.render(PayloadInputs.viewer(NOBODY));
        assertNotNull(comp3);
        if (comp3 instanceof final TranslatableComponent translatable) {
            assertEquals("test.key", translatable.key());
            assertEquals("fallback", translatable.fallback());
            assertEquals(2, translatable.arguments().size());
        }
    }

    @Test
    void scannerOutOfBoundsSafetyWithClosingSlash() {
        final PreComponentTree tree1 = compiler.compile("hello </> world");
        assertEquals("hello </> world", plain(tree1.render(PayloadInputs.viewer(NOBODY))));

        final PreComponentTree tree2 = compiler.compile("test <//> text");
        assertEquals("test <//> text", plain(tree2.render(PayloadInputs.viewer(NOBODY))));
    }

    @Test
    void shadowResolverValidationAndClamping() {
        final TagSpec.StyleRule shadow = (TagSpec.StyleRule) ResolverRegistry.withDefaults().resolve("shadow");
        assertNotNull(shadow);
        assertThrows(IllegalArgumentException.class, () ->
            shadow.rule().apply(new TagArgs("invalid_color", compiler), new CompileContext("shadow", compiler)));

        final PreComponentTree overAlpha = compiler.compile("<shadow:red:1.5>test</shadow>");
        final Component comp1 = overAlpha.render(PayloadInputs.viewer(NOBODY));
        assertNotNull(comp1.style().shadowColor());
        assertEquals(255, comp1.style().shadowColor().alpha());

        final PreComponentTree underAlpha = compiler.compile("<shadow:red:-0.5>test</shadow>");
        final Component comp2 = underAlpha.render(PayloadInputs.viewer(NOBODY));
        assertNotNull(comp2.style().shadowColor());
        assertEquals(0, comp2.style().shadowColor().alpha());
    }

    @Test
    void legacyFormatFastPath() {
        final String input = "hello world without ampersand";
        assertEquals(input, LegacyFormatParser.toMiniMessage(input));
        assertEquals("&", LegacyFormatParser.toMiniMessage("&&"));
    }

    @Test
    void deferredTagConcurrentRenders() throws InterruptedException {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> colKey = layout.key("col", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("col", (final var scope) -> Component.text(scope.payload(colKey)));
        final PreComponentTree tree = new MiniMessageCompiler(registry).compile("<color:'<col>'>sample</color>");
        final int threadCount = 8;
        final int iterations = 1000;
        final Thread @NotNull [] threads = new Thread[threadCount];
        final boolean[] success = new boolean[threadCount];
        for (int t = 0; t < threadCount; t++) {
            final int threadIndex = t;
            final String colorName = (t % 2 == 0) ? "red" : "blue";
            final TextColor expected = (t % 2 == 0) ? NamedTextColor.RED : NamedTextColor.BLUE;
            threads[t] = new Thread(() -> {
                boolean ok = true;
                for (int i = 0; i < iterations; i++) {
                    final Component c = tree.render((final var b) -> b.viewer(NOBODY).payload(colKey, colorName));
                    if (!expected.equals(c.style().color())) {
                        ok = false;
                        break;
                    }
                }
                success[threadIndex] = ok;
            });
        }
        for (final Thread t : threads) t.start();
        for (final Thread t : threads) t.join();
        for (int t = 0; t < threadCount; t++) assertEquals(true, success[t]);
    }

    @Test
    void unclosedTagsRenderAsUnifiedPlaceholders() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> prefixKey = layout.key("prefix", Component.class);
        final PayloadSchema.@NotNull Key<@NotNull Component> playerKey = layout.key("player", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("prefix", (final var scope) -> scope.payload(prefixKey))
            .registerPlaceholder("player", (final var scope) -> scope.payload(playerKey));
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(registry).compile("<prefix> Welcome <player>!");
        final @NotNull Component rendered = tree.render((final var b) -> b.viewer(NOBODY)
            .payload(prefixKey, Component.text("[Admin]"))
            .payload(playerKey, Component.text("Alice")));
        assertEquals("[Admin] Welcome Alice!", plain(rendered));
    }

    @Test
    void unifiedTagResolverMixingInsertingAndModifying() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> prefixKey = layout.key("prefix", Component.class);
        final PayloadSchema.@NotNull Key<@NotNull Component> playerKey = layout.key("player", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("prefix", (final var scope) -> scope.payload(prefixKey))
            .registerPlaceholder("player", (final var scope) -> scope.payload(playerKey))
            .registerModifier("shout", ComponentModifiers.uppercase());
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(registry).compile("<prefix><shout><player> joined</shout>!");
        final @NotNull Component rendered = tree.render((final var b) -> b.viewer(NOBODY)
            .payload(prefixKey, Component.text("[Server] "))
            .payload(playerKey, Component.text("Bob")));
        assertEquals("[Server] BOB JOINED!", plain(rendered));
    }

    @Test
    void unclosedUnknownTagRendersLiteralFallback() {
        final @NotNull PreComponentTree tree = compiler.compile("<unknown> hello world");
        final @NotNull Component rendered = tree.render(PayloadInputs.viewer(NOBODY));
        assertEquals("<unknown> hello world", plain(rendered));
    }

    @Test
    void unclosedPlaceholderInheritsSurroundingColor() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> playerKey = layout.key("player", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("player", (final var scope) -> scope.payload(playerKey));
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(registry).compile("<red>Hello <player>, welcome!</red>");
        final @NotNull Component rendered = tree.render((final var b) ->
            b.viewer(NOBODY).payload(playerKey, Component.text("Alice")));
        assertEquals("Hello Alice, welcome!", plain(rendered));
        final Component expected = miniMessage.deserialize("<red>Hello Alice, welcome!</red>");
        assertEquals(ComponentComparison.expression(expected), ComponentComparison.expression(rendered));
    }

    @Test
    void caseInsensitiveTagResolution() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> playerKey = layout.key("playername", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("PlayerName", (final var scope) -> scope.payload(playerKey));
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(registry).compile("<playername> <PlayerName> <PLAYERNAME>");
        final @NotNull Component rendered = tree.render((final var b) ->
            b.viewer(NOBODY).payload(playerKey, Component.text("Alice")));
        assertEquals("Alice Alice Alice", plain(rendered));
    }

    @Test
    void dynamicTagWithArguments() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> playerKey = layout.key("player", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("player", (final var scope) -> scope.payload(playerKey))
            .register(new TagSpec.LeafRule(Set.of("badge"), (final var args, final var context) -> {
                final var values = args.values();
                return values.map((final var vals) ->
                    Component.text("[" + vals.string(0, "").toUpperCase(Locale.ROOT) + "] "));
            }));
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(registry).compile("<badge:vip> Welcome <player>!");
        final @NotNull Component rendered = tree.render((final var b) ->
            b.viewer(NOBODY).payload(playerKey, Component.text("Alice")));
        assertEquals("[VIP]  Welcome Alice!", plain(rendered));
    }

    @Test
    void unclosedModifierFollowedByUnclosedPlaceholder() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> playerKey = layout.key("player", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("player", (final var scope) -> scope.payload(playerKey))
            .registerModifier("shout", ComponentModifiers.uppercase());
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(registry).compile("<shout>alert: <player> connected");
        final @NotNull Component rendered = tree.render((final var b) ->
            b.viewer(NOBODY).payload(playerKey, Component.text("Bob")));
        assertEquals("ALERT: BOB CONNECTED", plain(rendered));
    }

    @Test
    void unclosedTagCutOffByReset() {
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults()
            .registerModifier("shout", ComponentModifiers.uppercase());
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(registry).compile("<shout>first <reset>second");
        final @NotNull Component rendered = tree.render();
        assertEquals("FIRST second", plain(rendered));
    }

    @Test
    void transitionWithChildrenRendersColor() {
        final String[] samples = {
            "<transition:red:blue:0.5>test</transition>",
            "<transition:red:blue:-0.2>test</transition>",
            "<transition:#ff0000:#00ff00:0.3>test</transition>"
        };
        for (final String sample : samples) {
            final Component mm = miniMessage.deserialize(sample);
            final Component fmm = compiler.compile(sample).render(PayloadInputs.viewer(NOBODY));
            assertEquals(ComponentComparison.expression(mm), ComponentComparison.expression(fmm), sample);
        }
    }

    @Test
    void colorAliasesAndCaseInsensitivity() {
        final String[] samples = {
            "<c:red>test</c>",
            "<colour:blue>test</colour>",
            "<c:grey>test</c>",
            "<color:grey>test</color>",
            "<color:dark_grey>test</color>",
            "<gradient:grey:blue>test</gradient>"
        };
        for (final String sample : samples) {
            final Component mm = miniMessage.deserialize(sample);
            final Component fmm = compiler.compile(sample).render(PayloadInputs.viewer(NOBODY));
            assertEquals(ComponentComparison.expression(mm), ComponentComparison.expression(fmm), sample);
        }
    }

    @Test
    void decorationBooleanArguments() {
        final String[] samples = {
            "<bold:false>test</bold>",
            "<bold:true>test</bold>",
            "<italic:false>test</italic>",
            "<underlined:false>test</underlined>",
            "<strikethrough:false>test</strikethrough>",
            "<obfuscated:false>test</obfuscated>"
        };
        for (final String sample : samples) {
            final Component mm = miniMessage.deserialize(sample);
            final Component fmm = compiler.compile(sample).render(PayloadInputs.viewer(NOBODY));
            assertEquals(ComponentComparison.expression(mm), ComponentComparison.expression(fmm), sample);
        }
    }

    @Test
    void unquotedLessThanCharactersPreserveTags() {
        final String[] samples = {
            "<3 <red>alert</red>",
            "5 < 10 <blue>test</blue>",
            "a < b and c > d <green>ok</green>"
        };
        for (final String sample : samples) {
            final Component mm = miniMessage.deserialize(sample);
            final Component fmm = compiler.compile(sample).render(PayloadInputs.viewer(NOBODY));
            assertEquals(ComponentComparison.expression(mm), ComponentComparison.expression(fmm), sample);
        }
    }

    @Test
    void hexTagClosingIntegrity() {
        final String sample = "<#ff0000>test</#ff0000>";
        final Component mm = miniMessage.deserialize(sample);
        final Component fmm = compiler.compile(sample).render(PayloadInputs.viewer(NOBODY));
        assertEquals(ComponentComparison.expression(mm), ComponentComparison.expression(fmm));
    }

    @Test
    void caseInsensitiveTagNames() {
        final String[] samples = {
            "<RED>test</RED>",
            "<Red>test</red>",
            "<BOLD>bold</bold>"
        };
        for (final String sample : samples) {
            final Component mm = miniMessage.deserialize(sample);
            final Component fmm = compiler.compile(sample).render(PayloadInputs.viewer(NOBODY));
            assertEquals(ComponentComparison.expression(mm), ComponentComparison.expression(fmm), sample);
        }
    }

    @Test
    void legacyFormatHexCaseInsensitive() {
        assertEquals("<#FF0000>", LegacyFormatParser.toMiniMessage("&x&F&F&0&0&0&0"));
        assertEquals("<#FF0000>", LegacyFormatParser.toMiniMessage("&X&F&F&0&0&0&0"));
    }

    @Test
    void translatableSelfClosingWithSiblings() {
        final String[] samples = {
            "<lang:gui.ok> Hello",
            "<translate:gui.cancel> Bye",
            "<tr:gui.yes> Yes",
            "<lang_or:gui.ok:'Default'> World",
            "<translate_or:gui.cancel:'Cancel'> Next",
            "<tr_or:gui.yes:'Yes'> Finish"
        };
        for (final String sample : samples) {
            final Component mm = miniMessage.deserialize(sample);
            final Component fmm = compiler.compile(sample).render(PayloadInputs.viewer(NOBODY));
            assertEquals(ComponentComparison.expression(mm), ComponentComparison.expression(fmm), sample);
        }
    }

    @Test
    void inheritedStylePreservedOnComponentTags() {
        final String[] samples = {
            "<red><selector:@a></red>",
            "<green><score:Steve:kills></green>",
            "<blue><key:key.jump></blue>"
        };
        for (final String sample : samples) {
            final Component mm = miniMessage.deserialize(sample);
            final Component fmm = compiler.compile(sample).render(PayloadInputs.viewer(NOBODY));
            assertEquals(ComponentComparison.expression(mm), ComponentComparison.expression(fmm), sample);
        }
    }

    @Test
    void gradientPhaseOnlyAndWrapAround() {
        final String[] samples = {
            "<gradient:0.5>test</gradient>",
            "<gradient:-0.5>test</gradient>",
            "<gradient:red:blue:0.5>test</gradient>",
            "<gradient:red:blue:-0.5>test</gradient>"
        };
        for (final String sample : samples) {
            final Component mm = miniMessage.deserialize(sample);
            final Component fmm = compiler.compile(sample).render(PayloadInputs.viewer(NOBODY));
            assertEquals(ComponentComparison.expression(mm), ComponentComparison.expression(fmm), sample);
        }
    }

    @Test
    void closingTagsWithArguments() {
        final String[] samples = {
            "<click:run_command:'/spawn'>Spawn</click:run_command>",
            "<color:red>Red</color:red>",
            "<font:uniform>Uniform</font:uniform>"
        };
        for (final String sample : samples) {
            final Component mm = miniMessage.deserialize(sample);
            final Component fmm = compiler.compile(sample).render(PayloadInputs.viewer(NOBODY));
            assertEquals(ComponentComparison.expression(mm), ComponentComparison.expression(fmm), sample);
        }
    }

    @Test
    void escapedBackslashFollowedByTag() {
        final String[] samples = {
            "\\\\<red>hello</red>",
            "\\\\\\\\<blue>world</blue>",
            "\\\\<bold>test</bold>\\\\"
        };
        for (final String sample : samples) {
            final Component mm = miniMessage.deserialize(sample);
            final Component fmm = compiler.compile(sample).render(PayloadInputs.viewer(NOBODY));
            assertEquals(ComponentComparison.expression(mm), ComponentComparison.expression(fmm), sample);
        }
    }

    @Test
    void uppercaseColorInTagArguments() {
        final String[] samples = {
            "<color:RED>text</color>",
            "<c:BLUE>text</c>",
            "<colour:DARK_RED>text</colour>",
            "<color:Dark_Purple>text</color>"
        };
        for (final String sample : samples) {
            final Component mm = miniMessage.deserialize(sample);
            final Component fmm = compiler.compile(sample).render(PayloadInputs.viewer(NOBODY));
            assertEquals(ComponentComparison.expression(mm), ComponentComparison.expression(fmm), sample);
        }
    }

    @Test
    void unquotedUrlInClickTag() {
        final String sample = "<click:open_url:https://example.com>Click here</click>";
        final Component mm = miniMessage.deserialize(sample);
        final Component fmm = compiler.compile(sample).render(PayloadInputs.viewer(NOBODY));
        assertEquals(ComponentComparison.expression(mm), ComponentComparison.expression(fmm));
    }

    @Test
    void dynamicGradientInheritsStyle() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> c1Key = layout.key("c1", String.class);
        final PayloadSchema.@NotNull Key<@NotNull String> c2Key = layout.key("c2", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("c1", (final var scope) -> Component.text(scope.payload(c1Key)))
            .registerPlaceholder("c2", (final var scope) -> Component.text(scope.payload(c2Key)));
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(registry).compile("<bold><gradient:'<c1>':'<c2>'>text</gradient></bold>");
        final @NotNull Component rendered = tree.render((final var b) -> b.viewer(NOBODY)
            .payload(c1Key, "red")
            .payload(c2Key, "blue"));
        final @NotNull Component mm = miniMessage.deserialize("<bold><gradient:red:blue>text</gradient></bold>");
        assertEquals(ComponentComparison.expression(mm), ComponentComparison.expression(rendered));
    }

    @Test
    void dynamicTransitionInheritsStyle() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> phaseKey = layout.key("phase", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("phase", (final var scope) -> Component.text(scope.payload(phaseKey)));
        final @NotNull PreComponentTree tree = new MiniMessageCompiler(registry).compile(
            "<bold><transition:red:blue:'<phase>'>part1<yellow>part2</yellow></transition></bold>");
        final @NotNull Component rendered = tree.render((final var b) -> b.viewer(NOBODY)
            .payload(phaseKey, "0.5"));
        final @NotNull Component mm = miniMessage.deserialize(
            "<bold><transition:red:blue:0.5>part1<yellow>part2</yellow></transition></bold>");
        assertEquals(ComponentComparison.expression(mm), ComponentComparison.expression(rendered));
    }

    @Test
    void mathComparisonNotTreatedAsTags() {
        final String input = "Condition: 3 < 5 and 5 > 2 check";
        final Component mm = miniMessage.deserialize(input);
        final Component fmm = compiler.compile(input).render(PayloadInputs.viewer(NOBODY));
        assertEquals(ComponentComparison.expression(mm), ComponentComparison.expression(fmm));
    }

    @Test
    void deferredTagArgumentPlayerSubstitution() {
        final Pointered playerPointer = new Pointered() {
            @Override
            public @NotNull Pointers pointers() {
                return Pointers.builder().withDynamic(Identity.NAME, () -> "Alex").build();
            }
        };
        final PreComponentTree tree = compiler.compile("<hover:show_text:'User: <player>'>Profile</hover>");
        final Component rendered = tree.render(PayloadInputs.viewer(playerPointer));
        assertNotNull(rendered.hoverEvent());
        final Component hoverValue = (Component) rendered.hoverEvent().value();
        assertEquals("User: Alex", plain(hoverValue));
    }

    @Test
    void keybindMissingArgumentFallsBackToLiteral() {
        final String sample = "<key>";
        final Component mm = miniMessage.deserialize(sample);
        final Component fmm = compiler.compile(sample).render(PayloadInputs.viewer(NOBODY));
        assertEquals(ComponentComparison.expression(mm), ComponentComparison.expression(fmm));
    }

    private static @NotNull String plain(final @NotNull Component component) {
        final StringBuilder text = new StringBuilder();
        if (component instanceof final TextComponent textComponent) text.append(textComponent.content());
        for (final Component child : component.children()) text.append(plain(child));
        return text.toString();
    }
}
