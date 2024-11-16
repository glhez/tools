package com.github.glhez.jtools.oomph;

import javax.xml.stream.XMLStreamException;

public class XmlWriterSupport {
  public static final String XSI_NS_PREFIX = "xsi";
  public static final String XSI_NS_URI = "http://www.w3.org/2001/XMLSchema-instance";
  public static final String XMI_NS_PREFIX = "xmi";
  public static final String XMI_NS_URI = "http://www.omg.org/XMI";
  public static final String SETUP_NS_PREFIX = "setup";
  public static final String SETUP_NS_URI = "http://www.eclipse.org/oomph/setup/1.0";

  public static void writeAttributeIfNotBlank(final javax.xml.stream.XMLStreamWriter writer, final String localName, final String value)
      throws XMLStreamException {
    if (value != null && !value.isBlank()) {
      writer.writeAttribute(localName, value);
    }
  }
}
