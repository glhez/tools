package fr.glhez.jtools.warextractor.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

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
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (var is = Files.newInputStream(source); var bis = new BufferedInputStream(is)) {
      final Properties properties = new Properties();
      properties.load(bis);
      properties.store(bos, "filtered");
    }
    return new ByteArrayInputStream(bos.toByteArray());
  }

}
