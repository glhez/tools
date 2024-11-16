package com.github.glhez.jtools.warextractor.internal.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 * Filter files using ASM {@link org.objectweb.asm.util.Textifier}.
 *
 * @author gael.lhez
 */
public enum ASMFileFilter implements Filter {
  INSTANCE;

  @Override
  public InputStreamWithCharset filter(final InputStreamWithCharset stream) throws IOException {
    final var cs = stream.getCharset();
    if (null != cs) {
      return stream;
    }

    final var charset = StandardCharsets.UTF_8;
    final var bos = new ByteArrayOutputStream();
    try (var pw = new PrintWriter(bos, false, charset)) {
      final org.objectweb.asm.ClassVisitor traceClassVisitor = new TraceClassVisitor(null,
          new org.objectweb.asm.util.Textifier(), pw);
      new ClassReader(stream.getStream()).accept(traceClassVisitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE
          | ClassReader.SKIP_FRAMES);
    }
    return stream.filter(bos, charset);
  }

  @Override
  public String toString() {
    return "ASM";
  }
}
