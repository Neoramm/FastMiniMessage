package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.tag.TagSpec;
import io.github.neoramm.fastminimessage.v2.template.TemplateNode;
import io.github.neoramm.fastminimessage.v2.template.ValueSource;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

final class RegisteredDynamicTagsTest {
    @Test
    void argumentAndPayloadChangesAreEvaluatedByTheSameCompiledTag() {
        final var layout = PayloadSchema.builder();
        final var name = layout.key("name", Component.class);
        final var prefix = layout.key("prefix", String.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("name", (final var scope) -> scope.payload(name));
        registry.register(new TagSpec.LeafRule(Set.of("greet"), (final var args, final var context) -> {
            args.require(1);
            final var values = args.values();
            return new ValueSource.Computed<>((final var scope) ->
                Component.text(scope.payload(prefix) + values.evaluate(scope).string(0, "")));
        }));
        final var tree = new MiniMessageCompiler(registry).compile("<greet:'<name>'/>");
        final var first = tree.render((final var builder) ->
            builder.payload(name, Component.text("Alex")).payload(prefix, "Hello "));
        final var second = tree.render((final var builder) ->
            builder.payload(name, Component.text("Hector")).payload(prefix, "Hola "));
        assertEquals(Component.text("Hello Alex"), first);
        assertEquals(Component.text("Hola Hector"), second);
        assertFalse(tree.isStatic());
        tree.inspect((final var node) -> assertFalse(node instanceof TemplateNode.Slot));
    }

    @Test
    void constantArgumentsDoNotFreezePayloadDependentValues() {
        final var layout = PayloadSchema.builder();
        final var suffix = layout.key("suffix", String.class);
        final var registry = ResolverRegistry.withDefaults(layout.build());
        registry.register(new TagSpec.LeafRule(Set.of("greet"), (final var args, final var context) -> {
            final var prefix = args.raw(0, "Hello ");
            return new ValueSource.Computed<>((final var scope) -> Component.text(prefix + scope.payload(suffix)));
        }));
        final var tree = new MiniMessageCompiler(registry).compile("<greet:'Hi '/>");
        assertEquals(Component.text("Hi Alex"), tree.render((final var builder) -> builder.payload(suffix, "Alex")));
        assertEquals(Component.text("Hi Hector"), tree
            .render((final var builder) -> builder.payload(suffix, "Hector")));
    }

    @Test
    void registeredModifierUsesFreshPayloadsWithoutRecompilation() {
        final var layout = PayloadSchema.builder();
        final var name = layout.key("name", Component.class);
        final var color = layout.key("color", NamedTextColor.class);
        final var registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("name", (final var scope) -> scope.payload(name))
            .registerModifier("tint", (final var content, final var scope) -> content.color(scope.payload(color)));
        final var tree = new MiniMessageCompiler(registry).compile("<tint><name></tint>");
        final var first = tree.render((final var builder) ->
            builder.payload(name, Component.text("Alex")).payload(color, NamedTextColor.RED));
        final var second = tree.render((final var builder) ->
            builder.payload(name, Component.text("Hector")).payload(color, NamedTextColor.BLUE));
        assertEquals(Component.text("Alex", NamedTextColor.RED), first);
        assertEquals(Component.text("Hector", NamedTextColor.BLUE), second);
        assertSame(NamedTextColor.RED, first.color());
        tree.inspect((final var node) -> assertFalse(node instanceof TemplateNode.Slot));
    }
}
