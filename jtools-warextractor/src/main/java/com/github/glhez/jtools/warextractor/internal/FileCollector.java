package com.github.glhez.jtools.warextractor.internal;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;

public class FileCollector implements FileVisitor<Path> {
  /** Logger */
  private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(FileCollector.class);

  private final Set<Path> directories;
  private final SortedSet<PathWrapper> files;
  private final SortedSet<PathWrapper> archives;
  private final ExecutionContext context;
  private final Path archiveDirectory;
  private final Set<Path> ignoredDirectories;
  private final Predicate<PathWrapper> fileMatcher;

  private FileCollector(final ExecutionContext ctx, final Path archiveDirectory, final Set<Path> ignoredDirectories) {
    this.context = ctx;
    this.directories = new LinkedHashSet<>(); // store directories in encounter order, to create
                                              // them once.
    this.files = new TreeSet<>();
    this.archives = new TreeSet<>();
    this.archiveDirectory = archiveDirectory;
    this.ignoredDirectories = ignoredDirectories;

    this.fileMatcher = context.getFileMatcher();

    logger.debug("ignoring directories: {}", ignoredDirectories);
    logger.debug("archive directory: {}", () -> Objects.toString(archiveDirectory, "(none)"));
  }

  public static FileCollector rootFileCollector(final ExecutionContext ctx, final Path root) {
    return new FileCollector(ctx, root.resolve("WEB-INF/lib"),
        Set.of(root.resolve(".git"), root.resolve(".svn"), root.resolve("META-INF/maven")));
  }

  public static FileCollector jarFileCollector(final ExecutionContext ctx, final Path root) {
    return new FileCollector(ctx, null, Set.of(root.resolve("license"), root.resolve("META-INF/maven")));
  }

  public SortedSet<PathWrapper> getFiles() {
    return this.files;
  }

  public SortedSet<PathWrapper> getArchives() {
    return this.archives;
  }

  public Set<Path> getDirectories() {
    return this.directories;
  }

  @Override
  public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
    if (this.ignoredDirectories.contains(dir)) {
      logger.debug("ignoring directory: {}", dir);
      return FileVisitResult.SKIP_SUBTREE;
    }
    return FileVisitResult.CONTINUE;
  }

  private boolean isArchive(final PathWrapper file) {
    if ((this.archiveDirectory == null) || !file.startsWith(this.archiveDirectory)) {
      return false;
    }
    final var fn = file.getFileName();
    if (null == fn) {
      return false;
    }
    return fn.extension.equals("jar");
  }

  @Override
  public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
    final var wrapper = new PathWrapper(file);
    if (this.fileMatcher.test(wrapper)) {
      this.directories.add(file.getParent());
      if (isArchive(wrapper)) {
        logger.debug("adding new archive: {}", file);
        this.archives.add(wrapper);
      } else {
        logger.debug("adding new file: {}", file);
        this.files.add(wrapper);
      }
    } else {
      logger.debug("ignoring file: {}", file);
    }
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
    throw exc;
  }

  @Override
  public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
    return FileVisitResult.CONTINUE;
  }

}
