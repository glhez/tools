package com.github.glhez.jtools.jar.internal;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.JarFile;

/**
 * Information about a JAR file such as its origin and its child content (for deep archive).
 *
 * @author gael.lhez
 */
public class JARInformation implements Comparable<JARInformation> {
  /**
   * Path of the archive being opened.
   */
  public final Path archivePath;

  /**
   * Path in the archive being opened.
   */
  public final Optional<Path> pathInArchive;

  /**
   * Path to the content. Same as jarInformation unless there is a {@link #pathInArchive}.
   */
  public final Path tmpPath;

  /**
   * Multi release JAR.
   */
  public final boolean multiRelease;

  /**
   * "feature" version (the number after META-INF/versions).
   */
  public final int feature;

  private JARInformation(final Path archivePath, final Optional<Path> pathInArchive, final Path tmpPath,
      final boolean multiRelease, final int feature) {
    this.archivePath = Objects.requireNonNull(archivePath, "archivePath");
    this.pathInArchive = Objects.requireNonNull(pathInArchive, "pathInArchive");
    this.tmpPath = Objects.requireNonNull(tmpPath, "tmpPath");
    this.multiRelease = multiRelease;
    this.feature = feature;
  }

  public static JARInformation newJARInformation(final Path source) {
    return new JARInformation(source, Optional.empty(), source, false, JarFile.baseVersion().feature());
  }

  public static JARInformation newJARInformation(final Path source, final Path realPath, final Path tmpPath) {
    return new JARInformation(source, Optional.of(realPath), tmpPath, false, JarFile.baseVersion().feature());
  }

  public Path getFileName() {
    return pathInArchive.map(Path::getFileName).orElseGet(archivePath::getFileName);
  }

  public JARInformation asMultiRelease() {
    return new JARInformation(archivePath, pathInArchive, tmpPath, true, feature);
  }

  public JARInformation asMultiReleaseVersion(final int feature) {
    return new JARInformation(archivePath, pathInArchive, tmpPath, true, feature);
  }

  @Override
  public int hashCode() {
    return archivePath.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (null == obj || obj.getClass() != this.getClass()) {
      return false;
    }
    final var other = (JARInformation) obj;
    return archivePath.equals(other.archivePath) && pathInArchive.equals(other.pathInArchive)
        && multiRelease == other.multiRelease && feature == other.feature;
  }

  @Override
  public String toString() {
    return archivePath.toString()
        + pathInArchive.filter(p -> !p.equals(archivePath)).map(p -> " [" + p + "]").orElse("")
        + (multiRelease ? "@" + feature : "");
  }

  @Override
  public int compareTo(final JARInformation o) {
    var n = archivePath.compareTo(o.archivePath);
    if (n == 0) {
      n = pathInArchive.map(Object::toString).orElse("").compareTo(o.pathInArchive.map(Object::toString).orElse(""));
    }
    if (n == 0) {
      n = Boolean.compare(multiRelease, o.multiRelease);
    }
    if (n == 0 && multiRelease) {
      n = Integer.compare(feature, o.feature);
    }
    return n;
  }

}
