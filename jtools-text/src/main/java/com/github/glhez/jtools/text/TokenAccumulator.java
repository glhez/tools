package com.github.glhez.jtools.text;

/**
 * Accumulate character for token not being "specials".
 * <p>
 * These characters form a token of non whitespace characters.
 *
 * @author gael.lhez
 */
public class TokenAccumulator {
  static final int INVALID_OFFSET = -1;

  private int offset;

  public TokenAccumulator() {
    this.offset = -1;
  }

  /**
   * Reset the position eventually generating a new column.
   *
   * @param line containing the accumulated token
   * @param end end position (excluded)
   */
  public Column reset(final String line, final int end) {
    final int offset = this.offset;
    this.offset = INVALID_OFFSET;
    if (offset == INVALID_OFFSET || offset == end) {
      return null;
    }
    return new DefaultColumn(line.substring(offset, end));

  }

  /**
   * Reset the position eventually generating a new column.
   *
   * @param line containing the accumulated token
   */
  public Column finish(final String line) {
    return reset(line, line.length());
  }

  /**
   * Start a new token.
   * <p>
   * If the {@code offset} is {@link #INVALID_OFFSET}, then the position is reset to this one.
   * Otherwise, the method does nothing.
   *
   * @param index position in line for the new token.
   */
  public void start(final int index) {
    if (this.offset == INVALID_OFFSET) {
      this.offset = index;
    }
  }

}
