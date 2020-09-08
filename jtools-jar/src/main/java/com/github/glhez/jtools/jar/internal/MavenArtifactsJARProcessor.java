package com.github.glhez.jtools.jar.internal;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
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
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.csv.CSVPrinter;

public class MavenArtifactsJARProcessor implements JARProcessor {
  private static final String MAVEN_DIRECTORY = "META-INF/maven/";
  private static final String MAVEN_PROPERTY = "/pom.properties";
  private final ExportMode kind;
  private final Map<JARInformation, GAV> mavenArtifacts;
  private final CSVMavenArtifactsJARProcessor csvProcessor;

  private MavenArtifactsJARProcessor(final ReportFile reportFile, final ExportMode kind) {
    this.kind = kind;
    this.mavenArtifacts = new LinkedHashMap<>();
    this.csvProcessor = this.kind == ExportMode.CSV ? new CSVMavenArtifactsJARProcessor(reportFile) : null;
  }

  public static MavenArtifactsJARProcessor newShellScriptMavenArtifactsJARProcessor() {
    return new MavenArtifactsJARProcessor(null, ExportMode.BASH);
  }

  public static MavenArtifactsJARProcessor newCSVMavenArtifactsJARProcessor(final ReportFile reportFile) {
    return new MavenArtifactsJARProcessor(Objects.requireNonNull(reportFile, "reportFile"), ExportMode.CSV);
  }

  public static MavenArtifactsJARProcessor newMavenArtifactsJARProcessor() {
    return new MavenArtifactsJARProcessor(null, ExportMode.NONE);
  }

  @Override
  public void init() {
    mavenArtifacts.clear();
  }

  private Optional<GAV> getGAV(final JARInformation information) {
    return Optional.ofNullable(mavenArtifacts.get(information));
  }

  String getGAVAsString(final JARInformation information) {
    return getGAV(information).map(GAV::toString).orElse("");
  }

  @Override
  public void process(final ProcessorContext context, final JarFile jarFile) {
    final List<JarEntry> properties = getCandidateProperties(jarFile);

    if (properties.isEmpty()) {
      return;
    }

    for (final JarEntry jarEntry : properties) {
      final Set<GAV> gavs = new LinkedHashSet<>();
      try (InputStream is = jarFile.getInputStream(jarEntry)) {
        gavs.add(GAV.parse(is));
      } catch (final IOException | java.lang.SecurityException e) {
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
          context.addError("Multiple " + MAVEN_DIRECTORY + "**" + MAVEN_PROPERTY
              + " found. Could not determine a GAV with filename either.");
        }
      }
    }
  }

  private List<JarEntry> getCandidateProperties(final JarFile jarFile) {
    try (final var ss = jarFile.stream()) {
      return ss.filter(MavenArtifactsJARProcessor::isCandidateForMaven).collect(toList());
    }
  }

  @Override
  public void finish() {
    if (kind == ExportMode.NONE) {
      return;
    }
    if (kind == ExportMode.BASH) {
      if (!mavenArtifacts.isEmpty()) {
        System.out.println("#!/bin/bash");

        System.out.println("_mvn() {");
        System.out.println("  local groupId=\"$1\"");
        System.out.println("  local artifactId=\"$2\"");
        System.out.println("  local version=\"$3\"");
        System.out.println("  local file=\"$4\"");
        System.out.println(
            "  echo mvn deploy:deploy-file  \"-DgroupId=$groupId\" \"-DartifactId=$artifactId\"  \"-Dversion=$version\" \"-Dfile=$file\"");
        System.out.println("}");
        System.out.println("");
        System.out.println("");
        System.out.printf("# %d artifacts found%n", mavenArtifacts.size());

      }
      mavenArtifacts.forEach((jar, gav) -> System.out.printf("_mvn '%s' '%s' '%s' '%s'%n", gav.groupId, gav.artifactId,
          gav.version.orElse(""), jar));
    } else if (kind == ExportMode.CSV) {
      csvProcessor.finish();
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
    private final Optional<String> version;

    private GAV(final String groupId, final String artifactId, final Optional<String> version) {
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

      return new GAV(groupId, artifactId, Optional.of(version));
    }

    private String prefixVersion(final String prefix) {
      return version.map(ver -> prefix + ver).orElse("");
    }

    public CharSequence getFileNamePrefix() {
      return artifactId + prefixVersion("-");
    }

    @Override
    public String toString() {
      return groupId + ":" + artifactId + prefixVersion(":");
    }

    public GAV unversionned() {
      return new GAV(groupId, artifactId, Optional.empty());
    }

    public String getGroupId() {
      return groupId;
    }

    public String getArtifactId() {
      return artifactId;
    }

    public Optional<String> getVersion() {
      return version;
    }

    @Override
    public int hashCode() {
      return Objects.hash(groupId, artifactId, version);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final GAV other = (GAV) obj;
      return Objects.equals(groupId, other.groupId) && Objects.equals(artifactId, other.artifactId)
          && Objects.equals(version, other.version);
    }

  }

  private enum ExportMode {
    NONE,
    BASH,
    CSV;
  }

  class CSVMavenArtifactsJARProcessor extends ReportFileJARProcessor {

    public CSVMavenArtifactsJARProcessor(final ReportFile reportFile) {
      super("Maven CSV Report", reportFile);
    }

    @Override
    public void init() {
      // NOPE
    }

    @Override
    public void process(final ProcessorContext context, final JarFile jarFile) {
      // NOPE
    }

    @Override
    protected void finish(final CSVPrinter printer) throws IOException {

      final Map<GAV, Long> counters = mavenArtifacts.values().stream()
          .collect(groupingBy(Function.identity(), counting()));
      final Map<GAV, Long> unversionnedCounters = mavenArtifacts.values().stream()
          .collect(groupingBy(GAV::unversionned, counting()));

      printer.printRecord("groupId:artifactId", "version", "groupId:artifactId references in fileset",
          "groupId:artifactId:version references in fileset", "File");
      for (final var entry : mavenArtifacts.entrySet()) {
        final var jarInformation = entry.getKey();
        final var gav = entry.getValue();
        final var unversionnedGav = gav.unversionned();

        printer.printRecord(unversionnedGav, gav.getVersion().orElse(""),
            unversionnedCounters.getOrDefault(unversionnedGav, 0L), counters.getOrDefault(gav, 0L), jarInformation);
      }

    }

  }

}
