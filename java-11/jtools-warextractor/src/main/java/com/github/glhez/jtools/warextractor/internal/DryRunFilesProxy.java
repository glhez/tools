package com.github.glhez.jtools.warextractor.internal;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;

public class DryRunFilesProxy extends AbstractFilesProxy {
  /** Logger */
  private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager
      .getLogger("dry-run");

  private void log(final Object... args) {
    logger.info(Arrays.stream(args).map(this::convert).map(StringEscapeUtils::escapeXSI).collect(joining(" ")));
  }

  @Override
  public Set<Path> getCopiedFiles() {
    return Set.of(); // no files were copied
  }

  @Override
  public void delete(final Path start) throws IOException {
    log("rm", "-f", start);
  }

  @Override
  public void recursiveDelete(final Path start) throws IOException {
    log("rm", "-Rf", start);
  }

  @Override
  public void createDirectories(final Path directory, final FileAttribute<?>... attrs) throws IOException {
    log("mkdir", "-p", directory);
    addDirectoryTree(directory);
  }

  @Override
  public void createDirectory(final Path directory, final FileAttribute<?>... attrs) throws IOException {
    log("mkdir", directory);
    addDirectory(directory);
  }

  @Override
  public void createDirectoriesUpTo(final Path directory, final Path limit, final FileAttribute<?>... attrs)
      throws IOException {
    if (null != directory && !this.directoryWasCreated(directory) && !limit.equals(directory)) {
      // do the parent first
      createDirectoriesUpTo(directory.getParent(), limit);
      this.createDirectory(directory, attrs);
    }
  }

  @Override
  public void copy(final Path source, final Path target, final CopyOption... options) throws IOException {
    log("cp", source.toUri(), target);
    addFile(target);
  }

  private String convert(final Object arg) {
    if (arg instanceof PathWrapper) {
      return ((PathWrapper) arg).getFullPath();
    }
    if (arg instanceof Path) {
      return PathWrapper.pathToString((Path) arg);
    }
    return Objects.toString(arg, null);
  }

}
