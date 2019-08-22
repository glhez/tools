package com.github.glhez.tokengrep.internal;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Type of token we may find in a file.
 *
 * @author gael.lhez
 */
public enum TokenType {
  /**
   * A {@link String}.
   */
  STRING,
  /**
   * An identifier.
   */
  IDENTIFIER("id"),
  /**
   * A one line comment.
   */
  COMMENT_ONELINE("comment-one", "comment-1", "com-1"),
  /**
   * A multiline comment.
   */
  COMMENT_MULTILINE("comment-mul", "comment-multi"),
  /**
   * A javadoc comment.
   */
  COMMENT_JAVADOC("javadoc");

  private final Set<String> aliases;

  private TokenType(final String... aliases) {
    this.aliases = Set.of(aliases);
  }

  private static final Map<String, EnumSet<TokenType>> MAPPING;
  private static final Pattern SPLITTER = Pattern.compile(",");

  static {
    final var map = new HashMap<String, EnumSet<TokenType>>();

    for (final var type : TokenType.values()) {
      final var set = EnumSet.of(type);

      final var lc = type.name().toLowerCase();
      map.put(lc, set);
      map.put(lc.replace('_', '-'), set);
      type.aliases.forEach(alias -> map.put(alias.toLowerCase(), set));
    }

    map.put("comment", EnumSet.of(COMMENT_JAVADOC, COMMENT_MULTILINE, COMMENT_ONELINE));

    MAPPING = Map.copyOf(map);
  }

  public static Predicate<TokenType> buildPredicate(final String types) {
    // @formatter:off
    final EnumSet<TokenType> tt = SPLITTER.splitAsStream(types.toLowerCase())
            .map(String::strip)
            .flatMap(TokenType::getOrFail)
            .collect(() -> EnumSet.noneOf(TokenType.class), EnumSet::add, EnumSet::addAll);
    // @formatter:on

    return tt::contains;
  }

  private static Stream<TokenType> getOrFail(final String type) {
    final var enumSet = MAPPING.get(type);
    if (null == enumSet) {
      throw new IllegalArgumentException("invalid token type: " + type);
    }
    return enumSet.stream();
  }
}
