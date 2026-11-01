package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.SelectorPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SelectorResolver implements ComponentResolver {
    private static final String @NotNull [] NAMES = {"selector", "sel"};
    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        if (args.isEmpty()) throw new IllegalArgumentException("A selection key is required");
        final String key = args.getFirst();
        final @Nullable PreComponentTree separator =
            args.size() > 1 && args.get(1) != null ? compiler.compileArgument(args.get(1)) : null;
        return new SelectorPreComponent(key, separator).setStyle(inheritedStyle);
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return NAMES;
    }

    @Override
    public boolean isSelfClosing() {
        return true;
    }
}
