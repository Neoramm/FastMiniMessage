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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NbtResolver implements ComponentResolver {
    private static final @NotNull String BLOCK = "block";
    private static final @NotNull String ENTITY = "entity";
    private static final @NotNull String STORAGE = "storage";
    private static final @NotNull String INTERPRET = "interpret";

    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        if (args.isEmpty()) throw new IllegalArgumentException("a type of block, entity, or storage is required");
        final String type = args.get(0).toLowerCase();
        NBTComponentBuilder<?, ?> builder;
        int idx = 1;
        if (BLOCK.equals(type)) {
            if (args.size() < 2) throw new IllegalArgumentException("A position is required");
            final String pos = args.get(1);
            idx = 2;
            try {
                builder = Component.blockNBT().pos(BlockNBTComponent.Pos.fromString(pos));
            } catch (final IllegalArgumentException ex) {
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
            final String popped = args.get(idx);
            idx++;
            if (INTERPRET.equalsIgnoreCase(popped)) builder.interpret(true);
            else {
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
    public @NotNull String @NotNull [] names() {
        return new String[]{"nbt", "data"};
    }

    @Override
    public boolean isSelfClosing() {
        return true;
    }
}
