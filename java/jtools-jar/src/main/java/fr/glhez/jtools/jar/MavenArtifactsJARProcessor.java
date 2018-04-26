package fr.glhez.jtools.jar;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MavenArtifactsJARProcessor implements JARProcessor {
  private static final String MAVEN_DIRECTORY = "META-INF/maven/";
  private static final String MAVEN_PROPERTY = "/pom.properties";
  private final OptionKind kind;
  private final Map<JARInformation, GAV> mavenArtifacts;
  private final boolean silent;

  public MavenArtifactsJARProcessor(final OptionKind kind, final boolean silent) {
    this.kind = kind;
    this.silent = silent;
    this.mavenArtifacts = new LinkedHashMap<>();
  }

  @Override
  public void init() {
    mavenArtifacts.clear();
  }

  Optional<GAV> getGAV(final JARInformation information) {
    return Optional.ofNullable(mavenArtifacts.get(information));
  }

  @Override
  public void process(final ProcessorContext context, final JarFile jarFile) {
    final List<JarEntry> properties = jarFile.stream().filter(MavenArtifactsJARProcessor::isCandidateForMaven)
        .collect(toList());

    if (properties.isEmpty()) {
      return;
    }      

    for (final JarEntry jarEntry : properties) {
      final Set<GAV> gavs = new LinkedHashSet<>();
      try (InputStream is = jarFile.getInputStream(jarEntry)) {
        gavs.add(GAV.parse(is));
      } catch (final IOException e) {
        context.addError("Failed to read GAV definition: " + e.getMessage());
      }
      
      if (gavs.isEmpty()) {
        return; // this would have been a GAV read error.
      } else if (gavs.size() == 1) {
        mavenArtifacts.put(context.getJARInformation(), gavs.iterator().next());
      } else {
        // filter using the archive name, else fail 
        final String name = context.getJARInformation().getFileName().toString();
        final Set<GAV> newGavs = gavs.stream().filter(gav -> name.contains(gav.getFileNamePrefix())).collect(toSet());
        if (newGavs.isEmpty()) {
          return; // this would have been a GAV read error. 
        } else if (newGavs.size() == 1) {
          mavenArtifacts.put(context.getJARInformation(), newGavs.iterator().next());
        } else {
          context.addError("Multiple " + MAVEN_DIRECTORY + "**" + MAVEN_PROPERTY + " found. Could not determine a GAV with filename either.");
        }
      }      
    }
  }

  @Override
  public void finish() {
    if (silent) {
      return;
    }
    System.out.println("---- [Maven Metadata] ----");
    if (kind == OptionKind.LIST) {
      final int limit = mavenArtifacts.values().stream().map(Objects::toString).mapToInt(String::length).max()
          .orElse(30);
      final String pattern = "  %" + limit + "s => %s%n";
      mavenArtifacts
          .forEach((jar, gav) -> System.out.printf(pattern, StringUtils.rightPad(gav.toString(), limit), jar));
    } else if (kind == OptionKind.SCRIPT) {
      mavenArtifacts.forEach((jar, gav) -> System.out.printf("  _mvn '%s' '%s' '%s' '%s'%n", gav.groupId,
          gav.artifactId, gav.version, jar));
    } else {
      throw new IllegalStateException("Case not handled: " + kind);
    }
  }

  private static boolean isCandidateForMaven(final JarEntry jarEntry) {
    return isCandidateForMaven(jarEntry.getName());
  }

  private static boolean isCandidateForMaven(final String jarEntryName) {
    return jarEntryName.startsWith(MAVEN_DIRECTORY) && jarEntryName.endsWith(MAVEN_PROPERTY);
  }

  static class GAV {
    private final String groupId;
    private final String artifactId;
    private final String version;

    public GAV(final String groupId, final String artifactId, final String version) {
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.version = version;
    }

    public CharSequence getFileNamePrefix() {
      return artifactId + "-" + version;
    }

    public static GAV parse(final InputStream is) throws IOException {
      final Properties propz = new Properties();
      propz.load(is);
      final String groupId = propz.getProperty("groupId");
      final String artifactId = propz.getProperty("artifactId");
      final String version = propz.getProperty("version");

      if (null == groupId || null == artifactId || null == version) {
        throw new IOException("Missing fields: groupId % artifactId % version");
      }

      return new GAV(groupId, artifactId, version);
    }

    @Override
    public String toString() {
      return groupId + ":" + artifactId + ":" + version;
    }

  }

  public enum OptionKind {
    LIST,
    SCRIPT;
  }

}
