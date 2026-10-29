package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.BlockNBTComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.NBTComponentBuilder;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NbtPreComponent extends PreComponent {

    private enum Kind {
        BLOCK, ENTITY, STORAGE
    }

    private final @NotNull Kind kind;
    private final @Nullable BlockNBTComponent.Pos pos;
    private final @Nullable String selector;
    private final @Nullable Key storage;
    private final @NotNull String nbtPath;
    private final boolean interpret;
    private final @Nullable PreComponentTree separator;
    private volatile @Nullable Component cachedComponent;

    private NbtPreComponent(final @NotNull Kind kind, final @Nullable BlockNBTComponent.Pos pos,
        final @Nullable String selector, final @Nullable Key storage, final @NotNull String nbtPath,
        final boolean interpret, final @Nullable PreComponentTree separator) {
        this.kind = kind;
        this.pos = pos;
        this.selector = selector;
        this.storage = storage;
        this.nbtPath = nbtPath;
        this.interpret = interpret;
        this.separator = separator;
    }

    public static @NotNull NbtPreComponent block(final @NotNull BlockNBTComponent.Pos pos,
        final @NotNull String nbtPath, final boolean interpret, final @Nullable PreComponentTree separator) {
        return new NbtPreComponent(Kind.BLOCK, pos, null, null, nbtPath, interpret, separator);
    }

    public static @NotNull NbtPreComponent entity(final @NotNull String selector, final @NotNull String nbtPath,
        final boolean interpret, final @Nullable PreComponentTree separator) {
        return new NbtPreComponent(Kind.ENTITY, null, selector, null, nbtPath, interpret, separator);
    }

    public static @NotNull NbtPreComponent storage(final @NotNull Key storage, final @NotNull String nbtPath,
        final boolean interpret, final @Nullable PreComponentTree separator) {
        return new NbtPreComponent(Kind.STORAGE, null, null, storage, nbtPath, interpret, separator);
    }

    @Override
    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        final @Nullable Component cached = this.cachedComponent;
        if (cached != null && scope.isEmpty()) return cached;
        final NBTComponentBuilder<?, ?> builder = switch (kind) {
            case BLOCK -> Component.blockNBT().pos(pos);
            case ENTITY -> Component.entityNBT().selector(selector);
            case STORAGE -> Component.storageNBT().storage(storage);
        };
        builder.nbtPath(nbtPath);
        final @Nullable PreComponentTree separator = this.separator;
        if (separator != null) builder.separator(separator.render(pointered, scope));
        if (interpret) builder.interpret(true);
        final Component component = builder.build();
        final Style style = computedStyle != null ? computedStyle : Style.empty();
        final Component result = style.isEmpty() ? component : component.style(style.merge(component.style()));
        if (isStatic() && scope.isEmpty()) this.cachedComponent = result;
        return result;
    }

    @Override
    public @NotNull PreComponent setStyle(final @Nullable Style style) {
        super.setStyle(style);
        this.cachedComponent = null;
        return this;
    }

    @Override
    public boolean isStatic() {
        final @Nullable PreComponentTree separator = this.separator;
        return separator == null || separator.isStatic();
    }

    @Override
    public @NotNull PreComponent deepCopy() {
        final @Nullable PreComponentTree separator = this.separator;
        final NbtPreComponent copy = new NbtPreComponent(
            kind, pos, selector, storage, nbtPath, interpret, separator != null ? separator.copy() : null);
        copy.setStyle(computedStyle);
        return copy;
    }
}
