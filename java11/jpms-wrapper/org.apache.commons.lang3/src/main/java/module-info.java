module org.apache.commons.lang3 {
  exports org.apache.commons.lang3;

  requires java.base;
  // this was reported by jdeps.
  requires static java.desktop;
}

