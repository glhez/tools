package fr.glhez.jtools.jar;

import static java.util.Objects.requireNonNull;

public interface ProcessorContext {
  /**
   * Source being processed.
   *
   * @return some file.
   */
  JARInformation getJARInformation();

  /**
   * Add an error for the current jarInformation.
   *
   * @param message some message (not null).
   */
  void addError(String message);

  /**
   * Add an error for the current jarInformation.
   *
   * @param exception some exception (not null).
   */
  default void addError(final Exception exception) {
    requireNonNull(exception, "exception");
    addError(exception.getMessage());
  }
}
