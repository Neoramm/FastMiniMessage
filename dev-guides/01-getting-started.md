# 1. Getting Started & Core Concepts

FastMiniMessage v2 separates MiniMessage processing into two distinct phases:
1. **Compilation (`MiniMessageCompiler`)**: Parses the input string, resolves built-in tags, flattens inherited styles into leaf nodes, merges adjacent text literals with identical styles, and folds constant subtrees into immutable `TemplateNode.Static` instances.
2. **Rendering (`PreComponentTree.render`)**: Evaluates only the registered dynamic nodes (`RegisteredPlaceholder`, `DynamicLeaf`, `DynamicTransform`) and unresolved compatibility slots against a `RenderScope` containing its viewer and payloads. If the entire tree is static, `render()` returns the pre-built Adventure `Component` reference in ~0.90 ns with zero allocations.

---

## Setting Up the Compiler

Create a `MiniMessageCompiler` backed by `ResolverRegistry.withDefaults()`. Because both the compiler and compiled trees are thread-safe, store them as `static final` constants or in a singleton registry:

```java
package com.example.plugin;

import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.v2.ResolverRegistry;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;

public final class Messages {
    public static final MiniMessageCompiler COMPILER =
        new MiniMessageCompiler(ResolverRegistry.withDefaults());

    public static final PreComponentTree SERVER_MOTD = COMPILER.compile(
        "<gradient:#ff5555:#5555ff><bold>MyServer</bold></gradient> <gray>—</gray> <green>Welcome, <player>!</green>"
    );

    private Messages() {}
}
```

---

## Rendering & Viewer Context (`Pointered`)

`PreComponentTree` provides overloaded `render` methods:

```java

Component staticMsg = Messages.SERVER_MOTD.render();

// 2. Render with a Pointered viewer (e.g., an Adventure Audience or Bukkit Player)
Component playerMsg = Messages.SERVER_MOTD.render((final var builder) -> builder.viewer(player));
```

### Automatic `<player>` / `<name>` Resolution
When a non-null `Pointered` viewer is passed to `tree.render(builder -> builder.viewer(viewer))` and no registered placeholder or compatibility resolver overrides `<player>` or `<name>`, `RenderScope` automatically queries `viewer.get(Identity.NAME)` and renders the viewer's name as a `Component`.

---

## Static Detection & Immutability

You can check whether a compiled template contains any dynamic elements:

```java
PreComponentTree staticTree = compiler.compile("<red><bold>Static Notice</bold></red>");
boolean isStatic = staticTree.isStatic(); // true (root is TemplateNode.Static)


Component sameInstance = staticTree.render((final var builder) -> builder.viewer(player));
```

---

## Legacy Ampersand (`&`) Formatting

FastMiniMessage supports legacy formatting codes natively in two ways:

1. **Direct Scanner Tokenization (`DefaultTagScanner`)**: During `compiler.compile(...)`, legacy codes (`&a`–`&f`, `&0`–`&9`, `&l`, `&o`, `&n`, `&m`, `&k`, `&r`, `&#rrggbb`, and Bungee `&x&r&r&g&g&b&b`) are tokenized directly into the AST without allocating an intermediate string. Use `&&` or `\&` to escape a literal ampersand.
2. **String Conversion Utility (`LegacyFormatParser`)**: If you need to convert legacy strings to MiniMessage syntax before storage or external processing:

```java
import io.github.neoramm.fastminimessage.v2.LegacyFormatParser;

String mm = LegacyFormatParser.toMiniMessage("&#ff0000&lAlert &r&7Check <click:open_url:'https://example.com?a=1&b=2'>link</click>");
// Result: "<reset><#ff0000><bold>Alert <reset><reset><gray>Check <click:open_url:'https://example.com?a=1&b=2'>link</click>"
```
Ampersands inside valid MiniMessage tags (such as URL query strings) are preserved untouched.

## Native dynamic values

For application placeholders, define typed keys with `PayloadSchema.builder()`, pass the finished schema to
`ResolverRegistry.withDefaults(schema)`, and register callbacks before compiling. Use `registerPlaceholder` for values,
`registerModifier` for child transformations, and `TagSpec.LeafRule` for argument-aware values. The next guide contains
complete examples. Render calls supply payload objects, not newly constructed resolver functions.

`render(scope)` reuses an existing scope. `render(consumer)` invokes the consumer and builds a fresh scope even for a
static template. `render()` is the cheapest path for static content. Scopes with custom required keys must supply every
key; payload objects and callbacks must follow their own concurrency rules.
