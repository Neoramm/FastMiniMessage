package io.github.neoramm.fastminimessage.paper;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.DialogResolver;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import net.kyori.adventure.dialog.DialogLike;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PaperTagsTest {

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

        final Component rendered = compiler.compile("<click:show_dialog:'test:dialog'>Open</click>").render(null);

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
            .render(null);

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

        final Component rendered = compiler.compile("<item_hover:'minecraft:diamond':2>Hover</item_hover>").render(null);

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

        final Component rendered = compiler.compile("<item_hover:'missing'>Hover</item_hover>").render(null);

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

        assertEquals("Hi Hector!", plain(tree.render(null)));

        value.set("Steve");
        assertEquals("Hi Steve!", plain(tree.render(null)));
    }

    private static PaperTagOptions options(final DialogResolver registryDialogs,
        final DialogFactory inlineDialogs, final ItemHoverFactory itemHovers, final PapiRenderer placeholders) {
        return new PaperTagOptions(registryDialogs, inlineDialogs, itemHovers, placeholders);
    }

    private static String plain(final Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
