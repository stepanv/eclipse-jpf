package gov.nasa.runjpf.tab.internal;

import java.util.Arrays;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * This class adds a possibility to sort a table. The table content is sorted as
 * {@link String}.
 * 
 * @author http://www.dzone.com/snippets/javaswt-click-table-column
 * 
 */
public class TableSorter {

  private final TableViewer tableViewer;

  /**
   * Table sorter constructor.
   * 
   * @param tableViewer
   */
  public TableSorter(TableViewer tableViewer) {
    this.tableViewer = tableViewer;
    addColumnSelectionListeners(tableViewer);
    tableViewer.setComparator(new ViewerComparator() {
      public int compare(Viewer viewer, Object e1, Object e2) {
        return compareElements(e1, e2);
      }
    });
  }

  /**
   * Add a column selection listeners to all columns.
   * 
   * @param tableViewer
   *          Where to add the listener
   */
  private void addColumnSelectionListeners(TableViewer tableViewer) {
    for (TableColumn column : tableViewer.getTable().getColumns()) {
      column.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          tableColumnClicked((TableColumn) e.widget);
        }
      });
    }
  }

  /**
   * The action to perform when a column header is clicked.
   * 
   * @param column
   *          The column that is used.
   */
  private void tableColumnClicked(TableColumn column) {
    Table table = column.getParent();
    if (column.equals(table.getSortColumn())) {
      table.setSortDirection(table.getSortDirection() == SWT.UP ? SWT.DOWN : SWT.UP);
    } else {
      table.setSortColumn(column);
      table.setSortDirection(SWT.UP);
    }
    tableViewer.refresh();
  }

  /**
   * {@link String} based compare of elements;
   * 
   * @param e1
   *          First element
   * @param e2
   *          Second element
   * @return The result of comparison as {@link Comparable#compareTo(Object)}
   *         would do.
   */
  private int compareElements(Object e1, Object e2) {
    ITableLabelProvider columnValueProvider = (ITableLabelProvider) tableViewer.getLabelProvider();
    Table table = tableViewer.getTable();
    int index = Arrays.asList(table.getColumns()).indexOf(table.getSortColumn());
    int result = 0;
    if (index != -1) {
      String c1 = columnValueProvider.getColumnText(e1, index);
      String c2 = columnValueProvider.getColumnText(e2, index);
      result = c1.compareTo(c2);
    }
    return table.getSortDirection() == SWT.UP ? result : -result;
  }

}
