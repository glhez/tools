package com.github.glhez.tokengrep.internal;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TokenMatcher {
  private static final Pattern SPLITTER = Pattern.compile("\\s*:\\s*");

  private final Predicate<TokenType> typePredicate;
  private final Pattern pattern;

  private TokenMatcher(final Predicate<TokenType> typePredicate, final Pattern pattern) {
    this.typePredicate = typePredicate;
    this.pattern = pattern;
  }

  public static TokenMatcher parse(final String pattern) {
    try {
      final String[] array = SPLITTER.split(pattern, 2);
      if (array.length == 1) {
        return new TokenMatcher(n -> true, Pattern.compile(array[0]));
      }
      return new TokenMatcher(TokenType.buildPredicate(array[0]), Pattern.compile(array[1]));
    } catch (final IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid pattern [" + pattern + "]: " + e.getMessage());
    }
  }

  public Stream<?> match(final Token token) {
    return Stream.empty();
  }
}
