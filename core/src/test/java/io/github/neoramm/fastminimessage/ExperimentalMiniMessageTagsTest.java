package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.compat.ExperimentalMiniMessageTags;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.pointer.Pointer;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.Modifying;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class ExperimentalMiniMessageTagsTest {

    private static final @NotNull Pointer<String> RANK_POINTER =
        Pointer.pointer(String.class, Key.key("test", "rank"));

    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());

    private static @NotNull String plain(final @NotNull Component component) {
        final StringBuilder text = new StringBuilder();
        if (component instanceof final TextComponent textComponent) text.append(textComponent.content());
        for (final Component child : component.children()) text.append(plain(child));
        return text.toString();
    }

    @Test
    void resolvesArgumentBasedTagResolverWithoutMiniMessage() {
        final TagResolver badgeResolver = TagResolver.resolver("badge", (args, ctx) -> {
            final String rank = args.pop().value();
            final String colorName = args.popOr("missing color").value();
            final @Nullable NamedTextColor color = NamedTextColor.NAMES.value(colorName);
            return Tag.inserting(Component.text("[" + rank.toUpperCase() + "]", color));
        });

        final PreComponentTree tree = compiler.compile("<badge:admin:red> <player>");
        final Component rendered = tree.render(null, ExperimentalMiniMessageTags.of(
            badgeResolver,
            Placeholder.unparsed("player", "Alice")
        ));

        assertEquals("[ADMIN] Alice", plain(rendered));
        assertEquals(NamedTextColor.RED, rendered.color());
    }

    @Test
    void resolvesModifyingTagResolverWithoutMiniMessage() {
        final Modifying modifier = (final @NotNull Component current, final int depth) -> {
            if (current instanceof final TextComponent tc) {
                return tc.content(tc.content().toUpperCase());
            }
            return current;
        };
        final TagResolver shoutResolver = TagResolver.resolver("shout", modifier);

        final PreComponentTree tree = compiler.compile("<shout>hello world</shout>");
        final Component rendered = tree.render(null, ExperimentalMiniMessageTags.of(shoutResolver));

        assertEquals("HELLO WORLD", plain(rendered));
    }

    @Test
    void resolvesContextTargetFromPointeredWithoutMiniMessage() {
        final TagResolver rankResolver = TagResolver.resolver("rank", (args, ctx) -> {
            final @Nullable Pointered target = ctx.target();
            final String rank = target != null ? target.getOrDefault(RANK_POINTER, "default") : "none";
            return Tag.inserting(Component.text(rank));
        });

        final Pointered viewer = new Pointered() {
            private final Pointers pointers = Pointers.builder().withStatic(RANK_POINTER, "VIP").build();

            @Override
            public @NotNull Pointers pointers() {
                return pointers;
            }
        };

        final PreComponentTree tree = compiler.compile("Rank: <rank>");
        final Component rendered = tree.render(viewer, ExperimentalMiniMessageTags.of(rankResolver));

        assertEquals("Rank: VIP", plain(rendered));
    }

    @Test
    void handlesMissingArgumentsGracefully() {
        final TagResolver strictResolver = TagResolver.resolver("strict", (args, ctx) -> {
            args.popOr("Arg required!");
            return Tag.inserting(Component.text("Success"));
        });

        final PreComponentTree tree = compiler.compile("<strict> Text");
        final Component rendered = tree.render(null, ExperimentalMiniMessageTags.of(strictResolver));

        assertNotNull(rendered);
        assertEquals("<strict> Text", plain(rendered));
    }
}
