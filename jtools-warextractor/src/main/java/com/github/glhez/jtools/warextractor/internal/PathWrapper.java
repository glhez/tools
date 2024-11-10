package com.github.glhez.jtools.warextractor.internal;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Wrapper around {@link Path}.
 * <p>
 * Ensure we get some consistent naming and behavior. Also avoid getting information twice.
 *
 * @author gael.lhez
 */
public class PathWrapper implements Comparable<PathWrapper> {
  private final Path path;
  private final String fullPath;
  private final FileName fileName;

  public PathWrapper(final Path path) {
    this.path = Objects.requireNonNull(path, "path");
    this.fullPath = pathToString(path);
    this.fileName = new FileName(Objects.requireNonNull(path.getFileName(), "path.getFileName").toString());
  }

  public Path getPath() {
    return this.path;
  }

  public String getFullPath() {
    return this.fullPath;
  }

  public FileName getFileName() {
    return this.fileName;
  }

  public boolean startsWith(final Path other) {
    return this.path.startsWith(other);
  }

  public boolean endsWith(final Path other) {
    return this.path.endsWith(other);
  }

  @Override
  public int compareTo(final PathWrapper o) {
    return this.path.compareTo(o.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.path);
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
    return Objects.equals(this.path, other.path);
  }

  public static String getFileNameNoExtension(final Path src) {
    final var fileName = Objects.requireNonNull(src.getFileName(), "src.getFileName()").toString();
    return new FileName(fileName).fileNameWithoutExtension;
  }

  public static String pathToString(final Path path) {
    return pathToString(path.toString());
  }

  public static String pathToString(final String path) {
    return path.replace('\\', '/');
  }

  static class FileName {
    public final String fileName;
    public final String fileNameWithoutExtension;
    public final String extension;

    public FileName(final String fullName) {
      this.fileName = fullName;
      final int n = fullName.lastIndexOf('.');
      if (n == -1) {
        this.fileNameWithoutExtension = fullName;
        this.extension = "";
      } else {
        this.fileNameWithoutExtension = fullName.substring(0, n);
        this.extension = fullName.substring(n + 1);
      }
    }

  }

}