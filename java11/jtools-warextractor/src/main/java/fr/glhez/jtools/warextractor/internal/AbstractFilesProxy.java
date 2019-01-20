package fr.glhez.jtools.warextractor.internal;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Cache some minor information about {@link FilesProxy}, such as created directories and files.
 *
 * @author gael.lhez
 *
 */
public abstract class AbstractFilesProxy implements FilesProxy {
  private final Set<Path> createdDirectories;
  private final Set<Path> createdFiles;
  private final Set<Path> copyFailedFiles;

  public AbstractFilesProxy() {
    this.createdDirectories = new HashSet<>();
    this.createdFiles = new HashSet<>();
    this.copyFailedFiles = new HashSet<>();
  }

  @Override
  public final boolean directoryWasCreated(final Path path) {
    return createdDirectories.contains(path);
  }

  @Override
  public final boolean fileWasCreated(final Path path) {
    return createdFiles.contains(path);
  }

  @Override
  public Set<Path> getCopiedFiles() {
    final SortedSet<Path> files = new TreeSet<>(createdFiles);
    files.removeAll(copyFailedFiles);
    return files;
  }

  protected void addDirectory(final Path path) {
    this.createdDirectories.add(path);
  }

  protected void addDirectoryTree(final Path path) {
    this.addDirectory(path);
    var parent = path.getParent();
    while (parent != null) {
      addDirectoryTree(parent);
      parent = parent.getParent();
    }
  }

  protected void addFile(final Path path) {
    this.createdFiles.add(path);
  }

  protected void addCopyFailedFile(final Path path) {
    this.copyFailedFiles.add(path);
  }
}
