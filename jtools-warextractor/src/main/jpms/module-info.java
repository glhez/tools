/**
 * @author gael.lhez
 */
module com.github.glhez.jtools.warextractor {
  exports com.github.glhez.jtools.warextractor;

  requires info.picocli;
  requires org.apache.commons.lang3;
  requires org.apache.commons.text;
  requires cfr;

  requires org.objectweb.asm;
  requires org.objectweb.asm.util;
  requires org.apache.logging.log4j;

  opens com.github.glhez.jtools.warextractor to info.picocli;

  uses java.nio.file.spi.FileSystemProvider;

}
