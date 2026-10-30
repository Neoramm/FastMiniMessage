package io.github.neoramm.fastminimessage.internal;

import io.github.neoramm.fastminimessage.LegacyFormatParser;
import io.github.neoramm.fastminimessage.api.TagScanner;
import io.github.neoramm.fastminimessage.model.TagNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
                final int tagStart = i;
                String content = normalized.substring(i + 1, end);
                i = end + 1;
                if (content.isEmpty()) {
                    textBuffer.append("<>");
                    continue;
                }
                final boolean isClosing = content.startsWith("/");
                final boolean isSelfClosing = content.endsWith("/");
                if (isSelfClosing && content.length() > 1) content = content.substring(0, content.length() - 1);
                if (isClosing) {
                    if (content.length() <= 1) {
                        textBuffer.append(normalized, tagStart, end + 1);
                        continue;
                    }
                    final TagParts closeParts = splitTag(content.substring(1));
                    final String closeTagName = closeParts.tagName();
                    if (!isValidTagName(closeTagName)) {
                        textBuffer.append(normalized, tagStart, end + 1);
                        continue;
                    }
                    final String closeArgs = closeParts.rawArgs();
                    if ("reset".equalsIgnoreCase(closeTagName)) continue;
                    boolean open = false;
                    for (final OpenTag tag : stack) {
                        if (tagMatches(tag, closeTagName, closeArgs)) {
                            open = true;
                            break;
                        }
                    }
                    if (!open) {
                        textBuffer.append(normalized, tagStart, end + 1);
                        continue;
                    }
                    closeUpTo(closeTagName, closeArgs, stack, roots);
                } else {
                    final TagParts parts = splitTag(content);
                    final String tagName = parts.tagName();
                    if (!isValidTagName(tagName)) {
                        textBuffer.append(normalized, tagStart, end + 1);
                        continue;
                    }
                    final boolean hasArguments = parts.rawArgs() != null;
                    final String rawArgs = hasArguments ? parts.rawArgs() : "";
                    if ("reset".equalsIgnoreCase(tagName)) {
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
            } else if (c == '\\' && i + 1 < normalized.length()) {
                final char next = normalized.charAt(i + 1);
                if (next == '\\') {
                    textBuffer.append('\\');
                    i += 2;
                } else if (next == '<') {
                    textBuffer.append('<');
                    i += 2;
                } else {
                    textBuffer.append(c);
                    i++;
                }
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

    private void flushText(final @NotNull StringBuilder buffer, final @NotNull Deque<OpenTag> stack,
        final @NotNull List<TagNode> roots) {
        if (buffer.isEmpty()) return;
        final String text = buffer.toString();
        buffer.setLength(0);
        final TagNode textNode = new TagNode("", "", text, List.of(), false, false, false, false);
        if (stack.isEmpty()) roots.add(textNode);
        else stack.peek().addChild(textNode);
    }

    private static boolean tagMatches(final @NotNull OpenTag openTag, final @NotNull String closeTagName,
        final @Nullable String closeArgs) {
        if (!openTag.tagName.equalsIgnoreCase(closeTagName)) return false;
        if (closeArgs == null) return true;
        return openTag.rawArgs.equals(closeArgs) || openTag.rawArgs.startsWith(closeArgs + ":");
    }

    private void closeUpTo(final @NotNull String closeTagName, final @Nullable String closeArgs,
        final @NotNull Deque<OpenTag> stack, final @NotNull List<TagNode> roots) {
        boolean open = false;
        for (final OpenTag tag : stack) {
            if (tagMatches(tag, closeTagName, closeArgs)) {
                open = true;
                break;
            }
        }
        if (!open) return;
        TagNode closed = null;
        while (!stack.isEmpty()) {
            final OpenTag tag = stack.pop();
            if (closed != null) tag.addChild(closed);
            final boolean matched = tagMatches(tag, closeTagName, closeArgs);
            closed = tag.build(matched);
            if (matched) break;
        }
        if (stack.isEmpty()) roots.add(closed);
        else stack.peek().addChild(closed);
    }

    private int findTagEnd(final @NotNull String input, final int start) {
        char quote = 0;
        int unclosedGreater = -1;
        for (int i = start + 1; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length()) {
                i++;
                continue;
            }
            if (quote == 0 && (c == '\'' || c == '"')) quote = c;
            else if (quote != 0 && c == quote) quote = 0;
            else if (c == '<' && quote == 0) return -1;
            else if (c == '>' && quote == 0) return i;
            else if (c == '>' && unclosedGreater == -1) unclosedGreater = i;
        }
        return quote != 0 ? unclosedGreater : -1;
    }

    private record TagParts(@NotNull String tagName, @Nullable String rawArgs) {
    }

    private static @NotNull TagParts splitTag(final @NotNull String content) {
        if (content.startsWith("#")) return new TagParts(content, content);
        final int colon = content.indexOf(':');
        if (colon == -1) return new TagParts(content, null);
        return new TagParts(content.substring(0, colon), content.substring(colon + 1));
    }

    private static boolean isValidTagName(final @NotNull String name) {
        if (name.isEmpty()) return false;
        int start = 0;
        final char first = name.charAt(0);
        if (first == '!' || first == '?' || first == '#') start = 1;
        if (start == name.length()) return false;
        for (int i = start; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || c == '_' || c == '-')) {
                return false;
            }
        }
        return true;
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
