package fr.glhez.jtools.warextractor.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.TreeMap;

public class PropertiesFileFilter implements FileFilter {
  private final ExecutionContext context;
  private final Path source;

  public PropertiesFileFilter(final ExecutionContext context, final Path source) {
    this.context = context;
    this.source = source;
  }

  @Override
  public InputStream getFilteredInputStream() throws IOException {
    context.verbose(() -> String.format("filtering [%s] using properties", source));
    final StringBuilder bos = new StringBuilder(8192);
    final String ls = System.lineSeparator();
    try (var is = Files.newInputStream(source); var bis = new BufferedInputStream(is)) {
      final Properties properties = new Properties();
      properties.load(bis);

      // avoid comment and the annoying "date" String; also ensure that key are sorted
      final var map = new TreeMap<String, String>();
      properties.forEach((key, value) -> map.put((String) key, (String) value));

      bos.append("# filtered").append(ls);
      for (final var entry : map.entrySet()) {
        appendConvert(bos, entry.getKey(), true);
        bos.append('=');
        appendConvert(bos, entry.getValue(), false);
        bos.append(ls);
      }

    }
    return new ByteArrayInputStream(bos.toString().getBytes(StandardCharsets.UTF_8));
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
