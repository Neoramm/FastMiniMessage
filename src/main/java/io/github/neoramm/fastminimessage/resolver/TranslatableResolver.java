package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.component.TranslatablePreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import io.github.neoramm.fastminimessage.model.PreComponentTree;
import net.kyori.adventure.text.format.Style;

public final class TranslatableResolver implements ComponentResolver {
    @Override
    public PreComponent resolve(String name, Arguments args, PreComponent[] children, Style inheritedStyle, MiniMessageCompiler compiler) {
        String key = args.get(0);
        if (key == null) return new LiteralPreComponent("<" + name + ">", inheritedStyle);


        PreComponentTree[] tArgs = new PreComponentTree[args.size() - 1];
        for (int i = 1; i < args.size(); i++) {
            tArgs[i - 1] = compiler.compile(args.get(i));
        }
        return new TranslatablePreComponent(key, tArgs, inheritedStyle);
    }

    @Override
    public String[] names() {
        return new String[]{"lang", "translate", "tr"};
    }
}
