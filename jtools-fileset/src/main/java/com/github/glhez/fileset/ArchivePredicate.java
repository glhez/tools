package com.github.glhez.fileset;

import java.util.function.Predicate;

/**
 * Determine how we navigate into archive.
 */
public interface ArchivePredicate {
  /**
   * Indicate if file is an archive.
   */
  boolean isArchive(CollectedFile file);

  /**
   * Produce a predicate for navigating into files of parent.
   */
  Predicate<CollectedFile> filter(CollectedFile parent, Predicate<CollectedFile> topPredicate);

}
