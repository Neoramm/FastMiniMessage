package io.github.neoramm.fastminimessage.model;

import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;

/**
 * Base type of every node in a precompiled component tree.
 */
public abstract class PreComponent {

  private static final PreComponent[] EMPTY_CHILDREN = new PreComponent[0];

  protected PreComponent[] children;
  protected Style computedStyle = Style.empty();

  public Component render(final @NotNull Pointered pointered) {
    return render(pointered, RenderScope.EMPTY);
  }

  public abstract Component render(final @NotNull Pointered pointered, final @NotNull RenderScope scope);

  /**
   * Appends this component's rendered form to an existing builder.
   *
   * <p>Containers whose style is already on the builder can forward the call to their children,
   * avoiding one intermediate component per nesting level.</p>
   */
  public void renderInto(final @NotNull Pointered pointered, final @NotNull TextComponent.Builder builder) {
    renderInto(pointered, builder, RenderScope.EMPTY);
  }

  public void renderInto(final @NotNull Pointered pointered, final @NotNull TextComponent.Builder builder, final @NotNull RenderScope scope) {
    builder.append(render(pointered, scope));
  }

  public abstract PreComponent deepCopy();

  /**
   * Indicates whether rendering this component ignores the {@link Pointered} argument,
   * which allows the result to be cached across renders.
   */
  public boolean isStatic() {
    return false;
  }

  protected final boolean childrenAreStatic() {
    final PreComponent[] kids = children;
    if (kids == null) return true;
    for (final PreComponent child : kids) {
      if (!child.isStatic()) return false;
    }
    return true;
  }

  public PreComponent setChildren(PreComponent[] children) {
    this.children = children;
    return this;
  }

  public PreComponent setStyle(Style style) {
    this.computedStyle = style;
    return this;
  }

  public PreComponent prependChild(PreComponent child) {
    if (children == null) {
      children = new PreComponent[]{child};
      return this;
    }
    PreComponent[] next = new PreComponent[children.length + 1];
    next[0] = child;
    System.arraycopy(children, 0, next, 1, children.length);
    children = next;
    return this;
  }

  public PreComponent appendChild(PreComponent child) {
    if (children == null) {
      children = new PreComponent[]{child};
      return this;
    }
    PreComponent[] next = new PreComponent[children.length + 1];
    System.arraycopy(children, 0, next, 0, children.length);
    next[children.length] = child;
    children = next;
    return this;
  }

  public PreComponent replaceChild(int index, PreComponent replacement) {
    if (children != null && index >= 0 && index < children.length) {
      children[index] = replacement;
    }
    return this;
  }

  public PreComponent[] getChildren() {
    return children != null ? children : EMPTY_CHILDREN;
  }

  public Style getComputedStyle() {
    return computedStyle != null ? computedStyle : Style.empty();
  }
}