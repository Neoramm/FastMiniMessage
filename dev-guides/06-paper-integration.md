# 6. Paper Integration (`fastminimessage-paper`)

The `fastminimessage-paper` module targets **Paper 26.2+** (Java 25+, Adventure 5.2.0+) and adds compile-time and runtime tags backed by Paper's Registry, Dialog, ItemStack, and PlaceholderAPI systems.

---

## Creating the Paper Compiler

Use `PaperTags.compiler()` to get a `MiniMessageCompiler` preconfigured with all standard MiniMessage tags plus the Paper-specific tags and registry dialog resolver:

```java
import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
import io.github.neoramm.fastminimessage.v2.paper.PaperTags;
import net.kyori.adventure.text.Component;

MiniMessageCompiler compiler = PaperTags.compiler();
```

---

## Paper Tags Reference

### 1. Registry Dialog Click (`<click:show_dialog:'key'>`)
Resolves a dialog `Key` from Paper's `RegistryKey.DIALOG` (`RegistryAccess.registryAccess().getRegistry(RegistryKey.DIALOG)`):

```java
PreComponentTree links = compiler.compile(
    "<aqua><click:show_dialog:'minecraft:server_links'>[View Server Links]</click></aqua>"
);
```
> **Note:** Registry dialogs are resolved at compile time. Recompile templates if your server reloads datapack dialogs.

### 2. Inline Notice Dialog (`<show_dialog:'title':'body':'button'>`)
Builds a notice dialog using `Dialog.create(...)` with a title, optional body, and optional close button label (defaults to `"OK"`). All three arguments support full MiniMessage formatting and dynamic runtime placeholders:

```java
PreComponentTree rules = compiler.compile(
    "<yellow><show_dialog:'<gold><bold>Rules for <player></bold></gold>':'<gray>1. Be respectful<br/>2. No griefing</gray>':'<green>I Understand</green>'>[Read Rules]</show_dialog></yellow>"
);

Component rendered = rules.render((final var builder) -> builder.viewer(player));
```

### 3. Item Stack Hover (`<item_hover:'reference':count>`)
Displays a rich item tooltip using `ItemStack#asHoverEvent()`:
- **Material name**: `<item_hover:'minecraft:diamond_sword':1>Sword</item_hover>` or `<item_hover:diamond:16>16x Diamonds</item_hover>`.
- **Serialized Base64 bytes** (preserves custom name, lore, enchantments, and data components):

```java
import java.util.Base64;
import org.bukkit.inventory.ItemStack;

String encoded = Base64.getEncoder().encodeToString(itemStack.serializeAsBytes());
PreComponentTree itemMsg = compiler.compile(
    "<white>You received <item_hover:'" + encoded + "'>[Hover to Inspect]</item_hover>!</white>"
);
```
If the item reference is constant, the `HoverEvent` is built once during compilation and cached inside `TemplateNode.Static`.

### 4. PlaceholderAPI (`<papi:'expression'>`)
Evaluates PlaceholderAPI placeholders at render time when the `Pointered` viewer is an `org.bukkit.entity.Player` (or in global server context if `null`). Surrounding `%` symbols are optional:

```java
// Both '%player_ping%' and 'player_ping' work:
PreComponentTree stats = compiler.compile(
    "<gray>Ping: <green><papi:'player_ping'>ms</green> | Balance: <gold>$<papi:'%vault_eco_balance%'></gold></gray>"
);

Component rendered = stats.render((final var builder) -> builder.viewer(player));
```
You can also interpolate FastMiniMessage runtime placeholders inside the `<papi>` expression before it is sent to PlaceholderAPI:
```java
final var layout = PayloadSchema.builder();
final var stat = layout.key("stat", String.class);
final var registry = ResolverRegistry.withDefaults(layout.build())
    .registerPlaceholder("stat", (final var scope) -> Component.text(scope.payload(stat)));
PaperTags.apply(registry, PaperTagOptions.defaults());
final var dynamicPapi = new MiniMessageCompiler(registry).compile("<papi:'%statistic_<stat>%'/>");
final var timePlayed = dynamicPapi.render((final var builder) -> builder.viewer(player)
    .payload(stat, "play_one_minute"));
```
If PlaceholderAPI is not enabled on the server, `<papi>` safely falls back to rendering the expression string as-is.

---

## Testing Outside a Server (`PaperTagOptions`)

When running unit tests without a live Paper server, provide custom factories via `PaperTagOptions`:

```java
import io.github.neoramm.fastminimessage.v2.paper.PaperTagOptions;
import io.github.neoramm.fastminimessage.v2.paper.PaperTags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;

PaperTagOptions testOptions = new PaperTagOptions(
    ref -> mockDialog,                                      // DialogResolver (registryDialogs)
    inline -> mockDialog,                                   // DialogFactory (inlineDialogs)
    (ref, count) -> HoverEvent.showText(Component.text(ref)), // ItemHoverFactory (itemHovers)
    (viewer, expr) -> "mocked_" + expr                      // PapiRenderer (placeholders)
);

MiniMessageCompiler testCompiler = PaperTags.compiler(testOptions);
```
