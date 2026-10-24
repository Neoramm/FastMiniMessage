package io.github.neoramm.fastminimessage.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable parsed arguments of a tag.
 */
public final class Arguments {

    public static final @NotNull Arguments EMPTY = new Arguments(new String[0]);

    private final String @NotNull [] args;

    private Arguments(final @NotNull String[] args) {
        this.args = args;
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
        final List<String> result = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < rawArgs.length(); i++) {
            final char c = rawArgs.charAt(i);
            if (quote == 0 && (c == '\'' || c == '"')) quote = c;
            else if (quote != 0 && c == quote) quote = 0;
            else if (c == delimiter && quote == 0) {
                result.add(current.toString());
                current.setLength(0);
            } else current.append(c);
        }
        if (!current.isEmpty()) result.add(current.toString());
        return new Arguments(result.toArray(new String[0]));
    }

    public @Nullable String get(final int index, final @Nullable String defaultValue) {
        return index >= 0 && index < args.length ? args[index] : defaultValue;
    }

    public @Nullable String get(final int index) {
        return get(index, null);
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
}
