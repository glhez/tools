package fr.glhez.jtools.jar.internal;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

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

  private JARInformation(final Path archivePath, final Optional<Path> pathInArchive, final Path tmpPath) {
    this.archivePath = Objects.requireNonNull(archivePath, "archivePath");
    this.pathInArchive = Objects.requireNonNull(pathInArchive, "pathInArchive");
    this.tmpPath = Objects.requireNonNull(tmpPath, "tmpPath");
  }

  public static JARInformation newJARInformation(final Path source) {
    return new JARInformation(source, Optional.empty(), source);
  }

  public static JARInformation newJARInformation(final Path source, final Path realPath, final Path tmpPath) {
    return new JARInformation(source, Optional.of(realPath), tmpPath);
  }

  public Path getFileName() {
    return pathInArchive.map(Path::getFileName).orElseGet(archivePath::getFileName);
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
    final JARInformation other = (JARInformation) obj;
    return archivePath.equals(other.archivePath) && pathInArchive.equals(other.pathInArchive);
  }

  @Override
  public String toString() {
    return archivePath.toString() + pathInArchive.filter(p -> !p.equals(archivePath)).map(p -> " [" + p + "]").orElse("");
  }

  @Override
  public int compareTo(final JARInformation o) {
    final int n = archivePath.compareTo(o.archivePath);
    if (n == 0) {
      return pathInArchive.map(Object::toString).orElse("").compareTo(o.pathInArchive.map(Object::toString).orElse(""));
    }
    return n;
  }

}
