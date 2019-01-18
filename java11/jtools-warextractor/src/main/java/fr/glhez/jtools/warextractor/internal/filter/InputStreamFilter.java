package fr.glhez.jtools.warextractor.internal.filter;

import java.io.IOException;
import java.io.InputStream;

import fr.glhez.jtools.warextractor.internal.ExecutionContext;

/**
 * Apply some filter to the {@link InputStream}.
 *
 * @author gael.lhez
 *
 */
public interface InputStreamFilter {
  /**
   * Filter the incoming input stream using the {@code source}.
   *
   * @param context parent context (mostly for verbosity)
   * @param stream source stream
   * @return the resulting stream
   * @throws IOException in case of error during filtering.
   */
  InputStreamWithCharset filter(ExecutionContext context, InputStreamWithCharset stream) throws IOException;
}
