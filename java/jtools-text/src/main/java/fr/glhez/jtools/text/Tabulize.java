package fr.glhez.jtools.text;

import static java.util.Objects.requireNonNull;

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

  public static String tabulize(final TabulizeOptions options, final String[] lines) {
    requireNonNull(options, "options");
    requireNonNull(lines, "lines");
    return null;
  }
}
