package fr.glhez.jtools.jar;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import fr.glhez.jtools.jar.JARFileLocator.DeepMode;
import fr.glhez.jtools.jar.MavenArtifactsJARProcessor.ExportMode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(mixinStandardHelpOptions = true, version = "JAR Tool")
public class MainCommand implements Runnable {

  /*
   * scan option
   */
  @Option(names = { "-d", "--directory" }, description = "Find all jar in this directory")
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
  @Option(names = { "-M",
      "--maven" }, description = "For each arg, produce the groupId:artifactId:version if available. ")
  private boolean mavenProcessor;

  @Option(names = "--maven-export", description = "Determine kind of export done (LIST or SCRIPT)")
  private MavenArtifactsJARProcessor.ExportMode mavenExportMode;

  @Option(names = { "-p", "--permission" }, description = "Check for permissions codebase for JNLP")
  private boolean manifestPermissionProcessor;

  @Option(names = { "-s", "--service" }, description = "Search a service (SPI) file.")
  private boolean serviceProcessor;

  @Option(names = { "-u", "--service-module" }, description = "Ignore SPI file and only process module-info.")
  private boolean serviceModuleOnly;

  private boolean serviceFiltersEnabled;

  @Option(names = "--service-filter", description = "Filter of service to search")
  private Set<String> serviceFilters;

  @Option(names = { "-c", "--class-path" }, description = "Search for Class-Path entries in Manifest.")
  private boolean manifestClassPathProcessor;

  @Option(names = { "-w",
      "--java-version" }, description = "Determine which Java version was used to compile source code (read in byte code).")
  private boolean javaVersionProcessor;

  @Option(names = { "-m", "--module" }, description = "Scan JAR for Java module-info or Automatic-Module-Name.")
  private boolean moduleProcessor;

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

      int fileIndex = 0;
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
  }

  private <T> T getIfNull(final T value, final Supplier<? extends T> supplier) {
    return null == value ? supplier.get() : value;
  }

  private ListJARProcessor buildProcessor() {
    final List<JARProcessor> processors = new ArrayList<>();

    final boolean addModuleProcessor = this.moduleProcessor || this.serviceProcessor;

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
      moduleJARProcessor = new ModuleJARProcessor(mavenArtifactsJARProcessor, !this.moduleProcessor);
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
      add(processors, new ClassPathJARProcessor());
    }
    if (this.javaVersionProcessor) {
      add(processors, new JavaVersionJARProcessor(mavenArtifactsJARProcessor));
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
