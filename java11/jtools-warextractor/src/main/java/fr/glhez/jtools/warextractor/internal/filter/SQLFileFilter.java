package fr.glhez.jtools.warextractor.internal.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import fr.glhez.jtools.warextractor.internal.ExecutionContext;

/**
 * Filter files using a SQL filter.
 * <p>
 * While the name may sound fantastic, it only removes the {@code `} used by mySQL. The resulting
 * file will not be "valid".
 *
 * @author gael.lhez
 *
 */
public class SQLFileFilter implements InputStreamFilter {

  @Override
  public InputStreamWithCharset filter(final ExecutionContext context, final InputStreamWithCharset stream)
      throws IOException {
    return stream.filter(stream.getString(StandardCharsets.UTF_8).replace("`", ""));
  }

}
