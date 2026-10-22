package io.github.neoramm.fastminimessage.model;

import io.github.neoramm.fastminimessage.component.InlinePreComponent;
import net.kyori.adventure.text.Component;

/**
 * Binds a placeholder key to a component substituted at render time.
 */
public final class Placeholder implements RuntimeResolver {
  private final String key;
  private final PreComponent preComponent;

  public Placeholder(String key, PreComponent preComponent) {
    this.key = key;
    this.preComponent = preComponent;
  }

  public String getKey() {
    return key;
  }

  public PreComponent getPreComponent() {
    return preComponent;
  }

  /**
   * Creates an unparsed placeholder that renders the given plain text.
   *
   * @param key the placeholder key
   * @param plain the plain text to render
   * @return the placeholder
   */
  public static Placeholder ofUnparsed(String key, String plain) {
    return new Placeholder(key, new InlinePreComponent(Component.text(plain)));
  }

  /**
   * Creates a parsed placeholder that renders the given component.
   *
   * @param key the placeholder key
   * @param component the component to render
   * @return the placeholder
   */
  public static Placeholder ofParsed(String key, Component component) {
    return new Placeholder(key, new InlinePreComponent(component));
  }

  public static Placeholder of(String key, PreComponent preComponent) {
    return new Placeholder(key, preComponent);
  }
}
