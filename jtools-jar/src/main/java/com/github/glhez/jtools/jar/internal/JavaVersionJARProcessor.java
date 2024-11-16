package com.github.glhez.jtools.jar.internal;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.csv.CSVPrinter;

public class JavaVersionJARProcessor extends ReportFileJARProcessor {
  private static final int JAVA_CLASS_MAGIC = 0xCAFEBABE;

  private final Map<JARInformation, EnumMap<JavaVersion, Long>> entries = new LinkedHashMap<>();
  private final Optional<MavenArtifactsJARProcessor> mavenArtifactsJARProcessor;

  public JavaVersionJARProcessor(final ReportFile reportFile,
      final MavenArtifactsJARProcessor mavenArtifactsJARProcessor) {
    super("Java version", reportFile);
    this.mavenArtifactsJARProcessor = Optional.ofNullable(mavenArtifactsJARProcessor); // optional
  }

  @Override
  public void init() {
    entries.clear();
  }

  @Override
  public void process(final ProcessorContext context, final JarFile jarFile) {
    try (var ss = jarFile.stream()) {
      final var jvd = javaVersionDetector(context, jarFile);
      final var val = ss.filter(this::isValidEntry) //
                        .map(jvd)  //
                        .collect(groupingBy(Map.Entry::getValue, () -> new EnumMap<>(
                            JavaVersion.class), mapping(Map.Entry::getKey, counting())));
      entries.put(context.getJARInformation(), val);
    }
  }

  private boolean isValidEntry(final JarEntry entry) {
    return entry.getName().endsWith(".class");
  }

  private Function<JarEntry, Map.Entry<String, JavaVersion>> javaVersionDetector(final ProcessorContext context,
      final JarFile jarFile) {
    return entry -> {
      try (var dis = new DataInputStream(jarFile.getInputStream(entry))) {
        if (JAVA_CLASS_MAGIC == dis.readInt()) {
          final var minorVersion = dis.readUnsignedShort();
          final var majorVersion = dis.readUnsignedShort();
          return Map.entry(entry.getName(), JavaVersion.match(majorVersion, minorVersion));
        }
      } catch (final IOException | java.lang.SecurityException e) {
        context.addError("Unable to parse JarEntry: " + entry.getName() + ": " + e.getMessage());
      }
      return Map.entry(entry.getName(), JavaVersion.ERROR);
    };
  }

  @Override
  protected void finish(final CSVPrinter printer) throws IOException {
    printer.printRecord("JAR", "Maven GAV", "Java Version", "Files in JAR");
    for (final var entry : entries.entrySet()) {
      final var jarInfo = entry.getKey();
      final var gav = mavenArtifactsJARProcessor.map(p -> p.getGAVAsString(jarInfo)).orElse("");
      final var versions = entry.getValue();

      for (final var versionEntry : versions.entrySet()) {
        printer.printRecord(jarInfo, gav, versionEntry.getKey(), versionEntry.getValue());
      }
    }
  }

  public enum JavaVersion {
    JAVA_1_0(45, 3, "Java 1.0"),
    JAVA_1_1(45, 3, "Java 1.1"),
    JAVA_1_2(46, 0, "Java 1.2"),
    JAVA_1_3(47, 0, "Java 1.3"),
    JAVA_1_4(48, 0, "Java 1.4"),
    JAVA_5(49, 0, "Java 5"),
    JAVA_6(50, 0, "Java 6"),
    JAVA_7(51, 0, "Java 7"),
    JAVA_8(52, 0, "Java 8"),
    JAVA_9(53, 0, "Java 9"),
    JAVA_10(54, 0, "Java 10"),
    JAVA_11(55, 0, "Java 11"),
    JAVA_12(56, 0, "Java 12"),
    JAVA_13(57, 0, "Java 13"),
    JAVA_14(58, 0, "Java 14"),
    JAVA_15(59, 0, "Java 15"),
    JAVA_16(60, 0, "Java 16"),
    JAVA_17(61, 0, "Java 17"),
    JAVA_18(62, 0, "Java 18"),
    JAVA_19(63, 0, "Java 19"),
    JAVA_20(64, 0, "Java 20"),
    UNKNOWN(-1, -1, "Unrecognized min/maj"),
    ERROR(-1, -1, "Parsing error");

    private final int minor;
    private final int major;
    private final String name;

    JavaVersion(final int major, final int minor, final String name) {
      this.minor = minor;
      this.major = major;
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }

    public static JavaVersion match(final int major, final int minor) {
      for (final JavaVersion version : JavaVersion.values()) {
        if (version.major == major && version.minor == minor) {
          return version;
        }
      }
      return UNKNOWN;
    }
  }
}
