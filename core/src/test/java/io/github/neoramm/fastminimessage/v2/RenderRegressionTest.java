package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.internal.DefaultTagScanner;
import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import io.github.neoramm.fastminimessage.v2.template.TemplateNode;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class RenderRegressionTest {
    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());

    @Test
    void hoverPreservesLiteralArguments() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> valKey = layout.key("value", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("value", (final var scope) -> Component.text(scope.payload(valKey)));
        final @NotNull Component result = new MiniMessageCompiler(registry).compile("<hover:show_text:'<value>'>hover</hover>")
            .render((final var b) -> b.payload(valKey, "<red>literal"));
        final @NotNull HoverEvent<?> hover = result.hoverEvent();
        assertEquals(Component.text("<red>literal"), hover.value());
    }

    @Test
    void copyingWarmDeferredTagsDoesNotRetainOriginalChildren() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> cmdKey = layout.key("cmd", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("cmd", (final var scope) -> Component.text(scope.payload(cmdKey)));
        final var tree = new MiniMessageCompiler(registry).compile("<click:run_command:'/<cmd>'>old</click>");
        tree.render((final var b) -> b.payload(cmdKey, "help"));
        final var copy = tree.map((final var node) -> node instanceof TemplateNode.Static
            ? new TemplateNode.Static(Component.text("new")) : node);
        assertEquals("new", plain(copy.render((final var b) -> b.payload(cmdKey, "help"))));
        assertEquals("old", plain(tree.render((final var b) -> b.payload(cmdKey, "help"))));
    }

    @Test
    void mappingStaticTreePreservesOriginal() {
        final var tree = compiler.compile("<gradient:red:blue>old</gradient>");
        final var cached = tree.render();
        final var edited = tree.map((final var node) -> new TemplateNode.Static(Component.text("new")));
        assertEquals("new", plain(edited.render()));
        assertSame(cached, tree.render());
    }

    @Test
    void unusedScopeRetainsStaticRenderCache() {
        final @NotNull PreComponentTree tree = compiler.compile("<gradient:red:blue>static</gradient>");
        assertSame(tree.render(), tree.render());
    }

    @Test
    void malformedClickDoesNotDeleteVisibleText() {
        assertEquals("<click:run_command>visible</click>",
            plain(compiler.compile("<click:run_command>visible</click>").render()));
    }

    @Test
    void unknownHoverActionDoesNotDeleteVisibleText() {
        assertEquals("<hover:unknown:'value'>visible</hover>",
            plain(compiler.compile("<hover:unknown:'value'>visible</hover>").render()));
    }

    @Test
    void immutableSourceNodesCanBeShared() {
        final var literal = new TemplateNode.Static(Component.text("original"));
        final var tree = new PreComponentTree(literal);
        assertSame(literal, tree.root());
        assertSame(tree, tree.copy());
        assertEquals("original", plain(tree.render()));
    }

    @Test
    void hoverCompilesOnceAndPreservesComponentStyles() {
        final @NotNull AtomicInteger scans = new AtomicInteger();
        final @NotNull DefaultTagScanner scanner = new DefaultTagScanner();
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> valKey = layout.key("value", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("value", (final var scope) -> scope.payload(valKey));
        final @NotNull MiniMessageCompiler counted = new MiniMessageCompiler(registry,
            (final @NotNull String input) -> {
                scans.incrementAndGet();
                return scanner.scan(input);
            });
        final @NotNull PreComponentTree tree = counted.compile("<hover:show_text:'<value>'>hover</hover>");
        final int initial = scans.get();
        for (int i = 0; i < 5; i++) {
            final @NotNull Component value = Component.text("value" + i, NamedTextColor.BLUE);
            final @NotNull Component rendered = tree.render((final var b) -> b.payload(valKey, value));
            assertEquals(value, rendered.hoverEvent().value());
        }
        assertEquals(initial, scans.get());
    }

    @Test
    void failedMappingDoesNotChangeOriginal() {
        final @NotNull PreComponentTree tree = compiler.compile("original");
        tree.render();
        assertThrows(IllegalStateException.class, () -> tree.map((final var node) -> {
            throw new IllegalStateException("aborted");
        }));
        assertEquals("original", plain(tree.render()));
    }

    @Test
    void dynamicLeavesCanDependOnScope() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> valKey = layout.key("value", String.class);
        final @NotNull PayloadSchema schema = layout.build();
        final var tree = new PreComponentTree(new TemplateNode.DynamicLeaf(Style.empty(),
            (final var context) -> Component.text(context.isEmpty() ? "empty" : "provided")));
        assertEquals("empty", plain(tree.render()));
        assertEquals("provided", plain(tree.render(RenderScope.builder(schema).payload(valKey, "ignored").build())));
    }

    private static @NotNull String plain(final @NotNull Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
