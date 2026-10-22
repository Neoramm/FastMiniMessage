package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.api.ComponentModifier;
import io.github.neoramm.fastminimessage.api.TagScanner;
import io.github.neoramm.fastminimessage.resolver.ModifierResolver;
import io.github.neoramm.fastminimessage.component.DeferredTagPreComponent;
import io.github.neoramm.fastminimessage.component.GroupPreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.component.StyleFlattener;
import io.github.neoramm.fastminimessage.component.UnparsedPreComponent;
import io.github.neoramm.fastminimessage.internal.DefaultTagScanner;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import io.github.neoramm.fastminimessage.model.TagNode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Compiles MiniMessage strings into a precompiled component tree that renders to Adventure components.
 */
public class MiniMessageCompiler {
  private final ResolverRegistry registry;
  private final TagScanner scanner;
  private final MiniMessage miniMessage = MiniMessage.miniMessage();

  public MiniMessageCompiler(ResolverRegistry registry) {
    this.registry = registry;
    this.scanner = new DefaultTagScanner();
  }

  /**
   * Compiles the given MiniMessage string into a renderable tree.
   *
   * @param miniMessageString the MiniMessage string to compile
   * @return the compiled tree, or an empty tree when the input is null
   */
  public PreComponentTree compile(String miniMessageString) {
    if (miniMessageString == null) return PreComponentTree.EMPTY;
    List<TagNode> roots = scanner.scan(miniMessageString);
    roots = flattenSelfClosing(roots);
    List<PreComponent> components = new ArrayList<>();
    for (TagNode root : roots) {
      components.add(resolveStyled(buildStyledNode(root, Style.empty())));
    }
    PreComponent rootComponent;
    if (components.size() == 1) {
      rootComponent = components.getFirst();
    } else {
      rootComponent = new GroupPreComponent(components.toArray(new PreComponent[0]));
    }
    return new PreComponentTree(StyleFlattener.flatten(rootComponent));
  }

  public Component deserialize(String input) {
    return miniMessage.deserialize(input);
  }

  public void registerModifier(final String name, final ComponentModifier modifier) {
    registry.register(new ModifierResolver(name, modifier));
  }

  private List<TagNode> flattenSelfClosing(List<TagNode> nodes) {
    List<TagNode> result = new ArrayList<>();
    for (TagNode node : nodes) {
      ComponentResolver resolver = registry.resolve(node.tagName());
      boolean isSelfClosing = resolver != null && resolver.isSelfClosing();
      List<TagNode> flattenedChildren = flattenSelfClosing(node.children());

      if (isSelfClosing && !flattenedChildren.isEmpty()) {
        result.add(new TagNode(
          node.tagName(),
          node.rawArgs(),
          node.textContent(),
          List.of(),
          false,
          true,
          node.explicitlyClosed()
        ));
        result.addAll(flattenedChildren);
      } else {
        result.add(new TagNode(
          node.tagName(),
          node.rawArgs(),
          node.textContent(),
          flattenedChildren,
          node.isClosingTag(),
          node.isSelfClosing(),
          node.explicitlyClosed()
        ));
      }
    }
    return result;
  }

  private StyledNode buildStyledNode(TagNode node, Style computed) {
    Style childInherited = "reset".equals(node.tagName()) ? Style.empty() : computed;

    List<StyledNode> styledChildren = new ArrayList<>();
    for (TagNode child : node.children()) {
      styledChildren.add(buildStyledNode(child, childInherited));
    }
    return new StyledNode(node, computed, styledChildren);
  }

  private PreComponent resolveStyled(StyledNode styled) {
    TagNode node = styled.node;
    PreComponent[] childComponents = new PreComponent[styled.children.size()];
    for (int i = 0; i < styled.children.size(); i++) {
      childComponents[i] = resolveStyled(styled.children.get(i));
    }
    if (containsPlaceholder(node.rawArgs())) {
      return new DeferredTagPreComponent(node.tagName(), node.rawArgs(), childComponents, styled.computedStyle, node.isSelfClosing(), node.explicitlyClosed(), this);
    }
    if (node.tagName().isEmpty())
      return new LiteralPreComponent(node.textContent(), styled.computedStyle);
    ComponentResolver resolver = registry.resolve(node.tagName());
    if (node.isSelfClosing()) {
      if (resolver != null)
        return resolveSafely(resolver, node, new PreComponent[0], styled.computedStyle);
      String selfTag = node.rawArgs().isEmpty()
        ? "<" + node.tagName() + "/>"
        : "<" + node.tagName() + ":" + node.rawArgs() + "/>";
      return new UnparsedPreComponent(node.tagName(), selfTag);
    }
    if (resolver == null)
      return new DeferredTagPreComponent(node.tagName(), node.rawArgs(), childComponents, styled.computedStyle, node.isSelfClosing(), node.explicitlyClosed(), this);
    return resolveSafely(resolver, node, childComponents, styled.computedStyle);
  }

  private PreComponent resolveSafely(ComponentResolver resolver, TagNode node, PreComponent[] children, Style style) {
    try {
      return resolver.resolve(node.tagName(), Arguments.parse(node.rawArgs()), children, style, this);
    } catch (RuntimeException | LinkageError unsupported) {
      PreComponent[] parts = new PreComponent[children.length + 1];
      parts[0] = new UnparsedPreComponent(rawTag(node), rawTag(node));
      System.arraycopy(children, 0, parts, 1, children.length);
      return new GroupPreComponent(parts);
    }
  }

  private PreComponent unknownTag(TagNode node, PreComponent[] childComponents, Style style) {
    List<PreComponent> parts = new ArrayList<>();
    parts.add(new UnparsedPreComponent(node.tagName(), rawTag(node)));
    for (PreComponent child : childComponents) parts.add(child);
    if (node.explicitlyClosed()) {
      parts.add(new UnparsedPreComponent(node.tagName(), "</" + node.tagName() + ">"));
    }
    GroupPreComponent group = new GroupPreComponent(parts.toArray(new PreComponent[0]));
    group.setStyle(style);
    return group;
  }

  private static String rawTag(TagNode node) {
    return node.rawArgs().isEmpty()
      ? "<" + node.tagName() + ">"
      : "<" + node.tagName() + ":" + node.rawArgs() + ">";
  }

  public PreComponent resolveTag(final String tagName, final String rawArgs, final PreComponent[] children, final Style inheritedStyle, final boolean selfClosing, final boolean explicitlyClosed) {
    if (tagName.isEmpty()) return new LiteralPreComponent("", inheritedStyle);
    final ComponentResolver resolver = registry.resolve(tagName);
    if (selfClosing) {
      if (resolver != null) {
        try {
          return resolver.resolve(tagName, Arguments.parse(rawArgs), new PreComponent[0], inheritedStyle, this);
        } catch (RuntimeException | LinkageError unsupported) {
          final String selfTag = rawArgs.isEmpty() ? "<" + tagName + "/>" : "<" + tagName + ":" + rawArgs + "/>";
          return new UnparsedPreComponent(tagName, selfTag);
        }
      }
      final String selfTag = rawArgs.isEmpty() ? "<" + tagName + "/>" : "<" + tagName + ":" + rawArgs + "/>";
      return new UnparsedPreComponent(tagName, selfTag);
    }
    if (resolver == null) {
      final List<PreComponent> parts = new ArrayList<>();
      final String openTag = rawArgs.isEmpty() ? "<" + tagName + ">" : "<" + tagName + ":" + rawArgs + ">";
      parts.add(new UnparsedPreComponent(tagName, openTag));
      for (final PreComponent child : children) parts.add(child);
      if (explicitlyClosed) parts.add(new UnparsedPreComponent(tagName, "</" + tagName + ">"));
      final GroupPreComponent group = new GroupPreComponent(parts.toArray(new PreComponent[0]));
      group.setStyle(inheritedStyle);
      return group;
    }
    try {
      return resolver.resolve(tagName, Arguments.parse(rawArgs), children, inheritedStyle, this);
    } catch (RuntimeException | LinkageError unsupported) {
      final String openTag = rawArgs.isEmpty() ? "<" + tagName + ">" : "<" + tagName + ":" + rawArgs + ">";
      final PreComponent[] parts = new PreComponent[children.length + 1];
      parts[0] = new UnparsedPreComponent(openTag, openTag);
      System.arraycopy(children, 0, parts, 1, children.length);
      return new GroupPreComponent(parts).setStyle(inheritedStyle);
    }
  }

  private static boolean containsPlaceholder(final String rawArgs) {
    if (rawArgs == null || rawArgs.isEmpty()) return false;
    int i = 0;
    while (true) {
      final int open = rawArgs.indexOf('<', i);
      if (open == -1) break;
      final int close = rawArgs.indexOf('>', open);
      if (close == -1) break;
      final String candidate = rawArgs.substring(open + 1, close);
      if (!candidate.isEmpty()) {
        boolean identifier = true;
        for (int c = 0; c < candidate.length(); c++) {
          if (!Character.isJavaIdentifierPart(candidate.charAt(c))) { identifier = false; break; }
        }
        if (identifier) return true;
      }
      i = close + 1;
    }
    return false;
  }

  private record StyledNode(TagNode node, Style computedStyle, List<StyledNode> children) {
  }
}