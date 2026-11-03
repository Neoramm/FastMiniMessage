package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.example.ComponentModifiers;
import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
import io.github.neoramm.fastminimessage.v2.tag.TagSpec;
import io.github.neoramm.fastminimessage.v2.template.ValueSource;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.pointer.Pointer;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class AdventureTagsTest {

    private static final @NotNull Pointer<String> RANK_POINTER =
        Pointer.pointer(String.class, Key.key("test", "rank"));

    private static @NotNull String plain(final @NotNull Component component) {
        final StringBuilder text = new StringBuilder();
        if (component instanceof final TextComponent textComponent) text.append(textComponent.content());
        for (final Component child : component.children()) text.append(plain(child));
        return text.toString();
    }

    @Test
    void resolvesArgumentBasedTagResolverWithoutMiniMessage() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> playerKey = layout.key("player", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .register(new TagSpec.LeafRule(Set.of("badge"), (final var args, final var context) -> {
                final var values = args.values();
                return values.map((final var vals) -> {
                    final String rank = vals.string(0, "");
                    final String colorName = vals.string(1, "missing color");
                    final @Nullable NamedTextColor color = NamedTextColor.NAMES.value(colorName);
                    return Component.text("[" + rank.toUpperCase(Locale.ROOT) + "]", color);
                });
            }))
            .registerPlaceholder("player", (final var scope) -> Component.text(scope.payload(playerKey)));

        final PreComponentTree tree = new MiniMessageCompiler(registry).compile("<badge:admin:red> <player>");
        final Component rendered = tree.render((final var b) -> b.payload(playerKey, "Alice"));

        assertEquals("[ADMIN] Alice", plain(rendered));
        assertEquals(ComponentComparison.expression(
            Component.text().append(Component.text("[ADMIN]", NamedTextColor.RED)).append(Component.text(" Alice")).build()),
            ComponentComparison.expression(rendered));
    }

    @Test
    void resolvesModifyingTagResolverWithoutMiniMessage() {
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults()
            .registerModifier("shout", ComponentModifiers.uppercase());

        final PreComponentTree tree = new MiniMessageCompiler(registry).compile("<shout>hello world</shout>");
        final Component rendered = tree.render();

        assertEquals("HELLO WORLD", plain(rendered));
    }

    @Test
    void resolvesContextTargetFromPointeredWithoutMiniMessage() {
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults()
            .registerPlaceholder("rank", (final var scope) -> {
                final @Nullable Pointered target = scope.viewer();
                final String rank = target != null ? target.getOrDefault(RANK_POINTER, "default") : "none";
                return Component.text(rank);
            });

        final Pointered viewer = new Pointered() {
            private final Pointers pointers = Pointers.builder().withStatic(RANK_POINTER, "VIP").build();

            @Override
            public @NotNull Pointers pointers() {
                return pointers;
            }
        };

        final PreComponentTree tree = new MiniMessageCompiler(registry).compile("Rank: <rank>");
        final Component rendered = tree.render((final var b) -> b.viewer(viewer));

        assertEquals("Rank: VIP", plain(rendered));
    }

    @Test
    void handlesMissingArgumentsGracefully() {
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults()
            .register(new TagSpec.LeafRule(Set.of("strict"), (final var args, final var context) -> {
                args.require(1);
                return new ValueSource.Constant<>(Component.text("Success"));
            }));

        final PreComponentTree tree = new MiniMessageCompiler(registry).compile("<strict> Text");
        final Component rendered = tree.render();

        assertNotNull(rendered);
        assertEquals("<strict> Text", plain(rendered));
    }
}
