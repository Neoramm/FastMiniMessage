package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.EntityNBTComponent;
import net.kyori.adventure.text.SelectorComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickEvent;
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
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> valKey = layout.key("value", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("value", (final var scope) -> Component.text(scope.payload(valKey)));
        final var result = new MiniMessageCompiler(registry).compile("<click:suggest_command:'<value>'>text</click>")
            .render((final var b) -> b.payload(valKey, value));
        assertEquals(Component.text("text").clickEvent(
            ClickEvent.suggestCommand(value)), result);
    }

    @Test
    void translationArgumentsRemainLiteralWithDynamicKeys() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> keyKey = layout.key("key", String.class);
        final PayloadSchema.@NotNull Key<@NotNull String> valKey = layout.key("value", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("key", (final var scope) -> Component.text(scope.payload(keyKey)))
            .registerPlaceholder("value", (final var scope) -> Component.text(scope.payload(valKey)));
        final var result = (TranslatableComponent) new MiniMessageCompiler(registry).compile("<tr:'<key>':'<value>'>")
            .render((final var b) -> b.payload(keyKey, "custom.key").payload(valKey, "<red>literal"));
        assertEquals("custom.key", result.key());
        assertEquals(Component.text("<red>literal"), result.arguments().getFirst().asComponent());
    }

    @Test
    void selectorsReuseStaticCacheWithUnusedScope() {
        final var tree = compiler.compile("<selector:@a>");
        assertSame(tree.render(), tree.render());
    }

    @Test
    void inspectionPreservesCachedOutput() {
        final var tree = compiler.compile("<gradient:red:blue>hello</gradient>");
        final var cached = tree.render();
        tree.inspect((final var snapshot) -> {
            assertEquals("Static", snapshot.getClass().getSimpleName());
        });
        assertSame(cached, tree.render());
    }

    @Test
    void componentSeparatorsPreserveLiteralText() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> sepKey = layout.key("separator", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("separator", (final var scope) -> Component.text(scope.payload(sepKey)));
        final @NotNull MiniMessageCompiler comp = new MiniMessageCompiler(registry);
        final var selector = (SelectorComponent) comp.compile("<selector:@a:'<separator>'>")
            .render((final var b) -> b.payload(sepKey, "<red>:"));
        final var nbt = (EntityNBTComponent) comp.compile("<nbt:entity:@a:Inventory:'<separator>'>")
            .render((final var b) -> b.payload(sepKey, "<red>:"));
        assertEquals(Component.text("<red>:"), selector.separator());
        assertEquals(Component.text("<red>:"), nbt.separator());
    }

    @Test
    void fallbackTranslationRetainsComponentStyles() {
        final var value = Component.text("styled", NamedTextColor.BLUE);
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> valKey = layout.key("value", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("value", (final var scope) -> scope.payload(valKey));
        final var result = (TranslatableComponent) new MiniMessageCompiler(registry).compile("<tr_or:key:'fallback':'<value>'>")
            .render((final var b) -> b.payload(valKey, value));
        assertEquals(value, result.arguments().getFirst().asComponent());
        assertEquals("fallback", result.fallback());
    }

    @Test
    void nbtReusesStaticCacheWithUnusedScope() {
        final var tree = compiler.compile("<nbt:entity:@a:Inventory>");
        assertSame(tree.render(), tree.render());
    }
}
