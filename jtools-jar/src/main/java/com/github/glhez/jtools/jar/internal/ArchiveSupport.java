package com.github.glhez.jtools.jar.internal;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

import com.github.glhez.fileset.ArchivePredicate;
import com.github.glhez.fileset.CollectedFile;

public enum ArchiveSupport implements ArchivePredicate {
  ALL(Set.of("war", "ear", "zip")),
  JAKARTA_EE(Set.of("war", "ear")),
  NONE(Collections.emptySet());

  private final Set<String> extensions;

  ArchiveSupport(final Set<String> extensions) {
    this.extensions = extensions;
  }

  @Override
  public boolean isArchive(final CollectedFile file) {
    return extensions.contains(file.getExtension());
  }

  @Override
  public Predicate<CollectedFile> filter(final CollectedFile parent, final Predicate<CollectedFile> topPredicate) {
    return file -> false;
  }
}
