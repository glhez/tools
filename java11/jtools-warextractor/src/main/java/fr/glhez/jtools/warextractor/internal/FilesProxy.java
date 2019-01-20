package fr.glhez.jtools.warextractor.internal;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Set;

/**
 * Proxy to the {@link Files} class.
 *
 * @author gael.lhez
 */
public interface FilesProxy {

  /**
   * Indicate if an operation resulted in a directory creation.
   *
   * @param path
   * @return true if the directory was created calling
   *         {@link #createDirectories(Path, FileAttribute...)},
   *         {@link #createDirectoriesUpTo(Path, Path, FileAttribute...)} or
   *         {@link #createDirectory(Path, FileAttribute...)}.
   */
  boolean directoryWasCreated(final Path path);

  /**
   * Indicate if an operation resulted in a file creation.
   *
   * @param path
   * @return true if the file was created using {@link #fileWasCreated(Path)}.
   */
  boolean fileWasCreated(final Path path);

  /**
   * Get the list of files copied to the target directory, excluding those that failed.
   *
   * @return a list of files.
   */
  Set<Path> getCopiedFiles();

  /**
   * Delete the file or empty directory pointed by {@code start}.
   *
   * @param start start point
   */
  void delete(Path start) throws IOException;

  /**
   * Recursively delete the file or directory pointed by {@code start}.
   *
   * @param start start point
   */
  void recursiveDelete(Path start) throws IOException;

  /**
   * Create directory recursively.
   * <p>
   * Consider creation in {@link #directoryWasCreated(Path)} done unless if operation succeed.
   *
   * @param directory
   * @throws IOException
   * @see Files#createDirectories(Path, FileAttribute...)
   */
  void createDirectories(Path directory, FileAttribute<?>... attrs) throws IOException;

  /**
   * Create directory recursively until {@code limit} parent is reached.
   * <p>
   * Consider creation in {@link #directoryWasCreated(Path)} done unless if operation succeed.
   *
   * @param directory
   * @param limit
   * @throws IOException
   * @see Files#createDirectory(Path, FileAttribute...)
   */
  void createDirectoriesUpTo(Path directory, Path limit, FileAttribute<?>... attrs) throws IOException;

  /**
   * Create one directory.
   * <p>
   * Consider creation in {@link #directoryWasCreated(Path)} done unless if operation succeed.
   *
   * @param directory
   * @param attrs
   * @see Files#createDirectory(Path, FileAttribute...)
   */
  void createDirectory(Path directory, FileAttribute<?>... attrs) throws IOException;

  /**
   * Copy a file to another location.
   * <p>
   * Consider creation in {@link #fileWasCreated(Path)} done even if operation fails.
   *
   * @param source
   * @param target
   * @param options
   * @throws IOException
   * @see Files#copy(Path, Path, CopyOption...)
   */
  void copy(Path source, Path target, CopyOption... options) throws IOException;
}
