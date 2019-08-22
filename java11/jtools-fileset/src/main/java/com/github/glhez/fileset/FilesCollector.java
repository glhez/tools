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
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A simple API to collect files.
 * <p>
 * The collector needs to be enclosed in a try with resources to ensure that {@link FileSystem} are
 * properly closed. This is required for archive stored in archive.
 *
 * @author gael.lhez
 */
public class FilesCollector implements AutoCloseable {
  private final ArchiveMode archiveMode;
  private final Predicate<CollectedFile> filter;
  private final List<FileError> errors;
  private final SortedSet<CollectedFile> collectedFiles;

  private FilesCollector(final ArchiveMode archiveMode, final Predicate<CollectedFile> filter) {
    this.archiveMode = Objects.requireNonNull(archiveMode, "archiveMode");
    this.filter = Objects.requireNonNull(filter, "filter");

    this.collectedFiles = new TreeSet<>();
    this.errors = new ArrayList<>();
  }

  /**
   * Create a new collector.
   *
   * @param archiveMode determine what to do with archives file.
   * @param predicate how to filter file (mandatory).
   * @return a collector.
   */
  public static FilesCollector newFilesCollector(final ArchiveMode archiveMode,
      final Predicate<CollectedFile> predicate) {
    return new FilesCollector(archiveMode, predicate);
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
   * @param paths some entries, can be <code>null</code> or empty.
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
   * @param path an entry (not null).
   */
  public void addEntry(final Path path) {
    Objects.requireNonNull(path, "path");
    addEntry(null, path);
  }

  private void addEntry(final CollectedFile parent, final Path path) {
    final CollectedFile entry = new CollectedFile(parent, path);
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
    try (final Stream<Path> stream = Files.find(entry.getPath(), Integer.MAX_VALUE,
        (file, attrs) -> attrs.isRegularFile())) {
      stream.forEach(path -> {
        final var child = new CollectedFile(entry, path);
        try {
          addRegularFileEntry(child, false);
        } catch (final IOException e) {
          addError(child, e);
        }
      });
    }
  }

  private void addRegularFileEntry(final CollectedFile entry, final boolean ignoreFilter) throws IOException {
    if (!ignoreFilter && !filter.test(entry)) {
      return;
    }

    if (archiveMode.test(entry)) {
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
    // TODO Auto-generated method stub
  }

}
