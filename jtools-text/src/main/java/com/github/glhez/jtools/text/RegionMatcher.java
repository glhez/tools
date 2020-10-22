package com.github.glhez.jtools.text;

public interface RegionMatcher {
  /**
   * Test whether the region, starting at {@code offset}, match this particular token.
   *
   * @param line
   *          current line
   * @param offset
   *          starting point
   * @return position of last character of the region or {@code -1} if the region does not match.
   */
  int regionMatches(final String line, final int offset);
}
