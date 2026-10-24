package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.api.ComponentModifier;
import io.github.neoramm.fastminimessage.example.ComponentModifiers;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
        final MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
        compiler.registerModifier("shout", ComponentModifiers.uppercase());
        final Component rendered = compiler.compile("<shout>hello</shout>").render(NOBODY);
        assertEquals("HELLO", plain(rendered));
    }

    @Test
    void staticModifierIsCached() {
        final MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
        compiler.registerModifier("shout", ComponentModifiers.uppercase());
        final Component first = compiler.compile("<shout>hello</shout>").render(NOBODY);
        final Component second = compiler.compile("<shout>hello</shout>").render(NOBODY);
        assertEquals(first, second);
    }

    @Test
    void dynamicModifierDependsOnContext() {
        final ComponentModifier dynamic = (final @NotNull Component rendered, final @Nullable Pointered context) -> {
            final String suffix = context == OTHER ? "!" : ".";
            return Component.text(plain(rendered) + suffix, rendered.style());
        };
        final MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
        compiler.registerModifier("dyn", dynamic);
        final Component first = compiler.compile("<dyn>hi</dyn>").render(NOBODY);
        final Component second = compiler.compile("<dyn>hi</dyn>").render(OTHER);
        assertEquals("hi.", plain(first));
        assertEquals("hi!", plain(second));
    }

    @Test
    void modifierInsideGradientDoesNotThrow() {
        final MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
        compiler.registerModifier("shout", ComponentModifiers.uppercase());
        final Component rendered = compiler.compile("<gradient:red:blue><shout>ab</shout></gradient>").render(NOBODY);
        assertNotNull(rendered);
        assertFalse(plain(rendered).isEmpty());
    }

    @Test
    void modifierInheritsStyle() {
        final MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
        compiler.registerModifier("shout", ComponentModifiers.uppercase());
        final Component rendered = compiler.compile("<red><shout>x</shout></red>").render(NOBODY);
        assertEquals(NamedTextColor.RED, rendered.style().color());
    }

    @Test
    void staticColorModifier() {
        final MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
        compiler.registerModifier("blue", ComponentModifiers.staticColor(NamedTextColor.BLUE));
        final Component rendered = compiler.compile("<blue>hi</blue>").render(NOBODY);
        assertEquals(NamedTextColor.BLUE, rendered.style().color());
    }
}
