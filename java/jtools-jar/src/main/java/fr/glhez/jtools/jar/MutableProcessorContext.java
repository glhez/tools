package fr.glhez.jtools.jar;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MutableProcessorContext implements ProcessorContext {
  private JARInformation jarInformation;
  private final Map<JARInformation, List<String>> errors;

  public MutableProcessorContext() {
    this.errors = new LinkedHashMap<>();
  }

  public void setSource(final JARInformation information) {
    this.jarInformation = Objects.requireNonNull(information, "jarInformation");
  }

  public Map<JARInformation, List<String>> getErrors() {
    return errors;
  }

  @Override
  public JARInformation getJARInformation() {
    return jarInformation;
  }

  @Override
  public void addError(final String message) {
    requireNonNull(message, "message");
    errors.computeIfAbsent(jarInformation, (key) -> new ArrayList<>()).add(message);
  }

}
