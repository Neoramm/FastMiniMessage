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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpriteResolverTest {

  private static final Pointered NOBODY = new Pointered() {
    @Override
    public Pointers pointers() {
      return Pointers.empty();
    }
  };
  private static final Key ITEMS = Key.key("minecraft:items");
  private static final Key PICKAXE = Key.key("minecraft:item/diamond_pickaxe");

  private final MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());

  private static List<SpriteObjectContents> sprites(Component component) {
    List<SpriteObjectContents> found = new ArrayList<>();
    collect(component, found);
    return found;
  }

  private static void collect(Component component, List<SpriteObjectContents> found) {
    if (component instanceof ObjectComponent object && object.contents() instanceof SpriteObjectContents sprite) {
      found.add(sprite);
    }
    for (Component child : component.children()) {
      collect(child, found);
    }
  }

  private Component render(String input) {
    return compiler.compile(input).render(NOBODY);
  }

  @Test
  void atlasAndSpriteProduceAnObjectComponent() {
    List<SpriteObjectContents> sprites = sprites(render("<sprite:items:item/diamond_pickaxe> Miner"));
    assertEquals(1, sprites.size());
    assertEquals(ITEMS, sprites.getFirst().atlas());
    assertEquals(PICKAXE, sprites.getFirst().sprite());
  }

  @Test
  void singleAndDoubleQuotesAllowNamespacedKeys() {
    for (String input : List.of(
        "<sprite:'minecraft:items':'minecraft:item/diamond_pickaxe'>",
        "<sprite:\"minecraft:items\":\"minecraft:item/diamond_pickaxe\">",
        "<sprite:\"minecraft:items\":'item/diamond_pickaxe'/>")) {
      List<SpriteObjectContents> sprites = sprites(render(input));
      assertEquals(1, sprites.size(), input);
      assertEquals(ITEMS, sprites.getFirst().atlas(), input);
      assertEquals(PICKAXE, sprites.getFirst().sprite(), input);
    }
  }

  @Test
  void spriteWithoutAtlasUsesTheDefaultOne() {
    SpriteObjectContents sprite = sprites(render("<sprite:block/stone>")).getFirst();
    assertEquals(SpriteObjectContents.DEFAULT_ATLAS, sprite.atlas());
    assertEquals(Key.key("minecraft:block/stone"), sprite.sprite());
  }

  @Test
  void spriteSurvivesInsideOtherTags() {
    Component component = render("<gradient:#b0b0b0:#5ec8ff><sprite:items:item/diamond_pickaxe> <bold>Miner</bold></gradient>");
    assertEquals(1, sprites(component).size());
    assertTrue(PlainTextComponentSerializer.plainText().serialize(component).endsWith(" Miner"));
  }

  @Test
  void invalidSpriteKeepsTheRawTagInsteadOfDisappearing() {
    PreComponentTree tree = compiler.compile("<sprite:'Mal Clave'> Miner");
    List<String> unparsed = new ArrayList<>();
    tree.visit(component -> {
      if (component instanceof UnparsedPreComponent tag) {
        unparsed.add(tag.getUnparsedTag());
      }
    });
    assertEquals(List.of("<sprite:'Mal Clave'>"), unparsed);
    Component component = tree.render(NOBODY);
    assertTrue(sprites(component).isEmpty());
    assertEquals("<sprite:'Mal Clave'> Miner", PlainTextComponentSerializer.plainText().serialize(component));
  }

  @Test
  void quotedArgumentsKeepApostrophesAndColons() {
    Component component = render("<hover:show_text:\"It's 10:30\">hello</hover>");
    assertEquals("hello", PlainTextComponentSerializer.plainText().serialize(component));
    assertEquals("It's 10:30", PlainTextComponentSerializer.plainText()
        .serialize((Component) component.hoverEvent().value()));
  }
}
