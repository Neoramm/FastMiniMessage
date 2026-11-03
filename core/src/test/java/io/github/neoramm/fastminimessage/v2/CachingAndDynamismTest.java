package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.api.ComponentModifier;
import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import java.util.List;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.pointer.Pointer;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.SelectorComponent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CachingAndDynamismTest {

    private static final @NotNull Pointer<String> NAME_POINTER =
        Pointer.pointer(String.class, Key.key("fastminimessage", "name"));

    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());

    private static @NotNull Pointered viewer(final @NotNull String name) {
        final Pointers pointers = Pointers.builder().withDynamic(NAME_POINTER, () -> name).build();
        return new Pointered() {
            @Override
            public @NotNull Pointers pointers() {
                return pointers;
            }
        };
    }

    private static @NotNull String plain(final @NotNull Component component) {
        final StringBuilder text = new StringBuilder();
        if (component instanceof final TextComponent textComponent) text.append(textComponent.content());
        for (final Component child : component.children()) text.append(plain(child));
        return text.toString();
    }

    @Test
    void staticTreeCachingConsistencyWithDifferentPointered() {
        final PreComponentTree tree = compiler.compile("<red><bold>Static Message</bold></red>");
        assertTrue(tree.isStatic());
        final Component first = tree.render(PayloadInputs.viewer(viewer("Alice")));
        final Component second = tree.render(PayloadInputs.viewer(viewer("Bob")));
        assertEquals(first, second);
        assertEquals(NamedTextColor.RED, first.style().color());
    }

    @Test
    void dynamicModifierNotCachedAcrossPointered() {
        final ComponentModifier dynamicModifier =
            (final @NotNull Component rendered, final @NotNull RenderScope context) -> {
                final String name = context.viewer() != null
                    ? context.viewer().getOrDefault(NAME_POINTER, "Unknown")
                    : "Nobody";
                return Component.text(plain(rendered) + " [" + name + "]", rendered.style());
            };
        compiler.registerModifier("viewer", dynamicModifier);

        final PreComponentTree tree = compiler.compile("<viewer>Hello</viewer>");
        assertFalse(tree.isStatic());

        final Component forAlice = tree.render(PayloadInputs.viewer(viewer("Alice")));
        assertEquals("Hello [Alice]", plain(forAlice));

        final Component forBob = tree.render(PayloadInputs.viewer(viewer("Bob")));
        assertEquals("Hello [Bob]", plain(forBob));

        final Component forAliceAgain = tree.render(PayloadInputs.viewer(viewer("Alice")));
        assertEquals("Hello [Alice]", plain(forAliceAgain));
    }

    @Test
    void dynamicTagWithPointeredContext() {
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults()
            .registerPlaceholder("greeting", (final var ctx) -> {
                final String name = ctx.viewer() != null ? ctx.viewer().getOrDefault(NAME_POINTER, "Friend") : "Guest";
                return Component.text("Hello " + name);
            });
        final @NotNull PreComponentTree tree = new MiniMessageCompiler(registry).compile("<greeting> welcome!");
        assertFalse(tree.isStatic());

        final Component first = tree.render((final var b) -> b.viewer(viewer("Alice")));
        assertEquals("Hello Alice welcome!", plain(first));

        final Component second = tree.render((final var b) -> b.viewer(viewer("Bob")));
        assertEquals("Hello Bob welcome!", plain(second));

        final Component third = tree.render((final var b) -> b.viewer(null));
        assertEquals("Hello Guest welcome!", plain(third));
    }

    @Test
    void deferredTagWithDynamicArgumentSubstitutionAndChildren() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> themeKey = layout.key("theme", String.class);
        final PayloadSchema.@NotNull Key<@NotNull Component> playerKey = layout.key("player", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("theme", (final var scope) -> Component.text(scope.payload(themeKey)))
            .registerPlaceholder("player", (final var scope) -> scope.payload(playerKey));
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(registry).compile("<color:'<theme>'>Welcome <player>!</color>");
        assertFalse(tree.isStatic());

        final Component first = tree.render((final var b) ->
            b.payload(themeKey, "red").payload(playerKey, Component.text("Alice")));
        assertEquals(NamedTextColor.RED, first.style().color());
        assertEquals("Welcome Alice!", plain(first));

        final Component second = tree.render((final var b) ->
            b.payload(themeKey, "blue").payload(playerKey, Component.text("Bob")));
        assertEquals(NamedTextColor.BLUE, second.style().color());
        assertEquals("Welcome Bob!", plain(second));

        final Component third = tree.render((final var b) ->
            b.payload(themeKey, "red").payload(playerKey, Component.text("Charlie")));
        assertEquals(NamedTextColor.RED, third.style().color());
        assertEquals("Welcome Charlie!", plain(third));
    }

    @Test
    void gradientWithDynamicChildrenLengthVariations() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> playerKey = layout.key("player", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("player", (final var scope) -> scope.payload(playerKey));
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(registry).compile("<gradient:red:blue><player></gradient>");
        assertFalse(tree.isStatic());

        final Component shortName = tree.render((final var b) -> b.payload(playerKey, Component.text("Bo")));
        final Component longName = tree.render((final var b) ->
            b.payload(playerKey, Component.text("A Very Long Player Name That Changes Length")));
        final Component shortAgain = tree.render((final var b) -> b.payload(playerKey, Component.text("Bo")));

        assertEquals("Bo", plain(shortName));
        assertEquals("A Very Long Player Name That Changes Length", plain(longName));
        assertEquals("Bo", plain(shortAgain));

        final List<String> shortTokens = ComponentComparison.expression(shortName);
        final List<String> longTokens = ComponentComparison.expression(longName);
        final List<String> shortAgainTokens = ComponentComparison.expression(shortAgain);

        assertEquals(shortTokens, shortAgainTokens);
        assertNotEquals(shortTokens, longTokens);
    }

    @Test
    void scopeTransitionsFromNonEmptyToEmptyAndBack() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> prefixKey = layout.key("prefix", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("prefix", (final var scope) -> scope.payload(prefixKey));
        final @NotNull PreComponentTree tree = new MiniMessageCompiler(registry).compile("<prefix> Message");
        assertFalse(tree.isStatic());

        final Component first = tree.render((final var b) -> b.payload(prefixKey, Component.text("[Admin]")));
        assertEquals("[Admin] Message", plain(first));

        final Component second = tree.render((final var b) -> b.payload(prefixKey, Component.text("[Mod]")));
        assertEquals("[Mod] Message", plain(second));
    }

    @Test
    void deepCopyIsolationOfCachedResolution() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> cKey = layout.key("c", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("c", (final var scope) -> Component.text(scope.payload(cKey)));
        final @NotNull PreComponentTree original =
            new MiniMessageCompiler(registry).compile("<color:'<c>'>Sample Text</color>");
        final Component red = original.render((final var b) -> b.payload(cKey, "red"));
        assertEquals(NamedTextColor.RED, red.style().color());

        final PreComponentTree copy = original.copy();
        final Component blue = copy.render((final var b) -> b.payload(cKey, "blue"));
        assertEquals(NamedTextColor.BLUE, blue.style().color());

        final Component redAgain = original.render((final var b) -> b.payload(cKey, "red"));
        assertEquals(NamedTextColor.RED, redAgain.style().color());
    }

    @Test
    void multiThreadedConcurrentRendersWithDifferentScopes() throws InterruptedException {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> colKey = layout.key("col", String.class);
        final PayloadSchema.@NotNull Key<@NotNull Component> playerKey = layout.key("player", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("col", (final var scope) -> Component.text(scope.payload(colKey)))
            .registerPlaceholder("player", (final var scope) -> scope.payload(playerKey));
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(registry).compile("<color:'<col>'><player></color>");
        final int threadCount = 8;
        final int iterations = 500;
        final Thread @NotNull [] threads = new Thread[threadCount];
        final boolean[] results = new boolean[threadCount];

        for (int t = 0; t < threadCount; t++) {
            final int threadIndex = t;
            final String expectedName = "Player_" + t;
            final String colorName = (t % 2 == 0) ? "green" : "yellow";
            final NamedTextColor expectedColor = (t % 2 == 0) ? NamedTextColor.GREEN : NamedTextColor.YELLOW;

            threads[t] = new Thread(() -> {
                boolean ok = true;
                for (int i = 0; i < iterations; i++) {
                    final Component rendered = tree.render((final var b) ->
                        b.payload(colKey, colorName).payload(playerKey, Component.text(expectedName)));
                    if (!expectedName.equals(plain(rendered)) || !expectedColor.equals(rendered.style().color())) {
                        ok = false;
                        break;
                    }
                }
                results[threadIndex] = ok;
            });
        }

        for (final Thread thread : threads) thread.start();
        for (final Thread thread : threads) thread.join();
        for (int t = 0; t < threadCount; t++) assertTrue(results[t], "Thread " + t + " encountered race condition");
    }

    @Test
    void selectorWithDynamicSeparatorUpdatesPerRender() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> sepKey = layout.key("sep", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("sep", (final var scope) -> Component.text(scope.payload(sepKey)));
        final @NotNull PreComponentTree tree = new MiniMessageCompiler(registry).compile("<selector:@a:'<sep>'>");
        assertFalse(tree.isStatic());

        final SelectorComponent first = (SelectorComponent) tree.render((final var b) -> b.payload(sepKey, ", "));
        assertEquals(", ", plain(first.separator()));

        final SelectorComponent second = (SelectorComponent) tree.render((final var b) -> b.payload(sepKey, " | "));
        assertEquals(" | ", plain(second.separator()));

        final SelectorComponent third = (SelectorComponent) tree.render((final var b) -> b.payload(sepKey, ", "));
        assertEquals(", ", plain(third.separator()));
    }

    @Test
    void hoverWithDynamicPlaceholderUpdatesPerRender() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> playerKey = layout.key("player", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("player", (final var scope) -> scope.payload(playerKey));
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(registry).compile("<hover:show_text:'Hello <player>!'>Hover me</hover>");
        assertFalse(tree.isStatic());

        final Component forAlice = tree.render((final var b) -> b.payload(playerKey, Component.text("Alice")));
        final @Nullable HoverEvent<?> hoverAlice = forAlice.hoverEvent();
        assertNotNull(hoverAlice);
        assertEquals("Hello Alice!", plain((Component) hoverAlice.value()));

        final Component forBob = tree.render((final var b) -> b.payload(playerKey, Component.text("Bob")));
        final @Nullable HoverEvent<?> hoverBob = forBob.hoverEvent();
        assertNotNull(hoverBob);
        assertEquals("Hello Bob!", plain((Component) hoverBob.value()));
    }

    @Test
    void staticHoverIsRecognizedAsStaticAndCached() {
        final PreComponentTree tree = compiler.compile("<hover:show_text:'Static Tooltip'>Static text</hover>");
        assertTrue(tree.isStatic());

        final Component first = tree.render();
        final Component second = tree.render();
        assertEquals(first, second);
    }

    @Test
    void welcomeMessageResolvesPlayerFromPointeredWithoutExplicitPlaceholder() {
        final PreComponentTree tree = compiler.compile("<green>Welcome <player> to the server!</green>");
        final Pointered player = new Pointered() {
            @Override
            public @NotNull Pointers pointers() {
                return Pointers.builder().withDynamic(Identity.NAME, () -> "Steve").build();
            }
        };
        final Component rendered = tree.render(PayloadInputs.viewer(player));
        assertEquals("Welcome Steve to the server!", plain(rendered));
    }
}
