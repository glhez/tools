package com.github.glhez.jtools.text;

import java.util.Set;

/**
 * Matcher for XML Tags.
 *
 * @author gael.lhez
 */
class XmlTag implements RegionMatcher {

  private final Set<String> xmlTags;

  XmlTag(final Set<String> xmlTags) {
    this.xmlTags = xmlTags;
  }

  @Override
  public int regionMatches(final String line, final int start) {
    if (line.charAt(start) != '<') {
      return -1;
    }

    final var tagStart = start + 1;
    final var tagEnd = readName(line, tagStart);
    if ((tagEnd == -1) || (tagEnd < line.length() && line.charAt(tagEnd) != '>')) {
      return -1;
    }

    final var tagName = line.substring(tagStart, tagEnd);

    if (!this.xmlTags.contains(tagName)) {
      return -1;
    }
    final var endTagName = "</" + tagName + ">";
    final var end = line.indexOf(endTagName, tagEnd + 1);

    if (end == -1) {
      return -1;
    }

    return end + endTagName.length();
  }

  private int readName(final String line, final int start) {
    final var n = line.length();
    if (start < n) {
      if (isNameStart(line.charAt(start))) {
        var i = start + 1;
        while (i < n && isNamePart(line.charAt(i))) {
          ++i;
        }
        return i;
      }
    }
    return -1;
  }

  public boolean isEmpty() {
    return xmlTags.isEmpty();
  }

  /**
   * @see <a href="https://www.w3.org/TR/xml/#NT-NameStartChar">NameStartChar</a>
   */
  static boolean isNameStart(final int c) {
    return c == ':' || c >= 'A' && c <= 'Z' || c == '_' || c >= 'a' && c <= 'z' || c >= 0x000000C0 && c <= 0x000000D6
        || c >= 0x000000D8 && c <= 0x000000F6 || c >= 0x000000F8 && c <= 0x000002FF
        || c >= 0x00000370 && c <= 0x0000037D || c >= 0x0000037F && c <= 0x00001FFF
        || c >= 0x0000200C && c <= 0x0000200D || c >= 0x00002070 && c <= 0x0000218F
        || c >= 0x00002C00 && c <= 0x00002FEF || c >= 0x00003001 && c <= 0x0000D7FF
        || c >= 0x0000F900 && c <= 0x0000FDCF || c >= 0x0000FDF0 && c <= 0x0000FFFD
        || c >= 0x00010000 && c <= 0x000EFFFF;
  }

  /**
   * @see <a href="https://www.w3.org/TR/xml/#NT-NameChar">NameStart</a>
   */
  static boolean isNamePart(final int c) {
    return isNameStart(c) || c == '-' || c == '.' || c >= '0' && c <= '9' || c == 0x000000B7
        || c >= 0x00000300 && c <= 0x0000036F || c >= 0x00000203F && c <= 0x00002040;
  }
}
