package fr.glhez.jtools.jar;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;

import fr.glhez.jtools.jar.internal.ClassPathJARProcessor;
import fr.glhez.jtools.jar.internal.JARFileLocator;
import fr.glhez.jtools.jar.internal.JARFileLocator.DeepMode;
import fr.glhez.jtools.jar.internal.JARInformation;
import fr.glhez.jtools.jar.internal.JARProcessor;
import fr.glhez.jtools.jar.internal.JNLPPermissionsJARProcessor;
import fr.glhez.jtools.jar.internal.JavaVersionJARProcessor;
import fr.glhez.jtools.jar.internal.ListJARProcessor;
import fr.glhez.jtools.jar.internal.MavenArtifactsJARProcessor;
import fr.glhez.jtools.jar.internal.MavenArtifactsJARProcessor.ExportMode;
import fr.glhez.jtools.jar.internal.ModuleJARProcessor;
import fr.glhez.jtools.jar.internal.MutableProcessorContext;
import fr.glhez.jtools.jar.internal.ReportFile;
import fr.glhez.jtools.jar.internal.SPIServiceJARProcessor;
import fr.glhez.jtools.jar.internal.ShowDuplicateClassJARProcessor;
import fr.glhez.jtools.jar.internal.ShowPackageJARProcessor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(mixinStandardHelpOptions = true, version = "JAR Tool")
public class MainCommand implements Runnable {

  @Option(names = { "-O",
      "--output-directory" }, description = "Output directory when using reports. Created if it does not exists.")
  private Path outputDirectory;

  @Option(names = "--csv-separator", description = "CSV Separator to use. Default depends on locale (eg: FRENCH + GERMAN = ; otherwise ,).")
  private Character csvSeparator;

  /*
   * scan option
   */
  @Option(names = { "-d", "--directory" }, description = "Add a directory in which to scan for JARs.")
  private List<Path> directories;

  @Option(names = { "-j", "--jar" }, description = "Add a specific jar")
  private List<Path> jars;

  @Option(names = { "-i", "--include" }, description = {
      "Include file from the file system. File matched by the pattern will be added to any analysis.",
      "The pattern use java.util.regex.Pattern and can be added several times." })
  private List<Pattern> includes;

  @Option(names = { "-x", "--exclude" }, description = {
      "Exclude file from the file system. File matched by the pattern will be ignored from any analysis.",
      "The pattern use java.util.regex.Pattern and can be added several times." })
  private List<Pattern> excludes;

  @Option(names = { "-D",
      "--deep-scan" }, description = "Look for JAR in EAR/WAR files. The filter accepts value ALL (don't care about hierarchy) or the default STD (META-INF/lib/ only)")
  private JARFileLocator.DeepMode deepScan;

  @Option(names = { "-f", "--deep-filter" }, description = {
      "Filter embedded JAR/WAR. Path matched by the Pattern will be included.",
      "The pattern use java.util.regex.Pattern and can be added several times." })
  private List<Pattern> deepFilter;

  /*
   * processor options
   */
  @Option(names = "--maven", description = "Try to read information produced by Maven Archiver in /META-INF/maven/**/pom.properties. Might fail if there are multiple pom.properties.")
  private boolean mavenProcessor;

  @Option(names = "--maven-export", description = "Export GAV information as LIST (for humans), SCRIPT (for example, to upload on a local repository) or NONE")
  private MavenArtifactsJARProcessor.ExportMode mavenExportMode;

  @Option(names = "--permission", description = "Check for permissions codebase for JNLP")
  private boolean manifestPermissionProcessor;

  @Option(names = "--service", description = "Search for SPI; looks for file in /META-INF/services or in Java module if available.")
  private boolean serviceProcessor;

  @Option(names = "--service-module", description = "Ignore SPI file and only process module-info.")
  private boolean serviceModuleOnly;

  private boolean serviceFiltersEnabled;

  @Option(names = "--service-filter", description = "A list of interface to look for unsing SPI.")
  private Set<String> serviceFilters;

  @Option(names = "--class-path", description = "Search for Class-Path entries in Manifest.")
  private boolean manifestClassPathProcessor;

  @Option(names = "--java-version", description = "Determine which Java version was used to compile source code (read in bytecode).")
  private boolean javaVersionProcessor;

  @Option(names = { "--module", "--modules" }, description = "Scan JAR for Java module-info or Automatic-Module-Name.")
  private boolean moduleProcessor;

  @Option(names = { "--package", "--packages" }, description = "Show root packages contained in JAR.")
  private boolean showPackage;

  @Option(names = { "--duplicate-package",
      "--duplicate-packages" }, description = "Apply --package; show only package found in several JARs meaning probable problems with Java Modules.")
  private boolean showOnlyDuplicatePackage;

  @Option(names = { "--duplicate-class", "--duplicate-classes" }, description = "Show duplicate classes in JAR.")
  private boolean showDuplicateClasses;

  private Optional<ReportFile> moduleReportFile;
  private ReportFile classPathReportFile;
  private ReportFile javaVersionReportFile;
  private ReportFile packageReportFile;
  private ReportFile duplicateClassesReportFile;

  public static void main(final String[] args) {
    picocli.CommandLine.run(new fr.glhez.jtools.jar.MainCommand(), System.out, args);
  }

  @Override
  public void run() {
    prepareParameters();

    final ListJARProcessor processor = buildProcessor();

    try (final JARFileLocator locator = new JARFileLocator(this.deepScan, includes, excludes, this.deepFilter)) {
      locator.addFiles(this.jars);
      locator.addDirectories(this.directories);
      if (locator.hasErrors()) {
        System.err.println("Some file or directories could not be fetched:");
        locator.getErrors().forEach(System.err::println);
      }
      final Set<JARInformation> files = locator.getFiles();

      final MutableProcessorContext ctx = new MutableProcessorContext();
      processor.init();

      int fileIndex = 1;
      final int fileCount = files.size();
      for (final JARInformation file : files) {
        System.out.printf("Processing file: [%6.2f%%] %s%n", 100 * (fileIndex / (double) fileCount), file.source);
        ctx.setSource(file);
        try (JarFile jarFile = new JarFile(file.tmpPath.toFile())) {
          processor.process(ctx, jarFile);
        } catch (final Exception e) {
          ctx.addError(e);
        }
        ++fileIndex;
      }
      dumpErrors(ctx);
      processor.finish();
    }
  }

  private void prepareParameters() {
    this.directories = getIfNull(directories, Collections::emptyList);
    this.jars = getIfNull(jars, Collections::emptyList);
    this.includes = getIfNull(includes, Collections::emptyList);
    this.excludes = getIfNull(excludes, Collections::emptyList);
    this.deepScan = getIfNull(deepScan, () -> DeepMode.DISABLED);
    this.deepFilter = getIfNull(deepFilter, Collections::emptyList);
    this.serviceFiltersEnabled = this.serviceFilters != null;
    this.serviceFilters = getIfNull(this.serviceFilters, Collections::emptySet);
    this.mavenExportMode = getIfNull(mavenExportMode, () -> ExportMode.LIST);

    if (showOnlyDuplicatePackage) {
      showPackage = true;
    }

    if (null == csvSeparator) {
      final Locale locale = Locale.getDefault();
      if (Locale.FRANCE.equals(locale) || Locale.GERMANY.equals(locale)) {
        csvSeparator = ';';
      } else {
        csvSeparator = ',';
      }
    }
    final CSVFormat format = CSVFormat.EXCEL.withDelimiter(csvSeparator);

    final Path ood = Optional.ofNullable(outputDirectory).orElseGet(() -> Paths.get(""));

    moduleReportFile = moduleProcessor ? Optional.of(new ReportFile(format, ood.resolve("java-modules.csv")))
        : Optional.empty();
    classPathReportFile = new ReportFile(format, ood.resolve("class-path.csv"));
    javaVersionReportFile = new ReportFile(format, ood.resolve("java-version.csv"));
    packageReportFile = new ReportFile(format,
        ood.resolve(showOnlyDuplicatePackage ? "duplicate-package.csv" : "package.csv"));
    duplicateClassesReportFile = new ReportFile(format, ood.resolve("duplicate-classes"));
  }

  private <T> T getIfNull(final T value, final Supplier<? extends T> supplier) {
    return null == value ? supplier.get() : value;
  }

  private ListJARProcessor buildProcessor() {
    final List<JARProcessor> processors = new ArrayList<>();

    final boolean addModuleProcessor = this.moduleProcessor || this.serviceProcessor || this.showPackage
        || this.showDuplicateClasses;

    final MavenArtifactsJARProcessor mavenArtifactsJARProcessor;
    if (mavenProcessor) {
      mavenArtifactsJARProcessor = new MavenArtifactsJARProcessor(mavenExportMode);
    } else if (addModuleProcessor) {
      mavenArtifactsJARProcessor = new MavenArtifactsJARProcessor(ExportMode.NONE);
    } else {
      mavenArtifactsJARProcessor = null;
    }
    add(processors, mavenArtifactsJARProcessor);

    final ModuleJARProcessor moduleJARProcessor;
    if (addModuleProcessor) {
      moduleJARProcessor = new ModuleJARProcessor(moduleReportFile, mavenArtifactsJARProcessor);
    } else {
      moduleJARProcessor = null;
    }
    add(processors, moduleJARProcessor);

    if (this.serviceProcessor) {
      add(processors,
          new SPIServiceJARProcessor(moduleJARProcessor, !serviceFiltersEnabled, serviceFilters, serviceModuleOnly));
    }

    if (this.manifestPermissionProcessor) {
      add(processors, new JNLPPermissionsJARProcessor());
    }
    if (this.manifestClassPathProcessor) {
      add(processors, new ClassPathJARProcessor(classPathReportFile));
    }
    if (this.javaVersionProcessor) {
      add(processors, new JavaVersionJARProcessor(javaVersionReportFile, mavenArtifactsJARProcessor));
    }
    if (this.showPackage) {
      add(processors, new ShowPackageJARProcessor(packageReportFile, this.showOnlyDuplicatePackage,
          mavenArtifactsJARProcessor, moduleJARProcessor));
    }
    if (this.showDuplicateClasses) {
      add(processors, new ShowDuplicateClassJARProcessor(duplicateClassesReportFile, mavenArtifactsJARProcessor,
          moduleJARProcessor));
    }

    return new ListJARProcessor(processors);
  }

  private void add(final List<JARProcessor> processors, final JARProcessor processor) {
    if (null != processor) {
      processors.add(processor);
    }
  }

  private void dumpErrors(final MutableProcessorContext ctx) {
    final Map<JARInformation, List<String>> errors = ctx.getErrors();
    if (!errors.isEmpty()) {
      System.err.println("-------------------------------");
      System.err.println("There was " + errors.size() + " errors:");
      errors.forEach((information, messages) -> {
        final int n = messages.size();
        if (n == 1) {
          System.err.printf("  %s: %s%n", information, messages.get(0));
        } else if (n > 1) {
          System.err.printf("  %s:%n", information);
          messages.forEach(message -> System.err.printf("  %s%n", message));
        }
      });
    }
  }
}
