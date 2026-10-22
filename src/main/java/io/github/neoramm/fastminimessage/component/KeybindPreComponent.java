package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class KeybindPreComponent extends PreComponent {
    private final @NotNull String keybind;

    public KeybindPreComponent(final @NotNull String keybind, final @Nullable Style style) {
        this.keybind = keybind;
        this.computedStyle = style;
    }

    @Override
    public @NotNull Component render(final @Nullable Pointered pointered, final @NotNull RenderScope scope) {
        return Component.keybind(keybind, computedStyle);
    }

    @Override
    public boolean isStatic() {
        return true;
    }

    @Override
    public @NotNull PreComponent deepCopy() {
        return new KeybindPreComponent(keybind, computedStyle);
    }
}
