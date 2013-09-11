package gov.nasa.runjpf.tab.internal;

import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.tab.JPFSettingsTab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Button;

/**
 * Content provider for the config table.<br/>
 * The content is dynamically displayed according to what the user preferences.
 * 
 * @author stepan
 */
public class ExtendedPropertyContentProvider implements IStructuredContentProvider {
  /**
   * 
   */
  private Button checkAppProperties;
  private Button checkCmdargsProperties;
  private Button checkDefaultProperties;
  private Button checkDynamicProperties;

  private Map<String, String> configToNameMap;

  public ExtendedPropertyContentProvider(Button checkAppProperties, Button checkCmdargsProperties, Button checkDefaultProperties,
                                         Button checkDynamicProperties, Map<String, String> configToNameMap) {
    this.checkAppProperties = checkAppProperties;
    this.checkCmdargsProperties = checkCmdargsProperties;
    this.checkDefaultProperties = checkDefaultProperties;
    this.checkDynamicProperties = checkDynamicProperties;
    this.configToNameMap = configToNameMap;
  }

  @Override
  public Object[] getElements(Object inputElement) {
    List<ExtendedProperty> elements = new ArrayList<ExtendedProperty>();
    ILaunchConfiguration config = (ILaunchConfiguration) inputElement;

    List<String> attributes = new LinkedList<String>();
    if (checkDefaultProperties.getSelection()) {
      attributes.add(JPFSettingsTab.ATTR_JPF_DEFAULTCONFIG);
    }
    if (checkCmdargsProperties.getSelection()) {
      attributes.add(JPFSettingsTab.ATTR_JPF_CMDARGSCONFIG);
    }
    if (checkAppProperties.getSelection()) {
      attributes.add(JPFSettingsTab.ATTR_JPF_APPCONFIG);
    }
    if (checkDynamicProperties.getSelection()) {
      attributes.add(JPFSettingsTab.ATTR_JPF_DYNAMICCONFIG);
    }
    for (String attribute : attributes) {
      try {
        @SuppressWarnings("unchecked")
        Map<String, String> m = config.getAttribute(attribute, (Map<String, String>) Collections.<String, String> emptyMap());

        if (m != null && !m.isEmpty()) {
          String friendlyName = configToNameMap.get(attribute);
          for (String key : m.keySet()) {
            elements.add(new ExtendedProperty(key, m.get(key), friendlyName));
          }
        }

      } catch (CoreException e) {
        EclipseJPF.logError("Error reading configuration", e);
      }
    }

    return elements.toArray();
  }

  @Override
  public void dispose() {
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    if (newInput == null) {
      return;
    }
    if (viewer instanceof TableViewer) {
      TableViewer tableViewer = (TableViewer) viewer;
      if (tableViewer.getTable().isDisposed()) {
        return;
      }
    }
  }

}