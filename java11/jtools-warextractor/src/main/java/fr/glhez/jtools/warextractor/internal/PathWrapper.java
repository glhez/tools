package fr.glhez.jtools.warextractor.internal;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Wrapper around {@link Path}.
 * <p>
 * Ensure we get some consistent naming and behavior. Also avoid getting information twice.
 *
 * @author gael.lhez
 */
class PathWrapper implements Comparable<PathWrapper> {
  private final Path path;
  private final String fullPath;
  private final String fileName;

  public PathWrapper(final Path path) {
    this.path = Objects.requireNonNull(path, "path");
    this.fullPath = ExecutionContext.pathToString(path);
    this.fileName = Objects.toString(path.getFileName(), "");
  }

  public Path getPath() {
    return path;
  }

  public String getFullPath() {
    return fullPath;
  }

  public String getFileName() {
    return fileName;
  }

  public boolean startsWith(final Path other) {
    return path.startsWith(other);
  }

  public boolean endsWith(final Path other) {
    return path.endsWith(other);
  }

  @Override
  public int compareTo(final PathWrapper o) {
    return path.compareTo(o.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final PathWrapper other = (PathWrapper) obj;
    return Objects.equals(path, other.path);
  }

}