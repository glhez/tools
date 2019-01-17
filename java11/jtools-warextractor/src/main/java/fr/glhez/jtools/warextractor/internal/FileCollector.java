package fr.glhez.jtools.warextractor.internal;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class FileCollector implements FileVisitor<Path> {
  private final SortedSet<PathWrapper> files;
  private final SortedSet<PathWrapper> archives;
  private final ExecutionContext context;

  private final Path archiveDirectory;
  private final Set<Path> ignoredDirectories;

  private FileCollector(final ExecutionContext ctx, final Path archiveDirectory, final Set<Path> ignoredDirectories) {
    this.context = ctx;
    this.files = new TreeSet<>();
    this.archives = new TreeSet<>();
    this.archiveDirectory = archiveDirectory;
    this.ignoredDirectories = ignoredDirectories;

    context.verbose(() -> String.format("ignoring directories: %s", ignoredDirectories));
    context.verbose(() -> String.format("archive directory: %s", Objects.toString(archiveDirectory, "(none)")));
  }

  public static FileCollector rootFileCollector(final ExecutionContext ctx, final Path root) {
    return new FileCollector(ctx, root.resolve("WEB-INF/lib"),
        Set.of(root.resolve(".git"), root.resolve(".svn"), root.resolve("META-INF/maven")));
  }

  public static FileCollector jarFileCollector(final ExecutionContext ctx, final Path root) {
    return new FileCollector(ctx, null, Set.of(root.resolve("license"), root.resolve("META-INF/maven")));
  }

  public SortedSet<PathWrapper> getFiles() {
    return files;
  }

  public SortedSet<PathWrapper> getArchives() {
    return archives;
  }

  @Override
  public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
    if (ignoredDirectories.contains(dir)) {
      context.verbose(() -> String.format("ignoring directory: %s", dir));
      return FileVisitResult.SKIP_SUBTREE;
    }
    return FileVisitResult.CONTINUE;
  }

  private boolean isArchive(final PathWrapper file) {
    if (archiveDirectory == null) {
      return false;
    }
    if (!file.startsWith(archiveDirectory)) {
      return false;
    }
    final var fn = file.getFileName();
    if (null == fn) {
      return false;
    }
    return fn.toString().endsWith(".jar");
  }

  @Override
  public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
    final PathWrapper wrapper = new PathWrapper(file);
    if (context.accept(wrapper)) {
      if (isArchive(wrapper)) {
        context.verbose(() -> String.format("adding new archive: %s", file));
        archives.add(wrapper);
      } else {
        files.add(wrapper);
      }
    } else {
      context.verbose(() -> String.format("ignoring file: %s", file));
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
