package io.github.neoramm.fastminimessage.internal;

import io.github.neoramm.fastminimessage.LegacyFormatParser;
import io.github.neoramm.fastminimessage.api.TagScanner;
import io.github.neoramm.fastminimessage.model.TagNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class DefaultTagScanner implements TagScanner {

    @Override
    public @NotNull List<@NotNull TagNode> scan(final @NotNull String input) {
        final String normalized = LegacyFormatParser.toMiniMessage(input);
        final List<TagNode> roots = new ArrayList<>();
        final Deque<OpenTag> stack = new ArrayDeque<>();
        final StringBuilder textBuffer = new StringBuilder();
        int i = 0;
        while (i < normalized.length()) {
            final char c = normalized.charAt(i);
            if (c == '<') {
                flushText(textBuffer, stack, roots);
                final int end = findTagEnd(normalized, i);
                if (end == -1) {
                    textBuffer.append(c);
                    i++;
                    continue;
                }
                final String tagText = normalized.substring(i, end + 1);
                String content = normalized.substring(i + 1, end);
                i = end + 1;
                final boolean isClosing = content.startsWith("/");
                final boolean isSelfClosing = content.endsWith("/");
                if (isSelfClosing) content = content.substring(0, content.length() - 1);
                if (isClosing) {
                    final String tagName = normalizeName(content.substring(1));
                    if ("reset".equals(tagName)) continue;
                    boolean open = false;
                    for (final OpenTag tag : stack) {
                        if (tag.tagName.equals(tagName)) {
                            open = true;
                            break;
                        }
                    }
                    if (!open) {
                        textBuffer.append(tagText);
                        continue;
                    }
                    closeUpTo(tagName, stack, roots);
                } else {
                    final String[] parts = splitTag(content);
                    final String tagName = normalizeName(parts[0]);
                    final boolean hasArguments = parts.length > 1;
                    final String rawArgs = hasArguments ? parts[1] : "";
                    if ("reset".equals(tagName)) {
                        closeAll(stack, roots);
                        continue;
                    }
                    if (isSelfClosing) {
                        final TagNode selfClosing =
                            new TagNode(tagName, rawArgs, "", List.of(), hasArguments, false, true, false);
                        if (stack.isEmpty()) roots.add(selfClosing);
                        else stack.peek().addChild(selfClosing);
                    } else {
                        stack.push(new OpenTag(tagName, rawArgs, hasArguments));
                    }
                }
            } else if (c == '\\' && i + 1 < normalized.length() && normalized.charAt(i + 1) == '<') {
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

    private void closeAll(final @NotNull Deque<OpenTag> stack, final @NotNull List<TagNode> roots) {
        while (!stack.isEmpty()) {
            final OpenTag tag = stack.pop();
            final TagNode node = tag.build(false);
            if (stack.isEmpty()) roots.add(node);
            else stack.peek().addChild(node);
        }
    }

    private @NotNull String normalizeName(final @NotNull String name) {
        if (name.startsWith("#")) return "color";
        return name;
    }

    private void flushText(final @NotNull StringBuilder buffer, final @NotNull Deque<OpenTag> stack,
        final @NotNull List<TagNode> roots) {
        if (buffer.isEmpty()) return;
        final String text = buffer.toString();
        buffer.setLength(0);
        final TagNode textNode = new TagNode("", "", text, List.of(), false, false, false, false);
        if (stack.isEmpty()) roots.add(textNode);
        else stack.peek().addChild(textNode);
    }

    private void closeUpTo(final @NotNull String tagName, final @NotNull Deque<OpenTag> stack,
        final @NotNull List<TagNode> roots) {
        boolean open = false;
        for (final OpenTag tag : stack) {
            if (tag.tagName.equals(tagName)) {
                open = true;
                break;
            }
        }
        if (!open) return;
        TagNode closed = null;
        while (!stack.isEmpty()) {
            final OpenTag tag = stack.pop();
            if (closed != null) tag.addChild(closed);
            final boolean matched = tag.tagName.equals(tagName);
            closed = tag.build(matched);
            if (matched) break;
        }
        if (stack.isEmpty()) roots.add(closed);
        else stack.peek().addChild(closed);
    }

    private int findTagEnd(final @NotNull String input, final int start) {
        char quote = 0;
        for (int i = start + 1; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (quote == 0 && (c == '\'' || c == '"')) quote = c;
            else if (quote != 0 && c == quote) quote = 0;
            else if (c == '>' && quote == 0) return i;
        }
        return -1;
    }

    private @NotNull String @NotNull [] splitTag(final @NotNull String content) {
        if (content.startsWith("#")) return new String[]{"color", content};
        final int colon = content.indexOf(':');
        if (colon == -1) return new String[]{content};
        return new String[]{content.substring(0, colon), content.substring(colon + 1)};
    }

    private static final class OpenTag {
        final @NotNull String tagName;
        final @NotNull String rawArgs;
        final boolean hasArguments;
        final @NotNull List<@NotNull TagNode> children = new ArrayList<>();

        OpenTag(final @NotNull String tagName, final @NotNull String rawArgs, final boolean hasArguments) {
            this.tagName = tagName;
            this.rawArgs = rawArgs;
            this.hasArguments = hasArguments;
        }

        void addChild(final @NotNull TagNode child) {
            children.add(child);
        }

        @NotNull TagNode build(final boolean explicitlyClosed) {
            return new TagNode(tagName, rawArgs, "", children, hasArguments, false, false, explicitlyClosed);
        }
    }
}
