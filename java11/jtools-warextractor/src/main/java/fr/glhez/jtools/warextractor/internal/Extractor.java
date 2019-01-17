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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

  private void copyFiles(final Path root, final Path target, final SortedSet<PathWrapper> files, final Path archive) {
    ctx.msg(() -> String.format("found %d files to copy from %s", files.size(), archive));
    for (final var wrapper : files) {
      final var file = wrapper.getPath();
      final var relFile = root.relativize(file);
      final var relOutput = relativeOutput(target, relFile);
      try {
        ctx.execute(() -> this.createParentDirectories(relOutput, archive),
            () -> this.createParentDirectoriesShadow(relOutput, archive));
        ctx.execute(() -> this.copy(wrapper, relOutput, archive), () -> this.copyShadow(relFile, relOutput, archive));
      } catch (final IOException e) {
        ctx.addError("Unable to copy file to [" + relOutput + "]", a2s(archive, file), e);
      }
    }
  }

  private void copyArchives(final Path root, final Path target, final SortedSet<PathWrapper> archives) {
    ctx.msg(() -> String.format("found %d files to extract and copy", archives.size()));
    for (final var wrapper : archives) {
      final var file = wrapper.getPath();
      try {
        final var relFile = root.relativize(file);
        var relTarget = relativeOutput(target, relFile.getParent());
        if (ctx.isInPlace()) {
          relTarget = relTarget.resolve(ctx.rename(wrapper));
        }

        final Path archive = this.getArchiveFile(wrapper);
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

  private void copy(final PathWrapper wrapper, final Path target, final Path archive) throws IOException {
    final var source = wrapper.getPath();
    if (Files.notExists(target)) {
      final FileFilter fileFilter = ctx.getFilter(wrapper);
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
    private final MessageDigest digest;
    private final byte[] buffer;
    private final Path cacheDirectory;

    protected ArchiveFileSystemExtractor(final ExecutionContext ctx, final Path source, final Path target)
        throws IOException {
      super(ctx, source, target);
      this.fileSystem = FileSystems.newFileSystem(URI.create("jar:" + source.toUri()), Map.of());
      this.cacheDirectory = ctx.getCacheDirectory();
      if (null != cacheDirectory) {
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
      fileSystem.close();
    }

    @Override
    protected Path getRoot() {
      return fileSystem.getPath("/");
    }

    @Override
    protected Path getArchiveFile(final PathWrapper wrapper) throws IOException {
      final var path = wrapper.getPath();
      final var fileName = removeArchiveExtension(Objects.toString(wrapper.getFileName(), ""));

      if (null == cacheDirectory) {
        final Path tempFile = Files.createTempFile(fileName + "-", ".zip");
        this.ctx.verbose(() -> String.format("copying archive %s to %s", path, tempFile));
        Files.copy(path, tempFile, StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
      }

      this.ctx.verbose(() -> String.format("computing archive %s checksum", path));
      digest.reset();
      try (var is = Files.newInputStream(path); var bis = new BufferedInputStream(is)) {
        for (int n = 0; -1 != (n = is.read(buffer, 0, buffer.length));) {
          digest.update(buffer, 0, n);
        }
      }
      final var sha1 = bytesToHex(digest.digest());

      final var result = cacheDirectory.resolve(fileName + "-" + sha1 + ".zip");

      this.ctx.verbose(() -> String.format("checksum is %s: file=%s.", sha1, result));

      if (Files.notExists(result)) {
        this.ctx.verbose(() -> String.format("creating parent directories: %s", result.getParent()));
        Files.createDirectories(result.getParent());
        this.ctx.verbose(() -> String.format("copying archive %s to %s", path, result));
        Files.copy(path, result, StandardCopyOption.REPLACE_EXISTING);
      }
      return result;
    }

    private final static char[] hexArray = "0123456789abcdef".toCharArray();

    public static String bytesToHex(final byte[] bytes) {
      final char[] hexChars = new char[bytes.length * 2];
      for (int j = 0; j < bytes.length; j++) {
        final int v = bytes[j] & 0xFF;
        hexChars[j * 2] = hexArray[v >>> 4];
        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
      }
      return new String(hexChars);
    }

    private String removeArchiveExtension(final String filename) {
      final int n = filename.lastIndexOf(".");
      if (n == -1 || !".jar".equals(filename.substring(n))) {
        return filename;
      }
      return filename.substring(0, n);
    }
  }

}
