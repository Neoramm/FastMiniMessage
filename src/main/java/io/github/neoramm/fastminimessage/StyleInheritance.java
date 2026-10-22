package io.github.neoramm.fastminimessage;

import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StyleInheritance {

    private StyleInheritance() {
    }

    public static @NotNull Style compute(final @Nullable Style ownStyle, final @Nullable Style inheritedStyle) {
        if (ownStyle == null || ownStyle.isEmpty()) return inheritedStyle != null ? inheritedStyle : Style.empty();
        if (inheritedStyle == null || inheritedStyle.isEmpty()) return ownStyle;
        return inheritedStyle.merge(ownStyle);
    }

    public static @NotNull Style computeWithReset(final @Nullable Style ownStyle,
        final @Nullable Style inheritedStyle) {
        return ownStyle != null ? ownStyle : Style.empty();
    }

    /**
     * Pushes a tag style into its only literal child when that avoids a wrapper component,
     * so the render path creates one component less.
     *
     * @return {@code true} when the style was applied and the wrapper can be skipped
     */
    public static boolean mergeIntoOnlyLiteral(final PreComponent @NotNull [] children, final @Nullable Style inherited,
        final @NotNull Style own) {
        if (children.length != 1 || !(children[0] instanceof final LiteralPreComponent literal)) return false;
        final Style base = inherited != null ? inherited : Style.empty();
        literal.setStyle(base.merge(own).merge(literal.getComputedStyle()));
        return true;
    }
}
