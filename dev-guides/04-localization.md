# 4. Localization (`FastMiniMessageTranslationStore`)

FastMiniMessage integrates with Adventure's `GlobalTranslator` and `TranslationStore` APIs. Translation strings are compiled into `PreComponentTree` instances at registration time, eliminating MiniMessage parsing overhead during translation rendering.

---

## Registering Translations with `GlobalTranslator`

```java
import io.github.neoramm.fastminimessage.v2.translation.Argument;
import io.github.neoramm.fastminimessage.v2.translation.FastMiniMessageTranslationStore;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import java.nio.file.Path;
import java.util.Locale;

// Create the store (optionally pass a custom MiniMessageCompiler as the 2nd argument)
FastMiniMessageTranslationStore store =
    FastMiniMessageTranslationStore.create(Key.key("myplugin:translations"));

store.defaultLocale(Locale.ENGLISH);

// Register individual strings
store.register("welcome.user", Locale.ENGLISH, "<green>Welcome, <name>! You have <coins> coins.</green>");
store.register("welcome.user", Locale.of("es"), "<green>¡Bienvenido, <name>! Tienes <coins> monedas.</green>");

// Or load UTF-8 .properties files (pass escapeSingleQuotes = false to keep apostrophes intact)
store.registerAll(Locale.ENGLISH, Path.of("lang/messages_en.properties"), false);

// Add to Adventure's GlobalTranslator
GlobalTranslator.translator().addSource(store);
```

When disabling your plugin, unregister the source:
```java
GlobalTranslator.translator().removeSource(store);
```

---

## Positional & Named Arguments (`Argument`)

### 1. Standard Indexed Arguments (`<arg:0>`, `<argument:1>`)
Works with standard `Component.translatable` arguments:
```java
store.register("Broadcast", Locale.ENGLISH, "<gold>[<arg:0>]</gold> <white><argument:1></white>");

Component msg = Component.translatable("Broadcast",
    Component.text("Alert"),
    Component.text("Event starting!")
);
Component rendered = GlobalTranslator.render(msg, Locale.ENGLISH);
```

### 2. Named Arguments (`io.github.neoramm.fastminimessage.v2.translation.Argument`)
Named arguments can be referenced both **by name** (`<name>`) and **by index** (`<arg:0>`). String values are inserted as literal components and cannot inject formatting tags:

```java
Component msg = Component.translatable("welcome.user",
    Argument.string("name", "Alex"),
    Argument.numeric("coins", 250),
    Argument.bool("vip", true),
    Argument.component("badge", Component.text("[MVP]", NamedTextColor.AQUA))
);
```

> **Note:** Argument names must consist of lowercase letters (`a-z`), digits (`0-9`), `_`, or `-`, and must not collide with built-in compile-time tags (such as `<red>` or `<bold>`).

### 3. Passing Payloads and Viewer Target
Register translation placeholders before compiling translations, then pass their typed values through `Argument.payload`.
Payload and target arguments do not consume positional `<arg:N>` indices.

```java
final var layout = PayloadSchema.builderWithDefaults();
final var guild = layout.key("guild", String.class);
final var registry = ResolverRegistry.withDefaults(layout.build())
    .registerPlaceholder("guild", (final var scope) -> Component.text(scope.payload(guild)));
final var compiler = new MiniMessageCompiler(registry);
final var store = FastMiniMessageTranslationStore.create(Key.key("example:guild"), compiler);
store.register("guild.announce", Locale.ENGLISH, "Welcome to <guild>");
final var msg = Component.translatable("guild.announce",
    Argument.target(player), Argument.payload(guild, "Valkyrie"));
```

Use `builderWithDefaults()` for translation schemas so indexed and named translation arguments retain their reserved
payload slots. `RUNTIME_TAGS_KEY` remains available for legacy resolver integration, but is unnecessary for registered
native placeholders.

---

## Custom Translation Backends (`FastMiniMessageTranslator`)

If you store precompiled `PreComponentTree` instances in your own database or cache instead of `FastMiniMessageTranslationStore`, extend `FastMiniMessageTranslator`:

```java
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
import io.github.neoramm.fastminimessage.v2.translation.FastMiniMessageTranslator;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Locale;

public final class CustomDatabaseTranslator extends FastMiniMessageTranslator {
    @Override
    public @NotNull Key name() {
        return Key.key("myplugin:db_translator");
    }

    @Override
    protected @Nullable PreComponentTree getTranslation(@NotNull String key, @NotNull Locale locale) {
        return myCache.getCompiledTree(key, locale); // Return null if key is unknown
    }
}
```
