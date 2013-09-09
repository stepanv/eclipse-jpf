package gov.nasa.runjpf.tab.internal;

import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider for the config table
 */
@SuppressWarnings("restriction")
public class ExtendedPropertyLabelProvider extends LabelProvider implements ITableLabelProvider {
  public String getColumnText(Object element, int columnIndex)  {
    String result = null;
    if (element != null) {
      ExtendedProperty var = (ExtendedProperty) element;
      switch (columnIndex) {
        case 0: // variable
          result = var.getProperty();
          break;
        case 1: // value
          result = var.getValue();
          break;
        case 2: // location
          result = var.getConfigName();
          break;
      }
    }
    return result;
  }
  
  public Image getColumnImage(Object element, int columnIndex) {
    if (columnIndex == 0) {
      return DebugPluginImages.getImage(IDebugUIConstants.IMG_OBJS_ENV_VAR);
    }
    return null;
  }
}