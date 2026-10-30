package io.github.neoramm.fastminimessage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Converts legacy ampersand color/format codes into a MiniMessage string.
 *
 * <p>Optimized char-by-char scan; no regular expressions.</p>
 */
public final class LegacyFormatParser {
    private LegacyFormatParser() {
    }

    private static final String @NotNull [] COLOR_NAMES = {
        "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
        "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
        "yellow", "white"
    };

    public static @NotNull String toMiniMessage(final @Nullable String input) {
        if (input == null) return "";
        if (input.isEmpty()) return input;
        if (input.indexOf('&') == -1) return input;
        final StringBuilder out = new StringBuilder(input.length() + (input.length() >> 2) + 16);
        final int n = input.length();
        int i = 0;
        while (i < n) {
            final char c = input.charAt(i);
            if (c == '\\' && i + 1 < n) {
                final char next = input.charAt(i + 1);
                if (next == '&') {
                    out.append('&');
                    i += 2;
                    continue;
                }
                if (next == '\\') {
                    out.append("\\\\");
                    i += 2;
                    continue;
                }
                if (next == '<') {
                    out.append("\\<");
                    i += 2;
                    continue;
                }
                out.append(c);
                i++;
                continue;
            }
            if (c == '<') {
                final int tagEnd = findTagEnd(input, i);
                if (tagEnd != -1) {
                    out.append(input, i, tagEnd + 1);
                    i = tagEnd + 1;
                    continue;
                }
            }
            if (c == '&' && i + 1 < n) {
                if (input.charAt(i + 1) == '#' && i + 7 < n) {
                    if (isHex(input, i + 2, i + 8)) {
                        out.append("<reset><#").append(input, i + 2, i + 8).append('>');
                        i += 8;
                        continue;
                    }
                }
                final char nextChar = input.charAt(i + 1);
                if ((nextChar == 'x' || nextChar == 'X') && i + 13 < n && isLegacyHex(input, i)) {
                    out.append("<reset><#");
                    for (int k = 0; k < 6; k++) out.append(input.charAt(i + 3 + k * 2));
                    out.append('>');
                    i += 14;
                    continue;
                }
                final char code = Character.toLowerCase(input.charAt(i + 1));
                if (isColorCode(code)) {
                    out.append("<reset><").append(COLOR_NAMES[hexValue(code)]).append('>');
                    i += 2;
                    continue;
                }
                final String tag = formatTag(code);
                if (tag != null) {
                    out.append('<').append(tag).append('>');
                    i += 2;
                    continue;
                }
                if (code == 'r') {
                    out.append("<reset>");
                    i += 2;
                    continue;
                }
                if (code == '&') {
                    out.append('&');
                    i += 2;
                    continue;
                }
                out.append(c);
                i++;
                continue;
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static int findTagEnd(final @NotNull String input, final int start) {
        if (start + 1 >= input.length()) return -1;
        final char next = input.charAt(start + 1);
        if (next != '/' && next != '!' && next != '#' && next != '?'
            && !((next >= 'a' && next <= 'z') || (next >= 'A' && next <= 'Z'))) {
            return -1;
        }
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

    private static boolean isColorCode(final char code) {
        return (code >= '0' && code <= '9') || (code >= 'a' && code <= 'f');
    }

    private static int hexValue(final char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    }

    private static boolean isHexChar(final char c) {
        return hexValue(c) >= 0;
    }

    private static boolean isHex(final @NotNull String input, final int start, final int end) {
        for (int i = start; i < end; i++) if (!isHexChar(input.charAt(i))) return false;
        return true;
    }

    private static boolean isLegacyHex(final @NotNull String input, final int offset) {
        for (int k = 0; k < 6; k++) {
            if (input.charAt(offset + 2 + k * 2) != '&' || !isHexChar(input.charAt(offset + 3 + k * 2))) {
                return false;
            }
        }
        return true;
    }

    private static @Nullable String formatTag(final char code) {
        return switch (code) {
            case 'l' -> "bold";
            case 'o' -> "italic";
            case 'n' -> "underlined";
            case 'm' -> "strikethrough";
            case 'k' -> "obfuscated";
            default -> null;
        };
    }
}
