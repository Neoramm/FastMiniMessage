package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class MiniMessageCompilerRenderTest {

  private static final Pointered NOBODY = new Pointered() {
    @Override
    public Pointers pointers() {
      return Pointers.empty();
    }
  };

  private final MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());

  @Test
  void nestedColorsKeepTheInnermostColor() {
    final List<StyledChar> chars = flatten(compileAndRender("<red><blue>a</blue></red>"));
    assertEquals(1, chars.size());
    assertEquals(NamedTextColor.BLUE, chars.getFirst().style().color());
  }

  @Test
  void decorationWrappersReachEveryChild() {
    final List<StyledChar> chars = flatten(compileAndRender("<bold>a <red>b</red></bold>"));
    assertEquals(3, chars.size());
    for (final StyledChar styled : chars) {
      assertEquals(TextDecoration.State.TRUE, styled.style().decoration(TextDecoration.BOLD));
    }
    assertEquals(NamedTextColor.RED, chars.get(2).style().color());
  }

  @Test
  void gradientColorsTheEndpointsAndEverythingInBetween() {
    final List<StyledChar> chars = flatten(compileAndRender("<gradient:red:blue>abc</gradient>"));
    assertEquals(3, chars.size());
    assertEquals(NamedTextColor.RED, chars.getFirst().style().color());
    assertEquals(NamedTextColor.BLUE, chars.getLast().style().color());
    final TextColor middle = TextColor.lerp(0.5f, NamedTextColor.RED, NamedTextColor.BLUE);
    assertEquals(middle.red(), chars.get(1).style().color().red());
    assertEquals(middle.green(), chars.get(1).style().color().green());
    assertEquals(middle.blue(), chars.get(1).style().color().blue());
  }

  @Test
  void inheritedColorSurvivesAClickWrapper() {
    final List<StyledChar> chars = flatten(
      compileAndRender("<red><click:run_command:'/spawn'>a <bold>b</bold></click></red>"));
    assertEquals(3, chars.size());
    for (final StyledChar styled : chars) {
      assertEquals(NamedTextColor.RED, styled.style().color());
      assertEquals(ClickEvent.runCommand("/spawn"), styled.style().clickEvent());
    }
  }

  @Test
  void staticGradientIsRenderedOnlyOnce() {
    final PreComponentTree tree = compiler.compile("<gradient:red:blue>abc</gradient>");
    assertSame(tree.render(NOBODY), tree.render(NOBODY));
  }

  @Test
  void dynamicGradientIsRenderedOnEveryCall() {
    final ResolverRegistry registry = ResolverRegistry.withDefaults().register(new DynamicResolver());
    final PreComponentTree tree = new MiniMessageCompiler(registry)
      .compile("<gradient:red:blue>a <dyn/> c</gradient>");
    final Component first = tree.render(NOBODY);
    final Component second = tree.render(NOBODY);
    assertNotSame(first, second);
    assertEquals("a Z c", plainText(first));
    assertEquals("a Z c", plainText(second));
  }

  private Component compileAndRender(final @NotNull String miniMessage) {
    return compiler.compile(miniMessage).render(NOBODY);
  }

  private static String plainText(final @NotNull Component component) {
    final StringBuilder text = new StringBuilder();
    appendText(component, text);
    return text.toString();
  }

  private static void appendText(final @NotNull Component component, final @NotNull StringBuilder text) {
    if (component instanceof TextComponent textComponent) text.append(textComponent.content());
    for (final Component child : component.children()) appendText(child, text);
  }

  private static List<StyledChar> flatten(final @NotNull Component component) {
    final List<StyledChar> chars = new ArrayList<>();
    flatten(component, Style.empty(), chars);
    return chars;
  }

  private static void flatten(final @NotNull Component component, final @NotNull Style inherited,
                              final @NotNull List<StyledChar> chars) {
    final Style merged = inherited.merge(component.style());
    if (component instanceof TextComponent text) {
      for (int i = 0; i < text.content().length(); i++) {
        chars.add(new StyledChar(text.content().charAt(i), merged));
      }
    }
    for (final Component child : component.children()) flatten(child, merged, chars);
  }

  private record StyledChar(char value, Style style) {
  }

  private static final class DynamicResolver implements ComponentResolver {

    @Override
    public PreComponent resolve(final String name, final Arguments args, final PreComponent[] children,
                                final Style inheritedStyle, final MiniMessageCompiler compiler) {
      return new DynamicPreComponent();
    }

    @Override
    public String[] names() {
      return new String[]{"dyn"};
    }

    @Override
    public boolean isSelfClosing() {
      return true;
    }
  }

  private static final class DynamicPreComponent extends PreComponent {
    private static final Component VALUE = Component.text("Z");

    @Override
    public Component render(final @NotNull Pointered pointered, final @NotNull RenderScope scope) {
      return VALUE;
    }

    @Override
    public PreComponent deepCopy() {
      return new DynamicPreComponent();
    }
  }
}
