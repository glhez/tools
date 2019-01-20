package fr.glhez.jtools.warextractor.internal.filter;

import java.io.IOException;
import java.io.InputStream;

/**
 * Apply some filter to the {@link InputStream}.
 *
 * @author gael.lhez
 *
 */
public interface Filter {
  /**
   * Filter the incoming input stream using the {@code source}.
   *
   * @param stream source stream
   * @return the resulting stream
   * @throws IOException in case of error during filtering.
   */
  InputStreamWithCharset filter(InputStreamWithCharset stream) throws IOException;
}
