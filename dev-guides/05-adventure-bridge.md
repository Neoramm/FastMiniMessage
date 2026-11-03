# 5. Adventure TagResolver Compatibility Bridge

For native dynamic values, prefer the registered APIs in guide 2. Use this bridge when an existing integration supplies
Adventure resolvers.

`AdventureTags` adapts Kyori Adventure resolvers without calling `MiniMessage.deserialize`. Supply the adapter through
the optional `PayloadSchema.ADVENTURE_TAGS_KEY` in the default schema.

```java
import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.v2.ResolverRegistry;
import io.github.neoramm.fastminimessage.v2.compat.AdventureTags;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import static io.github.neoramm.fastminimessage.v2.model.PayloadSchema.ADVENTURE_TAGS_KEY;

final var compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
final var tree = compiler.compile("<prefix/> <name/>");
final var adventureTags = AdventureTags.of(
    Placeholder.parsed("prefix", "<gold>[Staff]</gold>"),
    Placeholder.unparsed("name", "Hector"));
final var rendered = tree.render((final var builder) -> builder.payload(ADVENTURE_TAGS_KEY, adventureTags));
```

Use `PayloadSchema.builderWithDefaults()` when combining this bridge with custom payload keys. Add
`.viewer(player)` to the scope builder when resolvers need viewer pointers. Reuse the adapter and finished scope when
their values remain valid. The MiniMessage dependency must be available at runtime when using this adapter.

## Supported constructs

- `Placeholder.unparsed` and `Placeholder.component` use a direct path for single resolvers without arguments.
- Styling placeholders apply their style to children.
- Parsed placeholders are compiled through the active compiler and cached in a bounded cache per adapter. Nested
  placeholders use the current scope.
- Argument-based resolvers, modifying tags and composed resolvers are supported.

## Semantics

Compile-time registered tags take precedence over runtime payload resolvers. Native runtime resolvers take precedence
over Adventure resolvers when both payloads contain the same tag.

Adventure self-closing inserting tags preserve a literal closing tag when the template incorrectly supplies one, matching
MiniMessage. Native `TagResolver.inserting` omits that literal closing tag. Parser directives cannot alter the surrounding
precompiled template and retain their literal fallback.
