package io.github.neoramm.fastminimessage.v2.paper;

import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.v2.ResolverRegistry;
import io.github.neoramm.fastminimessage.v2.api.DialogResolver;
import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
import net.kyori.adventure.dialog.DialogLike;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

final class PaperTagsTest {

    private static final DialogLike DIALOG = new DialogLike() {
    };
    private static final HoverEvent<?> HOVER = HoverEvent.showText(Component.text("hover"));

    @Test
    void clickShowDialogUsesRegistryResolver() {
        final MiniMessageCompiler compiler = PaperTags.compiler(options(
            reference -> DIALOG,
            dialog -> DIALOG,
            (reference, count) -> HOVER,
            (pointered, expression) -> ""
        ));

        final Component rendered = compiler.compile("<click:show_dialog:'test:dialog'>Open</click>").render();

        final ClickEvent<?> click = rendered.clickEvent();
        assertNotNull(click);
        assertEquals(ClickEvent.Action.SHOW_DIALOG, click.action());
        assertSame(DIALOG, ((ClickEvent.Payload.Dialog) click.payload()).dialog());
        assertEquals("Open", plain(rendered));
    }

    @Test
    void inlineDialogReceivesRenderedContents() {
        final AtomicReference<InlineDialog> captured = new AtomicReference<>();
        final MiniMessageCompiler compiler = PaperTags.compiler(options(
            reference -> DIALOG,
            dialog -> {
                captured.set(dialog);
                return DIALOG;
            },
            (reference, count) -> HOVER,
            (pointered, expression) -> ""
        ));

        final Component rendered = compiler
            .compile("<show_dialog:'<red>Title</red>':'Body':'<bold>OK</bold>'>Open</show_dialog>")
            .render();

        final InlineDialog dialog = captured.get();
        assertNotNull(dialog);
        assertEquals("Title", plain(dialog.title()));
        assertEquals("Body", plain(dialog.body()));
        assertEquals("OK", plain(dialog.button()));
        assertEquals("Open", plain(rendered));
        assertSame(DIALOG, ((ClickEvent.Payload.Dialog) rendered.clickEvent().payload()).dialog());
    }

    @Test
    void itemHoverUsesFactory() {
        final AtomicInteger count = new AtomicInteger();
        final MiniMessageCompiler compiler = PaperTags.compiler(options(
            reference -> DIALOG,
            dialog -> DIALOG,
            (reference, stackSize) -> {
                assertEquals("minecraft:diamond", reference);
                count.set(stackSize);
                return HOVER;
            },
            (pointered, expression) -> ""
        ));

        final Component rendered = compiler.compile("<item_hover:'minecraft:diamond':2>Hover</item_hover>").render();

        assertEquals(2, count.get());
        assertEquals(HOVER, rendered.hoverEvent());
        assertEquals("Hover", plain(rendered));
    }

    @Test
    void itemHoverKeepsLiteralFallbackForUnknownItem() {
        final MiniMessageCompiler compiler = PaperTags.compiler(options(
            reference -> DIALOG,
            dialog -> DIALOG,
            (reference, count) -> {
                throw new IllegalArgumentException("unknown item");
            },
            (pointered, expression) -> ""
        ));

        final Component rendered = compiler.compile("<item_hover:'missing'>Hover</item_hover>").render();

        assertNull(rendered.hoverEvent());
        assertEquals("<item_hover:'missing'>Hover</item_hover>", plain(rendered));
    }

    @Test
    void papiRendersValueOnEachRender() {
        final AtomicReference<String> value = new AtomicReference<>("Hector");
        final PreComponentTree tree = PaperTags.compiler(options(
            reference -> DIALOG,
            dialog -> DIALOG,
            (reference, count) -> HOVER,
            (pointered, expression) -> value.get()
        )).compile("Hi <papi:'%player_name%'>!");

        assertEquals("Hi Hector!", plain(tree.render()));

        value.set("Steve");
        assertEquals("Hi Steve!", plain(tree.render()));
    }

    @Test
    void staticItemHoverIsCreatedOnlyDuringCompilation() {
        final @NotNull AtomicInteger calls = new AtomicInteger();
        final var compiler = PaperTags.compiler(options(
            (final var reference) -> DIALOG,
            (final var dialog) -> DIALOG,
            (final var reference, final var count) -> {
                calls.incrementAndGet();
                return HOVER;
            },
            (final var viewer, final var expression) -> ""
        ));
        final var tree = compiler.compile("<item_hover:diamond:2>Hover</item_hover>");
        assertEquals(1, calls.get());
        assertSame(tree.render(), tree.render());
        assertEquals(1, calls.get());
    }

    @Test
    void dynamicDialogContentsUseTheCurrentScope() {
        final @NotNull AtomicReference<InlineDialog> captured = new AtomicReference<>();
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> nameKey = layout.key("name", Component.class);
        final PayloadSchema.@NotNull Key<@NotNull Component> bodyKey = layout.key("body", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("name", (final var scope) -> scope.payload(nameKey))
            .registerPlaceholder("body", (final var scope) -> scope.payload(bodyKey));
        PaperTags.apply(registry, options(
            (final var reference) -> DIALOG,
            (final var dialog) -> {
                captured.set(dialog);
                return DIALOG;
            },
            (final var reference, final var count) -> HOVER,
            (final var viewer, final var expression) -> ""
        ));
        final var compiler = new MiniMessageCompiler(registry);
        final var tree = compiler.compile("<show_dialog:'Hello <name>':'<body>':'OK'>Open</show_dialog>");
        assertNull(captured.get());
        tree.render((final var builder) -> builder.viewer(null)
            .payload(nameKey, Component.text("Ada"))
            .payload(bodyKey, Component.text("<red>literal")));
        assertEquals("Hello Ada", plain(captured.get().title()));
        assertEquals("<red>literal", plain(captured.get().body()));
        tree.render((final var builder) -> builder.viewer(null)
            .payload(nameKey, Component.text("Grace"))
            .payload(bodyKey, Component.text("updated")));
        assertEquals("Hello Grace", plain(captured.get().title()));
        assertEquals("updated", plain(captured.get().body()));
    }

    @Test
    void papiInterpolatesExpressionBeforeCallingTheRenderer() {
        final @NotNull AtomicReference<String> captured = new AtomicReference<>();
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> keyKey = layout.key("key", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("key", (final var scope) -> scope.payload(keyKey));
        PaperTags.apply(registry, options(
            (final var reference) -> DIALOG,
            (final var dialog) -> DIALOG,
            (final var reference, final var count) -> HOVER,
            (final var viewer, final var expression) -> {
                captured.set(expression);
                return "value";
            }
        ));
        final var compiler = new MiniMessageCompiler(registry);
        final var tree = compiler.compile("<papi:'%<key>%'>!");
        assertEquals("value!",
            plain(tree.render((final var builder) -> builder.viewer(null)
            .payload(keyKey, Component.text("player_name")))));
        assertEquals("%player_name%", captured.get());
    }

    @Test
    void papiConstantEmptyExpressionFailsAtCompileTime() {
        final var compiler = PaperTags.compiler(options(
            (final var reference) -> DIALOG,
            (final var dialog) -> DIALOG,
            (final var reference, final var count) -> HOVER,
            (final var viewer, final var expression) -> "rendered"
        ));
        final var treeEmpty = compiler.compile("<papi:>");
        assertEquals("<papi:>", plain(treeEmpty.render()));

        final var treeEmptyQuotes = compiler.compile("<papi:''>");
        assertEquals("<papi:''>", plain(treeEmptyQuotes.render()));
    }

    @Test
    void showDialogRequiresAtLeastOneArgumentAtCompileTime() {
        final var compiler = PaperTags.compiler(options(
            (final var reference) -> DIALOG,
            (final var dialog) -> DIALOG,
            (final var reference, final var count) -> HOVER,
            (final var viewer, final var expression) -> ""
        ));
        final var treeNoArgs = compiler.compile("<show_dialog>Click</show_dialog>");
        assertEquals("<show_dialog>Click</show_dialog>", plain(treeNoArgs.render()));
    }

    @Test
    void showDialogWithOnlyTitleUsesDefaultButton() {
        final @NotNull AtomicReference<InlineDialog> captured = new AtomicReference<>();
        final var compiler = PaperTags.compiler(options(
            (final var reference) -> DIALOG,
            (final var dialog) -> {
                captured.set(dialog);
                return DIALOG;
            },
            (final var reference, final var count) -> HOVER,
            (final var viewer, final var expression) -> ""
        ));
        final var tree = compiler.compile("<show_dialog:'Simple Title'>Click</show_dialog>");
        final @NotNull Component rendered = tree.render();
        assertEquals("Click", plain(rendered));
        assertNotNull(captured.get());
        assertEquals("Simple Title", plain(captured.get().title()));
        assertNull(captured.get().body());
        assertEquals("OK", plain(captured.get().button()));
    }

    private static PaperTagOptions options(final DialogResolver registryDialogs,
        final DialogFactory inlineDialogs, final ItemHoverFactory itemHovers, final PapiRenderer placeholders) {
        return new PaperTagOptions(registryDialogs, inlineDialogs, itemHovers, placeholders);
    }

    private static String plain(final Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
