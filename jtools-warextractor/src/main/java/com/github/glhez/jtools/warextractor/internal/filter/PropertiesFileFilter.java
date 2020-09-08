package com.github.glhez.jtools.warextractor.internal.filter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Filter file as {@link Properties}.
 * <p>
 * The file will be read as a Java {@link Properties} and rendered as a UTF-8 properties with keys
 * alphabetically
 * sorted.
 *
 * @author gael.lhez
 *
 */
public enum PropertiesFileFilter implements Filter {
  INSTANCE;

  @Override
  public InputStreamWithCharset filter(final InputStreamWithCharset stream) throws IOException {
    final Properties properties = new Properties();
    if (stream.getCharset() == null) {
      properties.load(stream.getStream());
    } else {
      try (InputStreamReader reader = stream.toReader()) {
        properties.load(reader);
      }
    }

    final StringBuilder sb = new StringBuilder(8192);
    // avoid comment and the annoying "date" String; also ensure that key are sorted
    final var map = new TreeMap<String, String>();
    properties.forEach((key, value) -> map.put((String) key, (String) value));

    final String ls = System.lineSeparator();
    sb.append("# filtered").append(ls);
    for (final var entry : map.entrySet()) {
      appendConvert(sb, entry.getKey(), true);
      sb.append('=');
      appendConvert(sb, entry.getValue(), false);
      sb.append(ls);
    }

    return stream.filter(sb);
  }

  @Override
  public String toString() {
    return "Properties";
  }

  // copied from Properties::saveConvert, modified to use StringBuilder
  private String appendConvert(final StringBuilder sb, final String theString, final boolean escapeSpace) {
    final int len = theString.length();

    /*
     * make it grow first; contrary to Properties::saveConvert, we don't multiply by 2 (properties
     * are mostly ISO), and
     * we don't handle overflow (done by StringBuilder::ensureCapacity).
     */
    sb.ensureCapacity(sb.length() + len);

    for (int i = 0; i < len; i++) {
      final char c = theString.charAt(i);
      // Handle common case first, selecting largest block that
      // avoids the specials below
      if (c > 61 && c < 127) {
        if (c == '\\') {
          sb.append('\\').append('\\');
          continue;
        }
        sb.append(c);
        continue;
      }
      switch (c) {
        case ' ':
          if (i == 0 || escapeSpace) {
            sb.append('\\');
          }
          sb.append(' ');
        break;
        case '\t':
          sb.append('\\').append('t');
        break;
        case '\n':
          sb.append('\\').append('n');
        break;
        case '\r':
          sb.append('\\').append('r');
        break;
        case '\f':
          sb.append('\\').append('f');
        break;
        case '=': // Fall through
        case ':': // Fall through
        case '#': // Fall through
        case '!':
          sb.append('\\').append(c);
        break;
        default:
          sb.append(c);
      }
    }
    return sb.toString();
  }

}
