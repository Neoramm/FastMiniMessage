package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.HoverPreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import io.github.neoramm.fastminimessage.model.RenderScope;
import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class HoverResolver implements ComponentResolver {

  @Override
  public PreComponent resolve(String name, Arguments args, PreComponent[] children, Style inheritedStyle, MiniMessageCompiler compiler) {
    String actionName = args.get(0, "show_text");
    @SuppressWarnings("unchecked")
    HoverEvent.Action<Object> action = (HoverEvent.Action<Object>) HoverEvent.Action.NAMES.value(actionName);
    if (action == null) {
      return new LiteralPreComponent("", inheritedStyle);
    }
    ActionHandler<Object> handler = actionHandler(action);
    if (handler == null) {
      return new LiteralPreComponent("", inheritedStyle);
    }
    if (children.length == 0) {
      return new LiteralPreComponent("", inheritedStyle);
    }
    BiFunction<Pointered, RenderScope, HoverEvent<?>> supplier = handler.buildSupplier(args, compiler);
    return new HoverPreComponent(supplier, children);
  }

  @SuppressWarnings("unchecked")
  private static <V> ActionHandler<V> actionHandler(HoverEvent.Action<V> action) {
    if (action == HoverEvent.Action.SHOW_TEXT) {
      return (ActionHandler<V>) ShowTextHandler.INSTANCE;
    } else if (action == HoverEvent.Action.SHOW_ITEM) {
      return (ActionHandler<V>) ShowItemHandler.INSTANCE;
    } else if (action == HoverEvent.Action.SHOW_ENTITY) {
      return (ActionHandler<V>) ShowEntityHandler.INSTANCE;
    }
    return null;
  }

  private interface ActionHandler<V> {
    BiFunction<Pointered, RenderScope, HoverEvent<?>> buildSupplier(Arguments args, MiniMessageCompiler compiler);
  }

  private static final class ShowTextHandler implements ActionHandler<Component> {
    static final ShowTextHandler INSTANCE = new ShowTextHandler();

    @Override
    public BiFunction<Pointered, RenderScope, HoverEvent<?>> buildSupplier(Arguments args, MiniMessageCompiler compiler) {
      String text = args.get(1, "");
      PreComponentTree tree = compiler.compile(text);
      return (pointered, scope) -> HoverEvent.showText(tree.render(pointered, scope));
    }
  }

  private static final class ShowItemHandler implements ActionHandler<HoverEvent.ShowItem> {
    static final ShowItemHandler INSTANCE = new ShowItemHandler();

    @Override
    public BiFunction<Pointered, RenderScope, HoverEvent<?>> buildSupplier(Arguments args, MiniMessageCompiler compiler) {
      try {
        String idRaw = args.get(1, "minecraft:air");
        Key key = Key.key(idRaw);
        int count = args.size() > 2 ? parseInt(args.get(2, "1")) : 1;
        if (args.size() > 3) {
          String value = args.get(3, "");
          if (value.startsWith("{")) {
            return (pointered, scope) -> HoverEvent.showItem(legacyShowItem(key, count, value));
          }
          Map<Key, BinaryTagHolder> datas = new HashMap<>();
          for (int i = 3; i < args.size(); i += 2) {
            Key dataKey = Key.key(args.get(i, ""));
            String dataVal = args.get(i + 1, "");
            datas.put(dataKey, BinaryTagHolder.binaryTagHolder(dataVal));
          }
          return (pointered, scope) -> HoverEvent.showItem(HoverEvent.ShowItem.showItem(key, count, datas));
        }
        HoverEvent.ShowItem item = HoverEvent.ShowItem.showItem(key, count);
        return (pointered, scope) -> HoverEvent.showItem(item);
      } catch (InvalidKeyException | NumberFormatException e) {
        return (pointered, scope) -> HoverEvent.showItem(HoverEvent.ShowItem.showItem(Key.key("minecraft:air"), 1));
      }
    }

    private static HoverEvent.ShowItem legacyShowItem(Key id, int count, String value) {
      return HoverEvent.ShowItem.showItem(id, count, BinaryTagHolder.binaryTagHolder(value));
    }

    private int parseInt(String raw) {
      try {
        return Integer.parseInt(raw);
      } catch (NumberFormatException e) {
        return 1;
      }
    }
  }

  private static final class ShowEntityHandler implements ActionHandler<HoverEvent.ShowEntity> {
    static final ShowEntityHandler INSTANCE = new ShowEntityHandler();

    @Override
    public BiFunction<Pointered, RenderScope, HoverEvent<?>> buildSupplier(Arguments args, MiniMessageCompiler compiler) {
      try {
        Key key = Key.key(args.get(1, "minecraft:pig"));
        UUID id = UUID.fromString(args.get(2, UUID.randomUUID().toString()));
        if (args.size() > 3) {
          String nameRaw = args.get(3, "");
          PreComponentTree nameTree = compiler.compile(nameRaw);
          return (pointered, scope) -> HoverEvent.showEntity(HoverEvent.ShowEntity.showEntity(key, id, nameTree.render(pointered, scope)));
        }
        HoverEvent.ShowEntity entity = HoverEvent.ShowEntity.showEntity(key, id);
        return (pointered, scope) -> HoverEvent.showEntity(entity);
      } catch (IllegalArgumentException | InvalidKeyException e) {
        return (pointered, scope) -> HoverEvent.showEntity(HoverEvent.ShowEntity.showEntity(Key.key("minecraft:pig"), UUID.randomUUID()));
      }
    }
  }

  @Override
  public String[] names() {
    return new String[]{"hover"};
  }
}