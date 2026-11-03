package io.github.neoramm.fastminimessage.v2.internal;

import io.github.neoramm.fastminimessage.v2.api.TagScanner;
import io.github.neoramm.fastminimessage.v2.model.TagNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DefaultTagScanner implements TagScanner {

    private static final @NotNull Set<@NotNull String> FORMATTING = Set.of(
        "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray", "grey",
        "dark_gray", "dark_grey", "blue", "green", "aqua", "red", "light_purple", "yellow", "white",
        "color", "colour", "c", "bold", "b", "italic", "i", "em", "underlined", "u", "strikethrough", "st",
        "obfuscated", "obf", "!bold", "!b", "!italic", "!i", "!em", "!underlined", "!u", "!strikethrough",
        "!st", "!obfuscated", "!obf");

    @Override
    public @NotNull List<@NotNull TagNode> scan(final @NotNull String input) {
        if (input.isEmpty()) return List.of();
        if (!hasMarkup(input)) return List.of(new TagNode("", "", input, List.of(), false, false, false, false));
        final @NotNull String normalized = input;
        final @NotNull List<@NotNull TagNode> roots = new ArrayList<>();
        final @NotNull Deque<@NotNull OpenTag> stack = new ArrayDeque<>();
        final @NotNull StringBuilder textBuffer = new StringBuilder();
        int i = 0;
        while (i < normalized.length()) {
            final char c = normalized.charAt(i);
            if (c == '&' && i + 1 < normalized.length()) {
                if (normalized.charAt(i + 1) == '&') {
                    textBuffer.append('&');
                    i += 2;
                    continue;
                }
                final @Nullable LegacyCode legacy = legacyCode(normalized, i);
                if (legacy != null) {
                    flushText(textBuffer, stack, roots);
                    if (legacy.reset()) {
                        if (legacy.name().isEmpty()) closeAll(stack, roots);
                        else closeFormatting(stack, roots);
                    }
                    if (!legacy.name().isEmpty()) stack.push(new OpenTag(legacy.name(), "", false));
                    i += legacy.length();
                    continue;
                }
                textBuffer.append(c);
                i++;
            } else if (c == '<') {
                flushText(textBuffer, stack, roots);
                final int end = findTagEnd(normalized, i);
                if (end == -1) {
                    textBuffer.append(c);
                    i++;
                    continue;
                }
                final int tagStart = i;
                final int contentStart = i + 1;
                int contentEnd = end;
                i = end + 1;
                if (contentStart == contentEnd) {
                    textBuffer.append("<>");
                    continue;
                }
                final boolean isClosing = normalized.charAt(contentStart) == '/';
                final boolean isSelfClosing = normalized.charAt(contentEnd - 1) == '/';
                if (isSelfClosing && contentEnd - contentStart > 1) contentEnd--;
                if (isClosing) {
                    if (contentEnd - contentStart <= 1) {
                        textBuffer.append(normalized, tagStart, end + 1);
                        continue;
                    }
                    final @NotNull TagParts closeParts = splitTag(normalized, contentStart + 1, contentEnd);
                    final @NotNull String closeTagName = closeParts.tagName();
                    if (!isValidTagName(closeTagName)) {
                        textBuffer.append(normalized, tagStart, end + 1);
                        continue;
                    }
                    final @Nullable String closeArgs = closeParts.rawArgs();
                    if ("reset".equalsIgnoreCase(closeTagName)) continue;
                    if (!closeUpTo(closeTagName, closeArgs, stack, roots)) {
                        textBuffer.append(normalized, tagStart, end + 1);
                        continue;
                    }
                } else {
                    final @NotNull TagParts parts = splitTag(normalized, contentStart, contentEnd);
                    final @NotNull String tagName = parts.tagName();
                    if (!isValidTagName(tagName)) {
                        textBuffer.append(normalized, tagStart, end + 1);
                        continue;
                    }
                    final boolean hasArguments = parts.rawArgs() != null;
                    final @NotNull String rawArgs = hasArguments ? parts.rawArgs() : "";
                    if ("reset".equalsIgnoreCase(tagName)) {
                        closeAll(stack, roots);
                        continue;
                    }
                    if (isSelfClosing) {
                        final @NotNull TagNode selfClosing =
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
                } else if (next == '<' || next == '&') {
                    textBuffer.append(next);
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

    private void closeFormatting(final @NotNull Deque<@NotNull OpenTag> stack,
        final @NotNull List<@NotNull TagNode> roots) {
        TagNode closed = null;
        while (!stack.isEmpty() && isFormatting(stack.peek().tagName)) {
            final @NotNull OpenTag tag = stack.pop();
            if (closed != null) tag.addChild(closed);
            closed = tag.build(false);
        }
        if (closed == null) return;
        if (stack.isEmpty()) roots.add(closed);
        else stack.peek().addChild(closed);
    }

    private static boolean isFormatting(final @NotNull String tagName) {
        if (tagName.startsWith("#")) return true;
        if (FORMATTING.contains(tagName)) return true;
        for (int i = 0, len = tagName.length(); i < len; i++) {
            final char c = tagName.charAt(i);
            if (c >= 'A' && c <= 'Z') return FORMATTING.contains(tagName.toLowerCase(Locale.ROOT));
        }
        return false;
    }

    private static boolean hasMarkup(final @NotNull String input) {
        for (int i = 0, len = input.length(); i < len; i++) {
            final char c = input.charAt(i);
            if (c == '<' || c == '&' || c == '\\') return true;
        }
        return false;
    }

    private void closeAll(final @NotNull Deque<@NotNull OpenTag> stack, final @NotNull List<@NotNull TagNode> roots) {
        while (!stack.isEmpty()) {
            final @NotNull OpenTag tag = stack.pop();
            final @NotNull TagNode node = tag.build(false);
            if (stack.isEmpty()) roots.add(node);
            else stack.peek().addChild(node);
        }
    }

    private void flushText(final @NotNull StringBuilder buffer, final @NotNull Deque<@NotNull OpenTag> stack,
        final @NotNull List<@NotNull TagNode> roots) {
        if (buffer.isEmpty()) return;
        final @NotNull String text = buffer.toString();
        buffer.setLength(0);
        final @NotNull TagNode textNode = new TagNode("", "", text, List.of(), false, false, false, false);
        if (stack.isEmpty()) roots.add(textNode);
        else stack.peek().addChild(textNode);
    }

    private static boolean tagMatches(final @NotNull OpenTag openTag, final @NotNull String closeTagName,
        final @Nullable String closeArgs) {
        if (!openTag.tagName.equalsIgnoreCase(closeTagName)) return false;
        if (closeArgs == null) return true;
        final int length = closeArgs.length();
        return openTag.rawArgs.regionMatches(true, 0, closeArgs, 0, length)
            && (openTag.rawArgs.length() == length
                || openTag.rawArgs.length() > length && openTag.rawArgs.charAt(length) == ':');
    }

    private boolean closeUpTo(final @NotNull String closeTagName, final @Nullable String closeArgs,
        final @NotNull Deque<@NotNull OpenTag> stack, final @NotNull List<@NotNull TagNode> roots) {
        boolean open = false;
        for (final @NotNull OpenTag tag : stack) {
            if (tagMatches(tag, closeTagName, closeArgs)) {
                open = true;
                break;
            }
        }
        if (!open) return false;
        TagNode closed = null;
        while (!stack.isEmpty()) {
            final @NotNull OpenTag tag = stack.pop();
            if (closed != null) tag.addChild(closed);
            final boolean matched = tagMatches(tag, closeTagName, closeArgs);
            closed = tag.build(matched);
            if (matched) break;
        }
        if (stack.isEmpty()) roots.add(closed);
        else stack.peek().addChild(closed);
        return true;
    }

    public static int findTagEnd(final @NotNull String input, final int start) {
        return TokenPatterns.tagEnd(input, start);
    }

    private static final @NotNull String @NotNull [] LEGACY_COLORS = {
        "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
        "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"
    };

    private static @Nullable LegacyCode legacyCode(final @NotNull String input, final int index) {
        final char code = Character.toLowerCase(input.charAt(index + 1));
        final int color = hexValue(code);
        if (color >= 0) return new LegacyCode(LEGACY_COLORS[color], 2, true);
        if (code == '#' && index + 7 < input.length()) {
            for (int i = index + 2; i < index + 8; i++) if (hexValue(input.charAt(i)) < 0) return null;
            return new LegacyCode(input.substring(index + 1, index + 8), 8, true);
        }
        if (code == 'x' && index + 13 < input.length()) {
            final @NotNull StringBuilder colorName = new StringBuilder(7).append('#');
            for (int i = 0; i < 6; i++) {
                final char digit = input.charAt(index + 3 + i * 2);
                if (input.charAt(index + 2 + i * 2) != '&' || hexValue(digit) < 0) return null;
                colorName.append(digit);
            }
            return new LegacyCode(colorName.toString(), 14, true);
        }
        return switch (code) {
            case 'l' -> new LegacyCode("bold", 2, false);
            case 'o' -> new LegacyCode("italic", 2, false);
            case 'n' -> new LegacyCode("underlined", 2, false);
            case 'm' -> new LegacyCode("strikethrough", 2, false);
            case 'k' -> new LegacyCode("obfuscated", 2, false);
            case 'r' -> new LegacyCode("", 2, true);
            default -> null;
        };
    }

    private static int hexValue(final char character) {
        if (character >= '0' && character <= '9') return character - '0';
        if (character >= 'a' && character <= 'f') return character - 'a' + 10;
        if (character >= 'A' && character <= 'F') return character - 'A' + 10;
        return -1;
    }

    private record LegacyCode(@NotNull String name, int length, boolean reset) {
    }

    private record TagParts(@NotNull String tagName, @Nullable String rawArgs) {
    }

    private static @NotNull TagParts splitTag(final @NotNull String input, final int start, final int end) {
        if (input.charAt(start) == '#') {
            final @NotNull String color = input.substring(start, end);
            return new TagParts(color, color);
        }
        final int colon = input.indexOf(':', start, end);
        if (colon == -1) return new TagParts(input.substring(start, end), null);
        return new TagParts(input.substring(start, colon), input.substring(colon + 1, end));
    }

    public static boolean isValidTagName(final @NotNull String name) {
        return TokenPatterns.isTagName(name);
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
