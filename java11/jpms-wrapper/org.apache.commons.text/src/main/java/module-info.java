module org.apache.commons.text {
  exports org.apache.commons.text;

  requires java.base;
  // this was reported by jdeps, not sure it is mandatory
  requires static java.scripting;
  requires static java.xml;

  requires org.apache.commons.lang3;
}

