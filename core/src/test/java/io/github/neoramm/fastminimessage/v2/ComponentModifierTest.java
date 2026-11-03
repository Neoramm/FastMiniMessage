package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.api.ComponentModifier;
import io.github.neoramm.fastminimessage.v2.example.ComponentModifiers;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class ComponentModifierTest {

    private static final @NotNull Pointered NOBODY = new Pointered() {
        @Override
        public @NotNull Pointers pointers() {
            return Pointers.empty();
        }
    };

    private static final @NotNull Pointered OTHER = new Pointered() {
        @Override
        public @NotNull Pointers pointers() {
            return Pointers.empty();
        }
    };

    private static @NotNull String plain(final @NotNull Component component) {
        final StringBuilder text = new StringBuilder();
        if (component instanceof TextComponent textComponent) text.append(textComponent.content());
        for (final Component child : component.children()) text.append(plain(child));
        return text.toString();
    }

    @Test
    void staticModifierTransformsContent() {
        final var registry = ResolverRegistry.withDefaults().registerModifier("shout", ComponentModifiers.uppercase());
        final var compiler = new MiniMessageCompiler(registry);
        final Component rendered =
            compiler.compile("<shout>hello</shout>").render((final var builder) -> builder.viewer(NOBODY));
        assertEquals("HELLO", plain(rendered));
    }

    @Test
    void staticModifierIsCached() {
        final var registry = ResolverRegistry.withDefaults().registerModifier("shout", ComponentModifiers.uppercase());
        final var compiler = new MiniMessageCompiler(registry);
        final Component first =
            compiler.compile("<shout>hello</shout>").render((final var builder) -> builder.viewer(NOBODY));
        final Component second =
            compiler.compile("<shout>hello</shout>").render((final var builder) -> builder.viewer(NOBODY));
        assertEquals(first, second);
    }

    @Test
    void dynamicModifierDependsOnContext() {
        final ComponentModifier dynamic = (final @NotNull Component rendered, final @NotNull RenderScope context) -> {
            final String suffix = context.viewer() == OTHER ? "!" : ".";
            return Component.text(plain(rendered) + suffix, rendered.style());
        };
        final var registry = ResolverRegistry.withDefaults().registerModifier("dyn", dynamic);
        final var compiler = new MiniMessageCompiler(registry);
        final var tree = compiler.compile("<dyn>hi</dyn>");
        final Component first = tree.render((final var builder) -> builder.viewer(NOBODY));
        final Component second = tree.render((final var builder) -> builder.viewer(OTHER));
        assertEquals("hi.", plain(first));
        assertEquals("hi!", plain(second));
    }

    @Test
    void modifierInsideGradientDoesNotThrow() {
        final var registry = ResolverRegistry.withDefaults().registerModifier("shout", ComponentModifiers.uppercase());
        final var compiler = new MiniMessageCompiler(registry);
        final Component rendered =
            compiler.compile("<gradient:red:blue><shout>ab</shout></gradient>")
            .render((final var builder) -> builder.viewer(NOBODY));
        assertNotNull(rendered);
        assertFalse(plain(rendered).isEmpty());
    }

    @Test
    void modifierInheritsStyle() {
        final var registry = ResolverRegistry.withDefaults().registerModifier("shout", ComponentModifiers.uppercase());
        final var compiler = new MiniMessageCompiler(registry);
        final Component rendered =
            compiler.compile("<red><shout>x</shout></red>").render((final var builder) -> builder.viewer(NOBODY));
        assertEquals(NamedTextColor.RED, rendered.style().color());
    }

    @Test
    void staticColorModifier() {
        final var registry =
            ResolverRegistry.withDefaults().registerModifier("blue", ComponentModifiers.staticColor(NamedTextColor.BLUE));
        final var compiler = new MiniMessageCompiler(registry);
        final Component rendered =
            compiler.compile("<blue>hi</blue>").render((final var builder) -> builder.viewer(NOBODY));
        assertEquals(NamedTextColor.BLUE, rendered.style().color());
    }
}
