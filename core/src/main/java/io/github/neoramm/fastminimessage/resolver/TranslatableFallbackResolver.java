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

public final class TranslatableFallbackResolver implements ComponentResolver {
    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        final String key = args.get(0);
        if (key == null) return new LiteralPreComponent("<" + name + ">", inheritedStyle);
        final String fallback = args.get(1);
        final PreComponentTree @NotNull [] translatableArgs = new PreComponentTree[args.size() - 2];
        for (int i = 2; i < args.size(); i++) translatableArgs[i - 2] = compiler.compile(args.get(i));
        return new TranslatablePreComponent(key, fallback, translatableArgs, inheritedStyle);
    }

    @Override
    public @NotNull String @NotNull [] names() {
        return new String[]{"lang_or", "translate_or", "tr_or"};
    }
}
