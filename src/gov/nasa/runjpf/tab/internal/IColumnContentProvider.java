package gov.nasa.runjpf.tab.internal;

/**
 * 
 * @author @author http://www.dzone.com/snippets/javaswt-click-table-column
 * 
 */
public interface IColumnContentProvider {
  Comparable<?> getValue(Object element, int column);
}