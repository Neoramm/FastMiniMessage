package io.github.neoramm.fastminimessage.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Immutable parsed arguments of a tag.
 */
public final class Arguments implements Iterator<String> {

    public static final @NotNull Arguments EMPTY = new Arguments(new String[0]);

    private final @NotNull String[] args;
    private int index;

    private Arguments(final @NotNull String[] args) {
        this.args = args;
    }

    public @NotNull Arguments copy() {
        if (args.length == 0) return EMPTY;
        return new Arguments(args);
    }

    /**
     * Parses arguments split by {@code ':'}, honouring single and double quotes.
     *
     * @param rawArgs the raw argument string
     * @return the parsed arguments
     */
    public static @NotNull Arguments parse(final @Nullable String rawArgs) {
        return parse(rawArgs, ':');
    }

    public static @NotNull Arguments parse(final @Nullable String rawArgs, final char delimiter) {
        if (rawArgs == null || rawArgs.isEmpty()) return EMPTY;
        final int len = rawArgs.length();
        boolean hasQuotesOrEscapes = false;
        int delimiterCount = 0;
        for (int i = 0; i < len; i++) {
            final char c = rawArgs.charAt(i);
            if (c == '\'' || c == '"' || c == '\\') {
                hasQuotesOrEscapes = true;
                break;
            }
            if (c == delimiter && !isUrlProtocol(rawArgs, i, delimiter)) delimiterCount++;
        }
        if (!hasQuotesOrEscapes) {
            if (delimiterCount == 0) return new Arguments(new String[]{rawArgs});
            final String[] result = new String[delimiterCount + 1];
            int start = 0;
            int idx = 0;
            for (int i = 0; i < len; i++) {
                if (rawArgs.charAt(i) == delimiter && !isUrlProtocol(rawArgs, i, delimiter)) {
                    result[idx++] = rawArgs.substring(start, i);
                    start = i + 1;
                }
            }
            result[idx] = rawArgs.substring(start, len);
            return new Arguments(result);
        }
        final List<String> result = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < len; i++) {
            final char c = rawArgs.charAt(i);
            if (c == '\\' && i + 1 < len) {
                final char next = rawArgs.charAt(i + 1);
                if (quote != 0 && (next == quote || next == '\\')) {
                    current.append(next);
                    i++;
                    continue;
                }
                if (quote == 0 && (next == delimiter || next == '\\' || next == '\'' || next == '"')) {
                    current.append(next);
                    i++;
                    continue;
                }
            }
            if (quote == 0 && (c == '\'' || c == '"')) quote = c;
            else if (quote != 0 && c == quote) quote = 0;
            else if (c == delimiter && quote == 0 && !isUrlProtocol(rawArgs, i, delimiter)) {
                result.add(current.toString());
                current.setLength(0);
            } else current.append(c);
        }
        result.add(current.toString());
        return new Arguments(result.toArray(String[]::new));
    }

    private static boolean isUrlProtocol(final @NotNull String rawArgs, final int index, final char delimiter) {
        return delimiter == ':' && index + 2 < rawArgs.length()
            && rawArgs.charAt(index + 1) == '/' && rawArgs.charAt(index + 2) == '/';
    }

    public @Nullable String get(final int index, final @Nullable String defaultValue) {
        return index >= 0 && index < args.length ? args[index] : defaultValue;
    }

    public @Nullable String get(final int index) {
        return get(index, null);
    }

    public @NotNull String getFirst() {
        if (args.length == 0) throw new NoSuchElementException();
        return args[0];
    }

    public @Nullable String getFirst(final @Nullable String defaultValue) {
        return args.length > 0 ? args[0] : defaultValue;
    }

    public @NotNull String getLast() {
        if (args.length == 0) throw new NoSuchElementException();
        return args[args.length - 1];
    }

    public @Nullable String getLast(final @Nullable String defaultValue) {
        return args.length > 0 ? args[args.length - 1] : defaultValue;
    }

    public int getInt(final int index, final int defaultValue) {
        final String val = get(index);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (final NumberFormatException e) {
            return defaultValue;
        }
    }

    public float getFloat(final int index, final float defaultValue) {
        final String val = get(index);
        if (val == null) return defaultValue;
        try {
            return Float.parseFloat(val);
        } catch (final NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean isEmpty() {
        return args.length == 0;
    }

    public int size() {
        return args.length;
    }

    @Override
    public boolean hasNext() {
        return index < args.length;
    }

    @Override
    public @NotNull String next() {
        if (!hasNext()) throw new NoSuchElementException();
        return args[index++];
    }

    public @NotNull String next(final @NotNull String message) {
        if (!hasNext()) throw new NoSuchElementException(message);
        return args[index++];
    }

    public @Nullable String nextOr(final @Nullable String def) {
        return hasNext() ? next() : def;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
