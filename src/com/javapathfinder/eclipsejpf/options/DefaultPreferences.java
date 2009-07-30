package com.javapathfinder.eclipsejpf.options;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.javapathfinder.eclipsejpf.EclipseJPF;
import com.javapathfinder.eclipsejpf.EclipseJPFLauncher;

public class DefaultPreferences extends AbstractPreferenceInitializer {

  @Override
  public void initializeDefaultPreferences(){
    IPreferenceStore store = EclipseJPF.getDefault().getPreferenceStore();
    store.setDefault(EclipseJPFLauncher.SITE_PROPERTIES_PATH, EclipseJPFLauncher.DEFAULT_SITE_PROPERTIES_PATH);
  }
}
