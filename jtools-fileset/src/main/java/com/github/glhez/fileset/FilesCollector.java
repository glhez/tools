package com.github.glhez.fileset;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;

import com.github.glhez.fileset.internal.NoArchive;

/**
 * A simple API to collect files.
 * <p>
 * The collector needs to be enclosed in a try with resources to ensure that {@link FileSystem} are
 * properly closed. This is required for archive stored in archive.
 *
 * @author gael.lhez
 */
public class FilesCollector implements AutoCloseable {
  private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(FilesCollector.class);

  private final Path tmpDirectory;
  private final Predicate<CollectedFile> rootFilter;
  private final ArchivePredicate archivePredicate;

  private final List<FileError> errors;
  private final SortedSet<CollectedFile> collectedFiles;
  private final List<FileSystem> fileSystems;
  private final List<Path> tempFiles;

  private FilesCollector(final FilesCollector.Builder builder) {
    this.tmpDirectory = builder.tmpDirectory;
    this.rootFilter = new CollectedFilePredicateBuilder().convert(builder.includes, builder.excludes);
    this.archivePredicate = Optional.ofNullable(builder.archivePredicate).orElseGet(() -> NoArchive.INSTANCE);

    this.collectedFiles = new TreeSet<>();
    this.errors = new ArrayList<>();
    this.fileSystems = new ArrayList<>();
    this.tempFiles = new ArrayList<>();
  }

  public static FilesCollector.Builder builder() {
    return new FilesCollector.Builder();
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  /**
   * Get a copy of error found some far.
   *
   * @return a list of {@link FileError}.
   */
  public List<FileError> getErrors() {
    return Collections.unmodifiableList(errors);
  }

  /**
   * Get an unmodifiable list of collected files.
   *
   * @return a set.
   */
  public SortedSet<CollectedFile> getCollectedFiles() {
    return Collections.unmodifiableSortedSet(collectedFiles);
  }

  /**
   * Add several entries.
   * <p>
   * Entries are directly processed for addition.
   *
   * @param paths
   *          some entries, can be <code>null</code> or empty.
   */
  public void addEntries(final Collection<Path> paths) {
    if (paths != null && !paths.isEmpty()) {
      paths.forEach(this::addEntry);
    }
  }

  /**
   * Add some entry to the file list.
   * <p>
   * The entry is directly processed for addition.
   *
   * @param path
   *          an entry (not null).
   */
  public void addEntry(final Path path) {
    Objects.requireNonNull(path, "path");
    addEntry(null, path);
  }

  private void addEntry(final CollectedFile parent, final Path path) {
    final var entry = new CollectedFile(parent, path);
    try {
      final var attributes = Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();
      if (attributes.isDirectory()) {
        addDirectoryEntry(entry.toRealPath());
      } else if (attributes.isRegularFile()) {
        addRegularFileEntry(entry.toRealPath(), true);
      } else {
        addError(entry, "Unsupported entry type: not a regular file or directory");
      }
    } catch (final IOException e) {
      addError(entry, e);
    }
  }

  private void addDirectoryEntry(final CollectedFile entry) throws IOException {
    try (final var stream = Files.find(entry.getPath(), Integer.MAX_VALUE, (file,
        attrs) -> attrs.isRegularFile())) {
      stream.forEach(path -> {
        final var child = new CollectedFile(entry, path);
        addRegularFileEntry(child, false);
      });
    }
  }

  private void addRegularFileEntry(final CollectedFile entry, final boolean ignoreFilter) {
    if (!ignoreFilter && !rootFilter.test(entry)) {
      return;
    }

    if (archivePredicate.isArchive(entry)) {
      throw new UnsupportedOperationException("Descending into archives not yet supported");
    }
    collectedFiles.add(entry);
  }

  private void addError(final CollectedFile entry, final String message) {
    this.errors.add(new FileError(entry, message));
  }

  private void addError(final CollectedFile entry, final IOException exception) {
    this.addError(entry, exception.getMessage());
  }

  @Override
  public void close() {
    this.fileSystems.forEach(this::closeFileSystem);
    this.tempFiles.forEach(this::deleteTempFile);
  }

  private void deleteTempFile(final Path tempFile) {
    try {
      Files.deleteIfExists(tempFile);
    } catch (IOException e) {
      logger.warn("could not delete temp file: {}", tempFile, e);
    }
  }

  private void closeFileSystem(final FileSystem fs) {
    try {
      fs.close();
    } catch (IOException e) {
      logger.warn("could not close file system: {}", fs, e);
    }
  }

  public static class Builder {
    private Path tmpDirectory;
    private ArchivePredicate archivePredicate;
    private List<String> includes;
    private List<String> excludes;

    public FilesCollector.Builder setTmpDirectory(final Path tmpDirectory) {
      this.tmpDirectory = tmpDirectory;
      return this;
    }

    public FilesCollector.Builder setArchivePredicate(final ArchivePredicate archivePredicate) {
      this.archivePredicate = archivePredicate;
      return this;
    }

    public FilesCollector.Builder setIncludes(final List<String> includes) {
      this.includes = includes;
      return this;
    }

    public FilesCollector.Builder setExcludes(final List<String> excludes) {
      this.excludes = excludes;
      return this;
    }

    public FilesCollector build() {
      return new FilesCollector(this);
    }

  }
}
