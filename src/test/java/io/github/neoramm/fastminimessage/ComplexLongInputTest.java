package io.github.neoramm.fastminimessage;

import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComplexLongInputTest {
  private final MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
  private final MiniMessage miniMessage = MiniMessage.builder().strict(false).build();
  private static final Pointered NOBODY = new Pointered() {
    public Pointers pointers() { return Pointers.empty(); }
  };

  private static final String LONG = """
    <bold>Lorem ipsum dolor sit amet</bold>, <red>consectetur adipiscing elit</red>, \
    <gradient:#ff0000:#0000ff>sed do eiusmod tempor incididunt ut labore et dolore magna aliqua</gradient>. \
    <italic>Ut enim ad minim veniam</italic>, <click:run_command:'/spawn'><hover:show_text:'<gray>hint <bold>x</bold></gray>'>quis nostrud exercitation</hover></click> \
    ullamco laboris nisi ut aliquip ex ea commodo consequat. <shadow:red:0.4>Duis aute irure dolor in reprehenderit</shadow> \
    in voluptate velit esse cillum dolore eu fugiat nulla pariatur. <rainbow>Excepteur sint occaecat cupidatat non proident</rainbow>, \
    sunt in culpa qui officia deserunt mollit anim id est laborum. <newline/>\
    <bold><red><blue>nested color override</blue></red></bold> \
    <red>a</blue>b malformed</red> </unknown>leftover \
    <unknown>x</unknown> <transition:red:blue:0.5/> <font:minecraft:default>font text</font> \
    <gradient:red:blue><key:key.jump/> mixed <sprite:items:item/diamond/> content</gradient> \
    <hover:show_item:'minecraft:diamond':1><bold>item hover</bold></hover> \
    <lang:chat.type.text:'<red>Steve</red>':hello/> <score:Steve:objective> <selector:@a> \
    <nbt:block:~ ~ ~:Items> <pride>pride text</pride> <insert:'x'>inserted <u>underlined</u></insert> \
    <obfuscated>secret</obfuscated> <strikethrough>struck</strikethrough> \
    <gradient:red:blue><gradient:green:yellow>nested gradient</gradient> tail</gradient> \
    <click:run_command:'/x'><gradient:red:blue>grad in click</gradient></click> \
    <hover:show_text:'h'><click:run_command:'/x'>click in hover</click></hover> \
    <red>😀 emoji and surrogate 😀</red> <reset>reset here <bold>bold after reset</bold></reset> \
    </red>unmatched close <unknown/> selfclosing unknown \
    <bold>unclosed bold span continues
    """;

  @Test
  void complexLongInputMatches() {
    Component ref = miniMessage.deserialize(LONG);
    Component rendered = compiler.compile(LONG).render(NOBODY);
    List<String> e = ComponentComparison.expression(ref);
    List<String> a = ComponentComparison.expression(rendered);
    if (!e.equals(a)) {
      int i = 0;
      int size = Math.min(e.size(), a.size());
      while (i < size && e.get(i).equals(a.get(i))) i++;
    }
    assertEquals(e, a, "complex long input expression differs");
  }
}
