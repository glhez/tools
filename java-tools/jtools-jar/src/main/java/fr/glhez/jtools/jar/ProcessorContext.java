package fr.glhez.jtools.jar;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

public interface ProcessorContext {
  /**
   * Source being processed.
   * 
   * @return some file.
   */
  Path getSource();

  /**
   * Add an error for the current source.
   * 
   * @param message some message (not null).
   */
  void addError(String message);

  /**
   * Add an error for the current source.
   * 
   * @param exception some exception (not null).
   */
  default void addError(final Exception exception) {
    requireNonNull(exception, "exception");
    addError(exception.getMessage());
  }
}
