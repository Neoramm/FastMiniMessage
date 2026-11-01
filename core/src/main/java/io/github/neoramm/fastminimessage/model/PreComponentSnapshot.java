package io.github.neoramm.fastminimessage.model;

import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record PreComponentSnapshot(@NotNull String type, @NotNull Style style, @Nullable String literalText,
    int childCount, boolean isStatic, boolean scopeIndependent) {
}
