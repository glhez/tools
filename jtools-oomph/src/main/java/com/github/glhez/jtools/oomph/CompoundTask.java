package com.github.glhez.jtools.oomph;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class CompoundTask implements ContainerTask {
  private final String name;
  private final List<Task> tasks;

  public CompoundTask(final String name) {
    this.name = name;
    this.tasks = new ArrayList<>();
  }

  @Override
  public CompoundTask addCompoundTask(final String name) {
    var compoundTask = new CompoundTask(name);
    this.tasks.add(compoundTask);
    return compoundTask;
  }

  @Override
  public StringSubstitutionTask addStringSubstitutionTask(final String name) {
    var stringSubstitutionTask = new StringSubstitutionTask(name);
    this.tasks.add(stringSubstitutionTask);
    return stringSubstitutionTask;
  }

  @Override
  public void write(final XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement("setupTask");
    writer.writeAttribute(XmlWriterSupport.XSI_NS_PREFIX, XmlWriterSupport.XSI_NS_URI, "type", XmlWriterSupport.SETUP_NS_PREFIX +":CompoundTask");
    writer.writeAttribute("name", this.name);
    for (var task: this.tasks) {
      task.write(writer);
    }
    writer.writeEndElement();
  }
}
