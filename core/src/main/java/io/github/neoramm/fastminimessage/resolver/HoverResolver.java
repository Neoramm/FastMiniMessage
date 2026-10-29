package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.HoverPreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

public final class HoverResolver implements ComponentResolver {
    private static final String @NotNull [] NAMES = {"hover"};

    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        final String actionName = args.getFirst("show_text");
        @SuppressWarnings("unchecked")
        final HoverEvent.Action<Object> action = (HoverEvent.Action<Object>) HoverEvent.Action.NAMES.value(actionName);
        if (action == null) return new LiteralPreComponent("", inheritedStyle);
        final ActionHandler<Object> handler = actionHandler(action);
        if (handler == null) return new LiteralPreComponent("", inheritedStyle);
        if (children.length == 0) return new LiteralPreComponent("", inheritedStyle);
        final BiFunction<Pointered, RenderScope, HoverEvent<?>> supplier = handler.buildSupplier(args, compiler);
        return new HoverPreComponent(supplier, children).setStyle(inheritedStyle);
    }

    @SuppressWarnings("unchecked")
    private static <V> @Nullable ActionHandler<V> actionHandler(final @NotNull HoverEvent.Action<V> action) {
        if (action == HoverEvent.Action.SHOW_TEXT) return (ActionHandler<V>) ShowTextHandler.INSTANCE;
        if (action == HoverEvent.Action.SHOW_ITEM) return (ActionHandler<V>) ShowItemHandler.INSTANCE;
        if (action == HoverEvent.Action.SHOW_ENTITY) return (ActionHandler<V>) ShowEntityHandler.INSTANCE;
        return null;
    }

    private interface ActionHandler<V> {
        @NotNull BiFunction<Pointered, RenderScope, HoverEvent<?>> buildSupplier(final @NotNull Arguments args,
            final @NotNull MiniMessageCompiler compiler);
    }

    private static final class ShowTextHandler implements ActionHandler<Component> {
        static final ShowTextHandler INSTANCE = new ShowTextHandler();

        @Override
        public @NotNull BiFunction<Pointered, RenderScope, HoverEvent<?>> buildSupplier(final @NotNull Arguments args,
            final @NotNull MiniMessageCompiler compiler) {
            final String text = args.get(1, "");
            final PreComponentTree tree = compiler.compile(text);
            if (tree.isStatic()) {
                final HoverEvent<Component> cachedEvent = HoverEvent.showText(tree.render(null));
                return (final @Nullable Pointered pointered, final @NotNull RenderScope scope) ->
                    scope.isEmpty() ? cachedEvent : HoverEvent.showText(tree.render(pointered, scope));
            }
            return (final @Nullable Pointered pointered, final @NotNull RenderScope scope) ->
                HoverEvent.showText(tree.render(pointered, scope));
        }
    }

    private static final class ShowItemHandler implements ActionHandler<HoverEvent.ShowItem> {
        static final ShowItemHandler INSTANCE = new ShowItemHandler();

        @Override
        public @NotNull BiFunction<Pointered, RenderScope, HoverEvent<?>> buildSupplier(final @NotNull Arguments args,
            final @NotNull MiniMessageCompiler compiler) {
            try {
                final String idRaw = args.get(1, "minecraft:air");
                final Key key = Key.key(idRaw);
                final int count = args.size() > 2 ? parseInt(args.get(2, "1")) : 1;
                if (args.size() > 3) {
                    final String value = args.get(3, "");
                    if (value.startsWith("{")) {
                        return (final @Nullable Pointered pointered, final @NotNull RenderScope scope) ->
                            HoverEvent.showItem(legacyShowItem(key, count, value));
                    }
                    final Map<@NotNull Key, @NotNull BinaryTagHolder> datas = new HashMap<>();
                    for (int i = 3; i < args.size(); i += 2) {
                        final Key dataKey = Key.key(args.get(i, ""));
                        final String dataValue = args.get(i + 1, "");
                        datas.put(dataKey, BinaryTagHolder.binaryTagHolder(dataValue));
                    }
                    return (final @Nullable Pointered pointered, final @NotNull RenderScope scope) ->
                        HoverEvent.showItem(HoverEvent.ShowItem.showItem(key, count, datas));
                }
                final HoverEvent.ShowItem item = HoverEvent.ShowItem.showItem(key, count);
                final HoverEvent<HoverEvent.ShowItem> event = HoverEvent.showItem(item);
                return (final @Nullable Pointered pointered, final @NotNull RenderScope scope) -> event;
            } catch (final InvalidKeyException | NumberFormatException e) {
                final HoverEvent<HoverEvent.ShowItem> fallback =
                    HoverEvent.showItem(HoverEvent.ShowItem.showItem(Key.key("minecraft:air"), 1));
                return (final @Nullable Pointered pointered, final @NotNull RenderScope scope) -> fallback;
            }
        }

        private static @NotNull HoverEvent.ShowItem legacyShowItem(final @NotNull Key id, final int count,
            final @NotNull String value) {
            return HoverEvent.ShowItem.showItem(id, count, BinaryTagHolder.binaryTagHolder(value));
        }

        private int parseInt(final @NotNull String raw) {
            try {
                return Integer.parseInt(raw);
            } catch (final NumberFormatException e) {
                return 1;
            }
        }
    }

    private static final class ShowEntityHandler implements ActionHandler<HoverEvent.ShowEntity> {
        static final ShowEntityHandler INSTANCE = new ShowEntityHandler();

        @Override
        public @NotNull BiFunction<Pointered, RenderScope, HoverEvent<?>> buildSupplier(final @NotNull Arguments args,
            final @NotNull MiniMessageCompiler compiler) {
            try {
                final Key key = Key.key(args.get(1, "minecraft:pig"));
                final String idRaw = args.get(2);
                final UUID id = idRaw != null ? UUID.fromString(idRaw) : UUID.randomUUID();
                if (args.size() > 3) {
                    final String nameRaw = args.get(3, "");
                    final PreComponentTree nameTree = compiler.compile(nameRaw);
                    if (nameTree.isStatic()) {
                        final HoverEvent<HoverEvent.ShowEntity> cachedEvent = HoverEvent.showEntity(
                            HoverEvent.ShowEntity.showEntity(key, id, nameTree.render(null)));
                        return (final @Nullable Pointered pointered, final @NotNull RenderScope scope) ->
                            scope.isEmpty() ? cachedEvent : HoverEvent.showEntity(
                                HoverEvent.ShowEntity.showEntity(key, id, nameTree.render(pointered, scope)));
                    }
                    return (final @Nullable Pointered pointered, final @NotNull RenderScope scope) ->
                        HoverEvent.showEntity(
                            HoverEvent.ShowEntity.showEntity(key, id, nameTree.render(pointered, scope)));
                }
                final HoverEvent.ShowEntity entity = HoverEvent.ShowEntity.showEntity(key, id);
                final HoverEvent<HoverEvent.ShowEntity> event = HoverEvent.showEntity(entity);
                return (final @Nullable Pointered pointered, final @NotNull RenderScope scope) -> event;
            } catch (final IllegalArgumentException | InvalidKeyException e) {
                final HoverEvent<HoverEvent.ShowEntity> fallback = HoverEvent.showEntity(
                    HoverEvent.ShowEntity.showEntity(Key.key("minecraft:pig"), UUID.randomUUID()));
                return (final @Nullable Pointered pointered, final @NotNull RenderScope scope) -> fallback;
            }
        }
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return NAMES;
    }
}
