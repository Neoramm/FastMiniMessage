package io.github.neoramm.fastminimessage.v2.template;

import io.github.neoramm.fastminimessage.v2.model.RenderScope;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface TreeTransformer {
    @NotNull Component transform(final @NotNull Component children, final @NotNull Style style,
        final @NotNull RenderScope context);
}
