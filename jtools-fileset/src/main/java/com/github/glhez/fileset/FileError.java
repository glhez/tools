package com.github.glhez.fileset;

import java.util.Objects;

/**
 * Represent some error when processing a file.
 *
 * @author gael.lhez
 */
public class FileError {
  private final CollectedFile file;
  private final String error;

  public FileError(final CollectedFile file, final String error) {
    this.file = Objects.requireNonNull(file, "file");
    this.error = Objects.requireNonNull(error, "error");
  }

  public CollectedFile getFile() {
    return file;
  }

  public String getError() {
    return error;
  }

  @Override
  public String toString() {
    return file + ": " + error;
  }
}
