package com.github.glhez.tokengrep.internal;

import com.github.glhez.fileset.CollectedFile;

public class TokenLocation {
  private final CollectedFile source;
  private final int startLine;
  private final int endLine;

  public TokenLocation(final CollectedFile source, final int startLine, final int endLine) {
    this.source = source;
    this.startLine = startLine;
    this.endLine = endLine;
  }

  @Override
  public String toString() {
    final String line = startLine == endLine ? Integer.toString(startLine) : startLine + "-" + endLine;
    return source + ":" + line;
  }
}
