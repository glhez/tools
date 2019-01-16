package fr.glhez.jtools.warextractor.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;

public abstract class Extractor implements AutoCloseable {
  protected final ExecutionContext ctx;
  private final Path source;
  private final Path target;

  private final Set<Path> createdFiles;

  protected Extractor(final ExecutionContext ctx, final Path source, final Path target) {
    this.ctx = ctx;
    this.source = source;
    this.target = target;
    this.createdFiles = new HashSet<>(); // for shadow operation
  }

  @Override
  public abstract void close() throws IOException;

  /**
   * The root, as in / in archive.
   *
   * @return
   */
  protected Path getRoot() {
    return source;
  }

  /**
   * Get the archive file. Will not be in local fs if archive.
   *
   * @param path path in jarfs or localfs
   * @return a path in local fs
   * @throws IOException if we could not copy path to tmpfs
   */
  protected Path getArchiveFile(final Path path) throws IOException {
    return path;
  }

  public static Extractor newExtractor(final ExecutionContext ctx, final Path source, final Path target)
      throws IOException {
    final var attr = Files.getFileAttributeView(source, BasicFileAttributeView.class).readAttributes();
    if (attr.isRegularFile()) {
      return new ArchiveFileSystemExtractor(ctx, source, target);
    }
    if (attr.isDirectory()) {
      return new LocalFileSystemExtractor(ctx, source, target);
    }
    throw new IOException("Unsupported file type: not a regular file or directory");
  }

  public void execute() {
    final var root = getRoot();

    try {
      final FileCollector collector = FileCollector.rootFileCollector(ctx, root);
      // walkFileTree() is a little bit more efficient than find when filtering.
      Files.walkFileTree(root, collector);

      copyFiles(root, target, collector.getFiles(), source);
      copyArchives(root, target, collector.getArchives());

    } catch (final IOException e) {
      ctx.addError("Unable to read content of root [" + root + "]", source, e);
    }
  }

  protected Path relativeOutput(final Path target, final Path relativeFile) {
    if (null == relativeFile) {
      return target; // if in place and parent is null
    }
    final String s = relativeFile.toString();
    if (s.startsWith("/")) {
      return target.resolve(s.substring(1));
    }
    return target.resolve(s);
  }

  private void copyFiles(final Path root, final Path target, final SortedSet<Path> files, final Path archive) {
    ctx.msg(() -> String.format("found %d files to copy from %s", files.size(), archive));
    for (final var file : files) {
      final var relFile = root.relativize(file);
      final var relOutput = relativeOutput(target, relFile);
      try {
        ctx.execute(() -> this.createParentDirectories(relOutput, archive),
            () -> this.createParentDirectoriesShadow(relOutput, archive));
        ctx.execute(() -> this.copy(relFile, relOutput, archive), () -> this.copyShadow(relFile, relOutput, archive));
      } catch (final IOException e) {
        ctx.addError("Unable to copy file to [" + relOutput + "]", a2s(archive, file), e);
      }
    }
  }

  private void copyArchives(final Path root, final Path target, final SortedSet<Path> archives) {
    ctx.msg(() -> String.format("found %d files to extract and copy", archives.size()));
    for (final var file : archives) {
      try {
        final var relFile = root.relativize(file);
        final var relTarget = relativeOutput(target, ctx.isInPlace() ? relFile : relFile.getParent());

        final Path archive = this.getArchiveFile(file);
        try (var fs = FileSystems.newFileSystem(URI.create("jar:" + archive.toUri()), Map.of())) {
          final var archiveRoot = fs.getPath("/");

          final FileCollector collector = FileCollector.jarFileCollector(ctx, archiveRoot);
          Files.walkFileTree(archiveRoot, collector);
          copyFiles(archiveRoot, relTarget, collector.getFiles(), file);
        }
      } catch (final IOException e) {
        ctx.addError("Unable to open archive", file, e);
      }
    }
  }

  private void copy(final Path source, final Path target, final Path archive) throws IOException {
    if (Files.notExists(target)) {
      final FileFilter fileFilter = ctx.getFilter(source);
      if (null == fileFilter) {
        ctx.verbose(() -> String.format("copying unfiltered %s to %s.", a2s(archive, source), target));
        Files.copy(source, target);
      } else {
        try (var is = fileFilter.getFilteredInputStream()) {
          // try to buffer if not already done
          final var k = is instanceof BufferedInputStream || is instanceof ByteArrayInputStream ? is
              : new BufferedInputStream(is);
          ctx.verbose(() -> String.format("copying filtered %s to %s.", a2s(archive, source), target));
          Files.copy(k, target);
        }
      }
    } else {
      ctx.verbose(() -> String.format("ignoring %s (already existing at %s)", a2s(archive, source), target));
    }
  }

  private void copyShadow(final Path source, final Path target, final Path archive) {
    if (!createdFiles.contains(target)) {
      ctx.cmd("cp", "-v", a2s(archive, source), target);
    }
  }

  /**
   * Return archive to source path.
   *
   * @param archive
   * @param source
   * @return some path with a common delimiter.
   */
  private String a2s(final Path archive, final Path source) {
    return ExecutionContext.pathToString(archive + "/" + source);
  }

  /**
   * Return archive to target path.
   *
   * @param archive
   * @param target
   * @return some path with a common delimiter.
   */
  private String a2t(final Path archive, final Path target) {
    return ExecutionContext.pathToString(archive + "->" + target);
  }

  private void createParentDirectories(final Path file, final Path archive) throws IOException {
    final Path parent = file.getParent();
    // we don't test using isDirectory() : there might be some file.
    if (null != parent && Files.notExists(parent)) {
      ctx.verbose(() -> String.format("creating directory %s", a2t(archive, parent)));
      Files.createDirectories(parent);
    }
  }

  private void createParentDirectoriesShadow(final Path file, final Path archive) {
    Path parent = file.getParent();
    if (null != parent && !createdFiles.contains(parent)) {
      ctx.cmd("mkdir", "-pv", a2t(archive, parent));
      for (; parent != null; parent = parent.getParent()) {
        createdFiles.add(parent);
      }
    }
  }

  static class LocalFileSystemExtractor extends Extractor {
    protected LocalFileSystemExtractor(final ExecutionContext ctx, final Path source, final Path target) {
      super(ctx, source, target);
    }

    @Override
    public void close() throws IOException {
      // not a directory.
    }

  }

  static class ArchiveFileSystemExtractor extends Extractor {
    private final FileSystem fileSystem;

    protected ArchiveFileSystemExtractor(final ExecutionContext ctx, final Path source, final Path target)
        throws IOException {
      super(ctx, source, target);
      this.fileSystem = FileSystems.newFileSystem(URI.create("jar:" + source.toUri()), Map.of());
    }

    @Override
    public void close() throws IOException {
      fileSystem.close();
    }

    @Override
    protected Path getRoot() {
      return fileSystem.getPath("/");
    }

    @Override
    protected Path getArchiveFile(final Path path) throws IOException {
      final var s = Objects.toString(path.getFileName(), "");
      final Path tempFile = Files.createTempFile(s + "_", ".zip");
      this.ctx.verbose(() -> String.format("copying archive %s to %s", path, tempFile));
      Files.copy(path, tempFile, StandardCopyOption.REPLACE_EXISTING);
      return tempFile;
    }

  }

}
