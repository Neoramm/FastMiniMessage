package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.StaticComponentPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ScoreResolver implements ComponentResolver {
    private static final String @NotNull [] NAMES = {"score"};
    @Override
    public @NotNull PreComponent resolve(final @NotNull String name, final @NotNull Arguments args,
        final PreComponent @NotNull [] children, final @Nullable Style inheritedStyle,
        final @NotNull MiniMessageCompiler compiler) {
        if (args.size() < 2) {
            throw new IllegalArgumentException("A scoreboard member name and an objective name are required");
        }
        final String member = args.getFirst();
        final String objective = args.get(1);
        return new StaticComponentPreComponent(Component.score(member, objective)).setStyle(inheritedStyle);
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
