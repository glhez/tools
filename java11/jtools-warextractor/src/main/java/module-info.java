/**
 * @author gael.lhez
 */
module fr.glhez.jtools.warextractor {
    exports fr.glhez.jtools.warextractor;

    requires info.picocli;
    requires org.apache.commons.lang3;
    requires org.apache.commons.text;

    requires org.benf.cfr;
    requires org.objectweb.asm;
    requires org.objectweb.asm.util;

    opens fr.glhez.jtools.warextractor to info.picocli;

    uses java.nio.file.spi.FileSystemProvider;

}
