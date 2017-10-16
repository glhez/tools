package fr.glhez.jtools.jar;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MavenArtifactsJARProcessor implements JARProcessor {
  private static final String MAVEN_DIRECTORY = "META-INF/maven/";
  private static final String MAVEN_PROPERTY = "/pom.properties";
  private final OptionKind kind;
  private final Map<Path, GAV> mavenArtifacts;

  public MavenArtifactsJARProcessor(final OptionKind kind) {
    this.kind = kind;
    this.mavenArtifacts = new LinkedHashMap<>();
  }

  @Override
  public void init() {
    mavenArtifacts.clear();
  }

  @Override
  public void process(final ProcessorContext context, final JarFile jarFile) {
    final List<JarEntry> properties = jarFile.stream().filter(MavenArtifactsJARProcessor::isCandidateForMaven)
        .collect(toList());

    if (properties.isEmpty()) {
      return;
    } else if (properties.size() > 1) {
      context.addError("Multiple " + MAVEN_DIRECTORY + "**" + MAVEN_PROPERTY + " found.");
      return;
    }

    final JarEntry jarEntry = properties.get(0);
    try (InputStream is = jarFile.getInputStream(jarEntry)) {
      mavenArtifacts.put(context.getSource(), GAV.parse(is));
    } catch (final IOException e) {
      context.addError("Failed to read GAV definition: " + e.getMessage());
    }
  }

  @Override
  public void finish() {
    kind.execute(mavenArtifacts);
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
    LIST {
      @Override
      void execute(final Map<Path, GAV> artifacts) {
        artifacts.forEach((path, gav) -> System.out.printf("%s: %s%n", path, gav));
      }
    },
    SCRIPT {
      @Override
      void execute(final Map<Path, GAV> artifacts) {
        artifacts.forEach((path, gav) -> System.out.printf("_mvn '%s' '%s' '%s' '%s'%n", gav.groupId, gav.artifactId,
            gav.version, path));
      }
    };

    abstract void execute(Map<Path, GAV> artifacts);
  }

}
