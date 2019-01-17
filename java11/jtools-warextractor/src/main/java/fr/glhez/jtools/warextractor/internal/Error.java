package fr.glhez.jtools.warextractor.internal;

import java.io.PrintStream;
import java.io.PrintWriter;

public class Error {
  private final String message;
  private final String resource;
  private final Exception exception;

  public Error(final String message, final String resource, final Exception exception) {
    this.message = message;
    this.resource = resource;
    this.exception = exception;
  }

  public String getMessage() {
    return message;
  }

  public String getResource() {
    return resource;
  }

  @Override
  public String toString() {
    if (resource == null) {
      return message;
    }
    return message + " [resource: '" + resource + "']";
  }

  public void printStackTrace() {
    if (null != exception) {
      exception.printStackTrace();
    }
  }

  public void printStackTrace(final PrintStream s) {
    if (null != exception) {
      exception.printStackTrace(s);
    }
  }

  public void printStackTrace(final PrintWriter s) {
    if (null != exception) {
      exception.printStackTrace(s);
    }
  }
}