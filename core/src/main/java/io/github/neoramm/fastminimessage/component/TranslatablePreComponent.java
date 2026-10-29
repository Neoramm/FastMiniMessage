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
    private volatile @Nullable Component cachedComponent;

    public TranslatablePreComponent(final @NotNull String key, final PreComponentTree @Nullable [] args,
        final @Nullable Style style) {
        this(key, null, args, style);
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
        final @Nullable Component cached = this.cachedComponent;
        if (cached != null && scope.isEmpty()) return cached;
        final TranslatableComponent.Builder builder = Component.translatable().key(key);
        if (computedStyle != null && !computedStyle.isEmpty()) builder.style(computedStyle);
        if (fallback != null) builder.fallback(fallback);
        final PreComponentTree @Nullable [] args = this.args;
        if (args != null && args.length > 0) {
            final Component[] components = new Component[args.length];
            for (int i = 0; i < args.length; i++) components[i] = args[i].render(pointered, scope);
            builder.arguments(components);
        }
        final Component result = builder.build();
        if (isStatic() && scope.isEmpty()) this.cachedComponent = result;
        return result;
    }

    @Override
    public boolean isStatic() {
        final PreComponentTree @Nullable [] args = this.args;
        if (args == null) return true;
        for (final PreComponentTree arg : args) if (!arg.isStatic()) return false;
        return true;
    }

    @Override
    public @NotNull PreComponent setStyle(final @Nullable Style style) {
        super.setStyle(style);
        this.cachedComponent = null;
        return this;
    }

    @Override
    public @NotNull PreComponent deepCopy() {
        final PreComponentTree @Nullable [] copiedArgs;
        if (args != null) {
            copiedArgs = new PreComponentTree[args.length];
            for (int i = 0; i < args.length; i++) copiedArgs[i] = args[i].copy();
        } else copiedArgs = null;
        return new TranslatablePreComponent(key, fallback, copiedArgs, computedStyle);
    }
}
