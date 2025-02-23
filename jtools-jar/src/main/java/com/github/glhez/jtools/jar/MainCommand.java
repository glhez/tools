package com.github.glhez.jtools.jar;

import static com.github.glhez.jtools.jar.internal.MavenArtifactsJARProcessor.newCSVMavenArtifactsJARProcessor;
import static com.github.glhez.jtools.jar.internal.MavenArtifactsJARProcessor.newMavenArtifactsJARProcessor;
import static com.github.glhez.jtools.jar.internal.MavenArtifactsJARProcessor.newShellScriptMavenArtifactsJARProcessor;
import static java.util.stream.Collectors.joining;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import org.apache.commons.csv.CSVFormat;

import com.github.glhez.jtools.jar.internal.ClassPathJARProcessor;
import com.github.glhez.jtools.jar.internal.JARFileLocator;
import com.github.glhez.jtools.jar.internal.JARFileLocator.DeepMode;
import com.github.glhez.jtools.jar.internal.JARInformation;
import com.github.glhez.jtools.jar.internal.JARProcessor;
import com.github.glhez.jtools.jar.internal.JNLPPermissionsJARProcessor;
import com.github.glhez.jtools.jar.internal.JavaVersionJARProcessor;
import com.github.glhez.jtools.jar.internal.ListJARProcessor;
import com.github.glhez.jtools.jar.internal.MavenArtifactsJARProcessor;
import com.github.glhez.jtools.jar.internal.ModuleJARProcessor;
import com.github.glhez.jtools.jar.internal.MutableProcessorContext;
import com.github.glhez.jtools.jar.internal.ReportFile;
import com.github.glhez.jtools.jar.internal.SPIServiceJARProcessor;
import com.github.glhez.jtools.jar.internal.ShowClassJARProcessor;
import com.github.glhez.jtools.jar.internal.ShowPackageJARProcessor;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true)
@SuppressWarnings("java:S106")
public class MainCommand implements Runnable {

  @Option(names = { "-O", "--output-directory" },
          description = "Output directory when using reports. Created if it does not exists.")
  private Path outputDirectory;

  @Option(names = "--csv-separator",
          description = "CSV Separator to use. Default depends on locale (eg: FRENCH + GERMAN = ; otherwise ,).")
  private Character csvSeparator;

  /*
   * scan option
   */
  @Parameters(description = "Add a directory/files; directories are searched for JAR/WAR/EAR.")
  private List<Path> fileset;

  @Option(names = { "-i", "--include", "--includes" },
          description = """
              Include file from the file system. File matched by the pattern will be added to any analysis.
              The pattern use java.util.regex.Pattern and can be added several times.
              By default the pattern match only the name of file; it may match the whole path by prefixing pattern by 'path:'
              """)
  private List<String> includes;

  @Option(names = { "-x", "--exclude", "--excludes" },
          description = """
                  Exclude file from the file system. File matched by the pattern will be ignored from any analysis.
                  The pattern use java.util.regex.Pattern and can be added several times.
                  By default the pattern match only the name of file; it may match the whole path by prefixing pattern by 'path:'
              """)
  private List<String> excludes;

  @Option(names = { "-D", "--deep-scan" },
          description = "Look for JAR in EAR/WAR files. The filter accepts value ALL (don't care about hierarchy) or the default STD (META-INF/lib/ only)")
  private JARFileLocator.DeepMode deepScan;

  @Option(names = { "-f", "--deep-filter" },
          description = """
              Filter embedded JAR/WAR. Path matched by the Pattern will be included.
              The pattern use java.util.regex.Pattern and can be added several times.
              By default the pattern match only the name of file; it may match the whole path by prefixing pattern by 'path:'
              """)
  private List<String> deepFilter;

  /*
   * processor options
   */
  @Option(names = "--all", description = { "Enable all processor." })
  private boolean allProcessor;

  @Option(names = "--maven",
          description = """
              Extract information stored by Maven Archiver in /META-INF/maven/**/pom.properties.
              The processor will ignore JAR with multiple pom.properties (probably über jar).
              """)
  private boolean mavenProcessor;

  @Option(names = { "--maven-bash", "--maven-shellscript" },
          description = """
              Export Maven information as a shell script (bash oriented; other shell may work or need to be adapted).
              The scriplet can be used to import the dependency to a local repository.
              """)
  private boolean mavenShellScriptExport;

  @Option(names = { "--jnlp-permission", "--jnlp-permissions" },
          description = "Check for permissions codebase for JNLP")
  private boolean manifestPermissionProcessor;

  @Option(names = { "--service", "--services", "--spi" },
          description = "Search for SPI; looks for file in /META-INF/services or in Java module if available.")
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

  @Option(names = { "--duplicate-package", "--duplicate-packages" },
          description = "Apply --package; show only package found in several JARs meaning probable problems with Java Modules.")
  private boolean showOnlyDuplicatePackage;

  @Option(names = { "--class", "--classes" }, description = "Show all classes in JAR.")
  private boolean showClasses;

  @Option(names = { "--duplicate-class", "--duplicate-classes" }, description = "Show duplicate classes in JAR.")
  private boolean showOnlyDuplicateClasses;

  private CSVFormat format;

  public static void main(final String[] args) {
    new picocli.CommandLine(new com.github.glhez.jtools.jar.MainCommand()).execute(args);
  }

  @Override
  public void run() {
    if (!canRun()) {
      return;
    }

    final var processor = buildProcessor();

    final var multiReleaseVersionPattern = Pattern.compile("^META-INF/versions/(\\d+)/$");

    try (final var locator = new JARFileLocator(deepScan, includes, excludes, deepFilter)) {
      locator.addFileset(fileset);
      if (locator.hasErrors()) {
        System.err.println("Some file or directories could not be fetched:");
        locator.getErrors().forEach(System.err::println);
      }
      final var files = locator.getFiles();

      final var ctx = new MutableProcessorContext();
      processor.init();

      var fileIndex = 1;
      final var fileCount = files.size();
      for (final JARInformation file : files) {
        System.out.printf("Processing file: [%6.2f%%] %s%n", 100 * (fileIndex / (double) fileCount), file.archivePath);
        ctx.setSource(file);

        int[] features = null;
        try (final var jarFile = new JarFile(file.tmpPath.toFile(), false)) {
          /*
           * look up for multi release version now
           */
          if (jarFile.isMultiRelease()) {
            ctx.setSource(file.asMultiRelease());

            /* @formatter:off */
            features = jarFile.stream()
                   .map(entry -> multiReleaseVersionPattern.matcher(entry.getName()))
                   .filter(Matcher::matches)
                   .map(matcher -> matcher.group(1))
                   .mapToInt(Integer::parseInt)
                   .toArray();
            /* @formatter:on */
            if (features.length > 0) {
              System.out.printf("Found %d version for Multi-Release JARs%n", features.length);
            }
          }

          processor.process(ctx, jarFile);
        } catch (final NullPointerException e) {
          throw e; // rethrow: this is probably OUR error.
        } catch (final Exception e) {
          ctx.addError(e);
        }

        /*
         * now use a multi release jar
         */
        if (features != null && features.length > 0) {
          for (final int feature : features) {
            ctx.setSource(file.asMultiReleaseVersion(feature));

            final var version = Runtime.Version.parse(Integer.toString(feature));
            try (final var jarFile = new JarFile(file.tmpPath.toFile(), false, ZipFile.OPEN_READ, version)) {
              processor.process(ctx, jarFile);
            } catch (final NullPointerException e) {
              throw e; // rethrow: this is probably OUR error.
            } catch (final Exception e) {
              ctx.addError(e);
            }
          }
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
    if (!mavenShellScriptExport && !mavenProcessor && !moduleProcessor && !serviceProcessor
        && !manifestPermissionProcessor && !manifestClassPathProcessor && !javaVersionProcessor && !showPackage
        && !showOnlyDuplicatePackage && !showClasses && !showOnlyDuplicateClasses) {
      problems.add("no processors registered");
    }
    if (fileset.isEmpty()) {
      problems.add("no files registered");
    }

    if (!problems.isEmpty()) {
      System.err.println(problems.stream().collect(joining(" and ")) + "; use --help for usage.");
      return false;
    }
    return true;
  }

  private void prepareParameters() {
    fileset = Objects.requireNonNullElseGet(fileset, Collections::emptyList);
    includes = Objects.requireNonNullElseGet(includes, Collections::emptyList);
    excludes = Objects.requireNonNullElseGet(excludes, Collections::emptyList);
    deepScan = Objects.requireNonNullElseGet(deepScan, () -> DeepMode.DISABLED);
    deepFilter = Objects.requireNonNullElseGet(deepFilter, Collections::emptyList);
    serviceFiltersEnabled = serviceFilters != null;
    serviceFilters = Objects.requireNonNullElseGet(serviceFilters, Collections::emptySet);
    outputDirectory = Objects.requireNonNullElseGet(outputDirectory, () -> Paths.get(""));

    if (allProcessor) {
      if (!mavenShellScriptExport && !mavenProcessor) {
        mavenProcessor = true;
        mavenShellScriptExport = false;
      }
      moduleProcessor = true;
      serviceProcessor = true;
      manifestPermissionProcessor = true;
      manifestClassPathProcessor = true;
      javaVersionProcessor = true;
      if (!showPackage && !showOnlyDuplicatePackage) {
        showPackage = false;
        showOnlyDuplicatePackage = true;
      }
      if (!showClasses && !showOnlyDuplicateClasses) {
        showClasses = false;
        showOnlyDuplicateClasses = true;
      }
    }

    if (showOnlyDuplicatePackage) {
      showPackage = true;
    }

    if (null == csvSeparator) {
      final var locale = Locale.getDefault();
      if (Locale.FRANCE.equals(locale) || Locale.GERMANY.equals(locale)) {
        csvSeparator = ';';
      } else {
        csvSeparator = ',';
      }
    }
    format = CSVFormat.EXCEL.builder().setDelimiter(csvSeparator).get();
  }

  private ListJARProcessor buildProcessor() {
    final List<JARProcessor> processors = new ArrayList<>();

    final var addModuleProcessor = moduleProcessor || serviceProcessor || showPackage || showClasses;

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

    if (serviceProcessor) {
      final var reportFile = newReportFile(serviceModuleOnly ? "services-module-only" : "services");
      add(processors, new SPIServiceJARProcessor(reportFile, moduleJARProcessor, !serviceFiltersEnabled, serviceFilters,
          serviceModuleOnly));
    }

    if (manifestPermissionProcessor) {
      add(processors, new JNLPPermissionsJARProcessor(newReportFile("jnlp-permissions")));
    }
    if (manifestClassPathProcessor) {
      add(processors, new ClassPathJARProcessor(newReportFile("class-path")));
    }
    if (javaVersionProcessor) {
      add(processors, new JavaVersionJARProcessor(newReportFile("java-version"), mavenArtifactsJARProcessor));
    }
    if (showPackage) {
      add(processors, new ShowPackageJARProcessor(
          newReportFile(showOnlyDuplicatePackage ? "duplicate-package" : "package"), showOnlyDuplicatePackage,
          mavenArtifactsJARProcessor, moduleJARProcessor));
    }
    if (showClasses) {
      add(processors, new ShowClassJARProcessor(newReportFile(showOnlyDuplicateClasses ? "duplicate-class" : "class"),
          showOnlyDuplicateClasses, mavenArtifactsJARProcessor, moduleJARProcessor));
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
    final var errors = ctx.getErrors();
    if (!errors.isEmpty()) {
      System.err.println("-------------------------------");
      System.err.println("There was " + errors.size() + " errors:");
      errors.forEach((information, messages) -> {
        final var n = messages.size();
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
