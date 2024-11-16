package com.github.glhez.jtools.jar.internal;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.csv.CSVPrinter;

public class ShowClassJARProcessor extends ReportFileJARProcessor {
  private final MavenArtifactsJARProcessor mavenArtifactsJARProcessor;
  private final ModuleJARProcessor moduleJARProcessor;
  private final Map<String, NavigableSet<JARInformation>> classesPerJAR;

  private final boolean showOnlyDuplicateClasses;

  public ShowClassJARProcessor(final ReportFile reportFile, final boolean showOnlyDuplicateClasses,
      final MavenArtifactsJARProcessor mavenArtifactsJARProcessor, final ModuleJARProcessor moduleJARProcessor) {
    super("Class in JAR set", reportFile);
    this.mavenArtifactsJARProcessor = Objects.requireNonNull(mavenArtifactsJARProcessor, "mavenArtifactsJARProcessor");
    this.moduleJARProcessor = Objects.requireNonNull(moduleJARProcessor, "moduleJARProcessor");
    this.showOnlyDuplicateClasses = showOnlyDuplicateClasses;
    classesPerJAR = new LinkedHashMap<>();
  }

  @Override
  public void init() {
    classesPerJAR.clear();
  }

  @Override
  public void process(final ProcessorContext context, final JarFile jarFile) {
    try (final var ss = jarFile.stream()) {
      ss.filter(ShowPackageJARProcessor::isClassFileEntry).map(JarEntry::getName).forEach(name -> {
        classesPerJAR.computeIfAbsent(name, n -> new TreeSet<>()).add(context.getJARInformation());
      });
    }

  }

  @Override
  protected void finish(final CSVPrinter printer) throws IOException {
    printer.printRecord("JAR", "GAV", "Module", "Class", "Number of classes references in all JARs");
    for (final var entry : classesPerJAR.entrySet()) {
      final var className = entry.getKey();
      final var jars = entry.getValue();

      if (!showOnlyDuplicateClasses || jars.size() > 1) {
        for (final var jar : jars) {
          final var gav = mavenArtifactsJARProcessor.getGAVAsString(jar);
          final var module = moduleJARProcessor.getModuleDescriptorAsString(jar);
          final var info = jar.toString();
          printer.printRecord(info, gav, module, className, jars.size());
        }
      }
    }
  }

}
