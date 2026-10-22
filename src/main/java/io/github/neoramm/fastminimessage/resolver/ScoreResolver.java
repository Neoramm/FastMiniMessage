/**
 * Resolver for the ScoreResolver tag.
 */
package io.github.neoramm.fastminimessage.resolver;

import io.github.neoramm.fastminimessage.MiniMessageCompiler;
import io.github.neoramm.fastminimessage.api.ComponentResolver;
import io.github.neoramm.fastminimessage.component.StaticComponentPreComponent;
import io.github.neoramm.fastminimessage.model.Arguments;
import io.github.neoramm.fastminimessage.model.PreComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;

public final class ScoreResolver implements ComponentResolver {
  @Override
  public PreComponent resolve(String name, Arguments args, PreComponent[] children, Style inheritedStyle, MiniMessageCompiler compiler) {
    if (args.size() < 2) throw new IllegalArgumentException("A scoreboard member name and an objective name are required");
    String member = args.get(0);
    String objective = args.get(1);
    return new StaticComponentPreComponent(Component.score(member, objective));
  }

  @Override
  public String[] names() {
    return new String[]{"score"};
  }

  @Override
  public boolean isSelfClosing() {
    return true;
  }
}
