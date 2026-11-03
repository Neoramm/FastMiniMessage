package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.api.ComponentModifier;
import io.github.neoramm.fastminimessage.v2.example.ComponentModifiers;
import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import io.github.neoramm.fastminimessage.v2.tag.TagSpec;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RuntimeTagBridgeCompatibilityTest {

    private static final @NotNull Pointered NOBODY = new Pointered() {
        @Override
        public @NotNull Pointers pointers() {
            return Pointers.empty();
        }
    };

    private static final @NotNull Pointered USER = new Pointered() {
        @Override
        public @NotNull Pointers pointers() {
            return Pointers.empty();
        }
    };

    @Test
    void unclosedModifierWithUnclosedPlaceholder() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> playerKey = layout.key("player", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("player", (final var scope) -> scope.payload(playerKey))
            .registerModifier("shout", ComponentModifiers.uppercase());
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(registry).compile("<shout>alerta: <player> conectado");
        final @NotNull Component rendered = tree.render((final var b) ->
            b.viewer(NOBODY).payload(playerKey, Component.text("Alice")));
        assertEquals("ALERTA: ALICE CONECTADO", ComponentComparison.plainText(rendered));
    }

    @Test
    void unclosedPlaceholderWithUnclosedModifier() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> prefixKey = layout.key("prefix", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("prefix", (final var scope) -> scope.payload(prefixKey))
            .registerModifier("shout", ComponentModifiers.uppercase());
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(registry).compile("<prefix> atencion: <shout>cuidado");
        final @NotNull Component rendered = tree.render((final var b) ->
            b.viewer(NOBODY).payload(prefixKey, Component.text("[INFO]")));
        assertEquals("[INFO] atencion: CUIDADO", ComponentComparison.plainText(rendered));
    }

    @Test
    void unclosedNestedModifiers() {
        final @NotNull ComponentModifier bracketModifier =
            (final @NotNull Component rendered, final @NotNull RenderScope ctx) ->
                Component.text("[").append(rendered).append(Component.text("]"));
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> playerKey = layout.key("player", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("player", (final var scope) -> scope.payload(playerKey))
            .registerModifier("bracket", bracketModifier)
            .registerModifier("shout", ComponentModifiers.uppercase());
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(registry).compile("<bracket><shout>hello <player>");
        final @NotNull Component rendered = tree.render((final var b) ->
            b.viewer(NOBODY).payload(playerKey, Component.text("bob")));
        assertEquals("[HELLO BOB]", ComponentComparison.plainText(rendered));
    }

    @Test
    void unclosedTagCutOffByReset() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> playerKey = layout.key("player", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("player", (final var scope) -> scope.payload(playerKey))
            .registerModifier("shout", ComponentModifiers.uppercase());
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(registry).compile("<shout>mayusculas <reset>minusculas <player>");
        final @NotNull Component rendered = tree.render((final var b) ->
            b.viewer(NOBODY).payload(playerKey, Component.text("charlie")));
        assertEquals("MAYUSCULAS minusculas charlie", ComponentComparison.plainText(rendered));
    }

    @Test
    void explicitlyClosedModifierInsideStyledBlock() {
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults()
            .registerModifier("shout", ComponentModifiers.uppercase());
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(registry).compile("<blue><shout>contenido</shout> fuera</blue>");
        final @NotNull Component rendered = tree.render((final var b) -> b.viewer(NOBODY));
        assertEquals("CONTENIDO fuera", ComponentComparison.plainText(rendered));
        for (final String token : ComponentComparison.expression(rendered)) {
            assertTrue(token.contains(NamedTextColor.BLUE.asHexString()), token);
        }
    }

    @Test
    void unclosedUnknownTagLiteralFallback() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> playerKey = layout.key("player", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("player", (final var scope) -> scope.payload(playerKey));
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(registry).compile("<unknown> texto normal <player>");
        final @NotNull Component rendered = tree.render((final var b) ->
            b.viewer(NOBODY).payload(playerKey, Component.text("david")));
        assertEquals("<unknown> texto normal david", ComponentComparison.plainText(rendered));
    }

    @Test
    void explicitlyClosedUnknownTagLiteralFallback() {
        final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
        final @NotNull PreComponentTree tree = compiler.compile("<unknown>texto</unknown> fuera");
        final @NotNull Component rendered = tree.render();
        assertEquals("<unknown>texto</unknown> fuera", ComponentComparison.plainText(rendered));
    }

    @Test
    void selfClosingUnknownTagLiteralFallback() {
        final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
        final @NotNull PreComponentTree tree = compiler.compile("<unknown/> texto");
        final @NotNull Component rendered = tree.render();
        assertEquals("<unknown/> texto", ComponentComparison.plainText(rendered));
    }

    @Test
    void dynamicTagWithMultipleArguments() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> playerKey = layout.key("player", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("player", (final var scope) -> scope.payload(playerKey))
            .register(new TagSpec.LeafRule(Set.of("badge"), (final var args, final var context) -> {
                final var values = args.values();
                return values.map((final var vals) -> {
                    final String role = vals.string(0, "default").toUpperCase(Locale.ROOT);
                    final String tier = vals.string(1, "none").toUpperCase(Locale.ROOT);
                    return Component.text("[" + role + "-" + tier + "]");
                });
            }));
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(registry).compile("<badge:admin:gold> <player> ha entrado");
        final @NotNull Component rendered = tree.render((final var b) ->
            b.viewer(NOBODY).payload(playerKey, Component.text("eve")));
        assertEquals("[ADMIN-GOLD] eve ha entrado", ComponentComparison.plainText(rendered));
    }

    @Test
    void dynamicTagWithContext() {
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults()
            .registerPlaceholder("greet", (final var scope) ->
                Component.text("Hello " + (scope.viewer() == USER ? "user" : "guest")));
        final @NotNull PreComponentTree tree = new MiniMessageCompiler(registry).compile("<greet>");

        final @NotNull Component withUser = tree.render((final var b) -> b.viewer(USER));
        assertEquals("Hello user", ComponentComparison.plainText(withUser));

        final @NotNull Component withNobody = tree.render((final var b) -> b.viewer(NOBODY));
        assertEquals("Hello guest", ComponentComparison.plainText(withNobody));
    }

    @Test
    void unclosedPlaceholderWithGradientAndColor() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> prefixKey = layout.key("prefix", Component.class);
        final PayloadSchema.@NotNull Key<@NotNull Component> playerKey = layout.key("player", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("prefix", (final var scope) -> scope.payload(prefixKey))
            .registerPlaceholder("player", (final var scope) -> scope.payload(playerKey));
        final @NotNull PreComponentTree tree = new MiniMessageCompiler(registry)
            .compile("<gradient:red:blue><prefix> texto</gradient> <yellow><player> sin cerrar");
        final @NotNull Component rendered = tree.render((final var b) -> b.viewer(NOBODY)
            .payload(prefixKey, Component.text("[TEST]"))
            .payload(playerKey, Component.text("frank")));
        assertEquals("[TEST] texto frank sin cerrar", ComponentComparison.plainText(rendered));
    }

    @Test
    void caseInsensitiveResolutionVariants() {
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults()
            .registerPlaceholder("my_tag", (final var scope) -> Component.text("OK"));
        final @NotNull PreComponentTree tree = new MiniMessageCompiler(registry).compile("<my_tag> <MY_TAG> <My_Tag>");
        final @NotNull Component rendered = tree.render((final var b) -> b.viewer(NOBODY));
        assertEquals("OK OK OK", ComponentComparison.plainText(rendered));
    }

    @Test
    void renderScopeWithMultiplePlaceholders() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> firstKey = layout.key("first", Component.class);
        final PayloadSchema.@NotNull Key<@NotNull Component> secondKey = layout.key("second", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("first", (final var scope) -> scope.payload(firstKey))
            .registerPlaceholder("second", (final var scope) -> scope.payload(secondKey));
        final @NotNull PreComponentTree tree = new MiniMessageCompiler(registry).compile("<first> <second>");
        final @NotNull Component rendered = tree.render((final var b) -> b.viewer(NOBODY)
            .payload(firstKey, Component.text("1"))
            .payload(secondKey, Component.text("2")));
        assertEquals("1 2", ComponentComparison.plainText(rendered));
    }

    @Test
    void unclosedPlaceholderAtBeginningAndEnd() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> firstKey = layout.key("first", Component.class);
        final PayloadSchema.@NotNull Key<@NotNull Component> lastKey = layout.key("last", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("first", (final var scope) -> scope.payload(firstKey))
            .registerPlaceholder("last", (final var scope) -> scope.payload(lastKey));
        final @NotNull PreComponentTree tree = new MiniMessageCompiler(registry).compile("<first> middle <last>");
        final @NotNull Component rendered = tree.render((final var b) -> b.viewer(NOBODY)
            .payload(firstKey, Component.text("START"))
            .payload(lastKey, Component.text("END")));
        assertEquals("START middle END", ComponentComparison.plainText(rendered));
    }

    @Test
    void emptyUnclosedPlaceholder() {
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults()
            .registerPlaceholder("empty", (final var scope) -> Component.empty());
        final @NotNull PreComponentTree tree = new MiniMessageCompiler(registry).compile("<empty>text");
        final @NotNull Component rendered = tree.render((final var b) -> b.viewer(NOBODY));
        assertEquals("text", ComponentComparison.plainText(rendered));
    }

    @Test
    void concurrentRenderingWithDifferentScopes() throws InterruptedException {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> prefixKey = layout.key("prefix", Component.class);
        final PayloadSchema.@NotNull Key<@NotNull Component> playerKey = layout.key("player", Component.class);
        final PayloadSchema.@NotNull Key<@NotNull Component> messageKey = layout.key("message", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("prefix", (final var scope) -> scope.payload(prefixKey))
            .registerPlaceholder("player", (final var scope) -> scope.payload(playerKey))
            .registerPlaceholder("message", (final var scope) -> scope.payload(messageKey));
        final @NotNull PreComponentTree tree =
            new MiniMessageCompiler(registry).compile("<prefix> <player>: <message>");
        final int threadCount = 8;
        final int iterations = 500;
        final Thread @NotNull [] threads = new Thread[threadCount];
        final boolean[] success = new boolean[threadCount];

        for (int t = 0; t < threadCount; t++) {
            final int threadIndex = t;
            final String player = "Player" + t;
            final String message = "Message" + t;
            final String expected = "[Chat] " + player + ": " + message;
            threads[t] = new Thread(() -> {
                boolean ok = true;
                for (int i = 0; i < iterations; i++) {
                    final Component rendered = tree.render((final var b) -> b.viewer(NOBODY)
                        .payload(prefixKey, Component.text("[Chat]"))
                        .payload(playerKey, Component.text(player))
                        .payload(messageKey, Component.text(message)));
                    if (!expected.equals(ComponentComparison.plainText(rendered))) {
                        ok = false;
                        break;
                    }
                }
                success[threadIndex] = ok;
            });
        }

        for (final Thread t : threads) t.start();
        for (final Thread t : threads) t.join();
        for (int t = 0; t < threadCount; t++) assertTrue(success[t], "Thread " + t + " failed");
    }
}
