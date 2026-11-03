package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.model.Arguments;
import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import io.github.neoramm.fastminimessage.v2.tag.CompileContext;
import io.github.neoramm.fastminimessage.v2.tag.TagArgs;
import io.github.neoramm.fastminimessage.v2.tag.TagSpec;
import io.github.neoramm.fastminimessage.v2.template.StringTemplate;
import io.github.neoramm.fastminimessage.v2.template.TemplateNode;
import io.github.neoramm.fastminimessage.v2.translation.Argument;
import io.github.neoramm.fastminimessage.v2.translation.FastMiniMessageTranslationStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.EntityNBTComponent;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.SelectorComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReportedRegressionTest {
    private final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults();
    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(registry);

    @ParameterizedTest
    @ValueSource(strings = {"&c", "&#ff0000", "&x&f&f&0&0&0&0"})
    void legacyColorsPreserveEvents(final @NotNull String color) {
        final var result = compiler.compile("<click:run_command:/go><hover:show_text:tip>" + color
            + "text</hover></click>").render();
        assertEquals("text", ComponentComparison.plainText(result));
        assertEquals(ClickEvent.runCommand("/go"), result.clickEvent());
        assertNotNull(result.hoverEvent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"<key>inside</key>", "<score:x>inside</score>", "<sprite>inside</sprite>"})
    void invalidLeavesRetainChildOrder(final @NotNull String input) {
        assertEquals(input, ComponentComparison.plainText(compiler.compile(input).render()));
    }

    @Test
    void invalidDynamicLeavesRetainChildOrderAndRecover() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> iconKey = layout.optionalKey("icon", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("icon", (final var scope) -> {
                final String icon = scope.payloadOrNull(iconKey);
                return icon != null ? Component.text(icon) : Component.empty();
            });
        final var tree = new MiniMessageCompiler(registry).compile("<sprite:'<icon>'>inside</sprite>");
        assertEquals("<sprite:'<icon>'>inside</sprite>", ComponentComparison.plainText(tree.render()));
        assertEquals("inside", ComponentComparison.plainText(
            tree.render((final var b) -> b.payload(iconKey, "minecraft:test"))));
    }

    @Test
    void scalarTemplatesSkipInvalidTagStarts() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> playerKey = layout.key("player", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("player", (final var scope) -> Component.text(scope.payload(playerKey)));
        final @NotNull MiniMessageCompiler comp = new MiniMessageCompiler(registry);
        final @NotNull RenderScope context = RenderScope.builder(registry.payloadSchema())
            .payload(playerKey, "Ada").build();
        assertEquals("1 < 2 Ada", StringTemplate.compile("1 < 2 <player>", comp).evaluate(context));
    }

    @Test
    void scalarTemplatesRespectEscapes() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> playerKey = layout.key("player", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("player", (final var scope) -> Component.text(scope.payload(playerKey)));
        final @NotNull MiniMessageCompiler comp = new MiniMessageCompiler(registry);
        final @NotNull RenderScope context = RenderScope.builder(registry.payloadSchema())
            .payload(playerKey, "Ada").build();
        assertEquals("<player> Ada", StringTemplate.compile("\\<player> <player>", comp).evaluate(context));
    }

    @Test
    void scalarTemplatesFlattenNonTextComponents() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> keyKey = layout.key("key", Component.class);
        final PayloadSchema.@NotNull Key<@NotNull Component> trKey = layout.key("tr", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("key", (final var scope) -> scope.payload(keyKey))
            .registerPlaceholder("tr", (final var scope) -> scope.payload(trKey));
        final @NotNull MiniMessageCompiler comp = new MiniMessageCompiler(registry);
        final @NotNull RenderScope context = RenderScope.builder(registry.payloadSchema())
            .payload(keyKey, Component.keybind("key.jump"))
            .payload(trKey, Component.translatable("translation.key"))
            .build();
        assertEquals("key.jump translation.key", StringTemplate.compile("<key> <tr>", comp).evaluate(context));
    }

    @Test
    void malformedQuotesDoNotConsumeFollowingTags() {
        final var result = compiler.compile("<hover:show_text:'broken <red>text</red>").render();
        assertEquals("<hover:show_text:'broken text", ComponentComparison.plainText(result));
        assertTrue(ComponentComparison.expression(result).getLast().contains(NamedTextColor.RED.asHexString()));
    }

    @Test
    void closeArgumentsIgnoreCase() {
        final var actual = compiler.compile("<color:RED>a</color:red>b").render();
        final var expected = MiniMessage.miniMessage().deserialize("<red>a</red>b");
        assertEquals(ComponentComparison.expression(expected), ComponentComparison.expression(actual));
    }

    @Test
    void unquotedUrlRetainsItsPort() {
        final var args = Arguments.parse("open_url:https://example.com:8080/path");
        assertEquals(2, args.size());
        assertEquals("https://example.com:8080/path", args.getLast());
    }

    @Test
    void hexadecimalComponentArgumentCompilesAsColor() {
        assertEquals(Component.empty(), compiler.compileArgument("<#ff0000>").render().compact());
    }

    @ParameterizedTest
    @ValueSource(strings = {"<hover>x</hover>", "<hover:show_entity:pig>x</hover>",
        "<hover:show_item:'bad key'>x</hover>", "<hover:show_item:stone:1:custom>x</hover>",
        "<click:change_page:abc>x</click>", "<rainbow:abc>x</rainbow>", "<pride:abc>x</pride>",
        "<pride:2>x</pride>", "<gradient:red:blue:NaN>x</gradient>", "<transition:red:blue:NaN>x</transition>",
        "<color:invalid>x</color>", "<hover:show_item:stone:abc>x</hover>"})
    void malformedStandardTagsRemainLiteral(final @NotNull String input) {
        assertEquals(input, ComponentComparison.plainText(compiler.compile(input).render()));
    }

    @Test
    void nbtInterpretCanChangeBetweenRenders() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> modeKey = layout.key("mode", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("mode", (final var scope) -> Component.text(scope.payload(modeKey)));
        final var tree = new MiniMessageCompiler(registry).compile("<nbt:entity:@a:Inventory:'<mode>'>");
        assertTrue(assertInstanceOf(EntityNBTComponent.class,
            tree.render((final var b) -> b.payload(modeKey, "interpret"))).interpret());
    }

    @Test
    void gradientPreservesNonTextHierarchy() {
        final var value = Component.keybind("key.jump").append(Component.text("child"));
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> valueKey = layout.key("value", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("value", (final var scope) -> scope.payload(valueKey));
        final var actual = new MiniMessageCompiler(registry).compile("<gradient:red:blue><value></gradient>")
            .render((final var b) -> b.payload(valueKey, value));
        final var keybind = assertInstanceOf(KeybindComponent.class, actual.children().getFirst());
        assertEquals("child", ComponentComparison.plainText(Component.text().append(keybind.children()).build()));
    }

    @Test
    void adventurePlaceholdersWorkInScalarArguments() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> cmdKey = layout.key("cmd", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("cmd", (final var scope) -> Component.text(scope.payload(cmdKey)));
        final var tree = new MiniMessageCompiler(registry).compile("<click:run_command:'/<cmd>'>x</click>");
        assertEquals(ClickEvent.runCommand("/go"),
            tree.render((final var b) -> b.payload(cmdKey, "go")).clickEvent());
    }

    @Test
    void externalBridgeKeepsChildrenByDefault() {
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults()
            .registerPlaceholder("custom", (final var scope) -> Component.text("value"));
        final var compiler = new MiniMessageCompiler(registry);
        assertEquals("valuechild", ComponentComparison.plainText(
            compiler.compile("<custom>child</custom>").render()));
        assertEquals("beforevaluechild", ComponentComparison.plainText(
            compiler.compile("before<custom>child</custom>").render()));
    }

    @Test
    void adventureContextCanDeserializeNestedResolvers() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> whoKey = layout.key("who", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("who", (final var scope) -> Component.text(scope.payload(whoKey)))
            .register(new TagSpec.LeafRule(Set.of("nested"), (final var args, final var context) -> {
                final var comp = args.component(0);
                return comp.map((final var values) -> values.component(0));
            }));
        final var tree = new MiniMessageCompiler(registry).compile("<nested:'<who>'>");
        assertEquals("Ada", ComponentComparison.plainText(tree.render((final var b) -> b.payload(whoKey, "Ada"))));
    }

    @Test
    void adventureContextResolvesSiblingTags() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> whoKey = layout.key("who", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("who", (final var scope) -> Component.text(scope.payload(whoKey)))
            .registerPlaceholder("nested", (final var scope) -> Component.text("Ada"));
        final var tree = new MiniMessageCompiler(registry).compile("<nested>");
        assertEquals("Ada", ComponentComparison.plainText(tree.render((final var b) -> b.payload(whoKey, "Ada"))));
    }

    @Test
    void sharedBridgeCacheSupportsConcurrentRenders() throws InterruptedException, ExecutionException {
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults()
            .register(new TagSpec.LeafRule(Set.of("parsed"), (final var args, final var context) -> {
                final var comp = args.component(0);
                return comp.map((final var values) -> values.component(0));
            }));
        final var compiler = new MiniMessageCompiler(registry);
        final @NotNull List<@NotNull Callable<@NotNull String>> tasks = new ArrayList<>();
        for (int i = 0; i < 96; i++) {
            final @NotNull String value = "value" + i;
            final var tree = compiler.compile("<parsed:'<red>" + value + "</red>'>");
            tasks.add(() -> {
                for (int render = 0; render < 10; render++) {
                    assertEquals(value, ComponentComparison.plainText(tree.render()));
                }
                return value;
            });
        }
        try (final @NotNull ExecutorService executor = Executors.newFixedThreadPool(4)) {
            final @NotNull List<@NotNull Future<@NotNull String>> results = executor.invokeAll(tasks);
            for (int i = 0; i < results.size(); i++) assertEquals("value" + i, results.get(i).get());
        }
    }

    @Test
    void indexedTranslationArgumentsWorkInsideScalarArguments() {
        final var store = FastMiniMessageTranslationStore.create(Key.key("test:indexed"));
        store.register("link", Locale.US, "<click:run_command:'/<arg:0>'>Go</click>");
        assertEquals(ClickEvent.runCommand("/home"), store.translate(
            Component.translatable("link", Component.text("home")), Locale.US).clickEvent());
        assertEquals(ClickEvent.runCommand("/spawn"), store.translate(
            Component.translatable("link", Component.text("spawn")), Locale.US).clickEvent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"<rainbow:20>abc</rainbow>", "<rainbow:!4>abc</rainbow>",
        "<rainbow:0.5>abc</rainbow>", "<pride:2>abc</pride>", "<color:invalid>x</color>"})
    void correctedValidationMatchesMiniMessage(final @NotNull String input) {
        assertEquals(ComponentComparison.expression(MiniMessage.miniMessage().deserialize(input)),
            ComponentComparison.expression(compiler.compile(input).render()));
    }

    @Test
    void quotedUrlsStillAllowFollowingArguments() {
        final var args = Arguments.parse("'https://example.com:8080':next");
        assertEquals(2, args.size());
        assertEquals("https://example.com:8080", args.getFirst());
        assertEquals("next", args.getLast());
    }

    @Test
    void adventureArgumentsPreserveLiteralQuotes() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> valueKey = layout.key("value", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("value", (final var scope) -> Component.text(scope.payload(valueKey)))
            .register(new TagSpec.LeafRule(Set.of("echo"), (final var args, final var context) -> {
                final var values = args.values();
                return values.map((final var vals) -> Component.text(vals.string(0, "")));
            }));
        final var tree = new MiniMessageCompiler(registry).compile("<echo:'<value>'>");
        assertEquals("\"quoted\"", ComponentComparison.plainText(tree.render((final var b) ->
            b.payload(valueKey, "\"quoted\""))));
    }

    @Test
    void namedTranslationArgumentsIgnoreCase() {
        final var store = FastMiniMessageTranslationStore.create(Key.key("test:names"));
        store.register("greeting", Locale.US, "Hello <name>");
        assertEquals("Hello Ada", ComponentComparison.plainText(store.translate(
            Component.translatable("greeting", Argument.string("NAME", "Ada")), Locale.US)));
    }

    @Test
    void modifyingTagDoesNotDuplicateChildren() {
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults()
            .registerModifier("tint", (final var content, final var scope) -> content.color(NamedTextColor.AQUA));
        final var tree = new MiniMessageCompiler(registry).compile("<tint>tinted <bold>text</bold></tint>");
        final Component rendered = tree.render();
        assertEquals("tinted text", ComponentComparison.plainText(rendered));
        assertEquals(NamedTextColor.AQUA, rendered.color());
    }

    @Test
    void modifyingTagPreservesDeepChildHierarchyWithoutDuplication() {
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults()
            .registerModifier("tint", (final var content, final var scope) -> content.color(NamedTextColor.AQUA));
        final var tree = new MiniMessageCompiler(registry).compile("<tint>one <bold>two <italic>three</italic></bold> four</tint>");
        final Component rendered = tree.render();
        assertEquals("one two three four", ComponentComparison.plainText(rendered));
        assertEquals(NamedTextColor.AQUA, rendered.color());
    }

    @Test
    void tagArgsComponentAvoidsDoubleDynamicEvaluation() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> playerKey = layout.key("player", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("player", (final var scope) -> scope.payload(playerKey));
        final @NotNull MiniMessageCompiler comp = new MiniMessageCompiler(registry);
        final TagArgs args = new TagArgs("show_text:'Hola <player>'", comp);
        args.component(1);
        final var valuesSource = args.values();
        final var context = RenderScope.builder(registry.payloadSchema())
            .payload(playerKey, Component.text("Ada")).build();
        final TagArgs.Values values = valuesSource.evaluate(context);
        assertEquals("Hola Ada", ComponentComparison.plainText(values.component(1)));
        assertEquals("Hola <player>", values.string(1, ""));
    }

    @Test
    void dynamicComponentTagDoesNotEvaluateScalarStringTemplate() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> nameKey = layout.key("name", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("name", (final var scope) -> scope.payload(nameKey));
        final var tree = new MiniMessageCompiler(registry).compile("<hover:show_text:'User: <name>'>hover me</hover>");
        final Component rendered = tree.render((final var b) -> b.payload(nameKey, Component.text("Ada")));
        final var hover = rendered.hoverEvent();
        assertNotNull(hover);
        assertEquals("User: Ada", ComponentComparison.plainText((Component) hover.value()));
    }

    @Test
    void resetTagInCompileArgumentProducesEmptyTreeInsteadOfSlot() {
        final var tree = compiler.compileArgument("<reset>");
        assertInstanceOf(TemplateNode.Static.class, tree.root());
        assertEquals(Component.empty(), tree.render().compact());

        final var upperTree = compiler.compileArgument("<RESET>");
        assertInstanceOf(TemplateNode.Static.class, upperTree.root());
        assertEquals(Component.empty(), upperTree.render().compact());
    }

    @Test
    void resetArgumentInTagCompilesAsEmptyComponent() {
        final var tree = compiler.compile("<selector:@a:'<reset>'>");
        final Component rendered = tree.render();
        final var selector = assertInstanceOf(SelectorComponent.class, rendered);
        assertEquals(Component.empty(), selector.separator());
    }

    @Test
    void unquotedUrlWithPathDoesNotTreatPathSuffixAsPort() {
        final Arguments args = Arguments.parse("open_url:https://example.com/path:123");
        assertEquals(3, args.size());
        assertEquals("open_url", args.getFirst());
        assertEquals("https://example.com/path", args.get(1, ""));
        assertEquals("123", args.getLast());
    }

    @Test
    void unquotedUrlWithPortAndPathDoesNotTreatPathSuffixAsPort() {
        final Arguments args = Arguments.parse("open_url:https://example.com:8080/path:123");
        assertEquals(3, args.size());
        assertEquals("open_url", args.getFirst());
        assertEquals("https://example.com:8080/path", args.get(1, ""));
        assertEquals("123", args.getLast());
    }

    @Test
    void unquotedUrlWithMultiplePathSegmentsSplitsAtDelimiter() {
        final Arguments args = Arguments.parse("https://example.com/path/to/resource:456");
        assertEquals(2, args.size());
        assertEquals("https://example.com/path/to/resource", args.getFirst());
        assertEquals("456", args.getLast());
    }

    @Test
    void quotedUrlWithPathSplitsAtSubsequentDelimiter() {
        final Arguments args = Arguments.parse("'open_url':https://example.com/path:123");
        assertEquals(3, args.size());
        assertEquals("open_url", args.getFirst());
        assertEquals("https://example.com/path", args.get(1, ""));
        assertEquals("123", args.getLast());
    }

    @ParameterizedTest
    @ValueSource(strings = {"red", "green", "blue", "grey", "dark_grey"})
    void namedColorsRejectArguments(final @NotNull String color) {
        final @NotNull TagSpec.StyleRule rule = (TagSpec.StyleRule) registry.resolve(color);
        final @NotNull IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            rule.rule().apply(new TagArgs("extra", compiler), new CompileContext(color, compiler)));
        assertEquals("Color tag '" + color + "' does not take arguments", exception.getMessage());
        final @NotNull String input = "<" + color + ":extra>text</" + color + ">";
        assertEquals(input, ComponentComparison.plainText(compiler.compile(input).render()));
    }

    @Test
    void decorationRejectsInvalidState() {
        final @NotNull TagSpec.StyleRule rule = (TagSpec.StyleRule) registry.resolve("bold");
        final @NotNull IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            rule.rule().apply(new TagArgs("invalid", compiler), new CompileContext("bold", compiler)));
        assertEquals("Unknown decoration state: invalid", exception.getMessage());
        final @NotNull String input = "<bold:invalid>text</bold>";
        assertEquals(input, ComponentComparison.plainText(compiler.compile(input).render()));
    }

    @Test
    void clickCustomRequiresNonEmptyKey() {
        final @NotNull TagSpec.StyleRule rule = (TagSpec.StyleRule) registry.resolve("click");
        final @NotNull IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            rule.rule().apply(new TagArgs("custom:''", compiler), new CompileContext("click", compiler)));
        assertEquals("A key cannot be empty", exception.getMessage());
        final @NotNull String input = "<click:custom:''>text</click>";
        assertEquals(input, ComponentComparison.plainText(compiler.compile(input).render()));
    }

    @Test
    void entityHoverRequiresNonEmptyKey() {
        final @NotNull TagSpec.StyleRule rule = (TagSpec.StyleRule) registry.resolve("hover");
        final @NotNull IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            rule.rule().apply(new TagArgs("show_entity:'':11111111-1111-1111-1111-111111111111", compiler),
                new CompileContext("hover", compiler)));
        assertEquals("A key cannot be empty", exception.getMessage());
        final @NotNull String input = "<hover:show_entity:'':11111111-1111-1111-1111-111111111111>text</hover>";
        assertEquals(input, ComponentComparison.plainText(compiler.compile(input).render()));
    }

    @Test
    void itemHoverRequiresNonEmptyDataKey() {
        final @NotNull TagSpec.StyleRule rule = (TagSpec.StyleRule) registry.resolve("hover");
        final @NotNull IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            rule.rule().apply(new TagArgs("show_item:diamond:1:'':'{Custom:1}'", compiler),
                new CompileContext("hover", compiler)));
        assertEquals("A key cannot be empty", exception.getMessage());
        final @NotNull String input = "<hover:show_item:diamond:1:'':'{Custom:1}'>text</hover>";
        assertEquals(input, ComponentComparison.plainText(compiler.compile(input).render()));
    }

    @Test
    void spriteRejectsMoreThanTwoArgumentsAndEmptyKeys() {
        final @NotNull TagSpec.LeafRule rule = (TagSpec.LeafRule) registry.resolve("sprite");
        final @NotNull IllegalArgumentException extraEx = assertThrows(IllegalArgumentException.class, () ->
            rule.rule().apply(new TagArgs("a:b:c", compiler), new CompileContext("sprite", compiler)));
        assertEquals("Expected at most two arguments for sprite", extraEx.getMessage());

        final @NotNull IllegalArgumentException atlasEx = assertThrows(IllegalArgumentException.class, () ->
            rule.rule().apply(new TagArgs("atlas:''", compiler), new CompileContext("sprite", compiler)));
        assertEquals("A key cannot be empty", atlasEx.getMessage());

        assertEquals("<sprite:a:b:c>", ComponentComparison.plainText(compiler.compile("<sprite:a:b:c>").render()));
    }

    @Test
    void headRejectsInvalidHatBoolean() {
        final @NotNull TagSpec.LeafRule rule = (TagSpec.LeafRule) registry.resolve("head");
        final @NotNull IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            rule.rule().apply(new TagArgs("steve:invalid", compiler), new CompileContext("head", compiler)));
        assertEquals("Expected boolean for hat", exception.getMessage());

        assertEquals("<head:steve:invalid>",
            ComponentComparison.plainText(compiler.compile("<head:steve:invalid>").render()));
    }

    @Test
    void rainbowRejectsMoreThanOneArgument() {
        final @NotNull TagSpec.TransformRule rule = (TagSpec.TransformRule) registry.resolve("rainbow");
        final @NotNull IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            rule.rule().apply(new TagArgs("1:extra", compiler), new CompileContext("rainbow", compiler)));
        assertEquals("Expected at most one argument for rainbow", exception.getMessage());

        assertEquals("<rainbow:1:extra>text</rainbow>",
            ComponentComparison.plainText(compiler.compile("<rainbow:1:extra>text</rainbow>").render()));
    }

    @Test
    void prideRejectsMoreThanTwoArgumentsAndUnknownFlag() {
        final @NotNull TagSpec.TransformRule rule = (TagSpec.TransformRule) registry.resolve("pride");
        final @NotNull IllegalArgumentException extraEx = assertThrows(IllegalArgumentException.class, () ->
            rule.rule().apply(new TagArgs("trans:0.5:extra", compiler), new CompileContext("pride", compiler)));
        assertEquals("Expected at most two arguments for pride", extraEx.getMessage());

        final @NotNull IllegalArgumentException unknownEx = assertThrows(IllegalArgumentException.class, () ->
            rule.rule().apply(new TagArgs("0.5:unknown_flag", compiler), new CompileContext("pride", compiler)));
        assertEquals("Unknown pride flag: unknown_flag", unknownEx.getMessage());

        assertEquals("<pride:0.5:unknown_flag>text</pride>",
            ComponentComparison.plainText(compiler.compile("<pride:0.5:unknown_flag>text</pride>").render()));
    }

    @Test
    void gradientTransformCachesPaletteForSameLength() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> textKey = layout.key("text", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("text", (final var scope) -> scope.payload(textKey));
        final var tree = new MiniMessageCompiler(registry).compile("<gradient:red:blue><text></gradient>");
        final @NotNull Component first = tree.render((final var b) -> b.payload(textKey, Component.text("Hello World")));
        final @NotNull Component second = tree.render((final var b) -> b.payload(textKey, Component.text("Hello World")));
        assertEquals(first, second);
        assertEquals(11, first.children().size());
        for (int i = 0, size = first.children().size(); i < size; i++) {
            assertSame(first.children().get(i).style(), second.children().get(i).style());
        }
    }

    @Test
    void gradientTransformHandlesLengthChangeAndZeroLength() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> textKey = layout.key("text", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("text", (final var scope) -> scope.payload(textKey));
        final var tree = new MiniMessageCompiler(registry).compile("<gradient:red:blue><text></gradient>");
        final @NotNull Component empty = tree.render((final var b) -> b.payload(textKey, Component.empty()));
        assertEquals("", ComponentComparison.plainText(empty));

        final @NotNull Component shortText = tree.render((final var b) -> b.payload(textKey, Component.text("Hi")));
        assertEquals("Hi", ComponentComparison.plainText(shortText));
        assertEquals(2, shortText.children().size());

        final @NotNull Component sameShortAgain = tree.render((final var b) -> b.payload(textKey, Component.text("Hi")));
        assertSame(shortText.children().getFirst().style(), sameShortAgain.children().getFirst().style());
        assertSame(shortText.children().getLast().style(), sameShortAgain.children().getLast().style());

        final @NotNull Component longText = tree.render((final var b) -> b.payload(textKey, Component.text("Longer text")));
        assertEquals("Longer text", ComponentComparison.plainText(longText));
        assertEquals(11, longText.children().size());

        final @NotNull Component shortAgainAfterLong =
            tree.render((final var b) -> b.payload(textKey, Component.text("Hi")));
        assertSame(shortText.children().getFirst().style(), shortAgainAfterLong.children().getFirst().style());
        assertSame(shortText.children().getLast().style(), shortAgainAfterLong.children().getLast().style());
    }

    @Test
    void rainbowTransformCachesPaletteForSameLength() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> textKey = layout.key("text", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("text", (final var scope) -> scope.payload(textKey));
        final var tree = new MiniMessageCompiler(registry).compile("<rainbow><text></rainbow>");
        final @NotNull Component first = tree.render((final var b) -> b.payload(textKey, Component.text("Rainbow Test")));
        final @NotNull Component second = tree.render((final var b) -> b.payload(textKey, Component.text("Rainbow Test")));
        assertEquals(first, second);
        assertEquals(12, first.children().size());
        for (int i = 0, size = first.children().size(); i < size; i++) {
            assertSame(first.children().get(i).style(), second.children().get(i).style());
        }
    }

    @Test
    void argumentsGetIntFastPath() {
        final @NotNull Arguments singleDigit = Arguments.of("0", "5", "9", "-1", "42", "abc");
        assertEquals(0, singleDigit.getInt(0, -99));
        assertEquals(5, singleDigit.getInt(1, -99));
        assertEquals(9, singleDigit.getInt(2, -99));
        assertEquals(-1, singleDigit.getInt(3, -99));
        assertEquals(42, singleDigit.getInt(4, -99));
        assertEquals(-99, singleDigit.getInt(5, -99));
        assertEquals(-99, singleDigit.getInt(6, -99));
    }

    @Test
    void argumentsParseEscapedTagEnd() {
        final @NotNull Arguments parsed = Arguments.parse("arg1:foo\\>bar:arg3");
        assertEquals(3, parsed.size());
        assertEquals("arg1", parsed.getFirst());
        assertEquals("foo>bar", parsed.get(1));
        assertEquals("arg3", parsed.getLast());
    }

    @Test
    void tagArgsValuesRawArgumentsReturnsScalarsDirectly() {
        final @NotNull TagArgs args = new TagArgs("first:second", compiler);
        final TagArgs.@NotNull Values values = args.values().evaluate(RenderScope.EMPTY);
        assertSame(values.rawArguments(), values.rawArguments());
        assertSame(values.rawArguments(), values.arguments());
        assertEquals("first", values.rawArguments().getFirst());
        assertEquals("second", values.rawArguments().getLast());
    }

    @Test
    void argumentsCursorsAreIndependent() {
        final @NotNull Arguments args = Arguments.parse("first:second:third");
        final Arguments.@NotNull Cursor first = args.cursor();
        final Arguments.@NotNull Cursor second = args.cursor();
        assertEquals("first", first.next());
        assertEquals("first", second.next());
        assertEquals("second", first.next());
        assertEquals("second", second.next());
        assertEquals("third", first.next());
        assertEquals("third", second.next());
    }

    @Test
    void compileArgumentDoesNotConvertToSlotIfTagIsRegistered() {
        final var styleTree = compiler.compileArgument("<red>");
        final @NotNull Component styleComp = styleTree.render();
        assertEquals("", ComponentComparison.plainText(styleComp));
        assertEquals(Component.empty(), styleComp);

        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> slotKey = layout.key("my_slot", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("my_slot", (final var scope) -> scope.payload(slotKey));
        final var slotCompiler = new MiniMessageCompiler(registry);
        final var slottedTree = slotCompiler.compileArgument("<my_slot>");
        final @NotNull Component slotted = slottedTree.render((final var b) ->
            b.payload(slotKey, Component.text("Provided")));
        assertEquals("Provided", ComponentComparison.plainText(slotted));
    }

    @Test
    void gradientTransformHandlesEmptyAndNonTextComponents() {
        final var emptyTree = compiler.compile("<gradient:red:blue></gradient>");
        final @NotNull Component empty = emptyTree.render();
        assertEquals("", ComponentComparison.plainText(empty));

        final var nonTextTree = compiler.compile("<gradient:red:blue><key:key.jump/></gradient>");
        final @NotNull Component nonText = nonTextTree.render();
        assertInstanceOf(KeybindComponent.class, nonText);
        assertEquals(NamedTextColor.RED, nonText.style().color());
    }

    @Test
    void transitionPhasePrecisionAtBoundaries() {
        final var tree1 = compiler.compile("<transition:red:green:blue:1.0>Text</transition>");
        final @NotNull Component comp1 = tree1.render();
        assertEquals(NamedTextColor.BLUE, comp1.style().color());

        final var treeNegative = compiler.compile("<transition:red:green:blue:-1.0>Text</transition>");
        final @NotNull Component compNeg = treeNegative.render();
        assertEquals(NamedTextColor.BLUE, compNeg.style().color());
    }

    @Test
    void legacyFormatParserClosesActiveDecorationsOnColorArrival() {
        assertEquals("<bold>Bold </bold><red>Red", LegacyFormatParser.toMiniMessage("&lBold &cRed"));
        assertEquals("<bold><italic>BI </bold></italic><#ff0000>Hex",
            LegacyFormatParser.toMiniMessage("&l&oBI &#ff0000Hex"));
        assertEquals("<bold><italic>BI </bold></italic><#123456>Hex",
            LegacyFormatParser.toMiniMessage("&l&oBI &x&1&2&3&4&5&6Hex"));
        assertEquals("<bold>Bold </bold><red>Red <italic>Italic",
            LegacyFormatParser.toMiniMessage("&lBold &cRed &oItalic"));
        assertEquals("<bold>Bold <reset>Reset", LegacyFormatParser.toMiniMessage("&lBold &rReset"));
    }

    @Test
    void defaultCompilerRejectsModifierRegistration() {
        assertThrows(UnsupportedOperationException.class, () ->
            MiniMessageCompiler.defaults().registerModifier("custom", (component, viewer) -> component));
    }

    @Test
    void customCompilerAllowsModifierRegistration() {
        final @NotNull MiniMessageCompiler custom = new MiniMessageCompiler(ResolverRegistry.create());
        custom.registerModifier("custom", (component, viewer) -> component);
        assertNotNull(custom.findResolver("custom"));
    }
}
