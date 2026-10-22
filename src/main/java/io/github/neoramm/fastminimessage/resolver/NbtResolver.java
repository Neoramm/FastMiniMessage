package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.NbtPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.BlockNBTComponent;
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
        @Nullable BlockNBTComponent.Pos pos = null;
        @Nullable String selector = null;
        @Nullable Key storage = null;
        int idx = 1;
        if (BLOCK.equals(type)) {
            if (args.size() < 2) throw new IllegalArgumentException("A position is required");
            try {
                pos = BlockNBTComponent.Pos.fromString(args.get(1));
            } catch (final IllegalArgumentException ex) {
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
            idx = 2;
        } else if (ENTITY.equals(type)) {
            if (args.size() < 2) throw new IllegalArgumentException("A selector is required");
            selector = args.get(1);
            idx = 2;
        } else if (STORAGE.equals(type)) {
            if (args.size() < 2) throw new IllegalArgumentException("A storage key is required");
            storage = Key.key(args.get(1));
            idx = 2;
        } else {
            throw new IllegalArgumentException("Unknown nbt tag type '" + type + "'");
        }
        if (args.size() <= idx) throw new IllegalArgumentException("An NBT path is required");
        final String nbtPath = args.get(idx);
        idx++;
        boolean interpret = false;
        @Nullable PreComponentTree separator = null;
        if (args.size() > idx) {
            final String popped = args.get(idx);
            idx++;
            if (INTERPRET.equalsIgnoreCase(popped)) interpret = true;
            else {
                separator = compiler.compile(popped);
                if (args.size() > idx && INTERPRET.equalsIgnoreCase(args.get(idx))) interpret = true;
            }
        }
        return switch (type) {
            case BLOCK -> NbtPreComponent.block(pos, nbtPath, interpret, separator);
            case ENTITY -> NbtPreComponent.entity(selector, nbtPath, interpret, separator);
            default -> NbtPreComponent.storage(storage, nbtPath, interpret, separator);
        };
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
