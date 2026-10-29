package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.GroupPreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ResetResolver implements ComponentResolver {
    private static final String @NotNull [] NAMES = {"reset"};
    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        if (children.length == 0) return new LiteralPreComponent("", Style.empty());
        if (children.length == 1) return children[0].deepCopy().setStyle(Style.empty());
        return new GroupPreComponent(children).setStyle(Style.empty());
    }

    @Override
    public boolean isSelfClosing() {
        return false;
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return NAMES;
    }
}
