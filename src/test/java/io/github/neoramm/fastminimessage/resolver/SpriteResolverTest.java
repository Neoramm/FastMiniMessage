package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.ResolverRegistry;
import io.github.neoramm.fastminimessage.component.UnparsedPreComponent;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.object.SpriteObjectContents;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpriteResolverTest {

    private static final @NotNull Pointered NOBODY = new Pointered() {
        @Override
        public @NotNull Pointers pointers() {
            return Pointers.empty();
        }
    };

    private static final @NotNull Key ITEMS = Key.key("minecraft:items");
    private static final @NotNull Key PICKAXE = Key.key("minecraft:item/diamond_pickaxe");

    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());

    private static @NotNull List<SpriteObjectContents> sprites(final @NotNull Component component) {
        final List<SpriteObjectContents> found = new ArrayList<>();
        collect(component, found);
        return found;
    }

    private static void collect(final @NotNull Component component,
        final @NotNull List<SpriteObjectContents> found) {
        if (component instanceof ObjectComponent object && object.contents() instanceof SpriteObjectContents sprite) {
            found.add(sprite);
        }
        for (final Component child : component.children()) collect(child, found);
    }

    private @NotNull Component render(final @NotNull String input) {
        return compiler.compile(input).render(NOBODY);
    }

    @Test
    void atlasAndSpriteProduceAnObjectComponent() {
        final List<SpriteObjectContents> found = sprites(render("<sprite:items:item/diamond_pickaxe> Miner"));
        assertEquals(1, found.size());
        assertEquals(ITEMS, found.getFirst().atlas());
        assertEquals(PICKAXE, found.getFirst().sprite());
    }

    @Test
    void singleAndDoubleQuotesAllowNamespacedKeys() {
        final List<String> inputs = List.of(
            "<sprite:'minecraft:items':'minecraft:item/diamond_pickaxe'>",
            "<sprite:\"minecraft:items\":\"minecraft:item/diamond_pickaxe\">",
            "<sprite:\"minecraft:items\":'item/diamond_pickaxe'/>");
        for (final String input : inputs) {
            final List<SpriteObjectContents> found = sprites(render(input));
            assertEquals(1, found.size(), input);
            assertEquals(ITEMS, found.getFirst().atlas(), input);
            assertEquals(PICKAXE, found.getFirst().sprite(), input);
        }
    }

    @Test
    void spriteWithoutAtlasUsesTheDefaultOne() {
        final SpriteObjectContents sprite = sprites(render("<sprite:block/stone>")).getFirst();
        assertEquals(SpriteObjectContents.DEFAULT_ATLAS, sprite.atlas());
        assertEquals(Key.key("minecraft:block/stone"), sprite.sprite());
    }

    @Test
    void spriteSurvivesInsideOtherTags() {
        final Component component = render(
            "<gradient:#b0b0b0:#5ec8ff><sprite:items:item/diamond_pickaxe> <bold>Miner</bold></gradient>");
        assertEquals(1, sprites(component).size());
        assertTrue(PlainTextComponentSerializer.plainText().serialize(component).endsWith(" Miner"));
    }

    @Test
    void invalidSpriteKeepsTheRawTagInsteadOfDisappearing() {
        final PreComponentTree tree = compiler.compile("<sprite:'Mal Clave'> Miner");
        final List<String> unparsed = new ArrayList<>();
        tree.visit(component -> {
            if (component instanceof UnparsedPreComponent tag) unparsed.add(tag.getUnparsedTag());
        });
        assertEquals(List.of("<sprite:'Mal Clave'>"), unparsed);
        final Component component = tree.render(NOBODY);
        assertTrue(sprites(component).isEmpty());
        assertEquals("<sprite:'Mal Clave'> Miner",
            PlainTextComponentSerializer.plainText().serialize(component));
    }

    @Test
    void quotedArgumentsKeepApostrophesAndColons() {
        final Component component = render("<hover:show_text:\"It's 10:30\">hello</hover>");
        assertEquals("hello", PlainTextComponentSerializer.plainText().serialize(component));
        assertEquals("It's 10:30",
            PlainTextComponentSerializer.plainText().serialize((Component) component.hoverEvent().value()));
    }
}
