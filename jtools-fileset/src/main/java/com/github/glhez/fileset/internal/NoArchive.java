package com.github.glhez.fileset.internal;

import java.util.function.Predicate;

import com.github.glhez.fileset.ArchivePredicate;
import com.github.glhez.fileset.CollectedFile;

public enum NoArchive implements ArchivePredicate {
  INSTANCE;

  @Override
  public boolean isArchive(final CollectedFile file) {
    return false;
  }

  @Override
  public Predicate<CollectedFile> filter(final CollectedFile parent, final Predicate<CollectedFile> topPredicate) {
    return null;
  }

}
