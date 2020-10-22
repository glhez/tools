package com.github.glhez.tokengrep.internal.lexer;

import java.util.Iterator;

import com.github.glhez.fileset.CollectedFile;
import com.github.glhez.tokengrep.internal.Token;

/**
 * Represent a (very) simple lexer.
 *
 * @author gael.lhez
 */
public interface Lexer extends Iterator<Token> {
  /**
   * Determine if there is more to lex.
   */
  @Override
  boolean hasNext();

  /**
   * Return a token if there is still one available.
   *
   * @return a token
   * @throws java.util.NoSuchElementException
   *           if no token remains.
   */
  @Override
  Token next();

  static Lexer createLexer(final CollectedFile file) {
    if ("java".equals(file.getExtension())) {
      return new JavaLexer(file);
    }
    throw new IllegalArgumentException("Unsupported file extension: " + file.getExtension());
  }
}
