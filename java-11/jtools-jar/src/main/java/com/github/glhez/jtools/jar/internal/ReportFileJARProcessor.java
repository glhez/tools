package com.github.glhez.jtools.jar.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.csv.CSVPrinter;

public abstract class ReportFileJARProcessor implements JARProcessor {
  private final String reportName;
  private final Optional<ReportFile> reportFile;

  public ReportFileJARProcessor(final String reportName, final ReportFile reportFile) {
    this(reportName, Optional.of(Objects.requireNonNull(reportFile, "reportFile")));
  }

  public ReportFileJARProcessor(final String reportName, final Optional<ReportFile> reportFile) {
    this.reportName = Objects.requireNonNull(reportName, "reportName");
    this.reportFile = Objects.requireNonNull(reportFile, "reportFile");
  }

  @Override
  public final void finish() {
    reportFile.ifPresent(rf -> {
      try (var printer = rf.toCsvPrinter()) {
        finish(printer);
        System.out.println("Wrote report [" + reportName + "] to [" + rf + "]");
      } catch (final IOException e) {
        throw new UncheckedIOException("Could not write report [" + reportName + "] to [" + rf + "]", e);
      }
    });
  }

  protected abstract void finish(CSVPrinter printer) throws IOException;

}
