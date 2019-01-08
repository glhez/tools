package fr.glhez.jtools.jar.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class ReportFile {
  private final CSVFormat format;
  private final Path path;

  public ReportFile(final CSVFormat format, final Path path) {
    this.format = Objects.requireNonNull(format, "format");
    this.path = Objects.requireNonNull(path, "path");
  }

  public CSVPrinter toCsvPrinter() throws IOException {
    createParentIfNeeded();
    return format.print(path, StandardCharsets.UTF_8);
  }

  @Deprecated
  public CSVPrinter toCsvPrinter(final String... headers) throws IOException {
    createParentIfNeeded();
    return format.withHeader(headers).print(path, StandardCharsets.UTF_8);
  }

  @Deprecated
  public CSVPrinter toCsvPrinter(final List<String> headers) throws IOException {
    return toCsvPrinter(headers.toArray(String[]::new));
  }

  private void createParentIfNeeded() throws IOException {
    final Path parent = this.path.getParent();
    if (null != parent) {
      Files.createDirectories(parent);
    }
  }

  @Override
  public String toString() {
    return path.toString();
  }

}
