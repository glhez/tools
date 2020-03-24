package com.github.glhez.jtools.text;

import java.util.Objects;

/**
 * Default column.
 *
 * @author gael.lhez
 */
public class DefaultColumn implements Column {
  private final String value;

  public DefaultColumn(final String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final DefaultColumn other = (DefaultColumn) obj;
    return Objects.equals(value, other.value);
  }

}
