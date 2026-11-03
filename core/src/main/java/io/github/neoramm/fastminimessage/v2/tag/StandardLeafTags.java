package io.github.neoramm.fastminimessage.v2.tag;

import io.github.neoramm.fastminimessage.v2.ResolverRegistry;
import io.github.neoramm.fastminimessage.v2.model.Arguments;
import io.github.neoramm.fastminimessage.v2.template.ValueSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.BlockNBTComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.NBTComponentBuilder;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import net.kyori.adventure.util.TriState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StandardLeafTags {
    private StandardLeafTags() {
    }

    public static void register(final @NotNull ResolverRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        registry.register(new TagSpec.LeafRule(Set.of("newline", "br"),
            (final var args, final var context) -> new ValueSource.Constant<>(Component.newline())));
        registry.register(new TagSpec.LeafRule(Set.of("keybind", "key"), (final var args, final var context) -> {
            args.require(1);
            return args.map((final var values) -> Component.keybind(values.string(0, "")));
        }));
        registry.register(new TagSpec.LeafRule(Set.of("score"), (final var args, final var context) -> {
            args.require(2);
            return args.map((final var values) -> Component.score(values.string(0, ""), values.string(1, "")));
        }));
        registry.register(new TagSpec.LeafRule(Set.of("sprite"), (final var args, final var context) -> {
            args.require(1);
            return args.map((final var values) -> sprite(values.arguments()));
        }));
        registry.register(new TagSpec.LeafRule(Set.of("head"), (final var args, final var context) ->
            args.map((final var values) -> head(values.arguments()))));
        registry.register(new TagSpec.LeafRule(Set.of("selector", "sel"), (final var args, final var context) -> {
            args.require(1);
            args.component(1);
            return args.map((final var values) -> values.size() > 1
                ? Component.selector(values.string(0, ""), values.component(1))
                : Component.selector(values.string(0, "")));
        }));
        registry.register(new TagSpec.LeafRule(Set.of("lang", "translate", "tr"),
            (final var args, final var context) -> translation(args, false)));
        registry.register(new TagSpec.LeafRule(Set.of("lang_or", "translate_or", "tr_or"),
            (final var args, final var context) -> translation(args, true)));
        registry.register(new TagSpec.LeafRule(Set.of("nbt", "data"), (final var args, final var context) -> {
            args.require(3);
            if (args.raw(3, "").indexOf('<') >= 0) args.componentAlternative(3);
            else if (!isInterpret(args.raw(3, ""))) args.component(3);
            return args.map(StandardLeafTags::nbt);
        }));
    }

    private static @NotNull ValueSource<@NotNull Component> translation(final @NotNull TagArgs args,
        final boolean fallback) {
        final int start = fallback ? 2 : 1;
        args.require(start);
        for (int i = start; i < args.size(); i++) args.component(i);
        return args.map((final var values) -> {
            final TranslatableComponent.@NotNull Builder builder = Component.translatable().key(values.string(0, ""));
            if (fallback) builder.fallback(values.string(1, ""));
            final @NotNull List<@NotNull Component> arguments = new ArrayList<>(values.size() - start);
            for (int i = start; i < values.size(); i++) arguments.add(values.component(i));
            return builder.arguments(arguments).build();
        });
    }

    private static @NotNull Component nbt(final TagArgs.@NotNull Values values) {
        final @NotNull String target = values.string(1, "");
        final @NotNull NBTComponentBuilder<?, ?> builder = switch (values.string(0, "").toLowerCase(Locale.ROOT)) {
            case "entity" -> Component.entityNBT().selector(target);
            case "block" -> Component.blockNBT().pos(BlockNBTComponent.Pos.fromString(target));
            case "storage" -> Component.storageNBT().storage(Key.key(requireKey(target)));
            default -> throw new IllegalArgumentException("Unknown NBT type");
        };
        builder.nbtPath(values.string(2, ""));
        final boolean interpret = isInterpret(values.string(3, ""));
        if (values.size() > 3 && !interpret) builder.separator(values.component(3));
        return builder.interpret(interpret || isInterpret(values.string(4, ""))).build();
    }

    private static boolean isInterpret(final @NotNull String value) {
        return "true".equalsIgnoreCase(value) || "interpret".equalsIgnoreCase(value);
    }

    private static @NotNull String requireKey(final @NotNull String value) {
        if (value.isEmpty()) throw new IllegalArgumentException("A key cannot be empty");
        return value;
    }

    private static @NotNull Component sprite(final @NotNull Arguments args) {
        if (args.size() > 2) throw new IllegalArgumentException("Expected at most two arguments for sprite");
        final @NotNull String first = args.getFirst();
        if (first.isBlank()) throw new IllegalArgumentException("Empty sprite");
        final @NotNull Key sprite = Key.key(requireKey(first));
        final @Nullable String second = args.get(1);
        return Component.object(second == null ? ObjectContents.sprite(sprite)
            : ObjectContents.sprite(sprite, Key.key(requireKey(second))));
    }

    private static @NotNull Component head(final @NotNull Arguments args) {
        if (args.size() > 2) throw new IllegalArgumentException("Expected a target and optional hat boolean");
        final PlayerHeadObjectContents.@NotNull Builder head = ObjectContents.playerHead();
        if (args.isEmpty()) return Component.object(head.build());
        final @NotNull String target = args.getFirst();
        final @NotNull TriState single = booleanValue(target);
        if (args.size() == 1 && single != TriState.NOT_SET) {
            return Component.object(head.hat(single == TriState.TRUE).build());
        }
        if (args.size() == 2) {
            final @NotNull TriState hat = booleanValue(args.get(1, ""));
            if (hat == TriState.NOT_SET) throw new IllegalArgumentException("Expected boolean for hat");
            head.hat(hat == TriState.TRUE);
        }
        if (target.indexOf('/') >= 0) return Component.object(head.texture(Key.key(target)).build());
        if (target.length() == 36 && target.charAt(8) == '-') {
            try {
                return Component.object(head.id(UUID.fromString(target)).build());
            } catch (final IllegalArgumentException malformed) {
                return Component.object(head.name(target.trim()).build());
            }
        }
        return Component.object(head.name(target.trim()).build());
    }

    private static @NotNull TriState booleanValue(final @NotNull String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "yes", "on" -> TriState.TRUE;
            case "false", "no", "off" -> TriState.FALSE;
            default -> TriState.NOT_SET;
        };
    }
}
