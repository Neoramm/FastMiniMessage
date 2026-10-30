[![Maven Central](https://img.shields.io/maven-central/v/io.github.neoramm/fastminimessage.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.neoramm/fastminimessage)

# FastMiniMessage

FastMiniMessage is a fast, precompiled implementation of the MiniMessage format for
[Kyori Adventure](https://docs.papermc.io/adventure/). It parses MiniMessage strings at
compile time into a flattened component tree, so rendering at runtime is orders of
magnitude faster than calling `MiniMessage.deserialize` on every use.

It is a **layer on top of Adventure's API** — just like MiniMessage — not a replacement
for it. Adventure remains the underlying model (`Component`, `Style`, `TextColor`, …).

## Features

- Expressive equivalence with MiniMessage (verified by a matrix of ~70 equivalence tests).
- Compile-time style flattening and adjacent-literal merging (zero-wrapper render path).
- Cached renders for static trees; bounded work per render.
- All standard MiniMessage tags: colors, decorations, click/hover/insert/shadow, gradients
  (including nested), rainbow, pride, keybinds, translatables, sprites, heads, newlines,
  font, transition, selector, score, nbt, reset, and unknown-tag fallbacks.
- Unified runtime `TagResolver` architecture: seamless handling of unclosed tags (`<prefix> <player>`),
  dynamic placeholders (`TagResolver.inserting`), modifiers (`TagResolver.modifying`), and argument-based tags.
- Optional bridge for MiniMessage `Placeholder` and `TagResolver` instances, with an
  optional delegation mode that uses a `MiniMessage` instance for argument-based and
  `Modifying` tags.
- Optional Paper module with dialog, item hover and PlaceholderAPI tags.
- Java 21+ core, no new runtime dependencies beyond Adventure.

## Modules

| Module | Artifact | Requires |
|---|---|---|
| `core` | `io.github.neoramm:fastminimessage` | Java 21+, Adventure 5.2.0+ |
| `paper` | `io.github.neoramm:fastminimessage-paper` | Java 25+, Paper 26.2+ |

The Paper module is optional. It adds tags that need the Paper API (dialogs, item
stacks) and an optional PlaceholderAPI tag; the core module remains platform
independent.

## Installation (Maven)

```xml
<dependency>
    <groupId>io.github.neoramm</groupId>
    <artifactId>fastminimessage</artifactId>
    <version>{version}</version>
</dependency>
```

On Paper 26.2+, add the Paper module next to the core artifact:

```xml
<dependency>
    <groupId>io.github.neoramm</groupId>
    <artifactId>fastminimessage-paper</artifactId>
    <version>{version}</version>
</dependency>
```

Gradle (Kotlin DSL):

```kotlin
implementation("io.github.neoramm:fastminimessage:<version>")
implementation("io.github.neoramm:fastminimessage-paper:<version>") // Paper 26.2+
```

## Comparison

| Feature / Metric | FastMiniMessage (`PreComponentTree`) | Standard MiniMessage (`deserialize`) | MiniMessage + `TextReplacementConfig` |
|---|---|---|---|
| **Core Architecture** | Compiles string once into a flattened AST; renders directly via fast-path | Re-tokenizes, parses, and builds the full component tree on every render | Deserializes once, then recursively traverses and rebuilds the component tree |
| **Static Render Latency** | **~0.89 ns** (cached static reference) | **~10 236 ns** (10+ µs per call) | **~10 236 ns** (initial) + 0 ns (if cached Component without replacement) |
| **Dynamic Render Latency** | **~2 294 – 2 897 ns** (~2.3 – 2.9 µs with placeholders) | **~10 236 ns – 744 418 ns** (scales with template size) | **~15 000 ns – 120 000 ns** (recursive tree traversal & regex matching) |
| **Garbage Collection (GC)** | **0 B (Zero GC)** on static renders; minimal on dynamic | **14.0 KB – 805.6 KB** allocated per render | High GC; clones `Component` and `TextComponent` builders for every node match |
| **Static Tree Optimization** | Built-in `isStatic()` detection and automatic zero-allocation caching | None; full parsing is re-executed every single time | Manual; developer must manage cached `Component` instances |
| **Style Flattening** | Flattens nested wrappers and merges adjacent identical styles at compile time | Retains deeply nested component hierarchy and redundant style wrappers | No flattening; replacements split text components into more fragments |
| **Dynamic & Argument Tags** | Native support (`<tag:arg1:arg2>`), unclosed tags, and modifiers evaluated on render | Requires re-parsing the entire template with `TagResolver` | Not supported; limited to plain string or regex replacements |
| **Viewer Context (`Pointered`)** | First-class (`tree.render(player)` auto-resolves `<player>`, PAPI, dialogs) | Must pass context to deserializer on every invocation | None; developer must extract player properties manually beforehand |

## Example usage

### 1. Precompiling messages (`Messages.java`)

Compile templates once when loading classes or configurations. The compiled `PreComponentTree` is thread-safe and can be reused indefinitely across all players:

```java
package com.example.plugin;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.ResolverRegistry;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import org.jetbrains.annotations.NotNull;

public final class Messages {

    private static final @NotNull MiniMessageCompiler COMPILER =
        new MiniMessageCompiler(ResolverRegistry.withDefaults());

    public static final @NotNull PreComponentTree WELCOME =
        COMPILER.compile("<green>Welcome <gold><player></gold> to the server!</green>");

    private Messages() {
    }
}
```

### 2. Rendering in event listeners (`PlayerJoinListener.java`)

When the player joins, render the precompiled tree directly for the player. The `<player>` placeholder is resolved dynamically from the player's identity without re-parsing the MiniMessage format:

```java
package com.example.plugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(final @NotNull PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        player.sendMessage(Messages.WELCOME.render(player));
    }
}
```

## Quick start

```java
MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());

// Compile once, render many times.
PreComponentTree tree = compiler.compile("<red>Hello <bold>world</bold></red>");
Component rendered = tree.render(player);   // Pointered context

// Unified runtime TagResolvers (convenient unclosed tags in templates)
PreComponentTree chat = compiler.compile("<prefix> <player>: <message>");
Component message = chat.render(player,
    TagResolver.resolver("prefix", Component.text("[Admin]", NamedTextColor.RED)),
    TagResolver.resolver("player", player.getName()),
    TagResolver.resolver("message", "Hello everyone!")
);

// Dynamic tags with arguments
PreComponentTree badges = compiler.compile("<badge:vip:gold> <player> joined");
Component badged = badges.render(player,
    TagResolver.dynamic("badge", (args, ctx) ->
        Component.text("[" + args.getFirst().toUpperCase() + "] ", NamedTextColor.GOLD)),
    TagResolver.resolver("player", "Alice")
);

// Runtime modifiers (can wrap unclosed blocks or explicitly closed blocks)
PreComponentTree announcement = compiler.compile("<shout>alert: <player> connected");
Component shouted = announcement.render(player,
    TagResolver.modifying("shout", ComponentModifiers.uppercase()),
    TagResolver.resolver("player", "Bob")
); // "ALERT: BOB CONNECTED"
```

## Unified runtime tag resolvers

FastMiniMessage provides a unified `TagResolver` interface that combines:
1. **Inserting tags** (`TagResolver.inserting`, `TagResolver.resolver`, `Placeholder`): Inserts a string or component.
2. **Modifying tags** (`TagResolver.modifying`, `ModifierResolver`, `ComponentModifier`): Transforms wrapped or following child components at render time.
3. **Dynamic argument tags** (`TagResolver.dynamic`): Reads arguments at render time (`<tag:arg1:arg2>`) and returns a component based on arguments and viewer context.

### Real-world unclosed tags

In real Minecraft server templates, users frequently omit closing tags for convenience:
`<prefix> Welcome <player>!` or `<shout>alerta para todos`.

FastMiniMessage compiles templates into an optimized precomponent tree. When an unclosed tag is encountered:
- If resolved as an **inserting tag** (placeholder), it renders its value and seamlessly forwards all following text and siblings into the output without requiring `<player/>` or `</player>`.
- If resolved as a **modifying tag**, it applies its transformation to the scope.
- Enclosing styles (colors, decorations) naturally propagate to unclosed tags and following text, matching MiniMessage semantics.

## MiniMessage placeholders and tag resolvers

`MiniMessageTags` adapts Adventure's `Placeholder` factories and `TagResolver`
instances to the same render-time resolution used by the library. There are two
modes, and they differ in what happens to tags that the fast path cannot evaluate.

### Fast path: `MiniMessageTags.of(resolvers...)`

```java
import io.github.neoramm.fastminimessage.compat.MiniMessageTags;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

PreComponentTree tree = compiler.compile("<p> <name>");
Component component = tree.render(player, MiniMessageTags.of(
    Placeholder.unparsed("p", player.getName()),
    Placeholder.unparsed("name", "Hector")
));
```

This mode never calls MiniMessage. It handles:

- every `Placeholder` factory: `unparsed`, `component`, `parsed` and `styling`;
- custom `TagResolver.Single` and `TagResolver.WithoutArguments` resolvers;
- composed resolvers such as `TagResolver.resolver(a, b)` or
  `TagResolver.builder().tag(...).build()` when MiniMessage represents them as a map.

Tags that cannot be evaluated through the public tag API — argument-based
resolvers and `Modifying` tags — keep the **literal fallback**: the tag is
rendered exactly as written, so `<upper:hello>` stays visible in the output.

### Delegation: `MiniMessageTags.of(miniMessage, resolvers...)`

```java
MiniMessage miniMessage = MiniMessage.miniMessage();

Component component = tree.render(player, MiniMessageTags.of(miniMessage,
    Placeholder.unparsed("name", player.getName()),
    TagResolver.resolver("upper", (args, context) ->
        Tag.inserting(Component.text(args.pop().value().toUpperCase()))),
    TagResolver.resolver("tint", (Modifying) (current, depth) ->
        current.color(NamedTextColor.AQUA))
));
```

This mode still resolves placeholders and fixed tags on the fast path. Only the
unsupported tags from the resolver array are reconstructed as a small fragment,
rendered by MiniMessage with the same target and resolvers, and inserted back into
the precompiled tree. The instance supplies the parser and its configured tag set;
the resolver array determines which tags are recognized. Tags that are not in the
array are not delegated.

`ParserDirective` tags always keep the literal fallback, even with an instance,
because they change parser state outside the reconstructed fragment.

### Standalone (Experimental): `ExperimentalMiniMessageTags.of(resolvers...)`

```java
import io.github.neoramm.fastminimessage.compat.ExperimentalMiniMessageTags;

Component component = tree.render(player, ExperimentalMiniMessageTags.of(
    Placeholder.unparsed("name", player.getName()),
    TagResolver.resolver("upper", (args, context) ->
        Tag.inserting(Component.text(args.pop().value().toUpperCase()))),
    TagResolver.resolver("tint", (Modifying) (current, depth) ->
        current.color(NamedTextColor.AQUA))
));
```

This standalone adapter resolves argument-based resolvers and `Modifying` tags directly in memory
without requiring a `MiniMessage` instance. By resolving Kyori's internal `ContextImpl` and
`ArgumentQueueImpl` via `MethodHandle`, it eliminates string fragment reconstruction and avoids
invoking `MiniMessage.deserialize` altogether (~11x faster than delegation).

### Semantics

- Native tags always take priority over bridged resolvers, so a bridged resolver
  cannot override a built-in tag. Use placeholder keys that do not collide.
- Tag names are matched case-insensitively, as MiniMessage does.
- `Tag.selfClosingInserting` tags keep MiniMessage's literal closing-tag behavior
  when used with children.
- A `Placeholder.parsed` value is compiled once (bounded cache) and inserted as a
  component, so a value that opens a tag and closes it outside the placeholder
  boundary cannot affect the surrounding precompiled input.
- `Placeholder` values are fixed at the time the resolver is built; for per-player
  or otherwise dynamic values, build the resolver from the current value or use
  the library's own runtime placeholders (`Placeholder.ofUnparsed`, etc.).

### Cost

The difference between the modes is measurable (raw data in [`benchmark/tag-resolver.json`](benchmark/tag-resolver.json)):

| Benchmark (JDK 21, `-wi 10 -i 20 -f 3`) | ns/op | Allocation | Notes |
|---|---|---|---|
| `fastRenderPlaceholders` (fast path, no instance) | 2 897 | 6.1 KB | Baseline fast path |
| `fastRenderPlaceholdersWithInstance` (instance, nothing delegated) | 3 144 | 6.3 KB | Negligible instance overhead |
| `fastRenderPlaceholdersExperimental` (standalone experimental) | 3 716 | 8.1 KB | Direct in-memory dispatch |
| `fastRenderNativePlaceholders` (native TagResolver) | 2 294 | 4.1 KB | Lowest overhead |
| `fastRenderMixedLiteralFallback` (no instance, literal fallback) | 8 124 | 13.9 KB | Unsupported tags left unparsed |
| `fastRenderMixedExperimental` (standalone, arguments + modifying) | 7 466 | 14.9 KB | **~11.1x faster** than delegation |
| `fastRenderMixedWithDelegation` (instance, delegated tags) | 82 927 | 157.5 KB | Reconstructs fragments + deserializes |
| `miniMessageMixed` (plain MiniMessage) | 80 509 | 136.9 KB | Traditional MiniMessage baseline |
| `fastCompileAndRenderMixedExperimental` (compile + render) | 23 031 | 58.0 KB | **~4.3x faster** than delegation |
| `fastCompileAndRenderMixedWithDelegation` (compile + render) | 99 632 | 199.8 KB | Delegation compile + render |

Supplying an instance costs nothing when no tag is delegated. Delegating tags via `MiniMessageTags.of(miniMessage, ...)`
costs roughly **5–7 µs and ~10 KB per delegated tag**, because it triggers a full `MiniMessage.deserialize` on a
reconstructed fragment. The experimental standalone bridge (`ExperimentalMiniMessageTags.of(...)`) avoids this cost
by resolving arguments and transformations directly in memory, dropping mixed render times from **82.9 µs (157.5 KB)**
down to **7.5 µs (14.9 KB)**.

The bridge is optional: `adventure-text-minimessage` is only a compile-time
dependency of the core module, so consumers using the bridge must have it on their
runtime classpath.

## Paper module

`fastminimessage-paper` adds tags that need Paper API objects. It targets Paper
26.2+ (Adventure 5.2.0, Java 25) and is compiled against the Paper API and
PlaceholderAPI; both are `compileOnly`, so the server or plugin must provide them.

```java
import io.github.neoramm.fastminimessage.paper.PaperTags;

MiniMessageCompiler compiler = PaperTags.compiler();

PreComponentTree tree = compiler.compile("""
    <click:show_dialog:'minecraft:server_links'>Server links</click>
    <show_dialog:'Welcome':'Choose an option':'OK'>Inline dialog</show_dialog>
    <item_hover:'minecraft:diamond':3>A diamond</item_hover>
    Hello <papi:'%player_name%'>
    """);
```

| Tag | Arguments | Behavior |
|---|---|---|
| `<click:show_dialog:'key'>…</click>` | a key from the server dialog registry | the click opens that dialog |
| `<show_dialog:'title':'body':'button'>…</show_dialog>` | MiniMessage text; `body` and `button` are optional | opens a notice dialog built with `Dialog.create` |
| `<item_hover:'reference':count>…</item_hover>` | base64 item bytes or a material name, optional count | shows the item, including its data components |
| `<papi:'expression'>` | a PlaceholderAPI expression | resolves the value at render time |

`PaperTags.compiler()` installs the Paper tags and the registry-backed dialog
resolver. If you build the compiler yourself, use `PaperTags.withDefaults()` for
the registry and then `compiler.dialogResolver(PaperDialogs::registryDialog)`; the
Paper tags themselves are registered by `PaperTags.apply(registry, options)`.

### Behavior and caveats

- Registry dialogs are resolved **at compile time**. Recompile the tree if the
  dialog registry changes after a datapack reload.
- Inline dialog title, body and button are MiniMessage strings rendered at compile
  time. Use a registry dialog when the contents must change per render.
- `item_hover` accepts the output of
  `Base64.getEncoder().encodeToString(item.serializeAsBytes())` for a full item
  stack, or a material name such as `minecraft:diamond` with an optional count.
  Material references keep the requested count; byte references keep the count
  stored in the item.
- `<papi>` is the only Paper tag with a per-render cost: it resolves the expression
  for the rendering `Pointered` when it is a `Player`, and otherwise for the server
  context. If PlaceholderAPI is not installed, the expression is rendered as-is, so
  the message degrades to the raw placeholder text.
- Invalid dialog keys and item references keep the literal fallback (the tag is
  rendered as written), consistent with unknown tags.
- `PaperTagOptions` exposes the dialog, item and PlaceholderAPI factories so the
  tags can be compiled outside a running server or tested with fakes.

All Paper tags except `<papi>` are resolved during compilation, so the cached
render path is unchanged. `<papi>` renders with the same bounded cost as any other
dynamic node.

## Performance

Benchmarks run with JMH (`-wi 10 -i 20 -f 3 -w 3s -r 3s`, GC profiler). The render path is the hot path;
compilation happens once and is cached. Complete raw data is available in [`benchmark/results-intense.json`](benchmark/results-intense.json).

### Simple, common message

Input: `<green>[Server]</green> <white>Welcome!</white> <yellow>Type <bold>/help</bold> for commands.</yellow>`

| Benchmark | ns/op | Allocation | vs MiniMessage |
|---|---|---|---|
| MiniMessage.deserialize | 10 236 | 14.0 KB | baseline |
| FastMiniMessage render (cached) | 0.89 | 0 B | **~11 500x faster** |
| FastMiniMessage compile + render | 2 297 | 5.4 KB | **~4.5x faster** |

### Complex, long input (Lorem ipsum + nested tags, gradients, hover, click, malformed input)

Input: a long Lorem-ipsum string with deep nesting, all tags, emoji/surrogates and common
malformed constructs, repeated 3x.

| Benchmark | ns/op | Allocation | vs MiniMessage |
|---|---|---|---|
| MiniMessage.deserialize | 744 418 | 805.6 KB | baseline |
| FastMiniMessage render (cached) | 5 258 | 8.9 KB | **~142x faster** |
| FastMiniMessage compile + render | 169 276 | 277.3 KB | **~4.4x faster** |

Run the benchmarks yourself with the intense profile
(`-wi 10 -i 20 -f 3 -w 3s -r 3s`):

```bash
./gradlew :core:jmhIntense -PjmhArgs="MiniMessageRenderBenchmark"
./gradlew :core:jmhIntense -PjmhArgs="MiniMessageTagsBenchmark"
```

The full intense profile benchmark results are stored in [`benchmark/results-intense.json`](benchmark/results-intense.json), and the tag resolver bridge results are in [`benchmark/tag-resolver.json`](benchmark/tag-resolver.json).

`MiniMessageTagsBenchmark` covers the `TagResolver` bridge: fixed placeholders on the
fast path, the same path with a `MiniMessage` instance configured, the experimental standalone bridge,
and mixed messages where argument-based resolvers and `Modifying` tags are delegated to MiniMessage.
`SimpleCommonBenchmark` and `ComplexLongBenchmark` also include TagResolver samples,
including the fixed-argument and literal-closing edge cases.

The benchmark classes use the intense profile in their annotations, so running any of
them through `jmh` or `jmhIntense` uses `-wi 10 -i 20 -f 3 -w 3s -r 3s`.

## Equivalence

FastMiniMessage is verified against MiniMessage on a matrix of ~70 diverse inputs
(colors, decorations, click/hover/insert/shadow, gradients and nested gradients, rainbow,
pride, keybinds, translatables, sprites, heads, newlines, emoji/surrogates, reset,
placeholders, and common malformed input). Expression (per-code-point style) is compared,
not tree shape, since FastMiniMessage deliberately flattens style wrappers.

## Requirements

- Core: Java 21 or higher and Kyori Adventure 5.2.0+ (provided by the consumer).
- Paper module: Java 25+, Paper 26.2+, and optionally PlaceholderAPI 2.12+.

## License

MIT. See [LICENSE](LICENSE).
