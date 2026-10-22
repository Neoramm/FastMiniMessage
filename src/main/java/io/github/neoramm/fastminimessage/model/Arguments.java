package io.github.neoramm.fastminimessage.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable parsed arguments of a tag.
 */
public final class Arguments {

  public static final Arguments EMPTY = new Arguments(new String[0]);

  private final String[] args;

  private Arguments(String[] args) {
    this.args = args;
  }

  /**
   * Parses arguments split by {@code ':'}, honouring single and double quotes.
   *
   * @param rawArgs the raw argument string
   * @return the parsed arguments
   */
  public static Arguments parse(String rawArgs) {
    return parse(rawArgs, ':');
  }

  public static Arguments parse(String rawArgs, char delimiter) {
    if (rawArgs == null || rawArgs.isEmpty()) {
      return EMPTY;
    }
    List<String> result = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    char quote = 0;
    for (int i = 0; i < rawArgs.length(); i++) {
      char c = rawArgs.charAt(i);
      if (quote == 0 && (c == '\'' || c == '"')) {
        quote = c;
      } else if (quote != 0 && c == quote) {
        quote = 0;
      } else if (c == delimiter && quote == 0) {
        result.add(current.toString());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    if (!current.isEmpty()) {
      result.add(current.toString());
    }
    return new Arguments(result.toArray(new String[0]));
  }

  public String get(int index, String defaultValue) {
    return index >= 0 && index < args.length ? args[index] : defaultValue;
  }

  public String get(int index) {
    return get(index, null);
  }

  public int getInt(int index, int defaultValue) {
    String val = get(index);
    if (val == null) return defaultValue;
    try {
      return Integer.parseInt(val);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public float getFloat(int index, float defaultValue) {
    String val = get(index);
    if (val == null) return defaultValue;
    try {
      return Float.parseFloat(val);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public boolean isEmpty() {
    return args.length == 0;
  }

  public int size() {
    return args.length;
  }
}