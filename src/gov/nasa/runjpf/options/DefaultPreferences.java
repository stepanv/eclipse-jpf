package gov.nasa.runjpf.options;

import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.EclipseJPFLauncher;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;


public class DefaultPreferences extends AbstractPreferenceInitializer {

  @Override
  public void initializeDefaultPreferences(){
    IPreferenceStore store = EclipseJPF.getDefault().getPreferenceStore();
    store.setDefault(EclipseJPFLauncher.SITE_PROPERTIES_PATH, EclipseJPFLauncher.DEFAULT_SITE_PROPERTIES_PATH);
    store.setDefault(EclipseJPFLauncher.PORT, EclipseJPFLauncher.DEFAULT_PORT);
  }
}
