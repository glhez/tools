package fr.glhez.jtools.warextractor.internal.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import fr.glhez.jtools.warextractor.internal.ExecutionContext;

/**
 * Filter files using ASM {@link org.objectweb.asm.util.Textifier}.
 *
 * @author gael.lhez
 */
public class ASMFileFilter implements InputStreamFilter {

  @Override
  public InputStreamWithCharset filter(final ExecutionContext context, final InputStreamWithCharset stream)
      throws IOException {
    context.verbose(() -> String.format("filtering [%s] using asm", stream.getSource()));

    final var cs = stream.getCharset();
    if (null != cs) {
      context.msg(
          () -> String.format("file [%s] was already filtering using binary to %s filter.", stream.getSource(), cs));
      return stream;
    }

    final var charset = StandardCharsets.UTF_8;
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (var pw = new PrintWriter(bos, false, charset)) {
      final org.objectweb.asm.ClassVisitor traceClassVisitor = new TraceClassVisitor(null,
          new org.objectweb.asm.util.Textifier(), pw);
      new ClassReader(stream.getStream()).accept(traceClassVisitor,
          ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
    }
    return stream.filter(bos, charset);
  }

}
