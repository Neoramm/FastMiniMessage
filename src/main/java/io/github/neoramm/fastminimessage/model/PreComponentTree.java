package io.github.neoramm.fastminimessage.model;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.PreComponentVisitor;
import io.github.neoramm.fastminimessage.component.InlinePreComponent;
import io.github.neoramm.fastminimessage.resolver.ModifierResolver;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * A compiled, renderable component tree produced by {@link MiniMessageCompiler}.
 */
public final class PreComponentTree {
  public static final PreComponentTree EMPTY = new PreComponentTree(new InlinePreComponent(Component.empty()));
  private final PreComponent root;

  public PreComponentTree(PreComponent root) {
    this.root = root;
  }

  /**
   * Renders the tree for the given context without {@link Pointered}.
   *
   * @return the rendered component
   */
  public Component render() {
    return root.render(null, RenderScope.EMPTY);
  }

  /**
   * Renders the tree for the given context.
   *
   * @param pointered the context (usually the viewer)
   * @return the rendered component
   */
  public Component render(@Nullable Pointered pointered) {
    return root.render(pointered, RenderScope.EMPTY);
  }

  public Component render(@Nullable Pointered pointered, RenderScope scope) {
    if (scope == null) scope = RenderScope.EMPTY;
    return root.render(pointered, scope);
  }

  public Component render(@Nullable Pointered pointered, ModifierResolver... modifiers) {
    return root.render(pointered, RenderScope.of(modifiers));
  }

  public Component render(@Nullable Pointered pointered, RuntimeResolver... resolvers) {
    List<Placeholder> placeholders = new java.util.ArrayList<>();
    List<ModifierResolver> modifiers = new java.util.ArrayList<>();
    if (resolvers != null) {
      for (RuntimeResolver resolver : resolvers) {
        if (resolver instanceof Placeholder placeholder) placeholders.add(placeholder);
        else if (resolver instanceof ModifierResolver modifier) modifiers.add(modifier);
      }
    }
    return root.render(pointered, RenderScope.of(placeholders, modifiers));
  }

  public Component render(Pointered pointered, PreComponent extra) {
    Component base = root.render(pointered);
    Component appended = extra.render(pointered);
    return Component.text().append(base).append(appended).build();
  }

  public Component render(Pointered pointered, PreComponent[] extras) {
    TextComponent.Builder builder = Component.text().append(root.render(pointered));
    for (PreComponent extra : extras) {
      builder.append(extra.render(pointered));
    }
    return builder.build();
  }

  public Component render(Pointered pointered, List<Placeholder> placeholders) {
    return root.render(pointered, RenderScope.of(placeholders));
  }

  public Component render(Pointered pointered, Placeholder... placeholders) {
    return root.render(pointered, RenderScope.of(placeholders));
  }

  public PreComponentTree copy() {
    return new PreComponentTree(root.deepCopy());
  }

  public void visit(PreComponentVisitor visitor) {
    visitDepthFirst(root, visitor);
  }

  private void visitDepthFirst(PreComponent node, PreComponentVisitor visitor) {
    visitor.visit(node);
    PreComponent[] childArray = node.getChildren();
    for (PreComponent child : childArray) {
      visitDepthFirst(child, visitor);
    }
  }

  public Component render(Pointered pointered, UnaryOperator<PreComponent> modifier) {
    PreComponent modifiedRoot = modifyDepthFirst(root.deepCopy(), modifier);
    return modifiedRoot.render(pointered);
  }

  private PreComponent modifyDepthFirst(PreComponent node, UnaryOperator<PreComponent> modifier) {
    node = modifier.apply(node);
    PreComponent[] childArray = node.getChildren();
    for (int i = 0; i < childArray.length; i++) {
      childArray[i] = modifyDepthFirst(childArray[i], modifier);
    }
    return node;
  }
}
