package fr.glhez.jtools.warextractor.internal;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

public class JavaBaseFilesProxy extends AbstractFilesProxy {

  @Override
  public void recursiveDelete(final Path start) throws IOException {
    Files.walkFileTree(start, new FileDeletor(this));
  }

  @Override
  public void delete(final Path start) throws IOException {
    Files.delete(start);
  }

  @Override
  public void createDirectories(final Path directory, final FileAttribute<?>... attrs) throws IOException {
    Files.createDirectories(directory, attrs);
    addDirectoryTree(directory);
  }

  @Override
  public void createDirectory(final Path directory, final FileAttribute<?>... attrs) throws IOException {
    Files.createDirectory(directory, attrs);
    addDirectory(directory);
  }

  @Override
  public void createDirectoriesUpTo(final Path directory, final Path limit, final FileAttribute<?>... attrs)
      throws IOException {
    // we don't test using isDirectory() : there might be some file.
    if (null != directory && !this.directoryWasCreated(directory) && !limit.equals(directory)) {
      // do the parent first
      createDirectoriesUpTo(directory.getParent(), limit);
      this.createDirectory(directory, attrs);
    }
  }

  @Override
  public void copy(final Path source, final Path target, final CopyOption... options) throws IOException {
    this.addFile(target);
    try {
      Files.copy(source, target, options);
    } catch (final IOException e) {
      this.addCopyFailedFile(target);
      throw e;
    }
  }

}
