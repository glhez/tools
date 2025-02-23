package com.github.glhez.jtools.warextractor.internal.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Filter files using a SQL filter.
 * <p>
 * While the name may sound fantastic, it only removes the {@code `} used by mySQL. The resulting
 * file will not be "valid".
 *
 * @author gael.lhez
 *
 */
@SuppressWarnings("java:S6548")
public enum SQLFileFilter implements Filter {
  INSTANCE;

  @Override
  public InputStreamWithCharset filter(final InputStreamWithCharset stream) throws IOException {
    return stream.filter(stream.getString(StandardCharsets.UTF_8).replace("`", ""));
  }

  @Override
  public String toString() {
    return "SQL";
  }
}
