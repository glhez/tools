package fr.glhez.jtools.jar;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;

import fr.glhez.jtools.jar.MavenArtifactsJARProcessor.GAV;

public class JavaVersionJARProcessor implements JARProcessor {
  private static final int JAVA_CLASS_MAGIC = 0xCAFEBABE;

  private final Map<JARInformation, EnumMap<JavaVersion, Set<String>>> entries = new LinkedHashMap<>();
  private final Optional<MavenArtifactsJARProcessor> mavenArtifactsJARProcessor;

  public JavaVersionJARProcessor(final MavenArtifactsJARProcessor mavenArtifactsJARProcessor) {
    this.mavenArtifactsJARProcessor = Optional.ofNullable(mavenArtifactsJARProcessor); // optional
  }

  @Override
  public void init() {
    entries.clear();
  }

  @Override
  public void process(final ProcessorContext context, final JarFile jarFile) {
    final var val = jarFile.stream().filter(entry -> entry.getName().endsWith(".class")).map(entry -> {
      try (DataInputStream dis = new DataInputStream(jarFile.getInputStream(entry))) {
        if (JAVA_CLASS_MAGIC == dis.readInt()) {
          final int minorVersion = dis.readUnsignedShort();
          final int majorVersion = dis.readUnsignedShort();
          return Map.entry(entry.getName(), JavaVersion.match(majorVersion, minorVersion));
        }
      } catch (final IOException e) {
        context.addError("Unable to parse JarEntry: " + entry.getName() + ": " + e.getMessage());
      }
      return Map.entry(entry.getName(), JavaVersion.ERROR);
    }).collect(
        groupingBy(Map.Entry::getValue, () -> new EnumMap<>(JavaVersion.class), mapping(Map.Entry::getKey, toSet())));
    entries.put(context.getJARInformation(), val);
  }

  @Override
  public void finish() {
    System.out.println("-- [Java version for JAR] --");
    entries.forEach((jarInfo, versions) -> {
      Optional<GAV> gav = mavenArtifactsJARProcessor.flatMap(p -> p.getGAV(jarInfo));
      versions.forEach((version, files) -> {
        System.out.println("  " + jarInfo + gav.map(g -> " [" + g + "]").orElse("") + ": " + version + " ("
            + files.size() + " files)");
      });
    });
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
    UNKNOWN(-1, -1, "Unrecognized min/maj"),
    ERROR(-1, -1, "Parsing error");

    private final int minor;
    private final int major;
    private final String name;

    private JavaVersion(final int major, final int minor, final String name) {
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
