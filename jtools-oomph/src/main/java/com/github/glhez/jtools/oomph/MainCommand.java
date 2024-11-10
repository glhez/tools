package com.github.glhez.jtools.oomph;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(mixinStandardHelpOptions = true)
public class MainCommand implements Callable<Integer> {

  @Option(names = { "-i", "--input" }, description = "A Java Input file returning a MacroElement", required = true)
  private Path inputFile;
  @Option(names = { "-o", "--output" }, description = "Output file", required = true)
  private Path outputFile;

  public static void main(final String[] args) {
    new picocli.CommandLine(new MainCommand()).execute(args);
  }

  @Override
  public Integer call() throws IOException, XMLStreamException, TransformerException, ReflectiveOperationException {
    if (!compileInput()) {
      return CommandLine.ExitCode.SOFTWARE;
    }

    var url = inputFile.getParent().toUri().toURL();
    var className = inputFile.getFileName().toString().replaceAll("\\.java$", "");

    System.out.printf("Loading class %s from %s%n", className, url);

    try (var cl = new URLClassLoader(new URL[] { url })) {
      Class<?> javaType = cl.loadClass(className);
      if (javaType.isAssignableFrom(Supplier.class)) {
        System.err.printf("type %s does not implements %s%n", javaType.getName(), Supplier.class.getName());
        return CommandLine.ExitCode.SOFTWARE;
      }
      @SuppressWarnings("unchecked")
      var o = (Supplier<Object>) javaType.getConstructor().newInstance();

      var result = o.get();

      if (!(result instanceof MacroElement)) {
        System.err.printf("result %s does not implements %s%n", result == null ? "null" : result.getClass().getName(),
                          MacroElement.class.getName());
        return CommandLine.ExitCode.SOFTWARE;
      }
      var macro = MacroElement.class.cast(result);
      writeToOutputFile(macro);
      return CommandLine.ExitCode.OK;
    }
  }

  private boolean compileInput() {
    System.out.printf("About to compile: %s%n", inputFile);
    var diagnostics = new DiagnosticCollector<JavaFileObject>();
    var compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
    var fm = compiler.getStandardFileManager(diagnostics, Locale.ENGLISH, StandardCharsets.UTF_8);
    var units = fm.getJavaFileObjects(inputFile);

    var task = compiler.getTask(
                                null,
                                fm,
                                diagnostics,
                                List.of(
                                        "--class-path", System.getProperty("java.class.path"),
                                        "--module-path", System.getProperty("jdk.module.path")),
                                null,
                                units);
    task.addModules(List.of(this.getClass().getModule().getName()));
    task.setLocale(Locale.ENGLISH);

    if (!Boolean.TRUE.equals(task.call())) {
      System.err.printf("Compilation failed:%n");
      for (var diagnostic : diagnostics.getDiagnostics()) {
        System.err.printf("%s%n", diagnostic);
      }
      return false;
    }
    return true;
  }

  private void writeToOutputFile(final XmlWriter xmlw) throws IOException, XMLStreamException, TransformerException {
    System.out.printf("About to write XML file: %s%n", outputFile);
    var outFactory = XMLOutputFactory.newInstance();
    var transformerFactory = TransformerFactory.newInstance();
    transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

    try (var bos = new ByteArrayOutputStream()) {
      var writer = outFactory.createXMLStreamWriter(bos);
      xmlw.write(writer);

      try (var os = Files.newOutputStream(outputFile); var bufferedOutputStream = new BufferedOutputStream(os)) {
        var transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(
                              new StreamSource(new ByteArrayInputStream(bos.toByteArray())),
                              new StreamResult(bufferedOutputStream));
      }
    }
  }

}
