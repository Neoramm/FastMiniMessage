package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentModifier;
import io.github.neoramm.fastminimessage.component.ModifiedPreComponent;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DeferredTagPreComponent extends PreComponent {

  private final @NotNull String tagName;
  private final @NotNull String rawArgsTemplate;
  private final boolean selfClosing;
  private final boolean explicitlyClosed;
  private final @NotNull MiniMessageCompiler compiler;
  private @Nullable String lastArgs;
  private @Nullable PreComponent lastResolved;

  public DeferredTagPreComponent(
    final @NotNull String tagName,
    final @NotNull String rawArgsTemplate,
    final @NotNull PreComponent[] children,
    final @NotNull Style inheritedStyle,
    final boolean selfClosing,
    final boolean explicitlyClosed,
    final @NotNull MiniMessageCompiler compiler
  ) {
    this.tagName = tagName;
    this.rawArgsTemplate = rawArgsTemplate;
    this.selfClosing = selfClosing;
    this.explicitlyClosed = explicitlyClosed;
    this.compiler = compiler;
    this.children = children;
    this.computedStyle = inheritedStyle;
  }

  @Override
  public @NotNull Component render(final @NotNull Pointered pointered, final @NotNull RenderScope scope) {
    final String substituted = substitute(rawArgsTemplate, scope, pointered);
    if (lastResolved == null || lastArgs == null || !lastArgs.equals(substituted) || scope.resolveModifier(tagName) != null) {
      lastArgs = substituted;
      final ComponentModifier modifier = scope.resolveModifier(tagName);
      if (modifier != null) {
        lastResolved = new ModifiedPreComponent(modifier, children).setStyle(computedStyle);
      } else {
        lastResolved = compiler.resolveTag(tagName, substituted, children, computedStyle, selfClosing, explicitlyClosed);
      }
    }
    return lastResolved.render(pointered, scope);
  }

  @Override
  public boolean isStatic() {
    return false;
  }

  @Override
  public @NotNull PreComponent deepCopy() {
    final PreComponent[] copied = new PreComponent[children.length];
    for (int i = 0; i < children.length; i++) copied[i] = children[i].deepCopy();
    return new DeferredTagPreComponent(tagName, rawArgsTemplate, copied, computedStyle, selfClosing, explicitlyClosed, compiler);
  }

  private static @NotNull String substitute(
    final @NotNull String template, final @NotNull RenderScope scope, final @NotNull Pointered pointered
  ) {
    if (scope.isEmpty()) return template;
    final StringBuilder out = new StringBuilder(template.length());
    int i = 0;
    while (i < template.length()) {
      final int open = template.indexOf('<', i);
      if (open == -1) {
        out.append(template, i, template.length());
        break;
      }
      out.append(template, i, open);
      final int close = template.indexOf('>', open);
      if (close == -1) {
        out.append(template, open, template.length());
        break;
      }
      final String candidate = template.substring(open + 1, close);
      final PreComponent placeholder = scope.resolve(candidate);
      if (placeholder != null) {
        final Component rendered = placeholder.render(pointered, scope);
        out.append(plainText(rendered));
      } else {
        out.append(template, open, close + 1);
      }
      i = close + 1;
    }
    return out.toString();
  }

  private static @NotNull String plainText(final @NotNull Component component) {
    final StringBuilder sb = new StringBuilder();
    appendPlain(sb, component);
    return sb.toString();
  }

  private static void appendPlain(final @NotNull StringBuilder sb, final @NotNull Component component) {
    if (component instanceof TextComponent text) sb.append(text.content());
    for (final Component child : component.children()) appendPlain(sb, child);
  }
}
