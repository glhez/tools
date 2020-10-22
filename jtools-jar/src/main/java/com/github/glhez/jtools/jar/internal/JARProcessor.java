package com.github.glhez.jtools.jar.internal;

import java.util.jar.JarFile;

public interface JARProcessor {
  /**
   * Initialize the processor.
   */
  void init();

  /**
   * Process one JAR file.
   *
   * @param context
   *          a context.
   * @param jarFile
   *          a {@link JarFile} representing the jar.
   */
  void process(ProcessorContext context, JarFile jarFile);

  /**
   * Finish the process.
   */
  void finish();
}
