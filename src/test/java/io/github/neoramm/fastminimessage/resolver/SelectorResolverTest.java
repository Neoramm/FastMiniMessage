package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.ResolverRegistry;
import io.github.neoramm.fastminimessage.model.Placeholder;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.SelectorComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SelectorResolverTest {

    private static final @NotNull Pointered NOBODY = new Pointered() {
        @Override
        public @NotNull Pointers pointers() {
            return Pointers.empty();
        }
    };

    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
    private final @NotNull MiniMessage miniMessage = MiniMessage.builder().strict(false).build();

    private @NotNull SelectorComponent selector(final @NotNull String input) {
        return (SelectorComponent) compiler.compile(input).render(NOBODY);
    }

    private static @NotNull String plainText(final @Nullable Component component) {
        return component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void selectorWithoutSeparatorHasNoSeparator() {
        final SelectorComponent selector = selector("<selector:@a>");
        assertEquals("@a", selector.pattern());
        assertNull(selector.separator());
    }

    @Test
    void separatorIsCompiledWithTheFastPipeline() {
        final SelectorComponent selector = selector("<selector:@a:', '>");
        assertEquals(", ", plainText(selector.separator()));
    }

    @Test
    void styledSeparatorKeepsItsStyle() {
        final SelectorComponent selector = selector("<selector:@a:'<red>, </red>'>");
        assertEquals(", ", plainText(selector.separator()));
        assertEquals(NamedTextColor.RED, selector.separator().style().color());
    }

    @Test
    void separatorMatchesMiniMessage() {
        final String input = "<selector:@a:'<red>, </red>'>";
        final SelectorComponent reference = (SelectorComponent) miniMessage.deserialize(input);
        final SelectorComponent actual = selector(input);
        assertEquals(reference.pattern(), actual.pattern());
        assertEquals(plainText(reference.separator()), plainText(actual.separator()));
        assertEquals(reference.separator().style(), actual.separator().style());
    }

    @Test
    void separatorSupportsRuntimePlaceholders() {
        final PreComponentTree tree = compiler.compile("<selector:@a:'<p>'>");
        final SelectorComponent selector =
            (SelectorComponent) tree.render(NOBODY, Placeholder.ofUnparsed("p", "|"));
        assertEquals("|", plainText(selector.separator()));
    }

    @Test
    void staticnessFollowsTheSeparator() {
        assertTrue(compiler.compile("<selector:@a>").isStatic());
        assertTrue(compiler.compile("<selector:@a:', '>").isStatic());
        assertFalse(compiler.compile("<selector:@a:'<p>'>").isStatic());
    }
}
