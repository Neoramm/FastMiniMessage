package io.github.neoramm.fastminimessage.v2.api;

import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import io.github.neoramm.fastminimessage.v2.model.RuntimeTagBridge;
import io.github.neoramm.fastminimessage.v2.model.TagResolver;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ComponentModifier {
    @NotNull Component modify(final @NotNull Component rendered, final @NotNull RenderScope context);

    default boolean isStatic() {
        return false;
    }

    static @NotNull RuntimeTagBridge of(final @NotNull String name, final @NotNull ComponentModifier modifier) {
        return TagResolver.modifying(name, modifier);
    }
}
