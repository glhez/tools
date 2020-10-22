package com.github.glhez.jtools.jar.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVPrinter;

public class ClassPathJARProcessor extends ReportFileJARProcessor {
  private static final Pattern CLASS_PATH_SPLITTER = Pattern.compile("\\s+");
  private final Map<JARInformation, Optional<List<String>>> classPathEntries;

  public ClassPathJARProcessor(final ReportFile reportFile) {
    super("Class Path", reportFile);
    this.classPathEntries = new LinkedHashMap<>();
  }

  @Override
  public void init() {
    classPathEntries.clear();
  }

  @Override
  public void process(final ProcessorContext context, final JarFile jarFile) {
    try {
      final Optional<String> classPath = Optional.ofNullable(jarFile.getManifest())
                                                 .map(Manifest::getMainAttributes)
                                                 .map(attr -> attr.getValue(Name.CLASS_PATH));
      classPathEntries.put(context.getJARInformation(), classPath.map(CLASS_PATH_SPLITTER::split).map(Arrays::asList));
    } catch (final IOException e) {
      context.addError(e);
    }
  }

  @Override
  protected void finish(final CSVPrinter printer) throws IOException {
    printer.printRecord("File", "Class-Path Status", "Entry");
    for (final var entry : classPathEntries.entrySet()) {
      final var jar = entry.getKey();
      final var classPath = entry.getValue();
      final var cp = classPath.orElseGet(Collections::emptyList);

      if (classPath.isEmpty()) {
        printer.printRecord(jar, "Absent", "");
      } else if (cp.isEmpty()) {
        printer.printRecord(jar, "Empty", "");
      } else {
        for (final var c : cp) {
          printer.printRecord(jar, "Present", c);
        }
      }
    }
  }

}
