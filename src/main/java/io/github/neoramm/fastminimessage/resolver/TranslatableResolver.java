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
    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        final String key = args.get(0);
        if (key == null) return new LiteralPreComponent("<" + name + ">", inheritedStyle);
        final PreComponentTree @NotNull [] translatableArgs = new PreComponentTree[args.size() - 1];
        for (int i = 1; i < args.size(); i++) translatableArgs[i - 1] = compiler.compile(args.get(i));
        return new TranslatablePreComponent(key, translatableArgs, inheritedStyle);
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return new String[]{"lang", "translate", "tr"};
    }
}
