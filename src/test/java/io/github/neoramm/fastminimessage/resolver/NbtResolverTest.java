package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.ResolverRegistry;
import io.github.neoramm.fastminimessage.model.Placeholder;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.BlockNBTComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.EntityNBTComponent;
import net.kyori.adventure.text.NBTComponent;
import net.kyori.adventure.text.StorageNBTComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NbtResolverTest {

    private static final @NotNull Pointered NOBODY = new Pointered() {
        @Override
        public @NotNull Pointers pointers() {
            return Pointers.empty();
        }
    };

    private final @NotNull MiniMessageCompiler compiler = new MiniMessageCompiler(ResolverRegistry.withDefaults());
    private final @NotNull MiniMessage miniMessage = MiniMessage.builder().strict(false).build();

    private @NotNull Component render(final @NotNull String input) {
        return compiler.compile(input).render(NOBODY);
    }

    private static @NotNull String plainText(final @Nullable Component component) {
        return component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void blockNbtKeepsTheSeparator() {
        final BlockNBTComponent component = (BlockNBTComponent) render("<nbt:block:0 0 0:Items:', '>");
        assertEquals("0 0 0", component.pos().asString());
        assertEquals("Items", component.nbtPath());
        assertEquals(", ", plainText(component.separator()));
    }

    @Test
    void entityNbtCompilesAStyledSeparator() {
        final EntityNBTComponent component = (EntityNBTComponent) render("<nbt:entity:@e:Inventory:'<red>; </red>'>");
        assertEquals("@e", component.selector());
        assertEquals("; ", plainText(component.separator()));
        assertEquals(NamedTextColor.RED, component.separator().style().color());
    }

    @Test
    void storageNbtSeparatorMatchesMiniMessage() {
        final String input = "<nbt:storage:foo:Items:'<red>; </red>'>";
        final StorageNBTComponent reference = (StorageNBTComponent) miniMessage.deserialize(input);
        final StorageNBTComponent actual = (StorageNBTComponent) render(input);
        assertEquals(Key.key("foo"), actual.storage());
        assertEquals(reference.storage(), actual.storage());
        assertEquals(plainText(reference.separator()), plainText(actual.separator()));
        assertEquals(reference.separator().style(), actual.separator().style());
    }

    @Test
    void interpretFlagFollowsTheSeparator() {
        final NBTComponent<?, ?> component = (NBTComponent<?, ?>) render("<nbt:entity:@e:Inventory:', ':interpret>");
        assertTrue(component.interpret());
        assertEquals(", ", plainText(component.separator()));
    }

    @Test
    void separatorSupportsRuntimePlaceholders() {
        final PreComponentTree tree = compiler.compile("<nbt:entity:@e:Inventory:'<p>'>");
        final EntityNBTComponent component =
            (EntityNBTComponent) tree.render(NOBODY, Placeholder.ofUnparsed("p", "|"));
        assertEquals("|", plainText(component.separator()));
    }

    @Test
    void staticnessFollowsTheSeparator() {
        assertTrue(compiler.compile("<nbt:entity:@e:Inventory>").isStatic());
        assertTrue(compiler.compile("<nbt:entity:@e:Inventory:', '>").isStatic());
        assertFalse(compiler.compile("<nbt:entity:@e:Inventory:'<p>'>").isStatic());
    }
}
