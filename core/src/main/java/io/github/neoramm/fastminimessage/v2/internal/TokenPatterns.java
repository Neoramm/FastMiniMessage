package io.github.neoramm.fastminimessage.v2.internal;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Character-level validators and scanners used by the tag scanner.
 *
 * <p>Validators are implemented as 128-bit bitmasks over the ASCII range so that all accepted
 * characters ({@code a-z A-Z 0-9 _ - . ! ? #}) can be checked without allocating or touching
 * large transition tables.</p>
 */
public final class TokenPatterns {

    private static final long NAME_LOW;
    private static final long NAME_HIGH;

    static {
        long low = 0L;
        long high = 0L;
        for (final char c : "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_.-".toCharArray()) {
            if (c < 64) low |= 1L << c;
            else high |= 1L << (c - 64);
        }
        NAME_LOW = low;
        NAME_HIGH = high;
    }

    private TokenPatterns() {
    }

    /**
     * Returns the index of the {@code '>'} that closes the tag opening at {@code start}, or
     * {@code -1} when the tag is malformed and cannot be recovered.
     */
    public static int tagEnd(final @NotNull String input, final int start) {
        Objects.requireNonNull(input, "input");
        char quote = 0;
        int unclosedGreater = -1;
        int nested = -1;
        for (int i = start + 1; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length()) {
                i++;
                continue;
            }
            if (quote == 0 && (c == '\'' || c == '"')) quote = c;
            else if (quote != 0 && c == quote) quote = 0;
            else if (c == '<') {
                if (quote == 0) return -1;
                if (nested < 0) nested = i;
            } else if (c == '>' && quote == 0) return i;
            else if (c == '>' && unclosedGreater == -1) unclosedGreater = i;
        }
        return quote != 0 && (nested < 0 || unclosedGreater < nested) ? unclosedGreater : -1;
    }

    public static boolean isTagName(final @NotNull String value) {
        return matchesName(Objects.requireNonNull(value, "value"));
    }

    public static boolean isScalarKey(final @NotNull String value) {
        return matchesName(Objects.requireNonNull(value, "value"));
    }

    public static boolean isSerializedItem(final @NotNull String value) {
        Objects.requireNonNull(value, "value");
        if (value.length() < 16 || !value.startsWith("H4sI")) return false;
        final int length = value.length();
        int i = 4;
        while (i + 4 <= length && isBase64(value.charAt(i)) && isBase64(value.charAt(i + 1))
            && isBase64(value.charAt(i + 2)) && isBase64(value.charAt(i + 3))) {
            i += 4;
        }
        final int remaining = length - i;
        if (remaining == 0) return true;
        if (remaining == 2) return isBase64(value.charAt(i)) && isBase64(value.charAt(i + 1));
        if (remaining == 3) return isBase64(value.charAt(i)) && isBase64(value.charAt(i + 1))
            && isBase64(value.charAt(i + 2));
        if (remaining == 4) {
            final boolean tailTwo = isBase64(value.charAt(i)) && isBase64(value.charAt(i + 1))
                && value.charAt(i + 2) == '=' && value.charAt(i + 3) == '=';
            final boolean tailThree = isBase64(value.charAt(i)) && isBase64(value.charAt(i + 1))
                && isBase64(value.charAt(i + 2)) && value.charAt(i + 3) == '=';
            return tailTwo || tailThree;
        }
        return false;
    }

    private static boolean matchesName(final @NotNull String value) {
        final int length = value.length();
        if (length == 0) return false;
        int i = 0;
        final char first = value.charAt(0);
        if (first == '!' || first == '?' || first == '#') {
            if (length == 1) return false;
            i = 1;
        }
        for (; i < length; i++) if (!matches(NAME_LOW, NAME_HIGH, value.charAt(i))) return false;
        return true;
    }

    private static boolean matches(final long low, final long high, final char c) {
        return c < 64 ? (low & (1L << c)) != 0 : c < 128 && (high & (1L << (c - 64))) != 0;
    }

    private static boolean isBase64(final char c) {
        return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9'
            || c == '+' || c == '/';
    }
}
