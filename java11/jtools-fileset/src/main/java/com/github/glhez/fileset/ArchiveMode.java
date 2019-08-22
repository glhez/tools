package com.github.glhez.fileset;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Determine how to look for file in archive.
 *
 * @author gael.lhez
 */
public enum ArchiveMode implements Predicate<CollectedFile> {
  /**
   * Always scan file in archive.
   */
  SCAN_ALWAYS {
    @Override
    public boolean test(final CollectedFile t) {
      return ARCHIVE_EXTENSIONS.contains(t.getExtension());
    }

  },

  /**
   * Never scan file in archive.
   */
  DONT_SCAN {

    @Override
    public boolean test(final CollectedFile t) {
      return false;
    }

  };

  private static final Set<String> ARCHIVE_EXTENSIONS = Set.of("ear", "war", "jar", "zip");
}
