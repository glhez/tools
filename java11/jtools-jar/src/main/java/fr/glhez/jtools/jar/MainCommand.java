package fr.glhez.jtools.jar;

import static fr.glhez.jtools.jar.internal.MavenArtifactsJARProcessor.newCSVMavenArtifactsJARProcessor;
import static fr.glhez.jtools.jar.internal.MavenArtifactsJARProcessor.newMavenArtifactsJARProcessor;
import static fr.glhez.jtools.jar.internal.MavenArtifactsJARProcessor.newShellScriptMavenArtifactsJARProcessor;
import static java.util.stream.Collectors.joining;

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
import fr.glhez.jtools.jar.internal.ModuleJARProcessor;
import fr.glhez.jtools.jar.internal.MutableProcessorContext;
import fr.glhez.jtools.jar.internal.ReportFile;
import fr.glhez.jtools.jar.internal.SPIServiceJARProcessor;
import fr.glhez.jtools.jar.internal.ShowDuplicateClassJARProcessor;
import fr.glhez.jtools.jar.internal.ShowPackageJARProcessor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

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
  @Parameters(description = "Add a directory/files; directories are searched for JAR/WAR/EAR.")
  private List<Path> fileset;

  @Option(names = { "-i", "--include" }, description = {
      "Include file from the file system. File matched by the pattern will be added to any analysis.",
      "The pattern use java.util.regex.Pattern and can be added several times.",
      "By default the pattern match only the name of file; it may match the whole path by prefixing pattern by 'path:'" })
  private List<String> includes;

  @Option(names = { "-x", "--exclude" }, description = {
      "Exclude file from the file system. File matched by the pattern will be ignored from any analysis.",
      "The pattern use java.util.regex.Pattern and can be added several times.",
      "By default the pattern match only the name of file; it may match the whole path by prefixing pattern by 'path:'"

  })
  private List<String> excludes;

  @Option(names = { "-D",
      "--deep-scan" }, description = "Look for JAR in EAR/WAR files. The filter accepts value ALL (don't care about hierarchy) or the default STD (META-INF/lib/ only)")
  private JARFileLocator.DeepMode deepScan;

  @Option(names = { "-f", "--deep-filter" }, description = {
      "Filter embedded JAR/WAR. Path matched by the Pattern will be included.",
      "The pattern use java.util.regex.Pattern and can be added several times.",
      "By default the pattern match only the name of file; it may match the whole path by prefixing pattern by 'path:'" })
  private List<String> deepFilter;

  /*
   * processor options
   */
  @Option(names = "--all", description = { "Enable all processor." })
  private boolean allProcessor;

  @Option(names = "--maven", description = {
      "Extract information stored by Maven Archiver in /META-INF/maven/**/pom.properties.",
      "The processor will ignore JAR with multiple pom.properties (probably über jar)." })
  private boolean mavenProcessor;

  @Option(names = { "--maven-bash", "--maven-shellscript" }, description = {
      "Export Maven information as a shell script (bash oriented; other sh may work or need to be adapted).",
      "The scriplet can be used to import the dependency to a local repository.", })
  private boolean mavenShellScriptExport;

  @Option(names = { "--jnlp-permission",
      "--jnlp-permissions" }, description = "Check for permissions codebase for JNLP")
  private boolean manifestPermissionProcessor;

  @Option(names = { "--service", "--services",
      "--spi" }, description = "Search for SPI; looks for file in /META-INF/services or in Java module if available.")
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

  private CSVFormat format;

  public static void main(final String[] args) {
    picocli.CommandLine.run(new fr.glhez.jtools.jar.MainCommand(), System.out, args);
  }

  @Override
  public void run() {
    if (!canRun()) {
      return;
    }

    final ListJARProcessor processor = buildProcessor();

    try (final JARFileLocator locator = new JARFileLocator(this.deepScan, includes, excludes, this.deepFilter)) {
      locator.addFileset(this.fileset);
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
        System.out.printf("Processing file: [%6.2f%%] %s%n", 100 * (fileIndex / (double) fileCount), file.archivePath);
        ctx.setSource(file);
        try (JarFile jarFile = new JarFile(file.tmpPath.toFile(), false)) {
          processor.process(ctx, jarFile);
        } catch (final NullPointerException e) {
          throw e; // rethrow: this is probably OUR error.
        } catch (final Exception e) {
          ctx.addError(e);
        }
        ++fileIndex;
      }
      dumpErrors(ctx);
      processor.finish();
    }
  }

  private boolean canRun() {
    prepareParameters();

    final List<String> problems = new ArrayList<>();
    if (!this.mavenShellScriptExport && !this.mavenProcessor && !this.moduleProcessor && !this.serviceProcessor
        && !this.manifestPermissionProcessor && !this.manifestClassPathProcessor && !this.javaVersionProcessor
        && !this.showPackage && !this.showOnlyDuplicatePackage) {
      problems.add("no processors registered");
    }
    if (this.fileset.isEmpty()) {
      problems.add("no files registered");
    }

    if (!problems.isEmpty()) {
      System.err.println(problems.stream().collect(joining(" and ")) + "; use --help for usage.");
      return false;
    }
    return true;
  }

  private void prepareParameters() {
    this.fileset = getIfNull(fileset, Collections::emptyList);
    this.includes = getIfNull(includes, Collections::emptyList);
    this.excludes = getIfNull(excludes, Collections::emptyList);
    this.deepScan = getIfNull(deepScan, () -> DeepMode.DISABLED);
    this.deepFilter = getIfNull(deepFilter, Collections::emptyList);
    this.serviceFiltersEnabled = this.serviceFilters != null;
    this.serviceFilters = getIfNull(this.serviceFilters, Collections::emptySet);
    this.outputDirectory = getIfNull(outputDirectory, () -> Paths.get(""));

    if (allProcessor) {
      if (!this.mavenShellScriptExport && !this.mavenProcessor) {
        this.mavenProcessor = true;
        this.mavenShellScriptExport = false;
      }
      this.moduleProcessor = true;
      this.serviceProcessor = true;
      this.manifestPermissionProcessor = true;
      this.manifestClassPathProcessor = true;
      this.javaVersionProcessor = true;
      if (!this.showPackage && !this.showOnlyDuplicatePackage) {
        this.showPackage = false;
        this.showOnlyDuplicatePackage = true;
      }
      this.showDuplicateClasses = true;

    }

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
    this.format = CSVFormat.EXCEL.withDelimiter(csvSeparator);
  }

  private <T> T getIfNull(final T value, final Supplier<? extends T> supplier) {
    return null == value ? supplier.get() : value;
  }

  private ListJARProcessor buildProcessor() {
    final List<JARProcessor> processors = new ArrayList<>();

    final boolean addModuleProcessor = this.moduleProcessor || this.serviceProcessor || this.showPackage
        || this.showDuplicateClasses;

    final MavenArtifactsJARProcessor mavenArtifactsJARProcessor;
    if (mavenShellScriptExport) {
      mavenArtifactsJARProcessor = newShellScriptMavenArtifactsJARProcessor();
    } else if (mavenProcessor) {
      mavenArtifactsJARProcessor = newCSVMavenArtifactsJARProcessor(newReportFile("maven"));
    } else if (addModuleProcessor) {
      mavenArtifactsJARProcessor = newMavenArtifactsJARProcessor();
    } else {
      mavenArtifactsJARProcessor = null;
    }
    add(processors, mavenArtifactsJARProcessor);

    final ModuleJARProcessor moduleJARProcessor;
    if (moduleProcessor) {
      moduleJARProcessor = new ModuleJARProcessor(Optional.of(newReportFile("java-modules")),
          mavenArtifactsJARProcessor);
    } else if (addModuleProcessor) {
      moduleJARProcessor = new ModuleJARProcessor(Optional.empty(), mavenArtifactsJARProcessor);
    } else {
      moduleJARProcessor = null;
    }
    add(processors, moduleJARProcessor);

    if (this.serviceProcessor) {
      final var reportFile = newReportFile(serviceModuleOnly ? "services-module-only" : "services");
      add(processors, new SPIServiceJARProcessor(reportFile, moduleJARProcessor, !serviceFiltersEnabled, serviceFilters,
          serviceModuleOnly));
    }

    if (this.manifestPermissionProcessor) {
      add(processors, new JNLPPermissionsJARProcessor(newReportFile("jnlp-permissions")));
    }
    if (this.manifestClassPathProcessor) {
      add(processors, new ClassPathJARProcessor(newReportFile("class-path")));
    }
    if (this.javaVersionProcessor) {
      add(processors, new JavaVersionJARProcessor(newReportFile("java-version"), mavenArtifactsJARProcessor));
    }
    if (this.showPackage) {
      add(processors,
          new ShowPackageJARProcessor(newReportFile(showOnlyDuplicatePackage ? "duplicate-package.csv" : "package"),
              this.showOnlyDuplicatePackage, mavenArtifactsJARProcessor, moduleJARProcessor));
    }
    if (this.showDuplicateClasses) {
      add(processors, new ShowDuplicateClassJARProcessor(newReportFile("duplicate-classes"), mavenArtifactsJARProcessor,
          moduleJARProcessor));
    }

    return new ListJARProcessor(processors);
  }

  private ReportFile newReportFile(final String fileName) {
    return ReportFile.newReportFile(format, outputDirectory, fileName);
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
