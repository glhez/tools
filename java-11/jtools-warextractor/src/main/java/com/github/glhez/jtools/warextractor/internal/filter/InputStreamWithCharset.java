package com.github.glhez.jtools.warextractor.internal.filter;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * An {@link InputStream} with an optional {@link Charset} information.
 * <p>
 *
 * @author gael.lhez
 *
 */
public class InputStreamWithCharset implements AutoCloseable {
  private final Path source;
  private final InputStream stream;
  private final Charset charset;

  private InputStreamWithCharset(final Path source, final InputStream stream, final Charset charset) {
    this.source = Objects.requireNonNull(source, "source");
    this.stream = Objects.requireNonNull(stream, "stream");
    this.charset = charset;
  }

  public static InputStreamWithCharset open(final Path path) throws IOException {
    return new InputStreamWithCharset(path, new ByteArrayInputStream(Files.readAllBytes(path)), null);
  }

  public InputStreamWithCharset filter(final byte[] data, final Charset charset) {
    return new InputStreamWithCharset(source, new ByteArrayInputStream(data), charset);
  }

  public InputStreamWithCharset filter(final byte[] data) {
    return filter(data, null);
  }

  public InputStreamWithCharset filter(final ByteArrayOutputStream bos, final Charset charset) {
    return filter(bos.toByteArray(), charset);
  }

  public InputStreamWithCharset filter(final String s) {
    return filter(s.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
  }

  public InputStreamWithCharset filter(final StringBuilder sb) {
    return filter(sb.toString());
  }

  public Path getSource() {
    return source;
  }

  public InputStream getStream() {
    return stream;
  }

  /**
   * Get the charset if the resulting stream is now a text.
   *
   * @return
   */
  public Charset getCharset() {
    return charset;
  }

  /**
   * Get the entire content of the stream.
   * <p>
   * This method will not work as intended if the stream was already read.
   *
   * @throws IOException
   */
  public byte[] getBytes() throws IOException {
    return stream.readAllBytes();
  }

  /**
   * Get the entire content of the stream.
   * <p>
   * This method will not work as intended if the stream was already read.
   *
   * @throws IOException
   */
  public String getString() throws IOException {
    return getString(null);
  }

  /**
   * Get the entire content of the stream.
   * <p>
   * This method will not work as intended if the stream was already read.
   *
   * @param charset a custom charset to use if none is set {@link #getCharset()}.
   * @throws IOException
   */
  public String getString(final Charset charset) throws IOException {
    var cs = this.charset;
    if (null == cs) {
      cs = charset;
    }
    if (null == cs) {
      throw new IOException("unable to convert stream built of [" + source + "] to String: no charset");
    }
    return new String(stream.readAllBytes(), cs);
  }

  @Override
  public void close() throws IOException {
    stream.close();
  }

  public InputStreamReader toReader() throws IOException {
    if (null == charset) {
      throw new IOException("unable to convert stream built of [" + source + "] to InputStreamReader: no charset");
    }
    return new InputStreamReader(stream, charset);
  }

  public InputStream getBufferedStream() {
    if (stream instanceof BufferedInputStream || stream instanceof ByteArrayInputStream) {
      return stream;
    }
    return new BufferedInputStream(stream);
  }
}
