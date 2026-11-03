package io.github.neoramm.fastminimessage.v2.tag;

import io.github.neoramm.fastminimessage.v2.ResolverRegistry;
import io.github.neoramm.fastminimessage.v2.model.Arguments;
import io.github.neoramm.fastminimessage.v2.template.TreeTransformer;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static io.github.neoramm.fastminimessage.v2.tag.StandardStyleTags.resolveColor;

public final class StandardTransformTags {
    private static final @NotNull TextColor @NotNull [] DEFAULT_COLORS = {NamedTextColor.WHITE, NamedTextColor.BLACK};
    private static final @NotNull String PRIDE = "pride";

    private static final @NotNull Map<@NotNull String, TextColor @NotNull []> FLAGS;

    static {
        FLAGS = Map.ofEntries(
            Map.entry(PRIDE, colors(0xE50000, 0xFF8D00, 0xFFEE00, 0x028121, 0x004CFF, 0x770088)),
            Map.entry("progress", colors(0xFFFFFF, 0xFFAFC7, 0x73D7EE, 0x613915, 0x000000,
                0xE50000, 0xFF8D00, 0xFFEE00, 0x028121, 0x004CFF, 0x770088)),
            Map.entry("trans", colors(0x5BCFFB, 0xF5ABB9, 0xFFFFFF, 0xF5ABB9, 0x5BCFFB)),
            Map.entry("bi", colors(0xD60270, 0x9B4F96, 0x0038A8)),
            Map.entry("pan", colors(0xFF1C8D, 0xFFD700, 0x1AB3FF)),
            Map.entry("nb", colors(0xFCF431, 0xFCFCFC, 0x9D59D2, 0x282828)),
            Map.entry("lesbian", colors(0xD62800, 0xFF9B56, 0xFFFFFF, 0xD462A6, 0xA40062)),
            Map.entry("ace", colors(0x000000, 0xA4A4A4, 0xFFFFFF, 0x810081)),
            Map.entry("agender", colors(0x000000, 0xBABABA, 0xFFFFFF, 0xBAF484, 0xFFFFFF, 0xBABABA, 0x000000)),
            Map.entry("demisexual", colors(0x000000, 0xFFFFFF, 0x6E0071, 0xD3D3D3)),
            Map.entry("genderqueer", colors(0xB57FDD, 0xFFFFFF, 0x49821E)),
            Map.entry("genderfluid", colors(0xFE76A2, 0xFFFFFF, 0xBF12D7, 0x000000, 0x303CBE)),
            Map.entry("intersex", colors(0xFFD800, 0x7902AA, 0xFFD800)),
            Map.entry("aro", colors(0x3BA740, 0xA8D47A, 0xFFFFFF, 0xABABAB, 0x000000)),
            Map.entry("femboy", colors(0xD260A5, 0xE4AFCD, 0xFEFEFE, 0x57CEF8, 0xFEFEFE, 0xE4AFCD, 0xD260A5)),
            Map.entry("intersex inclusive", colors(0xFFD800, 0x7902AA, 0xFFD800, 0xFFFFFF,
                0xFFAFC7, 0x73D7EE, 0x613915, 0x000000, 0xE50000, 0xFF8D00, 0xFFEE00, 0x028121, 0x004CFF, 0x770088)),
            Map.entry("baker", colors(0xCD66FF, 0xFF6599, 0xFE0000, 0xFE9900, 0xFFFF01, 0x009900, 0x0099CB, 0x350099,
                0x990099)),
            Map.entry("philly", colors(0x000000, 0x784F17, 0xFE0000, 0xFD8C00, 0xFFE500, 0x119F0B, 0x0644B3, 0xC22EDC)),
            Map.entry("queer", colors(0x000000, 0x9AD9EA, 0x00A3E8, 0xB5E51D, 0xFFFFFF, 0xFFC90D, 0xFC6667, 0xFEAEC9,
                0x000000)),
            Map.entry("gay", colors(0x078E70, 0x26CEAA, 0x98E8C1, 0xFFFFFF, 0x7BADE2, 0x5049CB, 0x3D1A78)),
            Map.entry("bigender", colors(0xC479A0, 0xECA6CB, 0xD5C7E8, 0xFFFFFF, 0xD5C7E8, 0x9AC7E8, 0x6C83CF)),
            Map.entry("demigender", colors(0x7F7F7F, 0xC3C3C3, 0xFBFF74, 0xFFFFFF, 0xFBFF74, 0xC3C3C3, 0x7F7F7F))
        );
    }

    private StandardTransformTags() {
    }

    public static void register(final @NotNull ResolverRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        registry.register(new TagSpec.TransformRule(Set.of("gradient"), (final var args, final var context) ->
            args.map((final var values) -> gradient(values.arguments()))));
        registry.register(new TagSpec.TransformRule(Set.of("rainbow"), (final var args, final var context) ->
            args.map((final var values) -> rainbow(values.arguments()))));
        registry.register(new TagSpec.TransformRule(Set.of("pride"), (final var args, final var context) ->
            args.map((final var values) -> pride(values.arguments()))));
    }

    private static @NotNull TreeTransformer gradient(final @NotNull Arguments args) {
        if (args.isEmpty()) return new GradientTransform(DEFAULT_COLORS, 0.0f, false, false);
        int count = args.size();
        float phase = 0.0f;

        final @NotNull String last = args.getLast();
        final @Nullable TextColor lastColor = resolveColor(last);
        if (lastColor == null) {
            try {
                final float parsedPhase = Float.parseFloat(last);
                if (!Float.isFinite(parsedPhase) || parsedPhase < -1.0f || parsedPhase > 1.0f) {
                    throw new IllegalArgumentException(
                        "Gradient phase is out of range (" + parsedPhase + "). Must be in [-1.0, 1.0].");
                }
                phase = parsedPhase;
                count--;
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException("Unable to parse a color from '" + last + "'");
            }
        }
        if (count == 0) return new GradientTransform(DEFAULT_COLORS, phase, false, false);
        if (count == 1) throw new IllegalArgumentException("Invalid gradient, not enough colors.");
        final @NotNull TextColor @NotNull [] colors = new TextColor[count];
        for (int i = 0; i < count; i++) {
            final @NotNull String raw = args.get(i);
            final @Nullable TextColor color = resolveColor(raw);
            if (color == null) throw new IllegalArgumentException("Unable to parse a color from '" + raw + "'");
            colors[i] = color;
        }
        return new GradientTransform(colors, phase, false, false);
    }

    private static @NotNull TreeTransformer rainbow(final @NotNull Arguments args) {
        if (args.size() > 1) throw new IllegalArgumentException("Expected at most one argument for rainbow");
        boolean reversed = false;
        float phase = 0.0f;
        if (!args.isEmpty()) {
            String value = args.getFirst("");
            if (value.startsWith("!")) {
                reversed = true;
                value = value.substring(1);
            }
            if (!value.isEmpty()) phase = Integer.parseInt(value) / 10.0f;
        }
        return new GradientTransform(new TextColor[0], phase, true, reversed);
    }

    private static @NotNull TreeTransformer pride(final @NotNull Arguments args) {
        if (args.size() > 2) throw new IllegalArgumentException("Expected at most two arguments for pride");
        float phase = 0.0f;
        String flag = PRIDE;

        if (!args.isEmpty()) {
            final @NotNull String value = args.getFirst("").toLowerCase(Locale.ROOT);
            final @NotNull TextColor @Nullable [] matched = FLAGS.get(value);
            if (matched != null) {
                flag = value;
                if (args.size() > 1) phase = parsePhase(args.get(1, "0"));
            } else {
                phase = parsePhase(value);
                if (args.size() > 1) {
                    final @NotNull String second = args.get(1, "").toLowerCase(Locale.ROOT);
                    if (!FLAGS.containsKey(second)) throw new IllegalArgumentException("Unknown pride flag: " + second);
                    flag = second;
                }
            }
        }

        final @NotNull TextColor @NotNull [] colors = FLAGS.getOrDefault(flag, FLAGS.get(PRIDE));
        return new GradientTransform(colors, phase, false, false);
    }

    private static float parsePhase(final @NotNull String raw) {
        if (raw.isEmpty()) return 0.0f;
        final float phase = Float.parseFloat(raw);
        if (!Float.isFinite(phase) || phase < -1.0f || phase > 1.0f) {
            throw new IllegalArgumentException("Phase must be finite and in [-1.0, 1.0]");
        }
        return phase;
    }

    private static TextColor @NotNull [] colors(final int @NotNull ... values) {
        final @NotNull TextColor @NotNull [] palette = new TextColor[values.length];
        for (int i = 0; i < values.length; i++) palette[i] = TextColor.color(values[i]);
        return palette;
    }

}
