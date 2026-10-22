package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.api.ComponentModifier;
import io.github.neoramm.fastminimessage.example.ComponentModifiers;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentModifierTest {
  private static final Pointered NOBODY = new Pointered() {
    public Pointers pointers() { return Pointers.empty(); }
  };
  private static final Pointered OTHER = new Pointered() {
    public Pointers pointers() { return Pointers.empty(); }
  };

  private String plain(Component c) {
    StringBuilder sb = new StringBuilder();
    if (c instanceof TextComponent t) sb.append(t.content());
    for (Component ch : c.children()) sb.append(plain(ch));
    return sb.toString();
  }

  @Test
  void staticModifierTransformsContent() {
    MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
    compiler.registerModifier("shout", ComponentModifiers.uppercase());
    Component rendered = compiler.compile("<shout>hello</shout>").render(NOBODY);
    assertEquals("HELLO", plain(rendered));
  }

  @Test
  void staticModifierIsCached() {
    MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
    compiler.registerModifier("shout", ComponentModifiers.uppercase());
    Component a = compiler.compile("<shout>hello</shout>").render(NOBODY);
    Component b = compiler.compile("<shout>hello</shout>").render(NOBODY);
    assertEquals(a, b);
  }

  @Test
  void dynamicModifierDependsOnContext() {
    ComponentModifier dyn = (rendered, ctx) -> {
      String suffix = (ctx == OTHER) ? "!" : ".";
      return Component.text(plain(rendered) + suffix, rendered.style());
    };
    MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
    compiler.registerModifier("dyn", dyn);
    Component a = compiler.compile("<dyn>hi</dyn>").render(NOBODY);
    Component b = compiler.compile("<dyn>hi</dyn>").render(OTHER);
    assertEquals("hi.", plain(a));
    assertEquals("hi!", plain(b));
  }

  @Test
  void modifierInsideGradientDoesNotThrow() {
    MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
    compiler.registerModifier("shout", ComponentModifiers.uppercase());
    Component rendered = compiler.compile("<gradient:red:blue><shout>ab</shout></gradient>").render(NOBODY);
    assertTrue(rendered != null);
    assertTrue(!plain(rendered).isEmpty());
  }

  @Test
  void modifierInheritsStyle() {
    MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
    compiler.registerModifier("shout", ComponentModifiers.uppercase());
    Component rendered = compiler.compile("<red><shout>x</shout></red>").render(NOBODY);
    assertEquals(NamedTextColor.RED, rendered.style().color());
  }

  @Test
  void staticColorModifier() {
    MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
    compiler.registerModifier("blue", ComponentModifiers.staticColor(NamedTextColor.BLUE));
    Component rendered = compiler.compile("<blue>hi</blue>").render(NOBODY);
    assertEquals(NamedTextColor.BLUE, rendered.style().color());
  }
}
