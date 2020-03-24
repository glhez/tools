package com.github.glhez.tokengrep.internal;

/**
 * Represents a token.
 *
 * @author gael.lhez
 */
public class Token {
  public final TokenType type;
  public final TokenLocation location;
  public final String value;

  public Token(final TokenType type, final TokenLocation location, final String value) {
    this.type = type;
    this.location = location;
    this.value = value;
  }

}
