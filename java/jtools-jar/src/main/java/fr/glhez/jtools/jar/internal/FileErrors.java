package fr.glhez.jtools.jar.internal;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class FileErrors implements Iterable<FileErrors.FileError> {
  private final List<FileError> errors;

  public FileErrors() {
    this.errors = new ArrayList<>();
  }

  public void addError(final Path path, final String message) {
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(message, "message");
    this.errors.add(new FileError(path, message));
  }

  public void addError(final Path path, final Exception exception) {
    Objects.requireNonNull(exception, "exception");
    addError(path, exception.getMessage());
  }

  public boolean isEmpty() {
    return errors.isEmpty();
  }

  @Override
  public Iterator<FileErrors.FileError> iterator() {
    return errors.iterator();
  }

  public static class FileError {
    public final Path path;
    public final String message;

    private FileError(final Path path, final String message) {
      this.path = Objects.requireNonNull(path, "path");
      this.message = Objects.requireNonNull(message, "message");
    }

    @Override
    public String toString() {
      return path + ": " + message;
    }
  }

}
