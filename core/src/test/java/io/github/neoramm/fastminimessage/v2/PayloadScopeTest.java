package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.compat.AdventureTags;
import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class PayloadScopeTest {
    @Test
    void defaultPayloadsAreOptionalAndSharedWithExtendedSchemas() {
        final var layout = PayloadSchema.builderWithDefaults();
        final var name = layout.key("name", String.class);
        final var schema = layout.build();
        assertEquals(3, name.index());
        assertEquals(4, schema.size());
        assertEquals(1, schema.requiredCount());
        final var tags = AdventureTags.of(Placeholder.unparsed("bridge", "value"));
        final var scope = RenderScope.builder(schema).payload(name, "Hector")
            .payload(PayloadSchema.ADVENTURE_TAGS_KEY, tags).build();
        assertSame(tags, scope.payload(PayloadSchema.ADVENTURE_TAGS_KEY));
        assertEquals(null, scope.payloadOrNull(PayloadSchema.RUNTIME_TAGS_KEY));
        assertEquals(0, PayloadSchema.withDefaults().requiredCount());
        assertFalse(PayloadSchema.ADVENTURE_TAGS_KEY.isRequired());
    }

    @Test
    void defaultPayloadNamesCannotBeReassigned() {
        final var layout = PayloadSchema.builderWithDefaults();
        assertThrows(IllegalArgumentException.class, () -> layout.key("adventure_tags", String.class));
    }

    @Test
    void consumerSuppliesAdventureTagsThroughDefaultSchema() {
        final var compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
        final var tree = compiler.compile("<name/>");
        final var tags = AdventureTags.of(Placeholder.unparsed("name", "Hector"));
        assertEquals(Component.text("Hector"), tree.render((final var builder) ->
            builder.payload(PayloadSchema.ADVENTURE_TAGS_KEY, tags)));
        assertEquals(3, tree.payloadCount());
    }

    @Test
    void consumerRunsExactlyOnceForStaticTemplates() {
        final var tree = new MiniMessageCompiler(ResolverRegistry.withDefaults()).compile("static");
        final int[] calls = {0};
        assertSame(tree.render(), tree.render((final var builder) -> calls[0]++));
        assertEquals(1, calls[0]);
    }

    @Test
    void consumerRejectsMissingRequiredPayloads() {
        final var layout = PayloadSchema.builderWithDefaults();
        final var name = layout.key("name", String.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("name", (final var scope) -> Component.text(scope.payload(name)));
        final var tree = new MiniMessageCompiler(registry).compile("<name>");
        assertThrows(IllegalStateException.class, () -> tree.render((final var builder) -> builder.viewer(null)));
        assertEquals(Component.text("Hector"), tree.render((final var builder) -> builder.payload(name, "Hector")));
    }

    @Test
    void schemaAssignsConsecutiveIndicesAndClosesRegistration() {
        final var builder = PayloadSchema.builder();
        final var first = builder.key("first", String.class);
        final var second = builder.key("second", Component.class);
        final var schema = builder.build();
        assertEquals(0, first.index());
        assertEquals(1, second.index());
        assertEquals(2, schema.size());
        assertThrows(IllegalStateException.class, () -> builder.key("late", String.class));
    }

    @Test
    void duplicateNamesAndPrimitiveTypesAreRejected() {
        final var builder = PayloadSchema.builder();
        builder.key("player", String.class);
        assertThrows(IllegalArgumentException.class, () -> builder.key("player", Component.class));
        assertThrows(IllegalArgumentException.class, () -> builder.key("number", int.class));
        assertThrows(IllegalArgumentException.class, () -> builder.key("", String.class));
    }

    @Test
    void scopeBuilderRequiresEveryPayloadBeforeTransferringOwnership() {
        final var layout = PayloadSchema.builder();
        final var key = layout.key("message", String.class);
        final var schema = layout.build();
        final var builder = RenderScope.builder(schema);
        assertThrows(IllegalStateException.class, builder::build);
        final var scope = builder.payload(key, "hello").build();
        assertEquals("hello", scope.payload(key));
        assertThrows(IllegalStateException.class, () -> builder.payload(key, "changed"));
        assertThrows(IllegalStateException.class, builder::build);
        assertEquals("hello", scope.payload(key));
    }

    @Test
    void foreignKeysCannotAliasTheSameIndex() {
        final var first = PayloadSchema.builder();
        final var own = first.key("value", String.class);
        final var schema = first.build();
        final var second = PayloadSchema.builder();
        final var foreign = second.key("value", String.class);
        second.build();
        final var builder = RenderScope.builder(schema);
        assertThrows(IllegalArgumentException.class, () -> builder.payload(foreign, "wrong"));
        assertThrows(NullPointerException.class, () -> builder.payload(own, null));
        final var scope = builder.payload(own, "right").build();
        assertThrows(IllegalArgumentException.class, () -> scope.payload(foreign));
    }

    @Test
    void registeredPlaceholdersReadTheCurrentPayloadOnEveryRender() {
        final var layout = PayloadSchema.builder();
        final var message = layout.key("message", Component.class);
        final var schema = layout.build();
        final var registry = ResolverRegistry.withDefaults(schema)
            .registerPlaceholder("message", (final var scope) -> scope.payload(message));
        final var compiler = new MiniMessageCompiler(registry);
        final var tree = compiler.compile("<message>");
        final var first = Component.text("first");
        final var second = Component.text("second");
        assertEquals(1, compiler.payloadCount());
        assertEquals(1, tree.payloadCount());
        assertFalse(tree.isStatic());
        assertSame(first, tree.render(compiler.scopeBuilder().payload(message, first).build()));
        assertSame(second, tree.render(compiler.scopeBuilder().payload(message, second).build()));
    }

    @Test
    void registeredPlaceholdersPreserveSiblingAndChildStyles() {
        final var layout = PayloadSchema.builder();
        final var value = layout.key("value", Component.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("value", (final var scope) -> scope.payload(value));
        final var compiler = new MiniMessageCompiler(registry);
        final var scope = compiler.scopeBuilder().payload(value, Component.text("X", NamedTextColor.BLUE)).build();
        final var actual = compiler.compile("<red>a<value>child</value>tail</red>").render(scope);
        final var expected = Component.text().append(Component.text("a", NamedTextColor.RED))
            .append(Component.text("X", NamedTextColor.BLUE)).append(Component.text("childtail", NamedTextColor.RED))
            .build();
        assertEquals(ComponentComparison.expression(expected), ComponentComparison.expression(actual));
    }

    @Test
    void scalarArgumentsUseTheRegisteredPlaceholderWithoutRuntimeBindings() {
        final var layout = PayloadSchema.builder();
        final var target = layout.key("target", String.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("target", (final var scope) -> Component.text(scope.payload(target)));
        final var compiler = new MiniMessageCompiler(registry);
        final var tree = compiler.compile("<click:run_command:'/msg <target>'>hello</click>");
        final var scope = compiler.scopeBuilder().payload(target, "O'Brien:quoted").build();
        assertEquals(ClickEvent.runCommand("/msg O'Brien:quoted"), tree.render(scope).clickEvent());
    }

    @Test
    void scopedModifiersStayDynamicWithStaticChildren() {
        final var layout = PayloadSchema.builder();
        final var color = layout.key("color", NamedTextColor.class);
        final var registry = ResolverRegistry.withDefaults(layout.build()).registerModifier("tint",
            (final var component, final var scope) -> component.color(scope.payload(color)));
        final var compiler = new MiniMessageCompiler(registry);
        final var tree = compiler.compile("<tint>hello</tint>");
        assertFalse(tree.isStatic());
        assertEquals(NamedTextColor.RED, tree.render(compiler.scopeBuilder().payload(color,
            NamedTextColor.RED).build()).color());
        assertEquals(NamedTextColor.BLUE, tree.render(compiler.scopeBuilder().payload(color,
            NamedTextColor.BLUE).build()).color());
    }

    @Test
    void scopeCarriesTheViewerWithoutAnAdditionalContext() {
        final @NotNull Pointered viewer = new Pointered() {
            @Override
            public @NotNull Pointers pointers() {
                return Pointers.builder().withStatic(Identity.NAME, "Ada").build();
            }
        };
        final var compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
        final var scope = compiler.scopeBuilder().viewer(viewer).build();
        assertSame(viewer, scope.viewer());
        assertSame(scope, scope.withViewer(viewer));
        assertEquals("Ada", ComponentComparison.plainText(compiler.compile("<player>").render(scope)));
    }

    @Test
    void templatesRejectScopesFromDifferentSchemasBeforeCallingThePlaceholder() {
        final var layout = PayloadSchema.builder();
        final var key = layout.key("value", String.class);
        final var compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("value", (final var scope) -> Component.text(scope.payload(key))));
        final var tree = compiler.compile("<value>");
        assertThrows(IllegalArgumentException.class, () -> tree.render(RenderScope.EMPTY));
    }

    @Test
    void compiledTemplatesKeepTheirRegisteredPlaceholder() {
        final var registry = ResolverRegistry.withDefaults()
            .registerPlaceholder("value", (final var scope) -> Component.text("original"));
        final var compiler = new MiniMessageCompiler(registry);
        final var tree = compiler.compile("<value>");
        registry.registerPlaceholder("value", (final var scope) -> Component.text("replacement"));
        assertEquals("original", ComponentComparison.plainText(tree.render(RenderScope.EMPTY)));
        assertEquals("replacement", ComponentComparison.plainText(compiler.compile("<value>").render()));
    }

    @Test
    void scopeBuilderCannotBeMutatedAfterBuildThroughOtherSetters() {
        final var builder = RenderScope.builder(PayloadSchema.EMPTY);
        assertSame(RenderScope.EMPTY, builder.build());
        assertThrows(IllegalStateException.class, () -> builder.viewer(null));
        assertThrows(IllegalStateException.class, () -> builder.payload(PayloadSchema.RUNTIME_TAGS_KEY,
            PayloadInputs.combine()));
    }

    @Test
    void repeatedAssignmentsDoNotSatisfyOtherMissingPayloads() {
        final var layout = PayloadSchema.builder();
        final var first = layout.key("first", String.class);
        final var second = layout.key("second", String.class);
        final var builder = RenderScope.builder(layout.build());
        builder.payload(first, "one").payload(first, "updated");
        assertThrows(IllegalStateException.class, builder::build);
        final var scope = builder.payload(second, "two").build();
        assertEquals("updated", scope.payload(first));
        assertEquals("two", scope.payload(second));
        assertEquals(2, scope.payloadCount());
    }

    @Test
    void mutablePayloadValuesAreReadAgainWithoutReplacingTheScope() {
        final var layout = PayloadSchema.builder();
        final var key = layout.key("value", MutablePayload.class);
        final var compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("value", (final var scope) -> scope.payload(key).value));
        final var payload = new MutablePayload();
        final var scope = compiler.scopeBuilder().payload(key, payload).build();
        final var tree = compiler.compile("<value>");
        assertSame(payload.value, tree.render(scope));
        payload.value = Component.text("changed");
        assertSame(payload.value, tree.render(scope));
    }

    @Test
    void mappedTemplatesPreserveSchemaAndVisitRegisteredChildren() {
        final var layout = PayloadSchema.builder();
        final var key = layout.key("value", Component.class);
        final var compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("value", (final var scope) -> scope.payload(key)));
        final var tree = compiler.compile("<value>child</value>").map((final var node) -> node);
        final var scope = compiler.scopeBuilder().payload(key, Component.text("X")).build();
        assertEquals(1, tree.payloadCount());
        assertEquals("Xchild", ComponentComparison.plainText(tree.render(scope)));
        assertEquals(2, ComponentComparison.preCompilerNodeCount(tree));
    }

    @Test
    void concurrentRendersKeepIndependentScopes() throws InterruptedException, ExecutionException {
        final var layout = PayloadSchema.builder();
        final var key = layout.key("value", Component.class);
        final var compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("value", (final var scope) -> scope.payload(key)));
        final var tree = compiler.compile("<value>");
        final var first = Component.text("first");
        final var second = Component.text("second");
        final var firstScope = compiler.scopeBuilder().payload(key, first).build();
        final var secondScope = compiler.scopeBuilder().payload(key, second).build();
        final @NotNull List<@NotNull Callable<@NotNull Component>> tasks = List.of(
            () -> tree.render(firstScope), () -> tree.render(secondScope));
        try (final var executor = Executors.newFixedThreadPool(2,
            Thread.ofPlatform().name("payload-test-", 0).factory())) {
            final var results = executor.invokeAll(tasks);
            assertSame(first, results.getFirst().get());
            assertSame(second, results.getLast().get());
        }
    }

    @Test
    void componentArgumentsUseTheSamePayloadScope() {
        final var layout = PayloadSchema.builder();
        final var key = layout.key("value", Component.class);
        final var compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("value", (final var scope) -> scope.payload(key)));
        final var value = Component.text("hint", NamedTextColor.RED);
        final var scope = compiler.scopeBuilder().payload(key, value).build();
        final var result = compiler.compile("<hover:show_text:'<value>'>visible</hover>").render(scope);
        assertEquals(HoverEvent.showText(value), result.hoverEvent());
    }

    @Test
    void adventurePreprocessingKeepsThePayloadSchemaAndViewer() {
        final var layout = PayloadSchema.builderWithDefaults();
        final var key = layout.key("value", Component.class);
        final var compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("value", (final var scope) -> scope.payload(key)));
        final var bindings = AdventureTags.of(Placeholder.parsed("parsed", "<value>"));
        final var scope = compiler.scopeBuilder().payload(key,
            Component.text("payload")).payload(PayloadSchema.RUNTIME_TAGS_KEY, PayloadInputs.combine(bindings)).build();
        assertEquals("payload", ComponentComparison.plainText(compiler.compile("<parsed>").render(scope)));
    }

    @Test
    void mutableScopeCanBeRecycledWithNewPayloadsAndViewer() {
        final var layout = PayloadSchema.builder();
        final var nameKey = layout.key("name", String.class);
        final var coinsKey = layout.key("coins", Integer.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("name", (final var scope) -> Component.text(scope.payload(nameKey)))
            .registerPlaceholder("coins", (final var scope) -> Component.text(scope.payload(coinsKey)));
        final var compiler = new MiniMessageCompiler(registry);
        final var tree = compiler.compile("Player <name> has <coins> coins");

        final var mutableScope = tree.mutableScope();

        mutableScope.payload(nameKey, "Alice").payload(coinsKey, 100);
        assertEquals("Player Alice has 100 coins", ComponentComparison.plainText(tree.render(mutableScope)));

        mutableScope.payload(nameKey, "Bob").payload(coinsKey, 250);
        assertEquals("Player Bob has 250 coins", ComponentComparison.plainText(tree.render(mutableScope)));

        mutableScope.clear();
        assertThrows(NullPointerException.class, () -> tree.render(mutableScope));

        mutableScope.payload(nameKey, "Charlie").payload(coinsKey, 50);
        assertEquals("Player Charlie has 50 coins", ComponentComparison.plainText(tree.render(mutableScope)));
    }

    @Test
    void mutableScopeViewerAndSnapshotOperations() {
        final var layout = PayloadSchema.builder();
        final var key = layout.key("val", String.class);
        final var schema = layout.build();
        final var scope = RenderScope.mutable(schema);

        final Pointered viewer1 = new Pointered() {
            @Override
            public @NotNull Pointers pointers() {
                return Pointers.builder().withDynamic(Identity.NAME, () -> "Steve").build();
            }
        };
        final Pointered viewer2 = new Pointered() {
            @Override
            public @NotNull Pointers pointers() {
                return Pointers.builder().withDynamic(Identity.NAME, () -> "Alex").build();
            }
        };

        scope.viewer(viewer1).payload(key, "data");
        assertSame(viewer1, scope.viewer());
        assertEquals("data", scope.payload(key));

        final var immutable = scope.toImmutable();
        assertEquals("data", immutable.payload(key));
        assertSame(viewer1, immutable.viewer());

        scope.viewer(viewer2).payload(key, "updated");
        assertEquals("updated", scope.payload(key));
        assertSame(viewer2, scope.viewer());
        assertEquals("data", immutable.payload(key));
        assertSame(viewer1, immutable.viewer());

        final var recycled = immutable.toMutable();
        assertEquals("data", recycled.payload(key));
        assertSame(viewer1, recycled.viewer());
        recycled.payload(key, "recycled");
        assertEquals("recycled", recycled.payload(key));
        assertEquals("data", immutable.payload(key));
    }

    private static final class MutablePayload {
        private @NotNull Component value = Component.text("original");
    }
}
