package com.github.glhez.jtools.text;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * How a new line is wrote.
 *
 * @author gael.lhez
 */
public enum LineSeparator {
  CR("\r"),
  LF("\n"),
  CRLF("\r\n");

  private final String value;

  private LineSeparator(final String value) {
    this.value = value;
  }

  private static final Map<String, LineSeparator> MAPPING = Arrays.stream(LineSeparator.values())
      .collect(collectingAndThen(toMap(LineSeparator::toString, x -> x), Collections::unmodifiableMap));

  /**
   * Get the {@link LineSeparator} out of {@link String}.
   *
   * @param value some value
   * @return an {@link Optional} being empty if the mapping could not be found.
   */
  public static Optional<LineSeparator> of(final String value) {
    return Optional.ofNullable(MAPPING.get(value));
  }

  public static Optional<LineSeparator> ofSystemLineSeparator() {
    return of(System.lineSeparator());
  }

  @Override
  public String toString() {
    return value;
  }
}
