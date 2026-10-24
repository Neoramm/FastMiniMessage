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
- Dynamic placeholders and runtime `ComponentModifier`s for context-dependent rendering.
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

## Quick start

```java
MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());

// Compile once, render many times.
PreComponentTree tree = compiler.compile("<red>Hello <bold>world</bold></red>");
Component rendered = tree.render(player);   // Pointered context

// Dynamic placeholders.
Component withPlaceholder = tree.render(player, Placeholder.ofUnparsed("p", "X"));

// Placeholders can also appear inside tag arguments.
PreComponentTree colored = compiler.compile("<color:<c>>hello</color>");
Component red = colored.render(player, Placeholder.ofUnparsed("c", "red"));

// Parsed placeholder keeps its own style.
PreComponentTree bolded = compiler.compile("<bold><p></bold>");
Component blue = bolded.render(player, Placeholder.ofParsed("p", Component.text("X", NamedTextColor.BLUE)));

// Runtime modifier.
compiler.registerModifier("shout", ComponentModifiers.uppercase());
Component shouted = compiler.compile("<shout>hello</shout>").render(player); // "HELLO"
```

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

The difference between the two modes is measurable, so choose deliberately:

| Benchmark (JDK 25, `-wi 10 -i 20 -f 3`) | ns/op | Allocation |
|---|---|---|
| `fastRenderPlaceholders` (fast path, no instance) | 3 241 | 7.8 KB |
| `fastRenderPlaceholdersWithInstance` (instance, nothing delegated) | 3 245 | 7.8 KB |
| `fastRenderMixedLiteralFallback` (no instance, literal fallback) | 7 208 | 15.0 KB |
| `fastRenderMixedWithDelegation` (instance, delegated tags) | 79 396 | 162.7 KB |
| `miniMessageMixed` (plain MiniMessage) | 81 488 | 137.2 KB |

Supplying an instance costs nothing when no tag is delegated. Each delegated tag,
however, costs roughly **5–7 µs and ~10 KB**, because it triggers a full
`MiniMessage.deserialize` on a reconstructed fragment. In tag-dense messages that
can erase the benefit of precompilation: the `SimpleCommonBenchmark` tag sample
renders in 2.5 µs on the placeholder-only path, 3.1 µs with the literal fallback,
42.2 µs with delegation and 30.7 µs with plain MiniMessage. Keep delegation for
occasional messages, or keep argument-based and `Modifying` resolvers out of hot
paths.

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
compilation happens once and is cached.

### Simple, common message

Input: `<green>[Server]</green> <white>Welcome!</white> <yellow>Type <bold>/help</bold> for commands.</yellow>`

| Benchmark | ns/op | Allocation | vs MiniMessage |
|---|---|---|---|
| MiniMessage.deserialize | 10 025 | 13.5 KB | baseline |
| FastMiniMessage render (cached) | 96 | 296 B | **~105x faster** |
| FastMiniMessage compile + render | 2 206 | 5.9 KB | **~4.5x faster** |

### Complex, long input (Lorem ipsum + nested tags, gradients, hover, click, malformed input)

Input: a long Lorem-ipsum string with deep nesting, all tags, emoji/surrogates and common
malformed constructs, repeated 3x.

| Benchmark | ns/op | Allocation | vs MiniMessage |
|---|---|---|---|
| MiniMessage.deserialize | 658 349 | 662.7 KB | baseline |
| FastMiniMessage render (cached) | 4 455 | 9.2 KB | **~148x faster** |
| FastMiniMessage compile + render | 165 075 | 279.6 KB | **~4.0x faster** |

Run the benchmarks yourself with the intense profile
(`-wi 10 -i 20 -f 3 -w 3s -r 3s`):

```bash
./gradlew :core:jmhIntense -PjmhArgs="MiniMessageRenderBenchmark"
./gradlew :core:jmhIntense -PjmhArgs="MiniMessageTagsBenchmark"
```

`MiniMessageTagsBenchmark` covers the `TagResolver` bridge: fixed placeholders on the
fast path, the same path with a `MiniMessage` instance configured, and mixed messages
where argument-based resolvers and `Modifying` tags are delegated to MiniMessage.
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
