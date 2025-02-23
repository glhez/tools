package com.github.glhez.jtools.jar.internal;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

@SuppressWarnings("java:S106")
public class ListJARProcessor implements JARProcessor {
  private final List<JARProcessor> processors;

  public ListJARProcessor(final List<JARProcessor> processors) {
    this.processors = new ArrayList<>(requireNonNull(processors, "processors"));
  }

  @Override
  public void init() {
    processors.forEach(this::init);
  }

  private void init(final JARProcessor processor) {
    System.out.println("initializing " + processor);
    processor.init();
  }

  @Override
  public void process(final ProcessorContext context, final JarFile jarFile) {
    processors.forEach(processor -> processor.process(context, jarFile));
  }

  @Override
  public void finish() {
    processors.forEach(JARProcessor::finish);
  }

}
