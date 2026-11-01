package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.model.Placeholder;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import io.github.neoramm.fastminimessage.internal.DefaultTagScanner;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class RenderRegressionTest {
    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());

    @Test
    void hoverPreservesLiteralArguments() {
        final @NotNull Component result = compiler.compile("<hover:show_text:'<value>'>hover</hover>")
            .render(null, Placeholder.ofUnparsed("value", "<red>literal"));
        final @NotNull HoverEvent<?> hover = result.hoverEvent();
        assertEquals(Component.text("<red>literal"), hover.value());
    }

    @Test
    void copyingWarmDeferredTagsDoesNotRetainOriginalChildren() {
        final @NotNull PreComponentTree tree = compiler.compile("<click:run_command:'/<cmd>'>old</click>");
        tree.render(null, Placeholder.ofUnparsed("cmd", "help"));
        final @NotNull PreComponentTree copy = tree.copy();
        copy.visit((final var node) -> {
            if (node instanceof final LiteralPreComponent literal) literal.setText("new");
        });
        assertEquals("new", plain(copy.render(null, Placeholder.ofUnparsed("cmd", "help"))));
    }

    @Test
    void visitingWarmStaticTreeInvalidatesCaches() {
        final @NotNull PreComponentTree tree = compiler.compile("<gradient:red:blue>old</gradient>");
        tree.render();
        tree.visit((final var node) -> {
            if (node instanceof final LiteralPreComponent literal) literal.setText("new");
        });
        assertEquals("new", plain(tree.render()));
    }

    @Test
    void unusedScopeRetainsStaticRenderCache() {
        final @NotNull PreComponentTree tree = compiler.compile("<gradient:red:blue>static</gradient>");
        assertSame(tree.render(), tree.render(null, Placeholder.ofUnparsed("unused", "value")));
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
    void constructorIsolatesMutableSourceNodes() {
        final @NotNull LiteralPreComponent literal = new LiteralPreComponent("original", null);
        final @NotNull PreComponentTree tree = new PreComponentTree(literal);
        literal.setText("changed");
        assertEquals("original", plain(tree.render()));
    }

    @Test
    void hoverCompilesOnceAndPreservesComponentStyles() {
        final @NotNull AtomicInteger scans = new AtomicInteger();
        final @NotNull DefaultTagScanner scanner = new DefaultTagScanner();
        final @NotNull MiniMessageCompiler counted = new MiniMessageCompiler(ResolverRegistry.withDefaults(),
            (final @NotNull String input) -> {
                scans.incrementAndGet();
                return scanner.scan(input);
            });
        final @NotNull PreComponentTree tree = counted.compile("<hover:show_text:'<value>'>hover</hover>");
        final int initial = scans.get();
        for (int i = 0; i < 5; i++) {
            final @NotNull Component value = Component.text("value" + i, NamedTextColor.BLUE);
            final @NotNull Component rendered = tree.render(null, Placeholder.ofParsed("value", value));
            assertEquals(value, rendered.hoverEvent().value());
        }
        assertEquals(initial, scans.get());
    }

    @Test
    void failedVisitorDoesNotPublishPartialEdits() {
        final @NotNull PreComponentTree tree = compiler.compile("original");
        tree.render();
        assertThrows(IllegalStateException.class, () -> tree.visit((final var node) -> {
            if (node instanceof final LiteralPreComponent literal) literal.setText("changed");
            throw new IllegalStateException("aborted");
        }));
        assertEquals("original", plain(tree.render()));
    }

    @Test
    void staticCustomComponentsCanStillDependOnScope() {
        final @NotNull PreComponentTree tree = new PreComponentTree(new ScopeComponent());
        assertEquals("empty", plain(tree.render()));
        assertEquals("provided", plain(tree.render(null, Placeholder.ofUnparsed("value", "ignored"))));
    }

    private static final class ScopeComponent extends PreComponent {
        @Override
        public @NotNull Component render(final @Nullable Pointered target, final @NotNull RenderScope scope) {
            return Component.text(scope.isEmpty() ? "empty" : "provided");
        }

        @Override
        public boolean isStatic() {
            return true;
        }

        @Override
        public @NotNull PreComponent deepCopy() {
            return new ScopeComponent();
        }
    }

    private static @NotNull String plain(final @NotNull Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
