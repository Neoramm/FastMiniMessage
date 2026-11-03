# 3. Custom Compile-Time Tags & AST Manipulation

For native placeholders, modifiers and dynamic values with arguments, registering it in `ResolverRegistry` allows `MiniMessageCompiler` to fold constant invocations at compile time and avoid slot lookup overhead at render time.

---

## Defining Custom Tags with `TagSpec`

`TagSpec` is a sealed interface in `io.github.neoramm.fastminimessage.v2.tag` with direct `PlaceholderRule` and `ModifierRule` types, plus the following argument-aware rule types:

### 1. `TagSpec.StyleRule`
Produces a `ValueSource<Style>` that merges into child nodes. If the returned `ValueSource` is constant, the style is flattened directly into child text components at compile time.

```java
import io.github.neoramm.fastminimessage.v2.ResolverRegistry;
import io.github.neoramm.fastminimessage.v2.tag.TagSpec;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import java.util.Set;

ResolverRegistry registry = ResolverRegistry.withDefaults();

// Usage: <cmd:'/spawn'>Teleport to Spawn</cmd>
registry.register(new TagSpec.StyleRule(Set.of("cmd", "command"), (args, context) -> {
    args.require(1);
    return args.map(values -> Style.style()
        .color(NamedTextColor.YELLOW)
        .decoration(TextDecoration.UNDERLINED, true)
        .clickEvent(ClickEvent.runCommand(values.string(0, "")))
        .build()
    );
}));
```

### 2. `TagSpec.LeafRule`
Produces a self-contained `ValueSource<Component>`. Leaf rules do not wrap child text; any following text remains a sibling node.

```java
import io.github.neoramm.fastminimessage.v2.template.ValueSource;
import net.kyori.adventure.text.Component;

// Static or argument-driven leaf: <Mention:'<player>'>
registry.register(new TagSpec.LeafRule(Set.of("mention"), (args, context) -> {
    args.require(1);
    return args.map(values -> Component.text("@" + values.string(0, ""), NamedTextColor.AQUA));
}));

// Viewer-dependent leaf (always evaluated at render time):
registry.register(new TagSpec.LeafRule(Set.of("ping"), (args, context) ->
    new ValueSource.Computed<>(renderCtx -> {
        // Compute from renderCtx.viewer()
        return Component.text("24ms", NamedTextColor.GREEN);
    })
));
```

### 3. `TagSpec.TransformRule`
Produces a `ValueSource<TreeTransformer>` that transforms the rendered child `Component` subtree (used by `<gradient>`, `<rainbow>`, `<pride>`).

```java
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import io.github.neoramm.fastminimessage.v2.template.TreeTransformer;

registry.register(new TagSpec.TransformRule(Set.of("brackets"), (args, context) ->
    args.map(values -> (children, inheritedStyle, renderCtx) -> {
        Component wrapped = Component.text("[")
            .append(children)
            .append(Component.text("]"));
        return RenderScope.applyStyle(wrapped, inheritedStyle);
    })
));
```

For modifiers without arguments, register a `ComponentModifier` on the registry before compiling:
```java
registry.registerModifier("shout", ComponentModifiers.uppercase());
```
Override `ComponentModifier#isStatic()` to return `true` if the modifier does not depend on the `RenderScope` so constant blocks fold at compile time.

---

## Declaring Component Arguments in `TagArgs`

By default, `TagArgs` compiles each argument as a scalar `StringTemplate`. If an argument can contain MiniMessage formatting (like a tooltip or separator), declare it before calling `args.values()` or `args.map(...)`:

```java
registry.register(new TagSpec.LeafRule(Set.of("labeled_value"), (args, context) -> {
    args.require(2);
    args.component(0); // Argument 0 is compiled as a MiniMessage Component subtree
    return args.map(values -> Component.empty()
        .append(values.component(0))
        .append(Component.text(": " + values.string(1, "")))
    );
}));
```

---

## Inspecting and Mapping `PreComponentTree`

`PreComponentTree` wraps an immutable `TemplateNode` hierarchy (`Static`, `DynamicLeaf`, `DynamicTransform`, `Slot`):

```java
import io.github.neoramm.fastminimessage.v2.template.TemplateNode;

// 1. Inspect nodes without mutating the tree
tree.inspect(node -> {
    if (node instanceof TemplateNode.Slot slot) {
        System.out.println("Dynamic placeholder key: " + slot.key());
    }
});

// 2. Transform nodes immutably (returns a new PreComponentTree)
PreComponentTree rewritten = tree.map(node -> {
    if (node instanceof TemplateNode.Static fixed) {
        return new TemplateNode.Static(fixed.component().colorIfAbsent(NamedTextColor.GRAY));
    }
    return node;
});
```
