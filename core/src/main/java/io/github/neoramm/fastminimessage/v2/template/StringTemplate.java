package io.github.neoramm.fastminimessage.v2.template;

import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.v2.internal.DefaultTagScanner;
import io.github.neoramm.fastminimessage.v2.internal.TokenPatterns;
import io.github.neoramm.fastminimessage.v2.model.Arguments;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import io.github.neoramm.fastminimessage.v2.tag.TagSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StringTemplate {
    private StringTemplate() {
    }

    public static @NotNull ValueSource<@NotNull String> compile(final @NotNull String input) {
        return compile(input, null);
    }

    public static @NotNull ValueSource<@NotNull String> compile(final @NotNull String input,
        final @Nullable MiniMessageCompiler compiler) {
        Objects.requireNonNull(input, "input");
        if (input.indexOf('<') < 0) return new ValueSource.Constant<>(input);
        final @NotNull List<@NotNull Segment> segments = new ArrayList<>();
        final @NotNull StringBuilder literal = new StringBuilder();
        int position = 0;
        while (position < input.length()) {
            final char character = input.charAt(position);
            if (character == '\\' && position + 1 < input.length()
                && (input.charAt(position + 1) == '<' || input.charAt(position + 1) == '\\')) {
                literal.append(input.charAt(position + 1));
                position += 2;
                continue;
            }
            if (character != '<') {
                literal.append(character);
                position++;
                continue;
            }
            final int close = DefaultTagScanner.findTagEnd(input, position);
            if (close < 0) {
                literal.append(character);
                position++;
                continue;
            }
            final int contentEnd = close > position + 1 && input.charAt(close - 1) == '/' ? close - 1 : close;
            final int colon = input.indexOf(':', position + 1, contentEnd);
            final @NotNull String key = input.substring(position + 1, colon < 0 ? contentEnd : colon);
            if (!TokenPatterns.isScalarKey(key)) {
                literal.append(character);
                position++;
                continue;
            }
            if (!literal.isEmpty()) {
                segments.add(new Segment(literal.toString(), null, Arguments.EMPTY, null));
                literal.setLength(0);
            }
            final @NotNull Arguments args = colon < 0 ? Arguments.EMPTY
                : Arguments.parse(input.substring(colon + 1, contentEnd));
            final @Nullable TagSpec spec = compiler == null ? null : compiler.findResolver(key);
            segments.add(new Segment(input.substring(position, close + 1), key, args,
                spec instanceof final TagSpec.PlaceholderRule placeholder ? placeholder.renderer() : null));
            position = close + 1;
        }
        if (segments.isEmpty()) return new ValueSource.Constant<>(literal.toString());
        if (!literal.isEmpty()) segments.add(new Segment(literal.toString(), null, Arguments.EMPTY, null));
        final @NotNull List<@NotNull Segment> immutable = List.copyOf(segments);
        final @NotNull MiniMessageCompiler argumentCompiler = compiler == null
            ? MiniMessageCompiler.defaults() : compiler;
        return new ValueSource.Computed<>(
            (final var context) -> evaluate(immutable, input.length(), context, argumentCompiler));
    }

    private static @NotNull String evaluate(final @NotNull List<@NotNull Segment> segments, final int length,
        final @NotNull RenderScope context, final @Nullable MiniMessageCompiler compiler) {
        final @NotNull StringBuilder builder = new StringBuilder(length);
        for (int i = 0, size = segments.size(); i < size; i++) {
            final @NotNull Segment segment = segments.get(i);
            final @Nullable Component value = segment.renderer() != null
                ? Objects.requireNonNull(segment.renderer().apply(context), "placeholder result")
                : segment.key() == null ? null : context.resolveValue(segment.key(), segment.args(), compiler);
            if (value == null) builder.append(segment.literal());
            else appendPlain(builder, value);
        }
        return builder.toString();
    }

    private static void appendPlain(final @NotNull StringBuilder builder, final @NotNull Component component) {
        if (component instanceof final TextComponent text) {
            builder.append(text.content());
            final @NotNull List<@NotNull Component> children = component.children();
            for (int i = 0, size = children.size(); i < size; i++) appendPlain(builder, children.get(i));
        } else ComponentFlattener.basic().flatten(component, builder::append);
    }

    private record Segment(@NotNull String literal, @Nullable String key, @NotNull Arguments args,
        @Nullable Function<@NotNull RenderScope, @NotNull Component> renderer) {
    }
}
