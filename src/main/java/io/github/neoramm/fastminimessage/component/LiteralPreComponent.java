package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.pointer.Pointered;

public final class LiteralPreComponent extends PreComponent {
  private String text;
  private Component cached;

  public LiteralPreComponent(String text, Style style) {
    this.text = text;
    this.computedStyle = style;
  }

  @Override
  public Component render(Pointered pointered, RenderScope scope) {
    if (scope.isEmpty() && cached != null) return cached;
    if (computedStyle.isEmpty()) {
      cached = Component.text(text);
      return cached;
    }
    cached = Component.text(text, computedStyle);
    return cached;
  }

  @Override
  public boolean isStatic() {
    return true;
  }

  @Override
  public PreComponent setStyle(Style style) {
    cached = null;
    return super.setStyle(style);
  }

  @Override
  public PreComponent deepCopy() {
    LiteralPreComponent copy = new LiteralPreComponent(text, computedStyle);
    if (children != null) {
      PreComponent[] copied = new PreComponent[children.length];
      for (int i = 0; i < children.length; i++) {
        copied[i] = children[i].deepCopy();
      }
      copy.children = copied;
    }
    return copy;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
    this.cached = null;
  }
}