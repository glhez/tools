package fr.glhez.jtools.text;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;

/**
 * The {@link Tabulize} is text tool which produce a text aligned table given some lines.
 *
 * @author gael.lhez
 */
public class Tabulize {

  /**
   * Tabulize a set of lines using default options.
   *
   * @param lines a set of lines
   * @return the result of tabulizing the lines.
   */
  public static String tabulize(final String... lines) {
    return tabulize(TabulizeOptions.builder().build(), lines);
  }

  public static String tabulize(final TabulizeOptions options, final String[] originalLines) {
    requireNonNull(options, "options");
    requireNonNull(originalLines, "originalLines");
    for (int i = 0, n = originalLines.length; i < n; ++i) {
      if (null == originalLines[i]) {
        throw new NullPointerException("originalLines[" + i + "]");
      }
    }

    final int initialIndent = !options.detectInitialIndent ? 0 : detectInitialIndent(originalLines, options.tabSize);
    final String[] lines = trimLines(originalLines);

    return null;
  }

  /**
   * Detect the initial indent, which is the number of spaces to prepend before each lines.
   *
   * @param lines lines
   * @param tabSize size of tabs
   * @return the initial indent,
   */
  static int detectInitialIndent(final String[] lines, final int tabSize) {
    int initialIndent = 0;
    for (final String line : lines) {
      final int lineInitialIndent = detectInitialIndent(line, tabSize);
      if (lineInitialIndent > initialIndent) {
        initialIndent = lineInitialIndent;
      }
    }

    return initialIndent;
  }

  /**
   * Detect initial indent of one line.
   *
   * @param line line to check
   * @param tabSize
   * @return
   */
  static int detectInitialIndent(final String line, final int tabSize) {
    for (int i = 0, n = line.length(), indent = 0; i < n; ++i) {
      final char c = line.charAt(i);
      if (c == '\t') {
        indent += tabSize;
      } else if (c == ' ') {
        ++indent;
      } else {
        return indent;
      }
    }
    return 0; // empty line
  }

  static String[] trimLines(final String[] originalLines) {
    return Arrays.stream(originalLines).map(String::trim).toArray(String[]::new);
  }
}
