package com.github.glhez.jtools.warextractor.internal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Map;
import java.util.SortedSet;

public abstract class Extractor implements AutoCloseable {
  /** Logger */
  private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(Extractor.class);

  protected final ExecutionContext context;
  protected final FilesProxy filesProxy;
  protected final SimpleEd renamer;
  private final Path source;
  private final Path target;

  protected Extractor(final ExecutionContext ctx, final Path source, final Path target) {
    this.context = ctx;
    this.filesProxy = ctx.getFilesProxy();
    this.renamer = ctx.getArchiveRenamer();

    this.source = source;
    this.target = target;
  }

  @Override
  public abstract void close() throws IOException;

  /**
   * The root, as in / in archive.
   *
   * @return
   */
  protected Path getRoot() {
    return this.source;
  }

  /**
   * Get the archive file. Will not be in local fs if archive.
   *
   * @param path
   *          path in jarfs or localfs
   * @return a path in local fs
   * @throws IOException
   *           if we could not copy path to tmpfs
   */
  protected Path getArchiveFile(final PathWrapper path) throws IOException {
    return path.getPath();
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

    final var collector = FileCollector.rootFileCollector(this.context, root);
    try {
      // walkFileTree() is a little bit more efficient than find when filtering.
      Files.walkFileTree(root, collector);
    } catch (final IOException e) {
      logger.error("Unable to read content of [{}]", this.source, e);
      return;
    }

    createDirectories(root, this.target, collector.getDirectories(), this.source);
    copyFiles(root, this.target, collector.getFiles(), this.source);
    copyArchives(root, this.target, collector.getArchives());
    applyFiltering();
    logger.info("done processing file: {}", this.source);
  }

  protected Path relativeTarget(final Path target, final Path relativePath) {
    if (null == relativePath) {
      return target; // if in place and parent is null
    }
    final var s = relativePath.toString();
    if (s.startsWith("/")) {
      return target.resolve(s.substring(1));
    }
    return target.resolve(s);
  }

  private void createDirectories(final Path root, final Path target, final Collection<Path> directories,
      final Path archive) {
    logger.info("{}: found {} directories to create", archive.toUri(), directories.size());
    for (final var directory : directories) {
      final var relativePath = root.relativize(directory);
      final var targetDirectory = relativeTarget(target, relativePath);
      try {
        filesProxy.createDirectoriesUpTo(targetDirectory, this.target);
      } catch (final IOException e) {
        logger.error("unable to create directory [{}]", targetDirectory, e);
      }
    }
  }

  private void copyFiles(final Path root, final Path target, final SortedSet<PathWrapper> files, final Path archive) {
    logger.info("{}: found {} files to create", archive.toUri(), files.size());
    for (final var wrapper : files) {
      final var file = wrapper.getPath();
      final var relativePath = root.relativize(file);
      final var targetFile = relativeTarget(target, relativePath);

      if (filesProxy.fileWasCreated(targetFile)) {
        logger.debug("ignored [{}] (already exists at [{}])", relativePath, targetFile);
        continue;
      }

      final var parent = targetFile.getParent();
      if (!filesProxy.directoryWasCreated(parent)) {
        logger.debug("ignoring [{}] (parent directory could not be created)", targetFile);
        continue;
      }

      try {
        logger.trace("copying [{}] to [{}]", () -> file.toUri(), () -> targetFile);
        filesProxy.copy(file, targetFile);
      } catch (final IOException e) {
        logger.error("unable to copy [{}] to [{}]", file.toUri(), targetFile, e);
      }
    }
  }

  private void copyArchives(final Path root, final Path target, final SortedSet<PathWrapper> archives) {
    logger.info("{}: found {} archives to scan", root.toUri(), archives.size());
    for (final var archiveWrapper : archives) {
      final var file = archiveWrapper.getPath();
      try {
        final var relFile = root.relativize(file);
        var relTarget = relativeTarget(target, relFile.getParent());
        if (this.context.isInPlace()) {
          relTarget = relTarget.resolve(this.renamer.apply(archiveWrapper.getFileName().fileNameWithoutExtension));
        }

        final var archive = getArchiveFile(archiveWrapper);
        try (var fs = FileSystems.newFileSystem(URI.create("jar:" + archive.toUri()), Map.of())) {
          final var archiveRoot = fs.getPath("/");

          final var collector = FileCollector.jarFileCollector(this.context, archiveRoot);
          Files.walkFileTree(archiveRoot, collector);
          createDirectories(archiveRoot, relTarget, collector.getDirectories(), file);
          copyFiles(archiveRoot, relTarget, collector.getFiles(), file);
        }
      } catch (final IOException e) {
        logger.error("unable to open archive [{}] to [{}]", file.toUri(), e);
      }
    }
  }

  private void applyFiltering() {
    final var copiedFiles = filesProxy.getCopiedFiles();
    logger.info("{}: applying filtering on {} files.", () -> source.toUri(), () -> copiedFiles.size());
    context.getFilterChain().filter(copiedFiles);
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
    private final MessageDigest digest;
    private final byte[] buffer;
    private final Path cacheDirectory;

    protected ArchiveFileSystemExtractor(final ExecutionContext ctx, final Path source, final Path target)
        throws IOException {
      super(ctx, source, target);
      this.fileSystem = FileSystems.newFileSystem(URI.create("jar:" + source.toUri()), Map.of());
      this.cacheDirectory = ctx.getCacheDirectory();
      if (null != this.cacheDirectory) {
        try {
          this.digest = MessageDigest.getInstance("SHA-1");
        } catch (final NoSuchAlgorithmException e) {
          throw new IllegalStateException("missing SHA-1 algo", e);
        }
        this.buffer = new byte[2 * 1024 * 1024];
      } else {
        this.digest = null;
        this.buffer = null;
      }
    }

    @Override
    public void close() throws IOException {
      this.fileSystem.close();
    }

    @Override
    protected Path getRoot() {
      return this.fileSystem.getPath("/");
    }

    @Override
    protected Path getArchiveFile(final PathWrapper wrapper) throws IOException {
      final var path = wrapper.getPath();
      final var fileName = wrapper.getFileName().fileNameWithoutExtension;

      if (null == this.cacheDirectory) {
        final var tempFile = Files.createTempFile(fileName + "-", ".zip");
        logger.debug("copying archive {} to {}", path, tempFile);
        Files.copy(path, tempFile, StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
      }

      logger.debug("computing archive {} checksum", path);
      this.digest.reset();
      try (var is = Files.newInputStream(path); var bis = new BufferedInputStream(is)) {
        for (var n = 0; -1 != (n = bis.read(this.buffer, 0, this.buffer.length));) {
          this.digest.update(this.buffer, 0, n);
        }
      }
      final var sha1 = bytesToHex(this.digest.digest());

      final var result = this.cacheDirectory.resolve(fileName + "-" + sha1 + ".zip");

      logger.debug("checksum is {}, cache file is {}", sha1, result);

      if (Files.notExists(result)) {
        final var parent = result.getParent();
        logger.debug("creating parent directory tree: {}", parent);
        Files.createDirectories(result.getParent());
        logger.debug("copying archive {} to {}", path, result);
        Files.copy(path, result, StandardCopyOption.REPLACE_EXISTING);
      }
      return result;
    }

    private final static char[] hexArray = "0123456789abcdef".toCharArray();

    public static String bytesToHex(final byte[] bytes) {
      final var hexChars = new char[bytes.length * 2];
      for (var j = 0; j < bytes.length; j++) {
        final var v = bytes[j] & 0xFF;
        hexChars[j * 2] = hexArray[v >>> 4];
        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
      }
      return new String(hexChars);
    }
  }

}
