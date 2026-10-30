package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.api.ComponentModifier;
import io.github.neoramm.fastminimessage.model.Placeholder;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import io.github.neoramm.fastminimessage.model.TagResolver;
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

import java.util.List;
import java.util.Optional;

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
        final Component first = tree.render(viewer("Alice"));
        final Component second = tree.render(viewer("Bob"));
        assertEquals(first, second);
        assertEquals(NamedTextColor.RED, first.style().color());
    }

    @Test
    void dynamicModifierNotCachedAcrossPointered() {
        final ComponentModifier dynamicModifier =
            (final @NotNull Component rendered, final @Nullable Pointered context) -> {
                final String name = context != null
                    ? context.getOrDefault(NAME_POINTER, "Unknown")
                    : "Nobody";
                return Component.text(plain(rendered) + " [" + name + "]", rendered.style());
            };
        compiler.registerModifier("viewer", dynamicModifier);

        final PreComponentTree tree = compiler.compile("<viewer>Hello</viewer>");
        assertFalse(tree.isStatic());

        final Component forAlice = tree.render(viewer("Alice"));
        assertEquals("Hello [Alice]", plain(forAlice));

        final Component forBob = tree.render(viewer("Bob"));
        assertEquals("Hello [Bob]", plain(forBob));

        final Component forAliceAgain = tree.render(viewer("Alice"));
        assertEquals("Hello [Alice]", plain(forAliceAgain));
    }

    @Test
    void dynamicTagWithPointeredContext() {
        final PreComponentTree tree = compiler.compile("<greeting> welcome!");
        assertFalse(tree.isStatic());

        final TagResolver dynamicGreeting = TagResolver.dynamic("greeting", (args, ctx) -> {
            final String name = ctx != null ? ctx.getOrDefault(NAME_POINTER, "Friend") : "Guest";
            return Component.text("Hello " + name);
        });

        final Component first = tree.render(viewer("Alice"), dynamicGreeting);
        assertEquals("Hello Alice welcome!", plain(first));

        final Component second = tree.render(viewer("Bob"), dynamicGreeting);
        assertEquals("Hello Bob welcome!", plain(second));

        final Component third = tree.render(null, dynamicGreeting);
        assertEquals("Hello Guest welcome!", plain(third));
    }

    @Test
    void deferredTagWithDynamicArgumentSubstitutionAndChildren() {
        final PreComponentTree tree = compiler.compile("<color:'<theme>'>Welcome <player>!</color>");
        assertFalse(tree.isStatic());

        final Component first = tree.render(
            null,
            Placeholder.ofUnparsed("theme", "red"),
            Placeholder.ofUnparsed("player", "Alice")
        );
        assertEquals(NamedTextColor.RED, first.style().color());
        assertEquals("Welcome Alice!", plain(first));

        final Component second = tree.render(
            null,
            Placeholder.ofUnparsed("theme", "blue"),
            Placeholder.ofUnparsed("player", "Bob")
        );
        assertEquals(NamedTextColor.BLUE, second.style().color());
        assertEquals("Welcome Bob!", plain(second));

        final Component third = tree.render(
            null,
            Placeholder.ofUnparsed("theme", "red"),
            Placeholder.ofUnparsed("player", "Charlie")
        );
        assertEquals(NamedTextColor.RED, third.style().color());
        assertEquals("Welcome Charlie!", plain(third));
    }

    @Test
    void gradientWithDynamicChildrenLengthVariations() {
        final PreComponentTree tree = compiler.compile("<gradient:red:blue><player></gradient>");
        assertFalse(tree.isStatic());

        final Component shortName = tree.render(null, Placeholder.ofUnparsed("player", "Bo"));
        final Component longName = tree.render(
            null,
            Placeholder.ofUnparsed("player", "A Very Long Player Name That Changes Length")
        );
        final Component shortAgain = tree.render(null, Placeholder.ofUnparsed("player", "Bo"));

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
        final PreComponentTree tree = compiler.compile("<prefix> Message");
        assertFalse(tree.isStatic());

        final Component first = tree.render(null, Placeholder.ofUnparsed("prefix", "[Admin]"));
        assertEquals("[Admin] Message", plain(first));

        final Component fallback = tree.render(null);
        assertEquals("<prefix> Message", plain(fallback));

        final Component second = tree.render(null, Placeholder.ofUnparsed("prefix", "[Mod]"));
        assertEquals("[Mod] Message", plain(second));

        final Component fallbackAgain = tree.render(null);
        assertEquals("<prefix> Message", plain(fallbackAgain));
    }

    @Test
    void deepCopyIsolationOfCachedResolution() {
        final PreComponentTree original = compiler.compile("<color:'<c>'>Sample Text</color>");
        final Component red = original.render(null, Placeholder.ofUnparsed("c", "red"));
        assertEquals(NamedTextColor.RED, red.style().color());

        final PreComponentTree copy = original.copy();
        final Component blue = copy.render(null, Placeholder.ofUnparsed("c", "blue"));
        assertEquals(NamedTextColor.BLUE, blue.style().color());

        final Component redAgain = original.render(null, Placeholder.ofUnparsed("c", "red"));
        assertEquals(NamedTextColor.RED, redAgain.style().color());
    }

    @Test
    void multiThreadedConcurrentRendersWithDifferentScopes() throws InterruptedException {
        final PreComponentTree tree = compiler.compile("<color:'<col>'><player></color>");
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
                    final Component rendered = tree.render(
                        null,
                        Placeholder.ofUnparsed("col", colorName),
                        Placeholder.ofUnparsed("player", expectedName)
                    );
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
        final PreComponentTree tree = compiler.compile("<selector:@a:'<sep>'>");
        assertFalse(tree.isStatic());

        final SelectorComponent first = (SelectorComponent) tree.render(
            null,
            Placeholder.ofUnparsed("sep", ", ")
        );
        assertEquals(", ", plain(first.separator()));

        final SelectorComponent second = (SelectorComponent) tree.render(
            null,
            Placeholder.ofUnparsed("sep", " | ")
        );
        assertEquals(" | ", plain(second.separator()));

        final SelectorComponent third = (SelectorComponent) tree.render(
            null,
            Placeholder.ofUnparsed("sep", ", ")
        );
        assertEquals(", ", plain(third.separator()));
    }

    @Test
    void hoverWithDynamicPlaceholderUpdatesPerRender() {
        final PreComponentTree tree = compiler.compile("<hover:show_text:'Hello <player>!'>Hover me</hover>");
        assertFalse(tree.isStatic());

        final Component forAlice = tree.render(null, Placeholder.ofUnparsed("player", "Alice"));
        final @Nullable HoverEvent<?> hoverAlice = forAlice.hoverEvent();
        assertNotNull(hoverAlice);
        assertEquals("Hello Alice!", plain((Component) hoverAlice.value()));

        final Component forBob = tree.render(null, Placeholder.ofUnparsed("player", "Bob"));
        final @Nullable HoverEvent<?> hoverBob = forBob.hoverEvent();
        assertNotNull(hoverBob);
        assertEquals("Hello Bob!", plain((Component) hoverBob.value()));
    }

    @Test
    void staticHoverIsRecognizedAsStaticAndCached() {
        final PreComponentTree tree = compiler.compile("<hover:show_text:'Static Tooltip'>Static text</hover>");
        assertTrue(tree.isStatic());

        final Component first = tree.render(null);
        final Component second = tree.render(null);
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
        final Component rendered = tree.render(player);
        assertEquals("Welcome Steve to the server!", plain(rendered));
    }
}
