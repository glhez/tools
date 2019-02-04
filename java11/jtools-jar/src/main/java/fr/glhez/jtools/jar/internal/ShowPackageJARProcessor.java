package fr.glhez.jtools.jar.internal;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.csv.CSVPrinter;

public class ShowPackageJARProcessor extends ReportFileJARProcessor {
  private final MavenArtifactsJARProcessor mavenArtifactsJARProcessor;
  private final ModuleJARProcessor moduleJARProcessor;
  private final Map<JARInformation, NavigableSet<String>> packagesPerJar;
  private final boolean showDuplicatePackage;

  public ShowPackageJARProcessor(final ReportFile reportFile, final boolean showDuplicatePackage,
      final MavenArtifactsJARProcessor mavenArtifactsJARProcessor, final ModuleJARProcessor moduleJARProcessor) {
    super("Package in JARs", reportFile);
    this.showDuplicatePackage = showDuplicatePackage;
    this.mavenArtifactsJARProcessor = Objects.requireNonNull(mavenArtifactsJARProcessor, "mavenArtifactsJARProcessor");
    this.moduleJARProcessor = Objects.requireNonNull(moduleJARProcessor, "moduleJARProcessor");
    this.packagesPerJar = new LinkedHashMap<>();
  }

  @Override
  public void init() {
    packagesPerJar.clear();
  }

  @Override
  public void process(final ProcessorContext context, final JarFile jarFile) {
    try (final var ss = jarFile.stream()) {
      final TreeSet<String> packages = ss.filter(ShowPackageJARProcessor::isClassFileEntry).map(this::splitName)
          .collect(toCollection(TreeSet::new));
      packagesPerJar.put(context.getJARInformation(), packages);
    }
  }

  /**
   * Filter a class file (which is expected to contains a Java class).
   * <p>
   * The definition is shared with {@link ShowClassJARProcessor}.
   *
   * @param entry a jar entry
   * @return <code>true</code> if the entry correspond to a Java classes.
   */
  static boolean isClassFileEntry(final JarEntry entry) {
    // use the .class rather than the directory: this will lessen the false positive.
    final String name = entry.getName();
    return name.endsWith(".class") //
        && !"module-info.class".equals(name) // ignore module (this will produce "EMPTY" package)
        && !name.startsWith("META-INF/") //
        && !name.startsWith("WEB-INF/") //
    ;
  }

  private String splitName(final JarEntry entry) {
    final String name = entry.getName();
    final int n = name.lastIndexOf('/');
    if (n == -1) {
      return "";
    }
    return name.substring(0, n).replace('/', '.');
  }

  @Override
  protected void finish(final CSVPrinter printer) throws IOException {
    // compute duplicate per package
    final Map<String, Long> counters = this.packagesPerJar.values().stream().flatMap(Set::stream)
        .collect(groupingBy(Function.identity(), counting()));

    printer.printRecord("JAR", "GAV", "Module", "Package", "Number of package references in all JARs");
    for (final var entry : this.packagesPerJar.entrySet()) {
      final JARInformation jar = entry.getKey();
      final NavigableSet<String> packages = entry.getValue();
      final String gav = mavenArtifactsJARProcessor.getGAVAsString(jar);
      final String module = moduleJARProcessor.getModuleDescriptorAsString(jar);
      final String info = jar.toString();

      for (final var packageName : packages) {
        final Long counter = counters.getOrDefault(packageName, 1L);
        if (!showDuplicatePackage || counter > 1) {
          printer.printRecord(info, gav, module, packageName.isEmpty() ? "<EMPTY>" : packageName, counter);
        }
      }
    }
  }

}
