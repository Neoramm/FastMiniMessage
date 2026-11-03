[![Maven Central](https://img.shields.io/maven-central/v/io.github.neoramm/fastminimessage.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.neoramm/fastminimessage)

# FastMiniMessage

FastMiniMessage is a high-performance, precompiled MiniMessage implementation for [Kyori Adventure](https://docs.papermc.io/adventure/). It compiles MiniMessage strings once into an immutable, style-flattened `PreComponentTree` (`TemplateNode`), making runtime rendering orders of magnitude faster than calling `MiniMessage.deserialize` on every use.

## Features

- **Expressive Equivalence**: Verified against MiniMessage across ~70 diverse test cases (colors, decorations, click/hover/insert/shadow, nested gradients, rainbow, pride, translatables, sprites, heads, selectors, score, NBT, legacy `&` codes, and fallbacks).
- **Compile-Time Folding**: Flattens nested styles, merges adjacent literals, and pre-evaluates static subtrees into a single `TemplateNode.Static` with zero-allocation rendering.
- **Typed Runtime Payloads (`PayloadSchema`)**: Supports inserting placeholders, dynamic argument tags (`<tag:arg1:arg2>`), modifying decorators (`ComponentModifier`), and unclosed tags (`<prefix> <player>`).
- **Dynamic Tag Arguments**: Interpolates runtime placeholders inside tag arguments (`<click:run_command:'/<cmd>'>`, `<gradient:'<c1>':'<c2>'>`, `<hover:show_text:'<msg>'>`) without re-scanning at render time.
- **Adventure Bridge & Localization**: Optional zero-deserialize bridge (`AdventureTags`) for standard Adventure `TagResolver`s and precompiled `FastMiniMessageTranslationStore` for `GlobalTranslator`.
- **Paper Module**: Built-in support for registry dialogs, inline notice dialogs (`<show_dialog>`), item stack hovers (`<item_hover>`), and PlaceholderAPI (`<papi>`).

## Modules & Installation

| Module | Artifact | Requirements |
|--------|----------|--------------|
| `core` | `io.github.neoramm:fastminimessage` | Java 21+, Adventure 5.2.0+ |
| `paper` | `io.github.neoramm:fastminimessage-paper` | Java 25+, Paper 26.2+ |

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.neoramm:fastminimessage:<version>")
    implementation("io.github.neoramm:fastminimessage-paper:<version>") // Optional: Paper 26.2+
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.neoramm</groupId>
    <artifactId>fastminimessage</artifactId>
    <version>{version}</version>
</dependency>
```

## Quick Start

```java
import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.v2.ResolverRegistry;
import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import net.kyori.adventure.text.Component;

final var layout = PayloadSchema.builder();
final var name = layout.key("name", Component.class);
final var registry = ResolverRegistry.withDefaults(layout.build())
    .registerPlaceholder("name", (final var scope) -> scope.payload(name));
final var template = new MiniMessageCompiler(registry).compile("Hello <name>!");
final var rendered = template.render((final var builder) -> builder.payload(name, Component.text("Hector")));
```

## Registered Placeholders and Typed Payloads

This is the recommended native API. Register behavior before compilation and supply only payload values at render time.
For dynamic tags with arguments, register a `TagSpec.LeafRule`; see the runtime payload guide.

Create payload keys through one schema builder, finish the schema, then pass it to the registry. Indices are assigned
consecutively and key constructors are private. The compiler exposes `payloadCount()` and `scopeBuilder()` using that
fixed schema. Registered placeholder and modifier functions are captured during compilation.

```java
import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.v2.ResolverRegistry;
import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

final var layout = PayloadSchema.builder();
final var name = layout.key("name", Component.class);
final var message = layout.key("message", Component.class);
final var tint = layout.key("tint", NamedTextColor.class);
final var registry = ResolverRegistry.withDefaults(layout.build())
    .registerPlaceholder("name", (final var scope) -> scope.payload(name))
    .registerPlaceholder("message", (final var scope) -> scope.payload(message))
    .registerModifier("tint", (final var component, final var scope) -> component.color(scope.payload(tint)));
final var compiler = new MiniMessageCompiler(registry);
final var template = compiler.compile("<name>: <tint><message></tint>");

final var scope = compiler.scopeBuilder()
    .payload(name, Component.text("Hector"))
    .payload(message, Component.text("Hello!"))
    .payload(tint, NamedTextColor.GREEN)
    .build();
final var rendered = template.render(scope);
```

Use `.viewer(pointered)` on the scope builder when callbacks need a viewer. Every custom payload must be assigned a
non-null value of its declared type. Keys from another schema are rejected. The scope builder transfers its array to the
finished scope and cannot be reused after `build()`. Calling `build()` with missing payloads fails without consuming it.
Builders must be confined to one thread. Finished scopes can be shared when their payload objects and callbacks are safe
to share.

`render(scope)` reuses the scope directly, without allocating a separate context or copying payloads. Reusing a
scope is appropriate while its inputs remain valid. Creating a new builder and scope for each event still allocates;
callbacks and Adventure component construction can also allocate. Mutable payload objects remain caller-owned and must
follow their own threading rules. Replacing a registry entry affects future compilations, while existing templates retain
their captured functions. Use `render(consumer)` to configure a fresh scope for one call, or `render(scope)` to reuse a finished scope.

### Reusable Mutable Scope (`RenderScope.Mutable`)

For high-throughput loops (e.g. broadcasting personalized messages to many players), allocate a mutable scope once and update its values in-place with **zero GC allocations**:

```java
final var mutableScope = template.mutableScope();
for (final Player player : players) {
    mutableScope.viewer(player)
        .payload(name, player.displayName())
        .payload(coins, player.getCoins());
    player.sendMessage(template.render(mutableScope));
}
```

A `RenderScope.Mutable` is single-threaded and allows in-place mutation of `.viewer(pointered)` and `.payload(key, value)`. You can also snapshot it with `.toImmutable()` or convert any existing scope to mutable via `.toMutable()`.

## Default Payload Schema

`PayloadSchema.withDefaults()` contains three optional, typed keys: `ADVENTURE_TAGS_KEY` for `AdventureTags`,
`RUNTIME_TAGS_KEY` for a `RuntimeTagBridge`, and `TRANSLATION_ARGUMENTS_KEY` for indexed `TranslationPayload` arguments.
`ResolverRegistry.withDefaults()` uses this schema. Unset default keys have no resolver; `payloadOrNull(key)` returns
null for an unset key. `payload(key)` requires a value.

Use `PayloadSchema.builderWithDefaults()` to add required custom keys while retaining the shared default keys.
`PayloadSchema.builder()` creates a custom-only layout. `payloadCount()` counts all slots, including optional defaults.
Keys are created only by schema builders, and cannot be forged from arbitrary indices.

Legacy runtime resolver adapters can be combined once with `TagResolver.combine(...)` and supplied through
`builder.payload(RUNTIME_TAGS_KEY, resolvers)`. Registered callbacks receive the complete `RenderScope`, including
`scope.viewer()`. Translation arguments carry payloads with `Argument.payload(key, value)`.

This API replaces the old viewer/binding render overloads. `render(consumer)` creates a builder and scope; captured
lambdas may also allocate. For the lowest allocation overhead, prepare a scope once and call `render(scope)`.

## Benchmarks

JMH (`v1.37`) results on OpenJDK 21.0.12.1 (`-wi 5 -i 18 -f 2 -w 1s -r 1s`, `avgt` mode, GC profiler) comparing FastMiniMessage v2 against Adventure's `MiniMessage.deserialize` baseline and FastMiniMessage v1.

### 1. Simple, Common Message (`SimpleCommonBenchmark`)

Input: `<green>[Server]</green> <white>Welcome!</white> <yellow>Type <bold>/help</bold> for commands.</yellow>` alongside unclosed chat formats (`<prefix> <name><colon> <message>`) and dynamic placeholders.

| Benchmark Method | Description | Score (ns/op) | Allocation (B/op) | Speedup vs MiniMessage |
|------------------|-------------|--------------:|------------------:|-----------------------:|
| `miniMessageDeserialize` | `MiniMessage.deserialize` (baseline) | 10,516.45 ± 439.89 | 14,056 B (~13.7 KB) | 1.0x (baseline) |
| `miniPreCompilerRender` (v2) | Precompiled static render (cached) | **0.95 ± 0.03** | **0 B** | **~11,069.9x faster** |
| `v1Render` (v1) | Precompiled static render (1.x) | 1.54 ± 0.02 | 0 B | ~6,828.9x faster |
| `fastRenderUnclosedChat` (v2) | Unclosed chat tags (`<prefix> <name><colon> <message>`) | **175.17 ± 4.55** | **376 B** | **~60.0x faster** |
| `v1RenderUnclosedChat` (v1) | Unclosed chat tags (1.x) | 161.24 ± 4.25 | 320 B | ~65.2x faster |
| `fastRenderNativeTags` (v2) | Dynamic tags / placeholders (`<name>`, `<rank>`, `<upper>`, `<tint>`) | **758.37 ± 31.82** | **1,376 B (~1.3 KB)** | **~45.0x faster** |
| `v1RenderNativeTags` (v1) | Dynamic tags / placeholders (1.x) | 1,594.27 ± 49.26 | 1,824 B (~1.8 KB) | ~21.4x faster |
| `fastRenderTagsAll` (v2) | All tag types (custom + standard) | **642.90 ± 1.98** | **1,376 B (~1.3 KB)** | **~53.0x faster** |
| `fastRenderTagsPlaceholders` (v2) | Placeholders only | **960.20 ± 19.34** | **1,552 B (~1.5 KB)** | **~35.5x faster** |
| `miniMessageTagsDeserialize` | `MiniMessage.deserialize` with tags (baseline) | 34,099.51 ± 919.49 | 55,832 B (~54.5 KB) | 1.0x (baseline) |
| `miniPreCompilerCompileAndRender` (v2) | Cold compile + render | **2,797.74 ± 72.65** | **5,640 B (~5.5 KB)** | **~3.8x faster** |
| `v1CompileAndRender` (v1) | Cold compile + render (1.x) | 2,413.65 ± 46.35 | 5,640 B (~5.5 KB) | ~4.4x faster |

### 2. Complex, Long Message (`ComplexLongBenchmark`)

Input: A large multi-tag stress payload (nested style tags, color overrides, click commands, hover hints with nested styles, gradients, rainbow, shadow, keybinds, translations with args, item hovers, score, selector, NBT, pride, insert, and malformed tags repeated 3x).

| Benchmark Method | Description | Score (ns/op) | Allocation (B/op) | Speedup vs MiniMessage |
|------------------|-------------|--------------:|------------------:|-----------------------:|
| `miniMessageDeserialize` | `MiniMessage.deserialize` (baseline) | 774,322.10 ± 21,362.46 | 807,073 B (~788.2 KB) | 1.0x (baseline) |
| `miniPreCompilerRender` (v2) | Precompiled static render (cached) | **1,518.45 ± 19.50** | **2,360 B (~2.3 KB)** | **~509.9x faster** |
| `v1Render` (v1) | Precompiled static render (1.x) | 5,538.80 ± 218.21 | 8,856 B (~8.6 KB) | ~139.8x faster |
| `fastRenderNativeTags` (v2) | Dynamic tags / placeholders (v2) | **5,491.14 ± 115.60** | **8,920 B (~8.7 KB)** | **~48.6x faster** |
| `v1RenderNativeTags` (v1) | Dynamic tags / placeholders (1.x) | 8,729.01 ± 16.92 | 11,272 B (~11.0 KB) | ~30.6x faster |
| `fastRenderTagsAll` (v2) | All tag types (custom + standard) | **5,820.75 ± 261.78** | **8,920 B (~8.7 KB)** | **~45.9x faster** |
| `fastRenderTagsPlaceholders` (v2) | Placeholders only | **6,503.10 ± 89.98** | **10,648 B (~10.4 KB)** | **~41.1x faster** |
| `miniMessageTagsDeserialize` | `MiniMessage.deserialize` with tags (baseline) | 267,028.56 ± 2,069.34 | 332,946 B (~325.1 KB) | 1.0x (baseline) |
| `miniPreCompilerCompileAndRender` (v2) | Cold compile + render | **316,671.10 ± 4,505.69** | **441,220 B (~430.9 KB)** | **~2.45x faster** |
| `v1CompileAndRender` (v1) | Cold compile + render (1.x) | 181,200.43 ± 3,648.66 | 288,481 B (~281.7 KB) | ~4.27x faster |

### 3. Adventure `TagResolver` Bridge (`AdventureTagsBenchmark`)

Evaluates adapting standard Adventure `TagResolver` instances via the `AdventureTags` zero-deserialize bridge against native MiniMessage and FastMiniMessage v1.

| Benchmark Method | Description | Score (ns/op) | Allocation (B/op) | Speedup vs MiniMessage |
|------------------|-------------|--------------:|------------------:|-----------------------:|
| `miniMessagePlaceholders` | `MiniMessage.deserialize` with placeholders | 38,900.19 ± 574.13 | 72,184 B (~70.5 KB) | 1.0x (baseline) |
| `fastRenderPlaceholders` (v2) | FastMiniMessage `AdventureTags` bridge | **1,414.31 ± 99.20** | **2,664 B (~2.6 KB)** | **~27.5x faster** |
| `fastRenderNativePlaceholders` (v2) | FastMiniMessage native placeholders | **1,450.32 ± 52.72** | **2,664 B (~2.6 KB)** | **~26.8x faster** |
| `v1RenderPlaceholders` (v1) | FastMiniMessage placeholders (1.x) | 3,226.04 ± 23.81 | 6,256 B (~6.1 KB) | ~12.1x faster |
| `miniMessageMixed` | `MiniMessage.deserialize` mixed tags | 85,358.17 ± 1,531.90 | 140,665 B (~137.4 KB) | 1.0x (baseline) |
| `fastRenderMixed` (v2) | FastMiniMessage `AdventureTags` bridge (mixed) | **2,312.35 ± 45.67** | **4,008 B (~3.9 KB)** | **~36.9x faster** |
| `fastRenderNativeMixed` (v2) | FastMiniMessage native mixed tags | **2,251.02 ± 62.39** | **4,008 B (~3.9 KB)** | **~37.9x faster** |
| `v1RenderMixed` (v1) | FastMiniMessage mixed tags (1.x) | 8,790.67 ± 103.27 | 14,576 B (~14.2 KB) | ~9.7x faster |
| `miniMessageEdgeCases` | `MiniMessage.deserialize` edge cases | 347,075.67 ± 962,168.36 | 54,070 B (~52.8 KB) | 1.0x (baseline) |
| `fastRenderEdgeCases` (v2) | FastMiniMessage edge cases | **1,229.78 ± 27.90** | **2,160 B (~2.1 KB)** | **~282.2x faster** |
| `v1RenderEdgeCases` (v1) | FastMiniMessage edge cases (1.x) | 3,846.82 ± 84.66 | 6,304 B (~6.2 KB) | ~90.2x faster |

Run the benchmark suite locally:

```bash
./gradlew :core:jmhIntense
# Or run specific benchmark classes:
./gradlew :core:jmhIntense -PjmhArgs="SimpleCommonBenchmark"
./gradlew :core:jmhIntense -PjmhArgs="ComplexLongBenchmark"
./gradlew :core:jmhIntense -PjmhArgs="AdventureTagsBenchmark"
```

## Developer Guides

Detailed documentation on using and extending FastMiniMessage v2 is available in [`dev-guides/`](dev-guides/):

1. [**Getting Started & Core Concepts**](dev-guides/01-getting-started.md) — Compiler setup, `PreComponentTree`, static folding, viewer context, and legacy `&` formatting.
2. [**Runtime Tags, Placeholders & Modifiers**](dev-guides/02-runtime-tags-and-placeholders.md) — `TagResolver`, `Placeholder`, `RenderScope`, unclosed tags, and dynamic tag arguments.
3. [**Custom Compile-Time Tags & AST Manipulation**](dev-guides/03-custom-compile-tags.md) — `TagSpec` rules (`StyleRule`, `LeafRule`, `TransformRule`), `TagArgs`, `ValueSource`, and tree inspection/mapping.
4. [**Localization (`FastMiniMessageTranslationStore`)**](dev-guides/04-localization.md) — Precompiled translations, `GlobalTranslator`, indexed/named `Argument`s, and `.properties` bundles.
5. [**Adventure `TagResolver` Bridge (`AdventureTags`)**](dev-guides/05-adventure-bridge.md) — Adapting standard MiniMessage `Placeholder` and `TagResolver` instances at render time.
6. [**Paper Integration (`fastminimessage-paper`)**](dev-guides/06-paper-integration.md) — Dialogs (`<show_dialog>`), item hovers (`<item_hover>`), PlaceholderAPI (`<papi>`), and custom `PaperTagOptions`.

## License

MIT. See [LICENSE](LICENSE).
