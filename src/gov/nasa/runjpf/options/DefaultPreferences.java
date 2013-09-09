package gov.nasa.runjpf.options;

import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.EclipseJPFLauncher;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Simple class that sets the default values of the eclipse-jpf preferences
 * 
 * @author sandro
 * 
 */
public class DefaultPreferences extends AbstractPreferenceInitializer {

  @Override
  public void initializeDefaultPreferences() {
    IPreferenceStore store = EclipseJPF.getDefault().getPreferenceStore();
    store.setDefault(EclipseJPFLauncher.SITE_PROPERTIES_PATH, EclipseJPFLauncher.DEFAULT_SITE_PROPERTIES_PATH);
    store.setDefault(EclipseJPFLauncher.PORT, EclipseJPFLauncher.DEFAULT_PORT);
    store.setDefault(EclipseJPFLauncher.COMMON_DIR, EclipseJPFLauncher.COMMON_DIR_PATH);
  }
}
