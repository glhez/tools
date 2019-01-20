package fr.glhez.jtools.warextractor.internal;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

public class FileDeletor implements FileVisitor<Path> {
  /** Logger */
  private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager
      .getLogger(FileDeletor.class);

  private final FilesProxy proxy;

  public FileDeletor(final FilesProxy proxy) {
    this.proxy = Objects.requireNonNull(proxy, "proxy");
  }

  @Override
  public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
    logger.debug("entering directory: {}", dir);
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
    logger.debug("trying to delete file: {}", file);
    proxy.delete(file);
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
    logger.warn("failed to delete file: {}", file, exc);
    throw new IOException("Could not delete file [" + file + "]", exc); // rethrow
  }

  @Override
  public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
    if (null != exc) {
      logger.error("deleting directory {} can't be done due to prior exception", dir, exc);
      throw exc;
    }
    proxy.delete(dir);
    return FileVisitResult.CONTINUE;
  }

}
