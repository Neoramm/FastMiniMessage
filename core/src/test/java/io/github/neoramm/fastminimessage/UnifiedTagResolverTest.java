package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.api.ComponentModifier;
import io.github.neoramm.fastminimessage.example.ComponentModifiers;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import io.github.neoramm.fastminimessage.model.TagResolver;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UnifiedTagResolverTest {

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

    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
    private final @NotNull MiniMessage miniMessage = MiniMessage.miniMessage();

    @Test
    void unclosedModifierWithUnclosedPlaceholder() {
        final PreComponentTree tree = compiler.compile("<shout>alerta: <player> conectado");
        final Component rendered = tree.render(
            NOBODY,
            TagResolver.modifying("shout", ComponentModifiers.uppercase()),
            TagResolver.inserting("player", "Alice")
        );
        assertEquals("ALERTA: ALICE CONECTADO", ComponentComparison.plainText(rendered));
    }

    @Test
    void unclosedPlaceholderWithUnclosedModifier() {
        final PreComponentTree tree = compiler.compile("<prefix> atencion: <shout>cuidado");
        final Component rendered = tree.render(
            NOBODY,
            TagResolver.inserting("prefix", "[INFO]"),
            TagResolver.modifying("shout", ComponentModifiers.uppercase())
        );
        assertEquals("[INFO] atencion: CUIDADO", ComponentComparison.plainText(rendered));
    }

    @Test
    void unclosedNestedModifiers() {
        final ComponentModifier bracketModifier = (final @NotNull Component rendered, final @Nullable Pointered ctx) ->
            Component.text("[").append(rendered).append(Component.text("]"));
        final PreComponentTree tree = compiler.compile("<bracket><shout>hello <player>");
        final Component rendered = tree.render(
            NOBODY,
            TagResolver.modifying("bracket", bracketModifier),
            TagResolver.modifying("shout", ComponentModifiers.uppercase()),
            TagResolver.inserting("player", "bob")
        );
        assertEquals("[HELLO BOB]", ComponentComparison.plainText(rendered));
    }

    @Test
    void unclosedTagCutOffByReset() {
        final PreComponentTree tree = compiler.compile("<shout>mayusculas <reset>minusculas <player>");
        final Component rendered = tree.render(
            NOBODY,
            TagResolver.modifying("shout", ComponentModifiers.uppercase()),
            TagResolver.inserting("player", "charlie")
        );
        assertEquals("MAYUSCULAS minusculas charlie", ComponentComparison.plainText(rendered));
    }

    @Test
    void explicitlyClosedModifierInsideStyledBlock() {
        final PreComponentTree tree = compiler.compile("<blue><shout>contenido</shout> fuera</blue>");
        final Component rendered = tree.render(
            NOBODY,
            TagResolver.modifying("shout", ComponentModifiers.uppercase())
        );
        assertEquals("CONTENIDO fuera", ComponentComparison.plainText(rendered));
        for (final String token : ComponentComparison.expression(rendered)) {
            assertTrue(token.contains(NamedTextColor.BLUE.asHexString()), token);
        }
    }

    @Test
    void unclosedUnknownTagLiteralFallback() {
        final PreComponentTree tree = compiler.compile("<unknown> texto normal <player>");
        final Component rendered = tree.render(NOBODY, TagResolver.inserting("player", "david"));
        assertEquals("<unknown> texto normal david", ComponentComparison.plainText(rendered));
    }

    @Test
    void explicitlyClosedUnknownTagLiteralFallback() {
        final PreComponentTree tree = compiler.compile("<unknown>texto</unknown> fuera");
        final Component rendered = tree.render(NOBODY);
        assertEquals("<unknown>texto</unknown> fuera", ComponentComparison.plainText(rendered));
    }

    @Test
    void selfClosingUnknownTagLiteralFallback() {
        final PreComponentTree tree = compiler.compile("<unknown/> texto");
        final Component rendered = tree.render(NOBODY);
        assertEquals("<unknown/> texto", ComponentComparison.plainText(rendered));
    }

    @Test
    void dynamicTagWithMultipleArguments() {
        final PreComponentTree tree = compiler.compile("<badge:admin:gold> <player> ha entrado");
        final Component rendered = tree.render(
            NOBODY,
            TagResolver.dynamic("badge", (final @NotNull Arguments args, final @Nullable Pointered ctx) -> {
                final String role = args.getFirst("default").toUpperCase();
                final String tier = args.get(1, "none").toUpperCase();
                return Component.text("[" + role + "-" + tier + "]");
            }),
            TagResolver.inserting("player", "eve")
        );
        assertEquals("[ADMIN-GOLD] eve ha entrado", ComponentComparison.plainText(rendered));
    }

    @Test
    void dynamicTagWithContext() {
        final PreComponentTree tree = compiler.compile("<greet>");
        final TagResolver resolver = TagResolver.dynamic("greet",
            (final @NotNull Arguments args, final @Nullable Pointered ctx) ->
                Component.text("Hello " + (ctx == USER ? "user" : "guest")));

        final Component withUser = tree.render(USER, resolver);
        assertEquals("Hello user", ComponentComparison.plainText(withUser));

        final Component withNobody = tree.render(NOBODY, resolver);
        assertEquals("Hello guest", ComponentComparison.plainText(withNobody));
    }

    @Test
    void unclosedPlaceholderWithGradientAndColor() {
        final PreComponentTree tree =
            compiler.compile("<gradient:red:blue><prefix> texto</gradient> <yellow><player> sin cerrar");
        final Component rendered = tree.render(
            NOBODY,
            TagResolver.inserting("prefix", "[TEST]"),
            TagResolver.inserting("player", "frank")
        );
        assertEquals("[TEST] texto frank sin cerrar", ComponentComparison.plainText(rendered));
    }

    @Test
    void caseInsensitiveResolutionVariants() {
        final PreComponentTree tree = compiler.compile("<my_tag> <MY_TAG> <My_Tag>");
        final Component rendered = tree.render(
            NOBODY,
            TagResolver.resolver("my_tag", Component.text("OK"))
        );
        assertEquals("OK OK OK", ComponentComparison.plainText(rendered));
    }

    @Test
    void renderScopeWithTagResolverList() {
        final PreComponentTree tree = compiler.compile("<first> <second>");
        final List<TagResolver> list = List.of(
            TagResolver.resolver("first", "1"),
            TagResolver.resolver("second", Component.text("2"))
        );
        final Component rendered = tree.render(NOBODY, list);
        assertEquals("1 2", ComponentComparison.plainText(rendered));
    }

    @Test
    void unclosedPlaceholderAtBeginningAndEnd() {
        final PreComponentTree tree = compiler.compile("<first> middle <last>");
        final Component rendered = tree.render(
            NOBODY,
            TagResolver.inserting("first", "START"),
            TagResolver.inserting("last", "END")
        );
        assertEquals("START middle END", ComponentComparison.plainText(rendered));
    }

    @Test
    void emptyUnclosedPlaceholder() {
        final PreComponentTree tree = compiler.compile("<empty>text");
        final Component rendered = tree.render(
            NOBODY,
            TagResolver.inserting("empty", Component.empty())
        );
        assertEquals("text", ComponentComparison.plainText(rendered));
    }

    @Test
    void concurrentRenderingWithDifferentScopes() throws InterruptedException {
        final PreComponentTree tree = compiler.compile("<prefix> <player>: <message>");
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
                    final Component rendered = tree.render(
                        NOBODY,
                        TagResolver.inserting("prefix", "[Chat]"),
                        TagResolver.inserting("player", player),
                        TagResolver.inserting("message", message)
                    );
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
