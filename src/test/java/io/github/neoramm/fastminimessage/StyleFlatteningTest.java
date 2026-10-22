package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.ClickPreComponent;
import io.github.neoramm.fastminimessage.component.ColorPreComponent;
import io.github.neoramm.fastminimessage.component.GradientPreComponent;
import io.github.neoramm.fastminimessage.component.GroupPreComponent;
import io.github.neoramm.fastminimessage.component.HoverPreComponent;
import io.github.neoramm.fastminimessage.component.InsertingPreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.component.ShadowPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.Placeholder;
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
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class StyleFlatteningTest {

  private static final Pointered NOBODY = new Pointered() {
    @Override
    public Pointers pointers() {
      return Pointers.empty();
    }
  };

  private final MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());

  @Test
  void styleWrappersArePushedIntoLiteralLeaves() {
    final PreComponentTree tree = compiler.compile(
      "<bold><red>a</red> <click:run_command:'/x'>b</click></bold>");
    final List<PreComponent> nodes = nodesOf(tree);

    assertFalse(nodes.stream().anyMatch(node -> node instanceof ColorPreComponent));
    assertFalse(nodes.stream().anyMatch(node -> node instanceof ClickPreComponent));
    assertFalse(nodes.stream().anyMatch(node -> node instanceof InsertingPreComponent));
    assertFalse(nodes.stream().anyMatch(node -> node instanceof ShadowPreComponent));
    assertFalse(nodes.stream().anyMatch(node ->
      node instanceof GroupPreComponent group && !group.getComputedStyle().isEmpty()));

    final List<LiteralPreComponent> literals = literalsOf(nodes);
    assertEquals(3, literals.size());
    assertEquals("a", literals.getFirst().getText());
    assertEquals(NamedTextColor.RED, literals.getFirst().getComputedStyle().color());
    assertEquals(TextDecoration.State.TRUE, literals.getFirst().getComputedStyle().decoration(TextDecoration.BOLD));
    assertEquals(" ", literals.get(1).getText());
    assertEquals("b", literals.get(2).getText());
    assertEquals(ClickEvent.runCommand("/x"), literals.get(2).getComputedStyle().clickEvent());
    assertEquals(TextDecoration.State.TRUE, literals.get(2).getComputedStyle().decoration(TextDecoration.BOLD));
  }

  @Test
  void adjacentLiteralsWithTheSameStyleAreMerged() {
    final PreComponentTree tree = compiler.compile("<red>a</red><red>b</red><red>c</red>");
    final List<LiteralPreComponent> literals = literalsOf(nodesOf(tree));
    assertEquals(1, literals.size());
    assertEquals("abc", literals.getFirst().getText());
    assertEquals(NamedTextColor.RED, literals.getFirst().getComputedStyle().color());
  }

  @Test
  void adjacentLiteralsWithTheSameClickEventAreMerged() {
    final PreComponentTree tree = compiler.compile(
      "<click:run_command:'/x'>a</click><click:run_command:'/x'>b</click>");
    final List<LiteralPreComponent> literals = literalsOf(nodesOf(tree));
    assertEquals(1, literals.size());
    assertEquals("ab", literals.getFirst().getText());
    assertEquals(ClickEvent.runCommand("/x"), literals.getFirst().getComputedStyle().clickEvent());
  }

  @Test
  void literalsWithDifferentStylesOrInBetweenTextAreNotMerged() {
    final PreComponentTree tree = compiler.compile("<red>a</red><blue>b</blue>");
    final List<LiteralPreComponent> literals = literalsOf(nodesOf(tree));
    assertEquals(2, literals.size());
    assertEquals(NamedTextColor.RED, literals.getFirst().getComputedStyle().color());
    assertEquals(NamedTextColor.BLUE, literals.get(1).getComputedStyle().color());

    final List<LiteralPreComponent> separated = literalsOf(nodesOf(compiler.compile("<red>a</red>b<red>c</red>")));
    assertEquals(3, separated.size());
    assertEquals(NamedTextColor.RED, separated.getFirst().getComputedStyle().color());
    assertEquals(Style.empty(), separated.get(1).getComputedStyle());
    assertEquals(NamedTextColor.RED, separated.get(2).getComputedStyle().color());
  }

  @Test
  void negatedDecorationsStayOnTheirLiteral() {
    final List<LiteralPreComponent> literals = literalsOf(
      nodesOf(compiler.compile("<bold>a<!bold>b</!bold>c</bold>")));
    assertEquals(3, literals.size());
    assertEquals(TextDecoration.State.TRUE, literals.getFirst().getComputedStyle().decoration(TextDecoration.BOLD));
    assertEquals(TextDecoration.State.FALSE, literals.get(1).getComputedStyle().decoration(TextDecoration.BOLD));
    assertEquals(TextDecoration.State.TRUE, literals.get(2).getComputedStyle().decoration(TextDecoration.BOLD));
  }

  @Test
  void resetClosesOpenTags() {
    final List<LiteralPreComponent> literals = literalsOf(
      nodesOf(compiler.compile("<bold><red>a<reset>b")));
    assertEquals(2, literals.size());
    assertEquals(NamedTextColor.RED, literals.getFirst().getComputedStyle().color());
    assertEquals(TextDecoration.State.TRUE, literals.getFirst().getComputedStyle().decoration(TextDecoration.BOLD));
    assertEquals(Style.empty(), literals.get(1).getComputedStyle());
  }

  @Test
  void gradientsAndHoversAbsorbTheStyleOfTheirAncestors() {
    final PreComponentTree tree = compiler.compile(
      "<red><gradient:red:blue>abc</gradient><hover:show_text:'h'>x</hover></red>");
    final List<PreComponent> nodes = nodesOf(tree);
    final GradientPreComponent gradient = firstOf(nodes, GradientPreComponent.class);
    final HoverPreComponent hover = firstOf(nodes, HoverPreComponent.class);
    assertNotNull(gradient);
    assertNotNull(hover);
    assertEquals(NamedTextColor.RED, gradient.getComputedStyle().color());
    assertEquals(NamedTextColor.RED, hover.getComputedStyle().color());
  }

  @Test
  void unknownDynamicComponentsKeepTheInheritedStyle() {
    final ResolverRegistry registry = ResolverRegistry.withDefaults().register(new DynamicResolver());
    final PreComponentTree tree = new MiniMessageCompiler(registry).compile("<bold><red><dyn/></red></bold>");
    final List<PreComponent> nodes = nodesOf(tree);
    final GroupPreComponent wrapper = nodes.stream()
      .filter(node -> node instanceof GroupPreComponent)
      .map(GroupPreComponent.class::cast)
      .filter(group -> !group.getComputedStyle().isEmpty())
      .findFirst()
      .orElseThrow();
    assertEquals(NamedTextColor.RED, wrapper.getComputedStyle().color());
    assertEquals(TextDecoration.State.TRUE, wrapper.getComputedStyle().decoration(TextDecoration.BOLD));

    final List<StyledChar> chars = flatten(tree.render(NOBODY));
    assertEquals(1, chars.size());
    assertEquals(NamedTextColor.RED, chars.getFirst().style().color());
    assertEquals(TextDecoration.State.TRUE, chars.getFirst().style().decoration(TextDecoration.BOLD));
  }

  @Test
  void unparsedPlaceholdersKeepTheInheritedStyle() {
    final PreComponentTree tree = compiler.compile("<bold><red><p/></red></bold>");
    final Component rendered = tree.render(NOBODY, Placeholder.ofUnparsed("p", "X"));
    final List<StyledChar> chars = flatten(rendered);
    assertEquals(1, chars.size());
    assertEquals('X', chars.getFirst().value());
    assertEquals(NamedTextColor.RED, chars.getFirst().style().color());
    assertEquals(TextDecoration.State.TRUE, chars.getFirst().style().decoration(TextDecoration.BOLD));
  }

  @Test
  void unparsedFallbacksKeepTheInheritedStyle() {
    final PreComponentTree tree = compiler.compile("<bold><red><unknown></red></bold>");
    final List<StyledChar> chars = flatten(tree.render(NOBODY));
    assertEquals("<unknown>".length(), chars.size());
    for (final StyledChar styled : chars) {
      assertEquals(NamedTextColor.RED, styled.style().color());
      assertEquals(TextDecoration.State.TRUE, styled.style().decoration(TextDecoration.BOLD));
    }
  }

  @Test
  void unknownColorsFallBackToNoColor() {
    final List<StyledChar> named = flatten(compiler.compile("<color:nope>a</color>").render(NOBODY));
    assertEquals(1, named.size());
    assertEquals('a', named.getFirst().value());
    assertNull(named.getFirst().style().color());

    final List<StyledChar> empty = flatten(compiler.compile("<color>a</color>").render(NOBODY));
    assertEquals(1, empty.size());
    assertEquals('a', empty.getFirst().value());
    assertNull(empty.getFirst().style().color());
  }

  @Test
  void emptyAndMalformedInputsCompileAndRender() {
    assertDoesNotThrow(() -> compiler.compile("").render(NOBODY));
    assertDoesNotThrow(() -> compiler.compile("<bold>").render(NOBODY));
    assertDoesNotThrow(() -> compiler.compile("</bold>").render(NOBODY));
    assertDoesNotThrow(() -> compiler.compile("<red>a").render(NOBODY));
    assertDoesNotThrow(() -> compiler.compile("<color:nope>a</color>").render(NOBODY));
    assertDoesNotThrow(() -> compiler.compile("<sprite/>").render(NOBODY));
    assertDoesNotThrow(() -> compiler.compile("<hover:show_text:'x'/>").render(NOBODY));
    assertDoesNotThrow(() -> compiler.compile("<click/>").render(NOBODY));
  }

  private static List<PreComponent> nodesOf(final @NotNull PreComponentTree tree) {
    final List<PreComponent> nodes = new ArrayList<>();
    tree.visit(nodes::add);
    return nodes;
  }

  private static List<LiteralPreComponent> literalsOf(final @NotNull List<PreComponent> nodes) {
    final List<LiteralPreComponent> literals = new ArrayList<>();
    for (final PreComponent node : nodes) {
      if (node instanceof LiteralPreComponent literal) literals.add(literal);
    }
    return literals;
  }

  private static <T extends PreComponent> @Nullable T firstOf(final @NotNull List<PreComponent> nodes,
                                                             final @NotNull Class<T> type) {
    for (final PreComponent node : nodes) {
      if (type.isInstance(node)) return type.cast(node);
    }
    return null;
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
