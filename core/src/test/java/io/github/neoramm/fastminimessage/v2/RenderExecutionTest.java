package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import io.github.neoramm.fastminimessage.v2.model.RuntimeTagBridge;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class RenderExecutionTest {
    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());

    @Test
    void directInsertionPreservesLiteralClosingsAndChildStyles() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> valKey = layout.key("value", String.class);
        final @NotNull PayloadSchema schema = layout.build();
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(schema)
            .registerPlaceholder("value", (final var scope) -> Component.text(scope.payload(valKey)));
        final @NotNull MiniMessageCompiler comp = new MiniMessageCompiler(registry);
        final @NotNull RenderScope scope = RenderScope.builder(schema).payload(valKey, "X").build();
        for (final @NotNull String source : List.of("<value>child</value>", "<value/>",
            "<value:arg>child</value>", "<red><value><blue>child</blue></value></red>")) {
            assertEquivalentRenderPaths(comp, source, scope);
        }
    }

    @Test
    void directParsedInsertionPreservesChildrenAndSelfClosingBehavior() {
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults()
            .registerPlaceholder("value", (final var scope) -> Component.text("X", NamedTextColor.GREEN));
        final @NotNull MiniMessageCompiler comp = new MiniMessageCompiler(registry);
        for (final @NotNull String source : List.of("<value>child</value>", "<value/>", "<value>child")) {
            assertEquivalentRenderPaths(comp, source, RenderScope.EMPTY);
        }
    }

    @Test
    void componentInsertionsKeepTheirStyleOnChildren() {
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults()
            .registerPlaceholder("value", (final var scope) -> Component.text("X", NamedTextColor.RED));
        final @NotNull MiniMessageCompiler comp = new MiniMessageCompiler(registry);
        assertEquivalentRenderPaths(comp, "<value>child<blue>inner</blue></value>", RenderScope.EMPTY);
    }

    @Test
    void missingRegisteredValuesResolveOnceAndKeepStyledFallbacks() {
        final @NotNull AtomicInteger calls = new AtomicInteger();
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults()
            .registerPlaceholder("value", (final var scope) -> {
                calls.incrementAndGet();
                return Component.empty();
            });
        final @NotNull MiniMessageCompiler comp = new MiniMessageCompiler(registry);
        for (final @NotNull String source : List.of("before <red><value>child</value></red> after",
            "before <value>child</value> after")) {
            calls.set(0);
            final var tree = comp.compile(source);
            final var actual = tree.render();
            assertEquals(1, calls.get());
        }
    }

    @Test
    void modifyingTagsPreserveChildrenAddedByTheModifier() {
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults()
            .registerModifier("modify", (final var content, final var scope) -> content.append(Component.text("1")));
        final var source = "<modify><red>a</red><blue>b</blue><green>c</green></modify>";
        final var tree = new MiniMessageCompiler(registry).compile(source);
        assertEquals("abc1", ComponentComparison.plainText(tree.render()));
    }

    @Test
    void modifyingTagsCanRemoveChildren() {
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults()
            .registerModifier("modify", (final var content, final var scope) -> Component.text("root"));
        final var tree = new MiniMessageCompiler(registry).compile("<modify><red>a</red><blue>b</blue></modify>");
        assertEquals(Component.text("root"), tree.render());
    }

    @Test
    void scopesRejectNullBindingsInBothCollectionOverloads() {
        final @Nullable RuntimeTagBridge @NotNull [] bindings = {null};
        assertThrows(NullPointerException.class, () -> PayloadInputs.scope(bindings));
        assertThrows(NullPointerException.class, () -> PayloadInputs.scope(Arrays.asList(bindings)));
    }

    @Test
    void scopesKeepCaseInsensitiveFirstMatchPrecedence() {
        final @NotNull RuntimeTagBridge first = PayloadInputs.value("VALUE", Component.text("first"));
        final @NotNull RuntimeTagBridge second = PayloadInputs.value("value", Component.text("second"));
        assertEquals(first, PayloadInputs.scope(first, second).resolve("value"));
        assertEquals(first, PayloadInputs.scope(first).resolve("VaLuE"));
    }

    @Test
    void scopesOwnTheBindingsList() {
        final @NotNull RuntimeTagBridge binding = PayloadInputs.value("value", Component.text("original"));
        final @NotNull List<@NotNull RuntimeTagBridge> bindings = new ArrayList<>(List.of(binding));
        final @NotNull RenderScope scope = PayloadInputs.scope(bindings);
        bindings.clear();
        assertEquals(binding, scope.resolve("value"));
    }

    private void assertEquivalentRenderPaths(final @NotNull MiniMessageCompiler comp, final @NotNull String source,
        final @NotNull RenderScope scope) {
        final var root = comp.compile(source).root();
        final TextComponent.@NotNull Builder output = Component.text();
        root.renderInto(scope, output);
        assertEquals(ComponentComparison.expression(root.render(scope)),
            ComponentComparison.expression(output.build()), source);
    }
}
