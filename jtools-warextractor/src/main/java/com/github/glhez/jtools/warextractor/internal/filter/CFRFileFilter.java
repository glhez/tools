package com.github.glhez.jtools.warextractor.internal.filter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;

/**
 * Filter class file using CFR.
 * <p>
 * The resulting file is a Java decompiled file.
 *
 * @author gael.lhez
 *
 */
public enum CFRFileFilter implements Filter {
  INSTANCE;

  /** Logger */
  private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(CFRFileFilter.class);

  private static final Map<String, String> CFR_DEFAULT_OPTIONS = Map.of("usenametable", "false", "analyseas", "CLASS");

  @Override
  public InputStreamWithCharset filter(final InputStreamWithCharset stream) throws IOException {
    final var cs = stream.getCharset();
    if (null != cs) {
      return stream;
    }

    final var byteClassSource = new ByteClassSource(stream);

    final Callable<String> task = () -> {
      final var sinkFactory = new JavaOutputSinkFactory();
      final var driver = new CfrDriver.Builder().withOptions(CFR_DEFAULT_OPTIONS)
                                                .withOutputSink(sinkFactory)
                                                .withClassFileSource(byteClassSource)
                                                .build();

      // CFR sort the list, we can't use immutability and such
      driver.analyse(new ArrayList<>(List.of(byteClassSource.path)));
      return sinkFactory.getResult();
    };

    final Future<String> future = ForkJoinPool.commonPool().submit(task);

    try {
      return stream.filter(future.get(30, TimeUnit.SECONDS));
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      logger.warn("invokation of CFR failed, invoking ASM instead", e);
      return ASMFileFilter.INSTANCE.filter(stream.filter(byteClassSource.bytes));
    }
  }

  @Override
  public String toString() {
    return "CFR";
  }

  static class ByteClassSource implements ClassFileSource {
    private final byte[] bytes;
    private final String path;

    public ByteClassSource(final InputStreamWithCharset stream) throws IOException {
      this.bytes = stream.getBytes();
      this.path = stream.getSource().toString();
    }

    @Override
    public Collection<String> addJar(final String var1) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Pair<byte[], String> getClassFileContent(final String var1) throws IOException {
      if (!var1.equals(this.path)) {
        throw new FileNotFoundException("Can't find " + var1);
      }
      return new Pair<>(this.bytes, this.path);
    }

    @Override
    public String getPossiblyRenamedPath(final String var1) {
      return var1;
    }

    @Override
    public void informAnalysisRelativePathDetail(final String var1, final String var2) {

    }

  }

  static class JavaOutputSinkFactory implements OutputSinkFactory {
    private String result;

    @Override
    public <T> Sink<T> getSink(final SinkType sinkType, final SinkClass sinkClass) {
      if (sinkType == SinkType.PROGRESS) {
        return ignored -> {};
      }
      if (sinkType == SinkType.JAVA) {
        return this::setResult;
      }
      return value -> fail(sinkType, sinkClass, value);
    }

    public String getResult() {
      return this.result;
    }

    private void setResult(final Object result) {
      this.result = Objects.toString(result, null);
    }

    private <T> void fail(final SinkType sinkType, final SinkClass sinkClass, final T value) {
      final var exception = value instanceof Throwable ? (Throwable) value : null;
      throw new IllegalStateException(
          String.format("unsupported cfr case: sinkType: %s, sinkClass: %s, value: %s", sinkType, sinkClass, value),
          exception);
    }

    @Override
    public List<SinkClass> getSupportedSinks(final SinkType arg0, final Collection<SinkClass> arg1) {
      return List.of(SinkClass.STRING, SinkClass.EXCEPTION_MESSAGE, SinkClass.DECOMPILED);
    }

  }

}
