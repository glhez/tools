package fr.glhez.jtools.warextractor.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

public class ClassFileFilter implements FileFilter {
  private final ExecutionContext context;
  private final Path source;

  public ClassFileFilter(final ExecutionContext context, final Path source) {
    this.context = context;
    this.source = source;
  }

  @Override
  public InputStream getFilteredInputStream() throws IOException {
    context.verbose(() -> String.format("filtering [%s] using class", source));
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (var is = Files.newInputStream(source); var bis = new BufferedInputStream(is); var pw = new PrintWriter(bos)) {
      final org.objectweb.asm.ClassVisitor traceClassVisitor = new TraceClassVisitor(null,
          new org.objectweb.asm.util.Textifier(), new PrintWriter(bos));
      new ClassReader(bis).accept(traceClassVisitor,
          ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
    }
    return new ByteArrayInputStream(bos.toByteArray());
  }

}
