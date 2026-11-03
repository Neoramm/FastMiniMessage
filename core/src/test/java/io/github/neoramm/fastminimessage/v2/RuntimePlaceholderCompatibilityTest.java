package io.github.neoramm.fastminimessage.v2;

import io.github.neoramm.fastminimessage.v2.model.PayloadSchema;
import io.github.neoramm.fastminimessage.v2.model.PreComponentTree;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RuntimePlaceholderCompatibilityTest {
    private static @NotNull String plainText(final @NotNull Component component) {
        return ComponentComparison.plainText(component);
    }

    @Test
    void dynamicRendersUpdateViaRegisteredPlaceholder() {
        final @NotNull PayloadSchema.Builder layout = PayloadSchema.builder();
        final PayloadSchema.@NotNull Key<@NotNull Component> xKey = layout.key("x", Component.class);
        final @NotNull ResolverRegistry registry = ResolverRegistry.withDefaults(layout.build())
            .registerPlaceholder("x", (final var scope) -> scope.payload(xKey));
        final @NotNull PreComponentTree tree = new MiniMessageCompiler(registry).compile("<red><x></red>");
        assertEquals("A", plainText(tree.render((final var b) -> b.payload(xKey, Component.text("A")))));
        assertEquals("B", plainText(tree.render((final var b) -> b.payload(xKey, Component.text("B")))));
    }
}
