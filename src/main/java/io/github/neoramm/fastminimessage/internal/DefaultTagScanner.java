package io.github.neoramm.fastminimessage.internal;

import io.github.neoramm.fastminimessage.LegacyFormatParser;
import io.github.neoramm.fastminimessage.api.TagScanner;
import io.github.neoramm.fastminimessage.model.TagNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class DefaultTagScanner implements TagScanner {

  @Override
  public List<TagNode> scan(String input) {
    input = LegacyFormatParser.toMiniMessage(input);
    List<TagNode> roots = new ArrayList<>();
    Deque<OpenTag> stack = new ArrayDeque<>();
    StringBuilder textBuffer = new StringBuilder();
    int i = 0;
    while (i < input.length()) {
      char c = input.charAt(i);
      if (c == '<') {
        flushText(textBuffer, stack, roots);
        int end = findTagEnd(input, i);
        if (end == -1) {
          textBuffer.append(c);
          i++;
          continue;
        }
        String tagText = input.substring(i, end + 1);
        String content = input.substring(i + 1, end);
        i = end + 1;
        boolean isClosing = content.startsWith("/");
        boolean isSelfClosing = content.endsWith("/");
        if (isSelfClosing) {
          content = content.substring(0, content.length() - 1);
        }
        if (isClosing) {
          String tagName = content.substring(1);
          tagName = normalizeClosingTag(tagName);
          if ("reset".equals(tagName)) {
            continue;
          }
          boolean open = false;
          for (OpenTag tag : stack) {
            if (tag.tagName.equals(tagName)) { open = true; break; }
          }
          if (!open) {
            textBuffer.append(tagText);
            continue;
          }
          closeUpTo(tagName, stack, roots);
        } else {
          String[] parts = splitTag(content);
          String tagName = normalizeTagName(parts[0]);
          String rawArgs = parts.length > 1 ? parts[1] : "";
          if ("reset".equals(tagName)) {
            closeAll(stack, roots);
            continue;
          }
          if (isSelfClosing) {
            TagNode selfClosing = new TagNode(tagName, rawArgs, "", List.of(), false, true, false);
            if (stack.isEmpty()) {
              roots.add(selfClosing);
            } else {
              stack.peek().addChild(selfClosing);
            }
          } else {
            stack.push(new OpenTag(tagName, rawArgs));
          }
        }
      } else if (c == '\\' && i + 1 < input.length() && input.charAt(i + 1) == '<') {
        textBuffer.append('<');
        i += 2;
      } else {
        textBuffer.append(c);
        i++;
      }
    }
    flushText(textBuffer, stack, roots);
    closeAll(stack, roots);
    return roots;
  }

  private void closeAll(Deque<OpenTag> stack, List<TagNode> roots) {
    while (!stack.isEmpty()) {
      OpenTag tag = stack.pop();
      TagNode node = tag.build(false);
      if (stack.isEmpty()) {
        roots.add(node);
      } else {
        stack.peek().addChild(node);
      }
    }
  }

  private String normalizeTagName(String name) {
    if (name.startsWith("#")) {
      return "color";
    }
    return name;
  }

  private String normalizeClosingTag(String name) {
    if (name.startsWith("#")) {
      return "color";
    }
    return name;
  }

  private void flushText(StringBuilder buffer, Deque<OpenTag> stack, List<TagNode> roots) {
    if (buffer.isEmpty()) return;
    String text = buffer.toString();
    buffer.setLength(0);
    TagNode textNode = new TagNode("", "", text, List.of(), false, false, false);
    if (stack.isEmpty()) {
      roots.add(textNode);
    } else {
      stack.peek().addChild(textNode);
    }
  }

  private void closeUpTo(String tagName, Deque<OpenTag> stack, List<TagNode> roots) {
    boolean open = false;
    for (OpenTag tag : stack) {
      if (tag.tagName.equals(tagName)) {
        open = true;
        break;
      }
    }
    if (!open) {
      return;
    }
    TagNode closed = null;
    while (!stack.isEmpty()) {
      OpenTag tag = stack.pop();
      if (closed != null) {
        tag.addChild(closed);
      }
      boolean matched = tag.tagName.equals(tagName);
      closed = tag.build(matched);
      if (matched) {
        break;
      }
    }
    if (stack.isEmpty()) {
      roots.add(closed);
    } else {
      stack.peek().addChild(closed);
    }
  }

  private int findTagEnd(String input, int start) {
    char quote = 0;
    for (int i = start + 1; i < input.length(); i++) {
      char c = input.charAt(i);
      if (quote == 0 && (c == '\'' || c == '"')) {
        quote = c;
      } else if (quote != 0 && c == quote) {
        quote = 0;
      } else if (c == '>' && quote == 0) {
        return i;
      }
    }
    return -1;
  }

  private String[] splitTag(String content) {
    if (content.startsWith("#")) {
      return new String[]{"color", content};
    }
    int colon = content.indexOf(':');
    if (colon == -1) {
      return new String[]{content};
    }
    return new String[]{content.substring(0, colon), content.substring(colon + 1)};
  }

  private static final class OpenTag {
    final String tagName;
    final String rawArgs;
    final List<TagNode> children = new ArrayList<>();

    OpenTag(String tagName, String rawArgs) {
      this.tagName = tagName;
      this.rawArgs = rawArgs;
    }

    void addChild(TagNode child) {
      children.add(child);
    }

    TagNode build() {
      return build(false);
    }

    TagNode build(boolean explicitlyClosed) {
      return new TagNode(tagName, rawArgs, "", children, false, false, explicitlyClosed);
    }
  }
}