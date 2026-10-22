/**
 * Resolver for the NbtResolver tag.
 */
package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.StaticComponentPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.BlockNBTComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.NBTComponentBuilder;
import net.kyori.adventure.text.format.Style;

public final class NbtResolver implements ComponentResolver {
  private static final String BLOCK = "block";
  private static final String ENTITY = "entity";
  private static final String STORAGE = "storage";
  private static final String INTERPRET = "interpret";

  @Override
  public PreComponent resolve(String name, Arguments args, PreComponent[] children, Style inheritedStyle, MiniMessageCompiler compiler) {
    if (args.isEmpty()) throw new IllegalArgumentException("a type of block, entity, or storage is required");
    String type = args.get(0).toLowerCase();
    NBTComponentBuilder<?, ?> builder;
    int idx = 1;
    if (BLOCK.equals(type)) {
      if (args.size() < 2) throw new IllegalArgumentException("A position is required");
      String pos = args.get(1);
      idx = 2;
      try {
        builder = Component.blockNBT().pos(BlockNBTComponent.Pos.fromString(pos));
      } catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException(ex.getMessage(), ex);
      }
    } else if (ENTITY.equals(type)) {
      if (args.size() < 2) throw new IllegalArgumentException("A selector is required");
      builder = Component.entityNBT().selector(args.get(1));
      idx = 2;
    } else if (STORAGE.equals(type)) {
      if (args.size() < 2) throw new IllegalArgumentException("A storage key is required");
      builder = Component.storageNBT().storage(Key.key(args.get(1)));
      idx = 2;
    } else {
      throw new IllegalArgumentException("Unknown nbt tag type '" + type + "'");
    }
    if (args.size() <= idx) throw new IllegalArgumentException("An NBT path is required");
    builder.nbtPath(args.get(idx));
    idx++;
    if (args.size() > idx) {
      String popped = args.get(idx);
      idx++;
      if (INTERPRET.equalsIgnoreCase(popped)) {
        builder.interpret(true);
      } else {
        builder.separator(compiler.deserialize(popped));
        if (args.size() > idx && INTERPRET.equalsIgnoreCase(args.get(idx))) {
          builder.interpret(true);
          idx++;
        }
      }
    }
    return new StaticComponentPreComponent(builder.build());
  }

  @Override
  public String[] names() {
    return new String[]{"nbt", "data"};
  }

  @Override
  public boolean isSelfClosing() {
    return true;
  }
}
