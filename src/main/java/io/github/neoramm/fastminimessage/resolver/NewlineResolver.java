package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.format.Style;

public final class NewlineResolver implements ComponentResolver {
  @Override
  public PreComponent resolve(String name, Arguments args, PreComponent[] children, Style inheritedStyle, MiniMessageCompiler compiler) {
    return new LiteralPreComponent("\n", inheritedStyle);
  }

  @Override
  public boolean isSelfClosing() {
    return true;
  }
  @Override
  public String[] names() {
    return new String[]{"newline", "br"};
  }
}
