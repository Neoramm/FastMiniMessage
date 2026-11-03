# 2. Runtime Payloads, Placeholders & Modifiers

Register frequently used placeholders before compilation. The compiled template retains their callbacks and accesses
payloads by generated array index, avoiding a runtime resolver search.

```java
import io.github.neoramm.fastminimessage.v2.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.v2.ResolverRegistry;
import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

final var layout = PayloadSchema.builderWithDefaults();
final var name = layout.key("name", Component.class);
final var color = layout.key("color", NamedTextColor.class);
final var registry = ResolverRegistry.withDefaults(layout.build())
    .registerPlaceholder("name", (final var scope) -> scope.payload(name))
    .registerModifier("tint", (final var content, final var scope) -> content.color(scope.payload(color)));
final var compiler = new MiniMessageCompiler(registry);
final var tree = compiler.compile("<tint><name></tint>");
final var rendered = tree.render((final var builder) -> builder
    .payload(name, Component.text("Alex"))
    .payload(color, NamedTextColor.GREEN));
```

Schema builders assign indices and close registration at `build()`. Custom keys are required and cannot be replaced
by keys from another schema, even when their names and indices match. Default keys are shared and optional.
`builderWithDefaults()` reserves three slots for Adventure, runtime resolvers and translation arguments;
`builder()` starts with no reserved slots. The compiler and tree expose `payloadCount()` and `scopeBuilder()`.

## Dynamic tags with arguments

Register a `TagSpec.LeafRule` before compilation for values that depend on both tag arguments and payloads. Prepare
`args.values()` in the compile callback, then evaluate it against the current scope in a `ValueSource.Computed`.

```java
import io.github.neoramm.fastminimessage.v2.tag.TagSpec;
import io.github.neoramm.fastminimessage.v2.template.ValueSource;
import java.util.Set;

registry.register(new TagSpec.LeafRule(Set.of("mention"), (final var args, final var context) -> {
    args.require(1);
    final var values = args.values();
    return new ValueSource.Computed<>((final var scope) ->
        Component.text(values.evaluate(scope).string(0, ""))
            .append(scope.payload(name)));
}));
final var mention = compiler.compile("<mention:'Hello '/>");
final var result = mention.render((final var builder) -> builder
    .payload(name, Component.text("Alex"))
    .payload(color, NamedTextColor.GREEN));
```

Register the rule before compiling the template. The registry and compiler above are the instances from the first example.
The callback receives arguments starting at index zero, without the tag name. Constant arguments do not make a
payload-dependent callback constant: use `ValueSource.Computed` whenever the result reads the scope. For a result that
depends only on arguments, `args.map(...)` preserves compile-time folding of constant inputs.

## Arguments and reuse

Payload-backed placeholders work inside scalar arguments (`<click:run_command:'/msg <name>'>`) and component arguments
(`<hover:show_text:'<tooltip/>'>`). Scalar substitutions cannot escape the argument into new markup. Use self-closing
placeholders when a component argument must retain its complete styled Component.

`Arguments` supports indexed access, defaults, numeric conversion and cursor iteration. Each call to `cursor()` (or
`iterator()`) returns an independent cursor, so `Arguments` instances may be shared safely across renders.

For multiple renders with identical inputs, build a scope once:

```java
final var scope = tree.scopeBuilder()
    .payload(name, Component.text("Alex"))
    .payload(color, NamedTextColor.GREEN)
    .build();
final var first = tree.render(scope);
final var second = tree.render(scope);
```

A scope builder transfers ownership of its payload array and cannot be reused. `render(scope)` does not copy that array
or create another context. `render(consumer)` creates a fresh builder and scope; component construction and callbacks
may allocate too. Builders are confined to one thread. Finished scopes are shareable only when their payloads and
callbacks are safe to share. Custom schema templates require scopes built from that same schema.

## Legacy runtime compatibility

`RuntimeTagBridge`, `TagResolver` and the `RUNTIME_TAGS_KEY` payload remain available for legacy integrations that cannot
register their tag behavior before compilation. They perform runtime resolver lookup and are not the recommended native
placeholder API. Use `registerPlaceholder`, `registerModifier` and `TagSpec` for new code. Use `AdventureTags` only when
adapting Adventure resolvers supplied by another API; see the Adventure bridge guide.
