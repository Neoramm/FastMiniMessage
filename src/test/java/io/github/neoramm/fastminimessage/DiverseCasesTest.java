package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.model.Placeholder;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiverseCasesTest {
  private final MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
  private final MiniMessage miniMessage = MiniMessage.builder().strict(false).build();
  private static final Pointered NOBODY = new Pointered() {
    public Pointers pointers() { return Pointers.empty(); }
  };

  private void check(String input) {
    Component ref = miniMessage.deserialize(input);
    Component rendered = compiler.compile(input).render(NOBODY);
    List<String> e = ComponentComparison.expression(ref);
    List<String> a = ComponentComparison.expression(rendered);
    if (!e.equals(a)) {
    }
    assertEquals(e, a, input);
  }

  @Test
  void diverse() {
    String[] cases = {
      "hello world",
      "<red>red text</red>",
      "<#ff0000>hex</#ff0000>",
      "<red><bold>rb</bold></red>",
      "<bold><italic><underlined><strikethrough><obfuscated>x</obfuscated></strikethrough></underlined></italic></bold>",
      "<red>a<reset>b",
      "<bold><red>a<reset>b",
      "<reset>abc",
      "<red>a</red><reset>b<bold>c</bold>",
      "<red>a</red>b<red>c</red>",
      "<bold>a<!bold>b</!bold>c</bold>",
      "<red>a<!red>b",
      "<bold><red><click:run_command:'/spawn'>x</click></red></bold>",
      "<red><click:run_command:'/spawn'>a <bold>b</bold></click></red>",
      "<insert:'x'>a <underlined>b</underlined></insert>",
      "<shadow:red:0.4>a <bold>b</bold></shadow>",
      "<hover:show_text:'<gray>hint <bold>x</bold></gray>'>a <red>b</red></hover>",
      "<red><hover:show_text:'hint'>a <bold>b</bold></hover></red>",
      "<click:run_command:'/x'><hover:show_text:'h'>a <red>b</red></hover></click>",
      "<hover:show_text:'h'><click:run_command:'/x'>a</click></hover>",
      "<hover:show_item:'minecraft:diamond':1>a</hover>",
      "<red><hover:show_item:'minecraft:diamond':1>a</hover></red>",
      "<hover:show_entity:'minecraft:zombie':00000000-0000-0000-0000-000000000000:'Zombie'>a</hover>",
      "<key:key.jump/>",
      "<red><key:key.jump/></red>",
      "<lang:chat.type.text:Steve:hello/>",
      "<red><lang:chat.type.text:Steve:hello/></red>",
      "<lang:chat.type.text:'<red>Steve</red>':hello/>",
      "<sprite:items:item/diamond/>",
      "<red><sprite:items:item/diamond/></red>",
      "<head:Steve/>",
      "<gold><head:Steve/></gold>",
      "<gradient:red:blue>abc</gradient>",
      "<bold><gradient:red:blue>abc</gradient></bold>",
      "<gradient:red:blue><bold>a</bold>b</gradient>",
      "<gradient:red:blue>a <red>b</red> c</gradient>",
      "<red><gradient:red:blue>abc</gradient></red>",
      "<gradient:red:blue><gradient:green:yellow>abc</gradient>def</gradient>",
      "<gradient:red:blue><rainbow>abc</rainbow>def</gradient>",
      "<gradient:red:blue><key:key.jump/>abc</gradient>",
      "<gradient:#ff0000:#0000ff:0.5>abc</gradient>",
      "<rainbow>abc</rainbow>",
      "<rainbow:5>abc</rainbow>",
      "<rainbow:0>abc</rainbow>",
      "<rainbow:!5>abc</rainbow>",
      "<pride>abc</pride>",
      "<newline/>",
      "<bold>a<br/>b</bold>",
      "<red>a<newline/>b</red>",
      "<red>😀a</red>",
      "<gradient:red:blue>😀a</gradient>",
      "😀🎉",
      "",
      "<bold>",
      "<red>a",
      "</red>abc",
      "<red>a</blue>b",
      "<unknown>x</unknown>",
      "<unknown/>",
      "<unknown>",
      "<font:minecraft:default>text</font>",
      "<red><font:minecraft:default>text</font></red>",
      "<transition:red:blue:0.5/>",
      "<transition:red:blue/>",
      "<selector:@a>",
      "<score:Steve:objective>",
      "<red><score:Steve:objective></red>",
      "<nbt:block:~ ~ ~:Items>",
      "<nbt:entity:@e:Inventory>",
      "<nbt:storage:foo:bar>",
      "<hover:show_text:'h'><gradient:red:blue>abc</gradient></hover>",
      "<gradient:red:blue><hover:show_text:'h'>b</hover></gradient>",
      "<click:run_command:'/x'><gradient:red:blue>abc</gradient></click>",
      "<gradient:red:blue><click:run_command:'/x'>abc</click></gradient>",
      "<red>a</red><red>b</red>",
      "<red>a</red><blue>b</blue>",
      "<red><lang:chat.type.text:Steve:hello/></red>",
      "<shadow:red:0.4>a <bold>b</bold></shadow>",
      "<insert:'x'>a <underlined>b</underlined></insert>",
    };
    for (String s : cases) check(s);
  }

  @Test
  void dynamicPlaceholders() {
    String[][] cases = {
      {"<p>", "X"},
      {"<bold><red><p></red></bold>", "X"},
      {"<red>a<p>c</red>", "b"},
      {"<gradient:red:blue><p></gradient>", "abc"},
      {"<gradient:red:blue>a<p>c</gradient>", "b"},
      {"<hover:show_text:'h'><p></hover>", "X"},
      {"<click:run_command:'/x'><p></click>", "X"},
      {"<insert:'x'><bold><p></bold></insert>", "X"},
      {"<shadow:red:0.4><p></shadow>", "X"},
      {"<key:key.jump/>", "X"}
    };
    for (String[] c : cases) {
      PreComponentTree tree = compiler.compile(c[0]);
      Component actual = tree.render(NOBODY, Placeholder.ofUnparsed("p", c[1]));
      Component expected = miniMessage.deserialize(c[0].replace("<p>", c[1]));
      assertEquals(ComponentComparison.expression(expected), ComponentComparison.expression(actual), c[0]);
    }
  }

  @Test
  void parsedPlaceholderStyleWins() {
    PreComponentTree tree = compiler.compile("<bold><red><p></red></bold>");
    Component actual = tree.render(NOBODY, Placeholder.ofParsed("p", Component.text("X", NamedTextColor.BLUE)));
    Component expected = miniMessage.deserialize("<bold><red><blue>X</blue></red></bold>");
    assertEquals(ComponentComparison.expression(expected), ComponentComparison.expression(actual));
  }

  @Test
  void renderStability() {
    for (String s : new String[]{"<gradient:red:blue>abc</gradient>", "<red>a<reset>b", "<unknown>x</unknown>", "</red>abc"}) {
      PreComponentTree tree = compiler.compile(s);
      assertEquals(ComponentComparison.expression(tree.render(NOBODY)), ComponentComparison.expression(tree.render(NOBODY)));
    }
  }
}
