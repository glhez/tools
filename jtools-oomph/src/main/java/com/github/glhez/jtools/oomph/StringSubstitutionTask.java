package com.github.glhez.jtools.oomph;

import java.util.Objects;
import java.util.function.UnaryOperator;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class StringSubstitutionTask implements Task {
  private final String name;
  private final StringBuilder value;
  private boolean hasValue;
  private String filterVariable;

  private UnaryOperator<String> filterVariableFormatter;

  public StringSubstitutionTask(final String name) {
    this.name = name;
    this.hasValue = false;
    this.value = new StringBuilder();
  }

  public StringSubstitutionTask clearValue() {
    this.value.setLength(0);
    this.hasValue = false;
    return this;
  }

  public StringSubstitutionTask value(final String value) {
    return clearValue().appendValue(value);
  }

  public StringSubstitutionTask appendValue(final String value) {
    this.hasValue = true;
    this.value.append(' ').append(value);
    return this;
  }

  public StringSubstitutionTask appendVariable(final String value) {
    return appendValue("$${" + value + "}");
  }

  public StringSubstitutionTask appendSetupVariable(final String value) {
    return appendValue("${" + value + "}");
  }

  public StringSubstitutionTask appendJavaProperty(final String key, final String value) {
    this.hasValue = true;
    this.value.append(" -D").append(key).append("=").append(value);
    return this;
  }

  public StringSubstitutionTask filterVariable(final String filterVariable) {
    this.filterVariable = filterVariable;
    return this;
  }

  public StringSubstitutionTask selfFilterVariable() {
    return filterVariable(name);
  }

  public StringSubstitutionTask filterVariableFormatter(final UnaryOperator<String> formatter) {
    this.filterVariableFormatter = formatter;
    return this;
  }

  @Override
  public void write(final XMLStreamWriter writer) throws XMLStreamException {
    if (filterVariableFormatter == null) {
      filterVariableFormatter = asFilterVariable();
    }
    var whenFilterMatchValue = filterVariable != null ? filterVariableFormatter.apply(filterVariable) : null;

    var s = this.value.toString().strip();
    if (!this.hasValue) {
      write(writer, null, Objects.requireNonNullElse(whenFilterMatchValue, ""));
    } else if (whenFilterMatchValue == null) {
      write(writer, null, s);
    } else {
      var condition = "(" + filterVariable + "=*)";
      write(writer, "(!" + condition + ")", s);
      write(writer, condition, whenFilterMatchValue);
    }
  }

  private void write(final XMLStreamWriter writer, final String filter, final String value) throws XMLStreamException {
    writer.writeEmptyElement("setupTask");
    writer.writeAttribute(XmlWriterSupport.XSI_NS_PREFIX, XmlWriterSupport.XSI_NS_URI, "type",
                          XmlWriterSupport.SETUP_NS_PREFIX + ":StringSubstitutionTask");
    writer.writeAttribute("name", this.name);
    if (filter != null) {
      writer.writeAttribute("filter", filter);
    }
    writer.writeAttribute("value", value);
  }

  public static UnaryOperator<String> asFilterVariable() {
    return name -> "${" + name + "}";
  }

  public static UnaryOperator<String> asFilterVariable(final String modifier) {
    return name -> "${" + name + "|" + modifier + "}";
  }
}
