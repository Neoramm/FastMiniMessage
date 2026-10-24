package io.github.neoramm.fastminimessage.paper;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

final class PapiResolver implements ComponentResolver {

    private final @NotNull PapiRenderer renderer;

    PapiResolver(final @NotNull PapiRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        final String expression = Objects.requireNonNullElse(args.get(0, ""), "");
        if (expression.isEmpty()) throw new IllegalArgumentException("papi requires an expression");
        return new PapiPreComponent(renderer, expression, inheritedStyle);
    }

    @Override
    public boolean isSelfClosing() {
        return true;
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return new String[]{"papi"};
    }
}
