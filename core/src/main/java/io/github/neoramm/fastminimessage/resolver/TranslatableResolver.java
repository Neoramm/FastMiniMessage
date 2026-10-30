package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.component.TranslatablePreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TranslatableResolver implements ComponentResolver {
    private static final String @NotNull [] NAMES = {"lang", "translate", "tr"};
    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        if (args.isEmpty()) throw new IllegalArgumentException("A translation key is required");
        final String key = args.getFirst();
        final PreComponentTree @Nullable [] translatableArgs =
            args.size() > 1 ? new PreComponentTree[args.size() - 1] : null;
        if (translatableArgs != null) {
            for (int i = 1; i < args.size(); i++) translatableArgs[i - 1] = compiler.compile(args.get(i));
        }
        return new TranslatablePreComponent(key, translatableArgs, inheritedStyle);
    }

    @Override
    public boolean isSelfClosing() {
        return true;
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return NAMES;
    }
}
