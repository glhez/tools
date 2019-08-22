package com.github.glhez.tokengrep.internal.lexer;

import java.util.NoSuchElementException;
import java.util.Objects;

import com.github.glhez.fileset.CollectedFile;
import com.github.glhez.tokengrep.internal.Token;

class JavaLexer implements Lexer {
  private final CollectedFile file;

  public JavaLexer(final CollectedFile file) {
    this.file = Objects.requireNonNull(file, "file");
  }

  @Override
  public boolean hasNext() {
    return false;
  }

  @Override
  public Token next() {
    if (!hasNext()) {
      throw new NoSuchElementException("No token remains to read in " + file);
    }
    return null;
  }

}
