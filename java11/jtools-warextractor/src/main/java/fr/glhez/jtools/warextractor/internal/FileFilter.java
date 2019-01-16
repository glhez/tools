package fr.glhez.jtools.warextractor.internal;

import java.io.IOException;
import java.io.InputStream;

public interface FileFilter {
  InputStream getFilteredInputStream() throws IOException;
}
