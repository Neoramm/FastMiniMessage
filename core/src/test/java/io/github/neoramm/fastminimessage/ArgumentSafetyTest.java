package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.model.Placeholder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.SelectorComponent;
import net.kyori.adventure.text.EntityNBTComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class ArgumentSafetyTest {
    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());

    @Test
    void scalarArgumentsRetainDelimiters() {
        final @NotNull String value = "say O'Brien: \"hello\" \\\\path";
        final var result = compiler.compile("<click:suggest_command:'<value>'>text</click>")
            .render(null, Placeholder.ofUnparsed("value", value));
        assertEquals(Component.text("text").clickEvent(
            ClickEvent.suggestCommand(value)), result);
    }

    @Test
    void translationArgumentsRemainLiteralWithDynamicKeys() {
        final var result = (TranslatableComponent) compiler.compile("<tr:'<key>':'<value>'>")
            .render(null, Placeholder.ofUnparsed("key", "custom.key"),
                Placeholder.ofUnparsed("value", "<red>literal"));
        assertEquals("custom.key", result.key());
        assertEquals(Component.text("<red>literal"), result.arguments().getFirst().asComponent());
    }

    @Test
    void selectorsReuseStaticCacheWithUnusedScope() {
        final var tree = compiler.compile("<selector:@a>");
        assertSame(tree.render(), tree.render(null, Placeholder.ofUnparsed("unused", "value")));
    }
    @Test
    void inspectionPreservesCachedOutput() {
        final var tree = compiler.compile("<gradient:red:blue>hello</gradient>");
        final var cached = tree.render();
        tree.inspect((final var snapshot) -> {
            if (snapshot.literalText() != null) assertEquals("hello", snapshot.literalText());
        });
        assertSame(cached, tree.render());
    }

    @Test
    void componentSeparatorsPreserveLiteralText() {
        final var replacement = Placeholder.ofUnparsed("separator", "<red>:");
        final var selector = (SelectorComponent) compiler.compile("<selector:@a:'<separator>'>")
            .render(null, replacement);
        final var nbt = (EntityNBTComponent) compiler.compile("<nbt:entity:@a:Inventory:'<separator>'>")
            .render(null, replacement);
        assertEquals(Component.text("<red>:"), selector.separator());
        assertEquals(Component.text("<red>:"), nbt.separator());
    }

    @Test
    void fallbackTranslationRetainsComponentStyles() {
        final var value = Component.text("styled", NamedTextColor.BLUE);
        final var result = (TranslatableComponent) compiler.compile("<tr_or:key:'fallback':'<value>'>")
            .render(null, Placeholder.ofParsed("value", value));
        assertEquals(value, result.arguments().getFirst().asComponent());
        assertEquals("fallback", result.fallback());
    }

    @Test
    void nbtReusesStaticCacheWithUnusedScope() {
        final var tree = compiler.compile("<nbt:entity:@a:Inventory>");
        assertSame(tree.render(), tree.render(null, Placeholder.ofUnparsed("unused", "value")));
    }

}
