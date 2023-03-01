package com.github.glhez.fileset;

import java.util.Objects;

/**
 * Represent some error when processing a file.
 *
 * @author gael.lhez
 */
public record FileError(CollectedFile file, String error) {
  public FileError {
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(error, "error");
  }

  @Override
  public String toString() {
    return file + ": " + error;
  }
}
