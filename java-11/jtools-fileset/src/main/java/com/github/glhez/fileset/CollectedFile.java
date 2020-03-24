package com.github.glhez.fileset;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;

/**
 * Represent a collected file.
 * <p>
 * The file may have a parent it it comes from an archive.
 *
 * @author gael.lhez
 */
public final class CollectedFile implements Comparable<CollectedFile> {
  /**
   * Our comparator implementation, which sort by parent first, then path {@link #getPathAsString()}
   * (ignoring problems due to incompatible implementation of {@link Path}).
   */
  private static final Comparator<CollectedFile> COMPARATOR = Comparator
      .comparing(CollectedFile::getParent, Comparator.nullsFirst(Comparator.naturalOrder()))
      .thenComparing(CollectedFile::getPathAsString);

  private final CollectedFile parent;
  private final Path path;

  private transient String extensionCache;
  private transient String fileNameCache;
  private transient String pathAsStringCache;
  private transient String completePathCache;

  public CollectedFile(final CollectedFile parent, final Path path) {
    this.parent = parent;
    this.path = path;
  }

  /**
   * Return this with the real (absolute) path entries.
   *
   * @return a new {@link CollectedFile}
   * @throws IOException see {@link Path#toRealPath(java.nio.file.LinkOption...)}.
   */
  public CollectedFile toRealPath() throws IOException {
    return new CollectedFile(parent, path.toRealPath());
  }

  /**
   * Return the parent.
   * <p>
   * May be <code>null</code> if root file (usually the default file system).
   *
   * @return a {@link CollectedFile}.
   */
  public CollectedFile getParent() {
    return parent;
  }

  /**
   * Get internal path.
   *
   * @return the path.
   */
  public Path getPath() {
    return path;
  }

  /**
   * Get name of collected file.
   * <p>
   * The name is cached for performance reason (not all {@link Path} implementation cache it).
   *
   * @return a name.
   */
  public String getFileName() {
    var fileNameCache = this.fileNameCache;
    if (null == fileNameCache) {
      fileNameCache = path.getFileName().toString();
      this.fileNameCache = fileNameCache;
    }
    return fileNameCache;
  }

  /**
   * Get extension.
   * <p>
   * The extension is cached and always in lower case.
   *
   * @return an extension. Can be <code>null</code>.
   */
  public String getExtension() {
    var extensionCache = this.extensionCache;
    if (null == extensionCache) {
      extensionCache = getExtension0();
      this.extensionCache = extensionCache;
    }
    return extensionCache.isEmpty() ? null : extensionCache;
  }

  private String getExtension0() {
    final String name = getFileName();
    final int dotIndex = name.lastIndexOf('.');
    if (dotIndex == -1) {
      return "";
    }
    return name.substring(dotIndex + 1).toLowerCase();
  }

  /**
   * Get the path of the file in its {@link FileSystem}.
   * <p>
   * The path is normalized by replacing backslashes into slashes (which is specific to Windows).
   * <p>
   * The path is always relative to its {@link FileSystem} and is not suitable to inform an user of
   * the whereabouts of the file.
   *
   * @return a path
   * @see #getCompletePath()
   */
  public String getPathAsString() {
    var pathAsStringCache = this.pathAsStringCache;
    if (null == pathAsStringCache) {
      // normalize path as well
      pathAsStringCache = path.toString().replace('\\', '/');
      this.pathAsStringCache = pathAsStringCache;
    }
    return pathAsStringCache;
  }

  /**
   * Return the complete path of this file.
   * <p>
   * The complete path use the {@code !} to delimit archive part. For example,
   * {@code C:/foobar.jar!/Cla.java}.
   *
   * @return a complete path.
   */
  public String getCompletePath() {
    var completePathCache = this.completePathCache;
    if (null == completePathCache) {
      completePathCache = getCompletePath0();
      this.completePathCache = completePathCache;
    }
    return completePathCache;
  }

  private String getCompletePath0() {
    if (null != parent) {
      return parent.toString() + "!" + getPathAsString();
    }
    return path.toString();
  }

  @Override
  public String toString() {
    return getCompletePath();
  }

  @Override
  public int compareTo(final CollectedFile o) {
    return COMPARATOR.compare(this, o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.parent, this.getPathAsString());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final CollectedFile other = (CollectedFile) obj;
    return Objects.equals(parent, other.parent) && Objects.equals(getPathAsString(), other.getPathAsString());
  }

}
