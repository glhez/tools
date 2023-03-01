/**
 * @author gael.lhez
 */
module com.github.glhez.jtools.jar {
  exports com.github.glhez.jtools.jar;

  requires com.github.glhez.fileset;
  requires org.apache.logging.log4j;
  requires info.picocli;
  requires org.apache.commons.csv;
  requires java.sql; // due to org.apache.commons.csv

  opens com.github.glhez.jtools.jar to info.picocli;

  uses java.nio.file.spi.FileSystemProvider;


}
