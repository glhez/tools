package com.github.glhez.jtools.text;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

/**
 * Correspond to the start and end of some token.
 *
 * @author gael.lhez
 */
public final class BiToken implements RegionMatcher {
  public final String start;
  public final String end;
  public final String escape;

  private BiToken(final String start, final String end, final String escape) {
    this.start = requireNonNull(start, "start");
    this.end = requireNonNull(end, "end");
    this.escape = escape;
    if (this.start.isEmpty()) {
      throw new IllegalArgumentException("start is empty");
    }
    if (this.end.isEmpty()) {
      throw new IllegalArgumentException("end is empty");
    }
    if (null != escape && escape.isEmpty()) {
      throw new IllegalArgumentException("escape is empty");
    }
  }

  /**
   * Create a {@link BiToken} which does not allows escape.
   * <p>
   * Example:
   *
   * <pre>
   * BiToken xmlComment = of("%") // same as of("%", "%")
   * </pre>
   *
   * @param start
   *          start (can't be null)
   * @return a {@link BiToken}.
   */
  public static BiToken of(final String start) {
    return of(start, start);
  }

  /**
   * Create a {@link BiToken} which does not allows escape.
   * <p>
   * Example:
   *
   * <pre>
   * BiToken xmlComment = of("&lt;!--", "--&gt;")
   * </pre>
   *
   * @param start
   *          start (can't be null)
   * @param end
   *          end (can't be null)
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
   * BiToken jsQuotedString = string("'", "\\")
   * </pre>
   *
   * @param start
   *          start (can't be null)
   * @param escape
   *          escape character (can't be null)
   * @return a {@link BiToken}.
   */
  public static BiToken string(final String start, final String escape) {
    return new BiToken(start, start, requireNonNull(escape, "escape"));
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end, escape);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final BiToken other = (BiToken) obj;
    return Objects.equals(start, other.start) && Objects.equals(end, other.end) && Objects.equals(escape, other.escape);
  }

  @Override
  public int regionMatches(final String line, final int offset) {
    if (line.regionMatches(offset, start, 0, start.length())) {
      for (int initial = offset + start.length();;) {
        final int endIndex = line.indexOf(end, initial);
        if (endIndex == -1) {
          return -1; // invalid token, mismatch.
        }
        if (escape == null || !line.regionMatches(endIndex - escape.length(), escape, 0, escape.length())) {
          return endIndex + end.length();
        }
        initial = endIndex + end.length();
      }
    }
    return -1;
  }

}
