package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.internal.TokenPatterns;
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
        final @NotNull StringBuilder out = new StringBuilder(input.length() + (input.length() >> 2) + 16);
        final int n = input.length();
        int i = 0;
        int formats = 0;
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
                        formats = closeActiveFormats(out, formats);
                        out.append("<#").append(input, i + 2, i + 8).append('>');
                        i += 8;
                        continue;
                    }
                }
                final char nextChar = input.charAt(i + 1);
                if ((nextChar == 'x' || nextChar == 'X') && i + 13 < n && isLegacyHex(input, i)) {
                    formats = closeActiveFormats(out, formats);
                    out.append("<#");
                    for (int k = 0; k < 6; k++) out.append(input.charAt(i + 3 + k * 2));
                    out.append('>');
                    i += 14;
                    continue;
                }
                final char code = Character.toLowerCase(input.charAt(i + 1));
                if (isColorCode(code)) {
                    formats = closeActiveFormats(out, formats);
                    out.append('<').append(COLOR_NAMES[hexValue(code)]).append('>');
                    i += 2;
                    continue;
                }
                switch (code) {
                    case 'l' -> {
                        formats |= 1;
                        out.append("<bold>");
                        i += 2;
                        continue;
                    }
                    case 'o' -> {
                        formats |= 2;
                        out.append("<italic>");
                        i += 2;
                        continue;
                    }
                    case 'n' -> {
                        formats |= 4;
                        out.append("<underlined>");
                        i += 2;
                        continue;
                    }
                    case 'm' -> {
                        formats |= 8;
                        out.append("<strikethrough>");
                        i += 2;
                        continue;
                    }
                    case 'k' -> {
                        formats |= 16;
                        out.append("<obfuscated>");
                        i += 2;
                        continue;
                    }
                    case 'r' -> {
                        formats = 0;
                        out.append("<reset>");
                        i += 2;
                        continue;
                    }
                    case '&' -> {
                        out.append('&');
                        i += 2;
                        continue;
                    }
                    default -> {}
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

    private static int closeActiveFormats(final @NotNull StringBuilder out, final int formats) {
        if ((formats & 1) != 0) out.append("</bold>");
        if ((formats & 2) != 0) out.append("</italic>");
        if ((formats & 4) != 0) out.append("</underlined>");
        if ((formats & 8) != 0) out.append("</strikethrough>");
        if ((formats & 16) != 0) out.append("</obfuscated>");
        return 0;
    }

    private static int findTagEnd(final @NotNull String input, final int start) {
        return TokenPatterns.tagEnd(input, start);
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
            if (input.charAt(offset + 2 + k * 2) != '&' || !isHexChar(input.charAt(offset + 3 + k * 2))) return false;
        }
        return true;
    }
}
