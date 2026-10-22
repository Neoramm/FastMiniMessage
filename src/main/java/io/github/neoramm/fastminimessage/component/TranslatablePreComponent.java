package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TranslatablePreComponent extends PreComponent {
    private final @NotNull String key;
    private final @Nullable String fallback;
    private final PreComponentTree @Nullable [] args;

    public TranslatablePreComponent(final @NotNull String key, final PreComponentTree @Nullable [] args,
        final @Nullable Style style) {
        this.key = key;
        this.fallback = null;
        this.args = args;
        this.computedStyle = style;
    }

    public TranslatablePreComponent(final @NotNull String key, final @Nullable String fallback,
        final PreComponentTree @Nullable [] args, final @Nullable Style style) {
        this.key = key;
        this.fallback = fallback;
        this.args = args;
        this.computedStyle = style;
    }

    @Override
    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        final TranslatableComponent.Builder builder = Component.translatable().key(key);
        if (computedStyle != null && !computedStyle.isEmpty()) builder.style(computedStyle);
        if (fallback != null) builder.fallback(fallback);
        final PreComponentTree @Nullable [] args = this.args;
        if (args != null) {
            final Component[] components = new Component[args.length];
            for (int i = 0; i < args.length; i++) components[i] = args[i].render(pointered, scope);
            builder.arguments(components);
        }
        return builder.build();
    }

    @Override
    public @NotNull PreComponent deepCopy() {
        final PreComponentTree @Nullable [] copiedArgs = args != null ? args.clone() : null;
        if (fallback != null) return new TranslatablePreComponent(key, fallback, copiedArgs, computedStyle);
        return new TranslatablePreComponent(key, copiedArgs, computedStyle);
    }
}
