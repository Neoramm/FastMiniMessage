package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.KeybindPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import net.kyori.adventure.text.format.Style;

public final class KeybindResolver implements ComponentResolver {
    @Override
    public PreComponent resolve(String name, Arguments args, PreComponent[] children, Style inheritedStyle, MiniMessageCompiler compiler) {
        String keybind = args.get(0, "key.jump");
        return new KeybindPreComponent(keybind, inheritedStyle);
    }

    @Override
    public boolean isSelfClosing() {
        return true;
    }
    @Override
    public String[] names() {
        return new String[]{"key"};
    }
}