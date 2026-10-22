package io.github.neoramm.fastminimessage.component;

import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.RenderScope;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.pointer.Pointered;

public final class KeybindPreComponent extends PreComponent {
    private final String keybind;

    public KeybindPreComponent(String keybind, Style style) {
        this.keybind = keybind;
        this.computedStyle = style;
    }

    @Override
    public Component render(Pointered pointered, RenderScope scope) {
        return Component.keybind(keybind, computedStyle);
    }

    @Override
    public boolean isStatic() {
        return true;
    }

    @Override
    public PreComponent deepCopy() {
        return new KeybindPreComponent(keybind, computedStyle);
    }
}