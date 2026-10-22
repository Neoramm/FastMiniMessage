package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.StyleInheritance;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.InsertingPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.format.Style;

public final class InsertResolver implements ComponentResolver {
  @Override
  public PreComponent resolve(String name, Arguments args, PreComponent[] children, Style inheritedStyle, MiniMessageCompiler compiler) {
    String placeholder = args.get(0, "");
    final Style own = Style.style().insertion(placeholder).build();
    if (StyleInheritance.mergeIntoOnlyLiteral(children, inheritedStyle, own)) return children[0];
    return new InsertingPreComponent(placeholder, children);
  }

  @Override
  public String[] names() {
    return new String[]{"insert", "insertion"};
  }
}