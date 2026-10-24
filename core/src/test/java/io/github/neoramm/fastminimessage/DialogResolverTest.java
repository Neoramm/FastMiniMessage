package io.github.neoramm.fastminimessage;

import net.kyori.adventure.dialog.DialogLike;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class DialogResolverTest {

    private static final DialogLike DIALOG = new DialogLike() {
    };

    @Test
    void showDialogUsesConfiguredResolver() {
        final MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults())
            .dialogResolver(reference -> "test:dialog".equals(reference) ? DIALOG : null);

        final Component rendered = compiler.compile("<click:show_dialog:'test:dialog'>Open</click>").render(null);

        final ClickEvent<?> click = rendered.clickEvent();
        assertNotNull(click);
        assertEquals(ClickEvent.Action.SHOW_DIALOG, click.action());
        assertInstanceOf(ClickEvent.Payload.Dialog.class, click.payload());
        assertSame(DIALOG, ((ClickEvent.Payload.Dialog) click.payload()).dialog());
        assertEquals("Open", plain(rendered));
    }

    @Test
    void showDialogKeepsLiteralFallbackWithoutResolver() {
        final MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());

        final Component rendered = compiler.compile("<red><click:show_dialog:'test:dialog'>Open</click></red>").render(null);

        assertNull(rendered.clickEvent());
        assertEquals("<click:show_dialog:'test:dialog'>Open</click>", plain(rendered));
        assertRed(rendered, null);
    }

    @Test
    void showDialogKeepsLiteralFallbackForUnknownReference() {
        final MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults())
            .dialogResolver(reference -> null);

        final Component rendered = compiler.compile("<click:show_dialog:'missing'>Open</click>").render(null);

        assertNull(rendered.clickEvent());
        assertEquals("<click:show_dialog:'missing'>Open</click>", plain(rendered));
    }

    private static String plain(final Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static void assertRed(final Component component, final @Nullable TextColor inherited) {
        final @Nullable TextColor color = component.color() != null ? component.color() : inherited;
        if (component instanceof final TextComponent text && !text.content().isEmpty())
            assertEquals(NamedTextColor.RED, color);
        for (final Component child : component.children()) assertRed(child, color);
    }
}
