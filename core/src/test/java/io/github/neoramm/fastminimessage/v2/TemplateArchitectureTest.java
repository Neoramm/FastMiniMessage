package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.api.ComponentModifier;
import io.github.neoramm.fastminimessage.v2.internal.DefaultTagScanner;
import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import io.github.neoramm.fastminimessage.v2.tag.TagSpec;
import io.github.neoramm.fastminimessage.v2.template.FoldPass;
import io.github.neoramm.fastminimessage.v2.template.TemplateNode;
import io.github.neoramm.fastminimessage.v2.template.ValueSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TemplateArchitectureTest {
    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());

    @Test
    void staticTransformsRunDuringCompilationOnly() {
        final @NotNull AtomicInteger calls = new AtomicInteger();
        compiler.registerModifier("count", new ComponentModifier() {
            @Override
            public @NotNull Component modify(final @NotNull Component value, final @NotNull RenderScope viewer) {
                calls.incrementAndGet();
                return value;
            }

            @Override
            public boolean isStatic() {
                return true;
            }
        });
        final var tree = compiler.compile("<count><red>hello</red></count>");
        assertEquals(1, calls.get());
        assertInstanceOf(TemplateNode.Static.class, tree.root());
        assertSame(tree.render(), tree.render());
        assertEquals(1, calls.get());
    }

    @Test
    void scalarInterpolationNeverInvokesTheScannerAtRenderTime() {
        final @NotNull AtomicInteger scans = new AtomicInteger();
        final @NotNull DefaultTagScanner scanner = new DefaultTagScanner();
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> targetKey = layout.key("target", String.class);
        final PayloadSchema.@NotNull Key<@NotNull String> nameKey = layout.key("name", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("target", (final var scope) -> Component.text(scope.payload(targetKey)))
            .registerPlaceholder("name", (final var scope) -> Component.text(scope.payload(nameKey)));
        final var counted = new MiniMessageCompiler(registry, (final var source) -> {
            scans.incrementAndGet();
            return scanner.scan(source);
        });
        final var tree = counted.compile("<click:run_command:'/msg <target> hi'><red><name></red></click>");
        final int compiledScans = scans.get();
        for (int i = 0; i < 10; i++) {
            final @NotNull String target = "O'Brien:\"quoted\"\\path" + i;
            final var rendered = tree.render((final var b) -> b.payload(targetKey, target)
                .payload(nameKey, "<blue>literal"));
            assertEquals("<blue>literal", ComponentComparison.plainText(rendered));
            assertEquals(ClickEvent.runCommand("/msg " + target + " hi"), rendered.clickEvent());
        }
        assertEquals(compiledScans, scans.get());
    }

    @Test
    void customTagsDeclareComponentArgumentsWithoutNameHeuristics() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> nameKey = layout.key("name", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("name", (final var scope) -> Component.text(scope.payload(nameKey)))
            .register(new TagSpec.LeafRule(Set.of("custom"),
                (final var args, final var context) -> args.component(0).map((final var values) -> values.component(0))));
        final var tree = new MiniMessageCompiler(registry).compile("<custom:'<red>Hello <name></red>'>");
        final var actual = tree.render((final var b) -> b.payload(nameKey, "Ada"));
        final var expected = MiniMessage.miniMessage().deserialize("<red>Hello Ada</red>");
        assertEquals(ComponentComparison.expression(expected), ComponentComparison.expression(actual));
    }

    @Test
    void scopeProvidesPayloadResolution() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> nameKey = layout.key("name", String.class);
        final @NotNull PayloadSchema schema = layout.build();
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(schema)
            .registerPlaceholder("name", (final var scope) -> Component.text(scope.payload(nameKey)));
        final @NotNull MiniMessageCompiler comp = new MiniMessageCompiler(registry);
        final @NotNull RenderScope scope = RenderScope.builder(schema).payload(nameKey, "original").build();
        assertEquals("original", ComponentComparison.plainText(comp.compile("<name>").render(scope)));
    }

    @Test
    void transformOwnsItsChildren() {
        final @NotNull List<@NotNull TemplateNode> children = new ArrayList<>();
        children.add(new TemplateNode.Static(Component.text("original")));
        final var transform = new TemplateNode.DynamicTransform(Style.empty(), children,
            (final var content, final var style, final var context) -> content);
        children.clear();
        assertEquals(Component.text("original"), transform.render(RenderScope.EMPTY));
        assertThrows(UnsupportedOperationException.class, () -> transform.children().clear());
    }

    @Test
    void dynamicBindingsReceiveIndependentArgumentCursors() {
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults()
            .register(new TagSpec.LeafRule(Set.of("echo"), (final var args, final var context) -> {
                final var values = args.values();
                return values.map((final var vals) -> Component.text(vals.string(0, "")));
            }));
        final var tree = new MiniMessageCompiler(registry).compile("<echo:first:second>");
        assertEquals(Component.text("first"), tree.render());
        assertEquals(Component.text("first"), tree.render());
    }

    @Test
    void adjacentConstantsFoldAroundDynamicSlots() {
        final var tree = compiler.compile("<red>a</red><red>b</red><name/><blue>c</blue><blue>d</blue>");
        final var root = assertInstanceOf(TemplateNode.DynamicTransform.class, tree.root());
        assertEquals(3, root.children().size());
        assertEquals(Component.text("ab", NamedTextColor.RED),
            assertInstanceOf(TemplateNode.Static.class, root.children().getFirst()).component());
        assertEquals(Component.text("cd", NamedTextColor.BLUE),
            assertInstanceOf(TemplateNode.Static.class, root.children().getLast()).component());
    }

    @Test
    void nestedDynamicStylesRespectInnerOverrides() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> themeKey = layout.key("theme", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("theme", (final var scope) -> Component.text(scope.payload(themeKey)));
        final @NotNull String source = "<red><color:'<theme>'>a<blue>b</blue>c</color>d</red>";
        final var tree = new MiniMessageCompiler(registry).compile(source);
        final var rendered = tree.render((final var b) -> b.payload(themeKey, "green"));
        final var expected = MiniMessage.miniMessage().deserialize(source.replace("<theme>", "green"));
        assertEquals(ComponentComparison.expression(expected), ComponentComparison.expression(rendered));
    }

    @Test
    void missingDynamicGradientArgumentsKeepTheLiteralFallback() {
        final var tree = compiler.compile("<gradient:'<first>':blue>visible</gradient>");
        assertEquals("<gradient:'<first>':blue>visible</gradient>", ComponentComparison.plainText(tree.render()));
    }

    @Test
    void dynamicHoverActionsUsePrecompiledComponentArguments() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> actionKey = layout.key("action", String.class);
        final PayloadSchema.@NotNull Key<@NotNull String> nameKey = layout.key("name", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("action", (final var scope) -> Component.text(scope.payload(actionKey)))
            .registerPlaceholder("name", (final var scope) -> Component.text(scope.payload(nameKey)));
        final var tree = new MiniMessageCompiler(registry).compile("<hover:'<action>':'<red>Hello <name></red>'>visible</hover>");
        final var result = tree.render((final var b) -> b.payload(actionKey, "show_text").payload(nameKey, "Ada"));
        assertEquals(HoverEvent.Action.SHOW_TEXT, result.hoverEvent().action());
        assertEquals(ComponentComparison.expression(MiniMessage.miniMessage().deserialize("<red>Hello Ada</red>")),
            ComponentComparison.expression((Component) result.hoverEvent().value()));
    }

    @Test
    void invalidDynamicTransformsCanRecoverOnTheNextRender() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull String> firstKey = layout.optionalKey("first", String.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("first", (final var scope) -> {
                final String val = scope.payloadOrNull(firstKey);
                return val != null ? Component.text(val) : Component.empty();
            });
        final var tree = new MiniMessageCompiler(registry).compile("<gradient:'<first>':blue>abc</gradient>");
        assertEquals("<gradient:'<first>':blue>abc</gradient>", ComponentComparison.plainText(tree.render()));
        final var expected = MiniMessage.miniMessage().deserialize("<gradient:red:blue>abc</gradient>");
        assertEquals(ComponentComparison.expression(expected), ComponentComparison.expression(
            tree.render((final var b) -> b.payload(firstKey, "red"))));
    }

    @Test
    void constantSourcesRejectNullValues() {
        assertThrows(NullPointerException.class, () -> new ValueSource.Constant<>(null));
    }

    @Test
    void emptyFoldReturnsAnEmptyStaticComponent() {
        final var node = assertInstanceOf(TemplateNode.Static.class, FoldPass.fold(List.of()));
        assertSame(Component.empty(), node.component());
    }
}
