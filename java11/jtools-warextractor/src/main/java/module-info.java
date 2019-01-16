/**
 * @author gael.lhez
 */
module fr.glhez.jtools.warextractor {
    exports fr.glhez.jtools.warextractor;

    requires info.picocli;
    requires org.apache.commons.lang3;
    requires org.apache.commons.text;

    requires static transitive org.objectweb.asm;
    requires transitive org.objectweb.asm.util;

    opens fr.glhez.jtools.warextractor to info.picocli;

    uses java.nio.file.spi.FileSystemProvider;

}
