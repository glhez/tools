package fr.glhez.jtools.jar;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MutableProcessorContext implements ProcessorContext {
  private Path source;
  private final Map<Path, List<String>> errors;

  public MutableProcessorContext() {
    this.errors = new LinkedHashMap<>();
  }

  public void setSource(final Path source) {
    this.source = Objects.requireNonNull(source, "source");
  }

  public Map<Path, List<String>> getErrors() {
    return errors;
  }

  @Override
  public Path getSource() {
    return source;
  }

  @Override
  public void addError(final String message) {
    requireNonNull(message, "message");
    errors.computeIfAbsent(source, (key) -> new ArrayList<>()).add(message);
  }

}
