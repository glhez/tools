package com.github.glhez.jtools.warextractor.internal;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

public class SimpleEd {

  private final Function<String, String> f;

  private SimpleEd(final Function<String, String> f) {
    this.f = f;
  }

  public String apply(final String value) {
    return f.apply(value);
  }

  public static SimpleEd newSimpleEd(final List<String> commands) {
    requireNonNull(commands, "commands");
    if (commands.isEmpty()) {
      return new SimpleEd(s -> s);
    }
    Function<String, String> function = null;
    for (final var mask : commands) {
      final var n = findIndexOfDelimiter(mask, 0, "no type / delimiter");
      final var m = findIndexOfDelimiter(mask, n + 1, "no replace / delimiter");
      final var o = findIndexOfDelimiter(mask, m + 1, "replace not ended by / delimiter");

      final var type = mask.substring(0, n);
      final var patternStr = mask.substring(n + 1, m);
      final var replace = mask.substring(m + 1, o);
      final var options = mask.substring(o + 1);

      if (!"s".equals(type)) {
        throw newIllegalArgumentException(mask, "invalid type: [" + type + "] (expected: 's')");
      }
      int patternFlags = 0;
      boolean all = false;
      for (int i = 0, len = options.length(); i < len; ++i) {
        final char c = options.charAt(i);
        switch (c) {
          case 'i':
            patternFlags |= Pattern.CASE_INSENSITIVE;
          break;
          case 'g':
            all = true;
          break;
          default:
            throw newIllegalArgumentException(mask, "invalid options [" + c + "]: expected i, g");
        }
      }

      final var pattern = Pattern.compile(patternStr, patternFlags);
      final var matcher = pattern.matcher(""); // not threadsafe at all (we are not in this case)
      final Function<String, String> fctor;
      if (all) {
        fctor = s -> matcher.reset(s).replaceAll(replace);
      } else {
        fctor = s -> matcher.reset(s).replaceFirst(replace);
      }
      if (null == function) {
        function = fctor;
      } else {
        function = function.andThen(fctor);
      }
    }
    return new SimpleEd(function);
  }

  private static int findIndexOfDelimiter(final String mask, final int from, final String errorMessage) {
    final var n = mask.indexOf('/', from);
    if (n == -1) {
      throw newIllegalArgumentException(mask, errorMessage);
    }
    return n;
  }

  private static IllegalArgumentException newIllegalArgumentException(final String pattern, final String message) {
    return new IllegalArgumentException("invalid pattern: [" + pattern + "]: " + message);
  }

}
