package com.github.glhez.jtools.oomph;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class MacroElement implements XmlWriter {
  private final List<Task> tasks;
  private String name;
  private String label;


  public MacroElement(String name, String label) {
    this.name = name;
    this.label = label;
    this.tasks = new ArrayList<>();
  }

  public MacroElement() {
    this(null, null);
  }

  public MacroElement name(String name) {
    this.name = name;
    return this;
  }

  public MacroElement label(String label) {
    this.label = label;
    return this;
  }

  public CompoundTask addCompoundTask(String name) {
    CompoundTask compoundTask = new CompoundTask(name);
    this.tasks.add(compoundTask);
    return compoundTask;
  }

  public StringSubstitutionTask addStringSubstitutionTask(String name) {
    StringSubstitutionTask stringSubstitutionTask = new StringSubstitutionTask(name);
    this.tasks.add(stringSubstitutionTask);
    return stringSubstitutionTask;
  }

  @Override
  public void write(XMLStreamWriter writer) throws XMLStreamException {
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
