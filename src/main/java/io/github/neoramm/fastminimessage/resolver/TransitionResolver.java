/**
 * Resolver for the TransitionResolver tag.
 */
package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.StyleInheritance;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.GroupPreComponent;
import io.github.neoramm.fastminimessage.component.LiteralPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TransitionResolver implements ComponentResolver {
  @Override
  public PreComponent resolve(String name, Arguments args, PreComponent[] children, Style inheritedStyle, MiniMessageCompiler compiler) {
    if (args.isEmpty()) {
      return new LiteralPreComponent("", inheritedStyle);
    }
    float phase = 0.0f;
    final List<TextColor> colors = new ArrayList<>();
    for (int i = 0; i < args.size(); i++) {
      String raw = args.get(i);
      TextColor color = TextColor.fromHexString(raw);
      if (color == null) color = NamedTextColor.NAMES.value(raw);
      if (color != null) {
        colors.add(color);
      } else {
        if (i == args.size() - 1) {
          try {
            phase = Float.parseFloat(raw);
            if (phase < -1.0f || phase > 1.0f) throw new IllegalArgumentException("out of range");
          } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unable to parse a color from '" + raw + "'");
          }
        } else {
          throw new IllegalArgumentException("Unable to parse a color from '" + raw + "'");
        }
      }
    }
    if (colors.size() < 2) throw new IllegalArgumentException("Invalid transition, not enough colors.");
    TextColor[] arr = colors.toArray(new TextColor[0]);
  
    boolean negative = phase < 0;
    float p = negative ? 1 + phase : phase;
    if (negative) {

      List<TextColor> rev = new ArrayList<>(colors);
      Collections.reverse(rev);
      arr = rev.toArray(new TextColor[0]);
    }
    TextColor result = null;
    float steps = 1.0f / (arr.length - 1);
    int idx = 1;
    for (; idx < arr.length; idx++) {
      float val = idx * steps;
      if (val >= p) {
        float factor = 1 + (p - val) * (arr.length - 1);
        if (negative) {
          result = TextColor.lerp(1 - factor, arr[idx], arr[idx - 1]);
        } else {
          result = TextColor.lerp(factor, arr[idx - 1], arr[idx]);
        }
        break;
      }
    }
    if (idx >= arr.length) result = arr[0];
    final Style own = Style.style().color(result).build();
    if (StyleInheritance.mergeIntoOnlyLiteral(children, inheritedStyle, own)) return children[0];

    if (children.length == 0) return new LiteralPreComponent("", own);
    return new GroupPreComponent(children).setStyle(own);
  }

  @Override
  public String[] names() {
    return new String[]{"transition"};
  }

  @Override
  public boolean isSelfClosing() {
    return true;
  }
}
