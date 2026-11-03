package io.github.neoramm.fastminimessage.v2.tag;

import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.v2.ResolverRegistry;
import io.github.neoramm.fastminimessage.v2.model.Arguments;
import io.github.neoramm.fastminimessage.v2.template.ValueSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.dialog.DialogLike;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StandardStyleTags {
    private StandardStyleTags() {
    }

    public static void register(final @NotNull ResolverRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        for (final @NotNull NamedTextColor color : NamedTextColor.NAMES.values()) {
            final @NotNull ValueSource<@NotNull Style> source = new ValueSource.Constant<>(Style.style(color));
            registry.register(new TagSpec.StyleRule(Set.of(color.toString()), (final var args, final var context) -> {
                if (args.size() != 0) {
                    throw new IllegalArgumentException("Color tag '" + context.name() + "' does not take arguments");
                }
                return source;
            }));
        }
        final @NotNull ValueSource<@NotNull Style> grey =
            new ValueSource.Constant<>(Style.style(NamedTextColor.GRAY));
        registry.register(new TagSpec.StyleRule(Set.of("grey"), (final var args, final var context) -> {
            if (args.size() != 0) {
                throw new IllegalArgumentException("Color tag '" + context.name() + "' does not take arguments");
            }
            return grey;
        }));
        final @NotNull ValueSource<@NotNull Style> darkGrey =
            new ValueSource.Constant<>(Style.style(NamedTextColor.DARK_GRAY));
        registry.register(new TagSpec.StyleRule(Set.of("dark_grey"), (final var args, final var context) -> {
            if (args.size() != 0) {
                throw new IllegalArgumentException("Color tag '" + context.name() + "' does not take arguments");
            }
            return darkGrey;
        }));
        registry.register(new TagSpec.StyleRule(Set.of("color", "colour", "c"), (final var args, final var context) ->
            args.map((final var values) -> colorStyle(context.name(), values.string(0, "")))));
        decoration(registry, TextDecoration.BOLD, "bold", "b", "!bold", "!b");
        decoration(registry, TextDecoration.ITALIC, "italic", "i", "em", "!italic", "!i", "!em");
        decoration(registry, TextDecoration.UNDERLINED, "underlined", "u", "!underlined", "!u");
        decoration(registry, TextDecoration.STRIKETHROUGH, "strikethrough", "st", "!strikethrough", "!st");
        decoration(registry, TextDecoration.OBFUSCATED, "obfuscated", "obf", "!obfuscated", "!obf");
        registry.register(new TagSpec.StyleRule(Set.of("insert", "insertion"), (final var args, final var context) -> {
            args.require(1);
            return args.map((final var values) -> Style.style().insertion(values.string(0, "")).build());
        }));
        registry.register(new TagSpec.StyleRule(Set.of("font"), (final var args, final var context) -> {
            args.require(1);
            return args.map((final var values) -> {
                final @NotNull String namespace = requireKey(values.string(0, ""));
                return Style.style().font(values.size() == 1
                    ? Key.key(namespace) : Key.key(namespace, values.string(1, ""))).build();
            });
        }));
        registry.register(new TagSpec.StyleRule(Set.of("shadow"), (final var args, final var context) ->
            args.map((final var values) -> shadow(values.arguments()))));
        registry.register(new TagSpec.StyleRule(Set.of("transition"), (final var args, final var context) ->
            args.map((final var values) -> transition(values.arguments()))));
        registry.register(new TagSpec.StyleRule(Set.of("click"), (final var args, final var context) -> {
            args.require(2);
            return args.map((final var values) -> click(values.arguments(), context.compiler()));
        }));
        registry.register(new TagSpec.StyleRule(Set.of("hover"), StandardStyleTags::hover));
    }

    private static void decoration(final @NotNull ResolverRegistry registry, final @NotNull TextDecoration decoration,
        final @NotNull String @NotNull ... names) {
        final @NotNull ValueSource<@NotNull Style> enabled =
            new ValueSource.Constant<>(Style.style().decoration(decoration, true).build());
        final @NotNull ValueSource<@NotNull Style> disabled =
            new ValueSource.Constant<>(Style.style().decoration(decoration, false).build());
        registry.register(new TagSpec.StyleRule(Set.of(names), (final var args, final var context) -> {
            if (args.size() == 0) return context.name().startsWith("!") ? disabled : enabled;
            return args.map((final var values) -> Style.style().decoration(decoration,
                switch (values.string(0, "").toLowerCase(Locale.ROOT)) {
                    case "false", "off", "no" -> false;
                    case "true", "on", "yes" -> true;
                    default -> throw new IllegalArgumentException(
                        "Unknown decoration state: " + values.string(0, ""));
                }).build());
        }));
    }

    private static @NotNull Style colorStyle(final @NotNull String name, final @NotNull String value) {
        final @Nullable TextColor color = resolveColor(name.startsWith("#") ? name : value);
        if (color == null) throw new IllegalArgumentException("Invalid color: " + value);
        return Style.style(color);
    }

    static @Nullable TextColor resolveColor(final @Nullable String name) {
        if (name == null || name.isEmpty()) return null;
        if (name.charAt(0) == '#') return TextColor.fromHexString(name);
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "grey" -> NamedTextColor.GRAY;
            case "dark_grey" -> NamedTextColor.DARK_GRAY;
            default -> NamedTextColor.NAMES.value(name.toLowerCase(Locale.ROOT));
        };
    }

    private static @NotNull String requireKey(final @NotNull String value) {
        if (value.isEmpty()) throw new IllegalArgumentException("A key cannot be empty");
        return value;
    }

    private static @NotNull Style click(final @NotNull Arguments args, final @NotNull MiniMessageCompiler compiler) {
        final @NotNull String action = args.getFirst();
        final @Nullable ClickEvent<?> event = resolveClickEvent(compiler, action, args);
        if (event == null) throw new IllegalArgumentException("Missing click value");
        return Style.style().clickEvent(event).build();
    }

    private static @Nullable ClickEvent<?> resolveClickEvent(final @NotNull MiniMessageCompiler compiler,
        final @NotNull String actionName, final @NotNull Arguments args) {
        final @Nullable String rawValue = args.get(1);
        if (rawValue == null) return null;
        return switch (actionName.toLowerCase(Locale.ROOT)) {
            case "run_command" -> ClickEvent.runCommand(rawValue);
            case "suggest_command" -> ClickEvent.suggestCommand(rawValue);
            case "open_file" -> ClickEvent.openFile(rawValue);
            case "copy_to_clipboard" -> ClickEvent.copyToClipboard(rawValue);
            case "change_page" -> ClickEvent.changePage(Integer.parseInt(rawValue));
            case "custom" -> {
                final @Nullable String nbt = args.get(2);
                yield nbt != null && !nbt.isEmpty()
                    ? ClickEvent.custom(Key.key(requireKey(rawValue)), BinaryTagHolder.binaryTagHolder(nbt))
                    : ClickEvent.custom(Key.key(requireKey(rawValue)));
            }
            case "show_dialog" -> showDialog(compiler, rawValue);
            case "open_url" -> ClickEvent.openUrl(rawValue);
            default -> throw new IllegalArgumentException("Unknown click event action '" + actionName + "'");
        };
    }

    private static @NotNull ClickEvent<?> showDialog(final @NotNull MiniMessageCompiler compiler,
        final @NotNull String reference) {
        final @Nullable DialogLike dialog = compiler.resolveDialog(reference);
        if (dialog == null) throw new IllegalArgumentException("No dialog resolver for " + reference);
        return ClickEvent.showDialog(dialog);
    }

    private static @NotNull Style shadow(final @NotNull Arguments args) {
        if (args.isEmpty()) throw new IllegalArgumentException("A shadow color is required");
        final @NotNull String colorString = args.getFirst().toLowerCase(Locale.ROOT);
        final @NotNull ShadowColor color;
        if (colorString.startsWith(TextColor.HEX_PREFIX) && colorString.length() == 9) {
            color = ShadowColor.fromHexString(colorString);
            if (color == null) {
                throw new IllegalArgumentException(
                    "Unable to parse a shadow color from '" + colorString + "'. Please use #RRGGBBAA formatting.");
            }
        } else {
            final @Nullable TextColor textColor = resolveColor(colorString);
            if (textColor == null) {
                throw new IllegalArgumentException(
                    "Unable to parse a shadow color from '" + colorString
                        + "'. Please use named colors or hex (#RRGGBBAA) colors.");
            }
            final @NotNull String alphaText = args.get(1, "0.25");
            final double alpha;
            try {
                alpha = Double.parseDouble(alphaText);
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException("Invalid shadow alpha: " + alphaText, e);
            }
            if (!Double.isFinite(alpha)) throw new IllegalArgumentException("Invalid shadow alpha: " + alphaText);
            final int alphaInt = (int) Math.round(Math.clamp(alpha, 0.0, 1.0) * 0xff);
            color = ShadowColor.shadowColor(textColor, alphaInt);
        }
        return Style.style().shadowColor(color).build();

    }

    private static @NotNull Style transition(final @NotNull Arguments args) {
        float phase = 0.0f;
        final @NotNull List<@NotNull TextColor> colors = new ArrayList<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            final @NotNull String raw = args.get(i);
            final @Nullable TextColor color = resolveColor(raw);
            if (color != null) colors.add(color);
            else if (i == args.size() - 1) {
                try {
                    phase = Float.parseFloat(raw);
                    if (!Float.isFinite(phase) || phase < -1.0f || phase > 1.0f) {
                        throw new IllegalArgumentException("out of range");
                    }
                } catch (final NumberFormatException e) {
                    throw new IllegalArgumentException("Unable to parse a color from '" + raw + "'");
                }
            } else throw new IllegalArgumentException("Unable to parse a color from '" + raw + "'");
        }
        if (colors.size() < 2) throw new IllegalArgumentException("Invalid transition, not enough colors.");
        TextColor @NotNull [] palette = colors.toArray(TextColor[]::new);
        final boolean negative = phase < 0;
        final float position = negative ? 1 + phase : phase;
        if (negative) {
            final @NotNull TextColor @NotNull [] reversed = new TextColor[palette.length];
            for (int i = 0; i < palette.length; i++) reversed[i] = palette[palette.length - 1 - i];
            palette = reversed;
        }
        TextColor result = null;
        final float steps = 1.0f / (palette.length - 1);
        int idx = 1;
        for (; idx < palette.length; idx++) {
            final float bound = idx == palette.length - 1 ? 1.0f : idx * steps;
            if (bound >= position) {
                final float factor = 1 + (position - bound) * (palette.length - 1);
                if (negative) result = TextColor.lerp(1 - factor, palette[idx], palette[idx - 1]);
                else result = TextColor.lerp(factor, palette[idx - 1], palette[idx]);
                break;
            }
        }
        if (idx >= palette.length) result = palette[palette.length - 1];
        return Style.style().color(result).build();

    }

    private static @NotNull ValueSource<@NotNull Style> hover(final @NotNull TagArgs args,
        final @NotNull CompileContext context) {
        args.require(2);
        final @NotNull String action = args.raw(0, "show_text").toLowerCase(Locale.ROOT);
        if ("show_text".equals(action)) {
            args.component(1);
            return args.map((final var values) ->
                Style.style().hoverEvent(HoverEvent.showText(values.component(1))).build());
        }
        if ("show_entity".equals(action)) {
            args.component(3);
            return args.map((final var values) -> Style.style().hoverEvent(entityHover(values)).build());
        }
        if ("show_item".equals(action)) {
            return args.map((final var values) -> Style.style().hoverEvent(itemHover(values.arguments())).build());
        }
        if (action.indexOf('<') >= 0) {
            args.componentAlternative(1).componentAlternative(3);
            return args.map((final var values) -> Style.style().hoverEvent(
                switch (values.string(0, "show_text").toLowerCase(Locale.ROOT)) {
                    case "show_text" -> HoverEvent.showText(values.component(1));
                    case "show_item" -> itemHover(values.arguments());
                    case "show_entity" -> entityHover(values);
                    default -> throw new IllegalArgumentException("Unknown hover action");
                }).build());
        }
        throw new IllegalArgumentException("Unknown hover action: " + action);
    }

    private static @NotNull HoverEvent<HoverEvent.ShowEntity> entityHover(final TagArgs.@NotNull Values values) {
        if (values.size() < 3) throw new IllegalArgumentException("Expected entity type and UUID");
        final @NotNull Key type = Key.key(requireKey(values.string(1, "")));
        final @NotNull UUID id = UUID.fromString(values.string(2, ""));
        return values.size() > 3 ? HoverEvent.showEntity(type, id, values.component(3))
            : HoverEvent.showEntity(type, id);
    }

    private static @NotNull HoverEvent<HoverEvent.ShowItem> itemHover(final @NotNull Arguments args) {
        final @NotNull Key key = Key.key(requireKey(args.get(1, "")));
        final int count = Integer.parseInt(args.get(2, "1"));
        if (args.size() <= 3) return HoverEvent.showItem(key, count);
        final @NotNull String value = args.get(3, "");
        if (value.startsWith("{")) return HoverEvent.showItem(key, count, BinaryTagHolder.binaryTagHolder(value));
        if ((args.size() - 3) % 2 != 0) throw new IllegalArgumentException("Expected item component pairs");
        final @NotNull Map<@NotNull Key, @NotNull BinaryTagHolder> data = new HashMap<>((args.size() - 3) / 2);
        for (int i = 3; i < args.size(); i += 2) {
            data.put(Key.key(requireKey(args.get(i, ""))), BinaryTagHolder.binaryTagHolder(args.get(i + 1, "")));
        }
        return HoverEvent.showItem(HoverEvent.ShowItem.showItem(key, count, data));
    }
}
