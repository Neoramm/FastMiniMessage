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
- Java 21+, no new runtime dependencies beyond Adventure.

## Installation (Maven)

```xml
<dependency>
    <groupId>io.github.neoramm</groupId>
    <artifactId>fastminimessage</artifactId>
    <version>{version}</version>
</dependency>
```

Gradle (Kotlin DSL):

```kotlin
implementation("io.github.neoramm:fastminimessage:<version>")
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

## Performance

Benchmarks run with JMH (`-wi 10 -i 15 -f 3`, GC profiler). The render path is the hot path;
compilation happens once and is cached.

### Simple, common message

Input: `<green>[Server]</green> <white>Welcome!</white> <yellow>Type <bold>/help</bold> for commands.</yellow>`

| Benchmark | ns/op | vs MiniMessage |
|---|---|---|
| MiniMessage.deserialize | 10 612 | baseline |
| FastMiniMessage render (cached) | 113 | **~94x faster** |
| FastMiniMessage compile + render | 1 997 | **~5.3x faster** |

### Complex, long input (Lorem ipsum + nested tags, gradients, hover, click, malformed input)

Input: a long Lorem-ipsum string with deep nesting, all tags, emoji/surrogates and common
malformed constructs, repeated 3x.

| Benchmark | ns/op | vs MiniMessage |
|---|---|---|
| MiniMessage.deserialize | 695 979 | baseline |
| FastMiniMessage render (cached) | 5 138 | **~135x faster** |
| FastMiniMessage compile + render | 154 081 | **~4.5x faster** |

Run the benchmarks yourself:

```bash
./gradlew jmh -PjmhArgs="MiniMessageRenderBenchmark -wi 10 -i 20 -f 5 -w 3s -r 3s"
```

## Equivalence

FastMiniMessage is verified against MiniMessage on a matrix of ~70 diverse inputs
(colors, decorations, click/hover/insert/shadow, gradients and nested gradients, rainbow,
pride, keybinds, translatables, sprites, heads, newlines, emoji/surrogates, reset,
placeholders, and common malformed input). Expression (per-code-point style) is compared,
not tree shape, since FastMiniMessage deliberately flattens style wrappers.

## Requirements

- Java 21 or higher.
- Kyori Adventure 4.26+ (provided by the consumer).

## License

MIT. See [LICENSE](LICENSE).
