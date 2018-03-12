package fr.glhez.jtools.jar;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public class ClassPathJARProcessor implements JARProcessor {
  private static final Pattern CLASS_PATH_SPLITTER = Pattern.compile("\\s+");
  private final Map<JARInformation, Optional<List<String>>> classPathEntries;

  public ClassPathJARProcessor() {
    this.classPathEntries = new LinkedHashMap<>();
  }

  @Override
  public void init() {
    classPathEntries.clear();
  }

  @Override
  public void process(final ProcessorContext context, final JarFile jarFile) {
    try {
      final String classPath = jarFile.getManifest().getMainAttributes().getValue(Name.CLASS_PATH);
      classPathEntries.put(context.getJARInformation(),
          Optional.ofNullable(classPath).map(CLASS_PATH_SPLITTER::split).map(Arrays::asList));
    } catch (final IOException e) {
      context.addError(e);
    }
  }

  @Override
  public void finish() {
    System.out.println("Class-Path entries per jar: ");
    classPathEntries.forEach((jar, classPath) -> {
      System.out.print("File: " + jar);
      if (!classPath.isPresent()) {
        System.out.println(" (no Class-Path entries)");
      }
      classPath.ifPresent(entries -> {
        System.out.println(" (" + entries.size() + ")");
        entries.forEach(entry -> {
          System.out.println("  " + entry);
        });
      });
    });
  }

}
