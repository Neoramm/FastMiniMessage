package io.github.neoramm.fastminimessage;

/**
 * Converts legacy ampersand color/format codes into a MiniMessage string.
 *
 * <p>Optimized char-by-char scan; no regular expressions.</p>
 */
public final class LegacyFormatParser {
  private LegacyFormatParser() {
  }

  private static final String[] COLOR_NAMES = {
    "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
    "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
    "yellow", "white"
  };

  public static String toMiniMessage(final String input) {
    if (input == null) return "";
    if (input.isEmpty()) return input;
    final StringBuilder out = new StringBuilder(input.length() + 16);
    final int n = input.length();
    int i = 0;
    while (i < n) {
      final char c = input.charAt(i);
      if (c == '&' && i + 1 < n) {
        if (input.charAt(i + 1) == '#' && i + 7 < n) {
          final String hex = input.substring(i + 2, i + 8);
          if (isHex(hex)) {
            out.append("<reset><#").append(hex).append('>');
            i += 8;
            continue;
          }
        }
        if (input.charAt(i + 1) == 'x' && i + 13 < n) {
          final StringBuilder hex = new StringBuilder(6);
          boolean ok = true;
          for (int k = 0; k < 6; k++) {
            if (input.charAt(i + 2 + k * 2) != '&') { ok = false; break; }
            final char d = input.charAt(i + 3 + k * 2);
            if (!isHexChar(d)) { ok = false; break; }
            hex.append(d);
          }
          if (ok) {
            out.append("<reset><#").append(hex).append('>');
            i += 14;
            continue;
          }
        }
        final char code = Character.toLowerCase(input.charAt(i + 1));
        final int ci = hexValue(code);
        if (isColorCode(code)) {
          out.append("<reset><").append(COLOR_NAMES[ci]).append('>');
          i += 2;
          continue;
        }
        if (code == 'l' || code == 'o' || code == 'n' || code == 'm' || code == 'k') {
          out.append('<').append(formatTag(code)).append('>');
          i += 2;
          continue;
        }
        if (code == 'r') {
          out.append("<reset>");
          i += 2;
          continue;
        }
        if (code == '&') {
          out.append('&');
          i += 2;
          continue;
        }
        out.append(c);
        i++;
        continue;
      }
      if (c == '&' && i + 1 < n && input.charAt(i + 1) == '&') {
        out.append('&');
        i += 2;
        continue;
      }
      out.append(c);
      i++;
    }
    return out.toString();
  }

  private static boolean isColorCode(char c) {
    return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
  }

  private static int hexValue(char c) {
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'a' && c <= 'f') return c - 'a' + 10;
    if (c >= 'A' && c <= 'F') return c - 'A' + 10;
    return -1;
  }

  private static boolean isHexChar(char c) {
    return hexValue(c) >= 0;
  }

  private static boolean isHex(String s) {
    for (int i = 0; i < s.length(); i++) if (!isHexChar(s.charAt(i))) return false;
    return true;
  }

  private static String formatTag(char c) {
      return switch (c) {
          case 'l' -> "bold";
          case 'o' -> "italic";
          case 'n' -> "underlined";
          case 'm' -> "strikethrough";
          case 'k' -> "obfuscated";
          default -> null;
      };
  }
}
