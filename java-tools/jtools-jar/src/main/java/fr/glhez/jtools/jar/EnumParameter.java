package fr.glhez.jtools.jar;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.cli.Option;

public class EnumParameter<E extends Enum<E>> {
  private final Function<String, E> valueOf;
  private final Supplier<E[]> values;
  private final Optional<E> defaultValue;

  private EnumParameter(final Function<String, E> valueOf, final Supplier<E[]> values, final Optional<E> defaultValue) {
    this.valueOf = Objects.requireNonNull(valueOf, "valueOf");
    this.values = Objects.requireNonNull(values, "values");
    this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
  }

  public static <E extends Enum<E>> EnumParameter<E> parameter(final Function<String, E> valueOf,
      final Supplier<E[]> values) {
    return new EnumParameter<>(valueOf, values, Optional.empty());
  }

  public static <E extends Enum<E>> EnumParameter<E> parameter(final Function<String, E> valueOf,
      final Supplier<E[]> values, final E defaultValue) {
    Objects.requireNonNull(defaultValue, "defaultValue");
    return new EnumParameter<>(valueOf, values, Optional.of(defaultValue));
  }

  public E parse(final Option option, final String value) {
    final String v = Objects.toString(value, "").trim();
    if (v.isEmpty()) {
      return defaultValue.orElse(null);
    }

    // now transform
    final String uv = toEnumName(v);
    try {
      return valueOf.apply(uv);
    } catch (final IllegalArgumentException | NullPointerException e) {
      throw new IllegalArgumentException(
          "Invalid value [" + value + "] for '--" + option.getLongOpt() + "' option: try "
              + Arrays.stream(values.get()).map(EnumParameter::toUserFriendlyName).collect(Collectors.joining(", ")),
          e);
    }
  }

  private static String toEnumName(final String value) {
    return value.toUpperCase().replaceAll("\\s+", "_");
  }

  private static <E extends Enum<E>> String toUserFriendlyName(final E value) {
    return value.name().toLowerCase().replaceAll("_", " ");
  }
}
