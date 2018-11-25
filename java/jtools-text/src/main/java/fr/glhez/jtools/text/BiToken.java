package fr.glhez.jtools.text;

import static java.util.Objects.requireNonNull;

/**
 * Correspond to the start and end of some token.
 *
 * @author gael.lhez
 */
public class BiToken {
  public final String start;
  public final String end;
  public final String escape;

  private BiToken(final String start, final String end, final String escape) {
    this.start = requireNonNull(start, "start");
    this.end = requireNonNull(end, "end");
    this.escape = escape;
  }

  /**
   * Create a {@link BiToken} which does not allows escape.
   * <p>
   * Example:
   * <pre>
   * BiToken xmlComment = of("&lt;!--", "--&gt;")
   * </pre>
   *
   * @param start start (can't be null)
   * @param end end (can't be null)
   * @return a {@link BiToken}.
   */
  public static BiToken of(final String start, final String end) {
    return new BiToken(start, end, null);
  }
  /**
   * Create a {@link BiToken} which allows escape.
   * <p>
   * Example:
   *
   * <pre>
   * BiToken jsQuotedString = of("'", "'", "\\")
   * </pre>
   *
   * @param start start (can't be null)
   * @param end end (can't be null)
   * @return a {@link BiToken}.
   */
  public static BiToken of(final String start, final String end, final String escape) {
    return new BiToken(start, end, requireNonNull(escape, "escape"));
  }

}
