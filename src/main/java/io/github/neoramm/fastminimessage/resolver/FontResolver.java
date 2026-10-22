/**
 * Resolver for the FontResolver tag.
 */
package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.StyleInheritance;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.component.GroupPreComponent;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.Style;

public final class FontResolver implements ComponentResolver {
  @Override
  public PreComponent resolve(String name, Arguments args, PreComponent[] children, Style inheritedStyle, MiniMessageCompiler compiler) {
    if (args.isEmpty()) throw new IllegalArgumentException("A font tag must have either arguments of either <value> or <namespace:value>");
    final Key font;
    try {
      if (args.size() == 1) {
        font = Key.key(args.get(0));
      } else {
        font = Key.key(args.get(0), args.get(1));
      }
    } catch (InvalidKeyException ex) {
      throw new IllegalArgumentException(ex.getMessage(), ex);
    }
    final Style own = Style.style().font(font).build();
    if (StyleInheritance.mergeIntoOnlyLiteral(children, inheritedStyle, own)) return children[0];
    return new GroupPreComponent(children).setStyle(own);
  }

  @Override
  public String[] names() {
    return new String[]{"font"};
  }
}
