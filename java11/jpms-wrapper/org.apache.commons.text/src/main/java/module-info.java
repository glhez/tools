module org.apache.commons.text {
  exports org.apache.commons.text;
  exports org.apache.commons.text.diff;
  exports org.apache.commons.text.lookup;
  exports org.apache.commons.text.matcher;
  exports org.apache.commons.text.similarity;
  exports org.apache.commons.text.translate;

  requires java.base;
  // this was reported by jdeps, not sure it is mandatory
  requires static java.scripting;
  requires static java.xml;

  requires org.apache.commons.lang3;
}
