package fr.glhez.jtools.text;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;

/**
 * The {@link Tabulizer} is text tool which produce a text aligned table given some lines.
 *
 * @author gael.lhez
 */
public class Tabulizer {

  private final TabulizerOptions options;

  Tabulizer(final TabulizerOptions options) {
    this.options = requireNonNull(options, "options");
  }

  /**
   * Tabulize a set of lines using default options.
   *
   * @param lines a set of lines
   * @return lines padded with spaces to represent an ASCII table, without delimiters such as
   *         {@code !} or any other unicode character.
   */
  public static String tabulize(final String... lines) {
    return tabulize(TabulizerOptions.builder().build(), lines);
  }

  /**
   * Tabulize a set of lines using some options.
   * <p>
   * The method does nothing if length of {@code lines} is not greater than one (1).
   *
   * @param options options to use
   * @param lines a set of lines.
   * @return lines padded with spaces to represent an ASCII table, without delimiters such as
   *         {@code !} or any other Unicode character.
   */
  public static String tabulize(final TabulizerOptions options, final String... lines) {
    requireNonNull(options, "options");
    requireNonNull(lines, "lines");
    for (int i = 0, n = lines.length; i < n; ++i) {
      if (null == lines[i]) {
        throw new NullPointerException("lines[" + i + "]");
      }
    }
    if (lines.length == 0) {
      return "";
    }
    if (lines.length == 1) {
      return lines[0];
    }
    return options.toTabulizer().format(lines);
  }

  private String format(final String[] originalLines) {
    final int initialIndent = !options.detectInitialIndent ? 0 : detectInitialIndent(originalLines, options.tabSize);
    // TODO gael.lhez remove this: the trim is done by detectColumns.
    final String[] lines = trimLines(originalLines);

    final Row[] rows = detectColumns(lines);
    final String indent = indent(initialIndent);
    final String lineSeparator = options.lineSeparator.toString();

    // TODO gael.lhez finish the work
    final StringBuilder sb = new StringBuilder();
    for (final String line : lines) {
      sb.append(indent).append(line).append(lineSeparator);
    }
    return sb.toString();
  }

  /**
   * Detect a series of rows.
   *
   * @param lines a set of lines
   * @return a Row[] containing as much rows than there is lines.
   */
  Row[] detectColumns(final String[] lines) {
    final Row[] rows = new Row[lines.length];
    for (int i = 0; i < rows.length; ++i) {
      rows[i] = detectColumns(lines[i]);
    }
    return rows;
  }

  /**
   * Effectively split a line into a set of column, using the current options.
   * <p>
   * The delimiter is at least the whitespace; options marked by {@link TabulizeColumnFinder} will
   * affect this method.
   *
   * @param line
   * @return
   */
  Row detectColumns(final String line) {
    // additionalNumberToken
    // detectNumber
    // keywordCaseInsensitive
    // keywords
    // lineComment
    // multilineComment
    // string1
    // string2
    // xmlTags
    final Row row = new Row();

    /*
     * if we selected something, use a big fat algorithm; otherwise, use non whitespace transition.
     */
    if (options.detectNumber || !options.keywords.isEmpty() || options.lineComment != null
        || options.multilineComment != null || options.string1 != null || options.string1 != null
        || !options.xmlTags.isEmpty()) {
      // NOOP (for now)
    } else {
      for (int i = 0, n = line.length(); i < n; ) {
        // advance WS.
        while ( i < n && Character.isWhitespace(line.charAt(i))) {
          ++i;
        }

        // advance NWS
        final int start = i;
        while ( i < n && !Character.isWhitespace(line.charAt(i))) {
          ++i;
        }

        if (start != i) {
          row.add(new DefaultColumn(line.substring(start, i)));
        }
      }
    }


    return row;
  }

  /**
   * Build the indent {@link String}.
   * <p>
   * The indent {@link String} only contains spaces.
   *
   * @param indent indent value.
   */
  private static String indent(final int indent) {
    final char[] spaces = new char[indent];
    Arrays.fill(spaces, ' ');
    return new String(spaces);
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
