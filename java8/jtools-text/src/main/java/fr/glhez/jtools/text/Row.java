package fr.glhez.jtools.text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class Row implements Iterable<Column> {
  private final List<Column> columns;

  public Row() {
    this.columns = new ArrayList<>();
  }

  public void add(final Column column) {
    if (null != column) {
      this.columns.add(column);
    }
  }

  public int columnCount() {
    return columns.size();
  }

  public List<Column> getColumns() {
    return columns;
  }

  @Override
  public Iterator<Column> iterator() {
    return columns.iterator();
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append("Row [");
    if (columns != null) {
      builder.append("columns=").append(columns);
    }
    builder.append("]");
    return builder.toString();
  }
}
