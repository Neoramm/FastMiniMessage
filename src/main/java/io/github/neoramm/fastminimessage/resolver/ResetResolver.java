package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.GroupPreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import net.kyori.adventure.text.format.Style;

public final class ResetResolver implements ComponentResolver {
    @Override
    public PreComponent resolve(String name, Arguments args, PreComponent[] children, Style inheritedStyle, MiniMessageCompiler compiler) {
        if (children.length == 0) {
            return new LiteralPreComponent("", Style.empty());
        }
        if (children.length == 1) {
            children[0].setStyle(Style.empty());
            return children[0];
        }
        GroupPreComponent group = new GroupPreComponent(children);
        group.setStyle(Style.empty());
        return group;
    }

    // Fallback for custom scanners: a reset tag wraps the content it resets.
    @Override
    public boolean isSelfClosing() {
        return false;
    }

    @Override
    public String[] names() {
        return new String[]{"reset"};
    }
}
