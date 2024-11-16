package com.github.glhez.jtools.oomph;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class MacroElement implements XmlWriter {
  private final List<Task> tasks;
  private String name;
  private String label;

  public MacroElement(final String name, final String label) {
    this.name = name;
    this.label = label;
    this.tasks = new ArrayList<>();
  }

  public MacroElement() {
    this(null, null);
  }

  public MacroElement name(final String name) {
    this.name = name;
    return this;
  }

  public MacroElement label(final String label) {
    this.label = label;
    return this;
  }

  public CompoundTask addCompoundTask(final String name) {
    var compoundTask = new CompoundTask(name);
    this.tasks.add(compoundTask);
    return compoundTask;
  }

  public StringSubstitutionTask addStringSubstitutionTask(final String name) {
    var stringSubstitutionTask = new StringSubstitutionTask(name);
    this.tasks.add(stringSubstitutionTask);
    return stringSubstitutionTask;
  }

  @Override
  public void write(final XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartDocument("utf-8", "1.0");
    writer.writeStartElement(XmlWriterSupport.SETUP_NS_PREFIX, "Macro", XmlWriterSupport.SETUP_NS_URI);
    writer.writeNamespace(XmlWriterSupport.XSI_NS_PREFIX, XmlWriterSupport.XSI_NS_URI);
    writer.writeNamespace(XmlWriterSupport.SETUP_NS_PREFIX, XmlWriterSupport.SETUP_NS_URI);
    writer.writeNamespace(XmlWriterSupport.XMI_NS_PREFIX, XmlWriterSupport.XMI_NS_URI);
    writer.writeAttribute(XmlWriterSupport.XMI_NS_PREFIX, XmlWriterSupport.XMI_NS_URI, "version", "2.0");
    XmlWriterSupport.writeAttributeIfNotBlank(writer, "name", this.name);
    XmlWriterSupport.writeAttributeIfNotBlank(writer, "label", this.label);

    for (var task : this.tasks) {
      task.write(writer);
    }

    writer.writeEndElement();
    writer.writeEndDocument();
  }
}
