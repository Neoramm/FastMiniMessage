package io.github.neoramm.fastminimessage.v2.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.IntFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Arguments implements Iterable<@NotNull String> {

    public static final @NotNull Arguments EMPTY = new Arguments(new String[0]);

    private final @NotNull String @NotNull [] args;

    private Arguments(final @NotNull String @NotNull [] args) {
        this.args = args;
    }

    public static @NotNull Arguments of(final @NotNull String @NotNull ... values) {
        Objects.requireNonNull(values, "values");
        for (final @NotNull String value : values) Objects.requireNonNull(value, "value");
        return values.length == 0 ? EMPTY : new Arguments(values.clone());
    }

    public static @NotNull Arguments generate(final int size,
        final @NotNull IntFunction<@NotNull String> generator) {
        Objects.requireNonNull(generator, "generator");
        if (size < 0) throw new IllegalArgumentException("Negative argument count");
        if (size == 0) return EMPTY;
        final @NotNull String @NotNull [] values = new String[size];
        for (int i = 0; i < size; i++) values[i] = Objects.requireNonNull(generator.apply(i), "value");
        return new Arguments(values);
    }

    /**
     * Returns this instance, since arguments are immutable and safe to share across threads.
     */
    public @NotNull Arguments copy() {
        return this;
    }

    public static @NotNull Arguments parse(final @Nullable String rawArgs) {
        return parse(rawArgs, ':');
    }

    public static @NotNull Arguments parse(final @Nullable String rawArgs, final char delimiter) {
        if (rawArgs == null || rawArgs.isEmpty()) return EMPTY;
        final int len = rawArgs.length();
        boolean hasQuotesOrEscapes = false;
        for (int i = 0; i < len; i++) {
            final char c = rawArgs.charAt(i);
            if (c == '\'' || c == '"' || c == '\\') {
                hasQuotesOrEscapes = true;
                break;
            }
        }
        if (!hasQuotesOrEscapes) {
            boolean url = false;
            int delimiterCount = 0;
            int urlProtocolEnd = -1;
            for (int i = 0; i < len; i++) {
                final char c = rawArgs.charAt(i);
                if (c == '/' && url && i >= urlProtocolEnd) {
                    url = false;
                    continue;
                }
                if (c != delimiter) continue;
                if (isUrlColon(rawArgs, i, delimiter, url)) {
                    url = true;
                    if (hasProtocolSlash(rawArgs, i, len)) urlProtocolEnd = i + 3;
                } else {
                    url = false;
                    delimiterCount++;
                }
            }
            if (delimiterCount == 0) return new Arguments(new String[]{rawArgs});
            final @NotNull String[] result = new String[delimiterCount + 1];
            int start = 0;
            int idx = 0;
            url = false;
            urlProtocolEnd = -1;
            for (int i = 0; i < len; i++) {
                final char c = rawArgs.charAt(i);
                if (c == '/' && url && i >= urlProtocolEnd) {
                    url = false;
                    continue;
                }
                if (c != delimiter) continue;
                if (isUrlColon(rawArgs, i, delimiter, url)) {
                    url = true;
                    if (hasProtocolSlash(rawArgs, i, len)) urlProtocolEnd = i + 3;
                } else {
                    url = false;
                    result[idx++] = rawArgs.substring(start, i);
                    start = i + 1;
                }
            }
            result[idx] = rawArgs.substring(start, len);
            return new Arguments(result);
        }
        final @NotNull List<@NotNull String> result = new ArrayList<>();
        final @NotNull StringBuilder current = new StringBuilder();
        char quote = 0;
        boolean url = false;
        int urlProtocolEnd = -1;
        for (int i = 0; i < len; i++) {
            final char c = rawArgs.charAt(i);
            if (c == '\\' && i + 1 < len) {
                final char next = rawArgs.charAt(i + 1);
                if (quote != 0 && (next == quote || next == '\\')) {
                    current.append(next);
                    i++;
                    continue;
                }
                if (quote == 0
                    && (next == delimiter || next == '\\' || next == '\'' || next == '"' || next == '>')) {
                    current.append(next);
                    i++;
                    continue;
                }
            }
            if (quote == 0 && current.isEmpty() && (c == '\'' || c == '"')) {
                quote = c;
                continue;
            }
            if (quote != 0 && c == quote) {
                quote = 0;
                continue;
            }
            if (quote == 0 && c == '/' && url && i >= urlProtocolEnd) url = false;
            if (quote == 0 && c == delimiter) {
                if (isUrlColon(rawArgs, i, delimiter, url)) {
                    url = true;
                    if (hasProtocolSlash(rawArgs, i, len)) urlProtocolEnd = i + 3;
                    current.append(c);
                } else {
                    url = false;
                    result.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(c);
        }
        result.add(current.toString());
        return new Arguments(result.toArray(String[]::new));
    }

    private static boolean hasProtocolSlash(final @NotNull String rawArgs, final int index, final int len) {
        return index + 2 < len && rawArgs.charAt(index + 1) == '/' && rawArgs.charAt(index + 2) == '/';
    }

    private static boolean isUrlColon(final @NotNull String rawArgs, final int index, final char delimiter,
        final boolean url) {
        if (delimiter != ':' || rawArgs.charAt(index) != ':') return false;
        if (hasProtocolSlash(rawArgs, index, rawArgs.length())) return true;
        return url && index + 1 < rawArgs.length() && isAsciiDigit(rawArgs.charAt(index + 1));
    }

    private static boolean isAsciiDigit(final char c) {
        return c >= '0' && c <= '9';
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
        final @Nullable String val = get(index);
        if (val == null) return defaultValue;
        if (val.length() == 1) {
            final char c = val.charAt(0);
            if (c >= '0' && c <= '9') return c - '0';
        }
        try {
            return Integer.parseInt(val);
        } catch (final NumberFormatException e) {
            return defaultValue;
        }
    }

    public float getFloat(final int index, final float defaultValue) {
        final @Nullable String val = get(index);
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

    /**
     * Creates a cursor positioned before the first argument. Each call returns an independent cursor.
     */
    public @NotNull Cursor cursor() {
        return new Cursor(args);
    }

    @Override
    public @NotNull Cursor iterator() {
        return cursor();
    }

    /**
     * Sequential view over an immutable argument list. Cursor state is confined to its caller.
     */
    public static final class Cursor implements Iterator<@NotNull String> {
        private final @NotNull String @NotNull [] args;
        private int index;

        private Cursor(final @NotNull String @NotNull [] args) {
            this.args = args;
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
}
