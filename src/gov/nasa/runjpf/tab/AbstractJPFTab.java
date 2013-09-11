package gov.nasa.runjpf.tab;

import gov.nasa.jpf.Config;
import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.internal.ui.ClassSearchEngine;
import gov.nasa.runjpf.internal.ui.ExtensionInstallation;
import gov.nasa.runjpf.internal.ui.ExtensionInstallations;
import gov.nasa.runjpf.tab.internal.LookupConfigHelper;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaLaunchTab;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.launcher.DebugTypeSelectionDialog;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jdt.internal.debug.ui.launcher.MainMethodSearchEngine;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

@SuppressWarnings("restriction")
public abstract class AbstractJPFTab extends JavaLaunchTab {

  public static final String JPF_FILE_LOCATION = "JPF_FILE";
  public static final String JPF_DEBUG_BOTHVMS = "JPF_DEBUG_VM";
  public static final String JPF_DEBUG_JPF_INSTEADOFPROGRAM = "JPF_DEBUG_JPF_INSTEADOFPROGRAM";

  /**
   * If it's modified , just update the configuration directly.
   */
  private class UpdateModfiyListener implements ModifyListener {
    public void modifyText(ModifyEvent e) {
      updateLaunchConfigurationDialog();
    }
  }

  protected UpdateModfiyListener updatedListener = new UpdateModfiyListener();

  abstract private class InlineSearcher {
    abstract IType[] search() throws InvocationTargetException, InterruptedException;
  }

  protected IType handleSupertypeSearchButtonSelected(final String supertype, Text text, IType originalType) {
    return handleSearchButtonSelected(new InlineSearcher() {
      @Override
      IType[] search() throws InvocationTargetException, InterruptedException {
        ClassSearchEngine engine = new ClassSearchEngine();
        return engine.searchClasses(getLaunchConfigurationDialog(), simpleSearchScope(), true, supertype);
      }
    }, text, originalType);
  }

  protected IType handleSearchMainClassButtonSelected(Text text, IType originalType) {
    return handleSearchButtonSelected(new InlineSearcher() {
      @Override
      IType[] search() throws InvocationTargetException, InterruptedException {
        MainMethodSearchEngine engine = new MainMethodSearchEngine();
        return engine.searchMainMethods(getLaunchConfigurationDialog(), simpleSearchScope(), true);
      }
    }, text, originalType);
  }

  protected IJavaSearchScope simpleSearchScope() {
    IJavaElement[] elements = null;
    IJavaModel model = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
    if (model != null) {
      try {
        elements = model.getJavaProjects();
      } catch (JavaModelException e) {
        JDIDebugUIPlugin.log(e);
      }
    }

    if (elements == null) {
      elements = new IJavaElement[] {};
    }

    return SearchEngine.createJavaSearchScope(elements, 1);
  }

  /**
   * Show a dialog that lists all main types
   */
  protected IType handleSearchButtonSelected(InlineSearcher searcher, Text text, IType originalType) {

    IType[] types = null;
    try {
      types = searcher.search();

    } catch (InvocationTargetException e) {
      setErrorMessage(e.getMessage());
      return null;
    } catch (InterruptedException e) {
      setErrorMessage(e.getMessage());
      return null;
    }
    DebugTypeSelectionDialog mmsd = new DebugTypeSelectionDialog(getShell(), types, LauncherMessages.JavaMainTab_Choose_Main_Type_11);
    if (mmsd.open() == Window.CANCEL) {
      return null;
    }
    Object[] results = mmsd.getResult();
    IType type = (IType) results[0];
    if (type != null) {
      text.setText(type.getFullyQualifiedName());
      return type;
    }
    return originalType;
  }

  protected static Map<String, File> getSiteProjects(Config config) {
    Map<String, File> projects = new HashMap<>();

    for (String projId : config.getEntrySequence()) {
      if ("extensions".equals(projId)) {
        // we have to filter this out in case there is only a single project
        // in
        // the list, in which case we find a jpf.properties under its value
        continue;
      }

      String v = config.getString(projId);
      if (v == null) {
        continue;
      }
      File projDir = new File(v);

      if (projDir.isDirectory()) {
        File propFile = new File(projDir, "jpf.properties");
        if (propFile.isFile()) {
          projects.put(projId, propFile);
        }
      }
    }
    return projects;
  }

  protected static void lookupLocalInstallation(ILaunchConfiguration configuration, ExtensionInstallations extensionInstallations,
                                                String extension) {

    // let's clear all non embedded installations
    while (extensionInstallations.size() > 1) {
      extensionInstallations.remove(1);
    }
    
    Config config = LookupConfigHelper.defaultConfigFactory(configuration);

    Map<String, File> projects = getSiteProjects(config);
    if (projects.containsKey(extension)) {
      String pseudoPath = projects.get(extension).getAbsolutePath();
      ExtensionInstallation localJdwpInstallation = new ExtensionInstallation("Locally installed as " + extension + " extension",
          pseudoPath);
      if (!extensionInstallations.contains(localJdwpInstallation)) {
        extensionInstallations.add(localJdwpInstallation);
      }
    }
  }

  protected void initializeExtensionInstallations(ILaunchConfiguration configuration, ExtensionInstallations extensionInstallations,
                                                  Combo installationCombo, String installationIndexAttribute, String extension) {
    lookupLocalInstallation(configuration, extensionInstallations, extension);

    String[] jpfs = (String[]) extensionInstallations.toStringArray(new String[extensionInstallations.size()]);
    installationCombo.setItems(jpfs);
    installationCombo.setVisibleItemCount(Math.min(jpfs.length, 20));
    try {
      if (configuration == null || configuration.getAttribute(installationIndexAttribute, -1) == -1) {
        // this is the first initialization ever
        installationCombo.select(extensionInstallations.getDefaultInstallationIndex());
      } else {
        installationCombo.select(configuration.getAttribute(installationIndexAttribute, ExtensionInstallations.EMBEDDED_INSTALLATION_INDEX));
      }
    } catch (CoreException e) {
      EclipseJPF.logError("Error occurred while getting the selected extension installation: " + extension, e);
    }
  }
  
  protected static Image createImage(String relativeImagePath) {
    ImageDescriptor desc = ImageDescriptor.getMissingImageDescriptor();
    Bundle bundle = Platform.getBundle(EclipseJPF.BUNDLE_SYMBOLIC);
    URL url = null;
    if (bundle != null){
        url = FileLocator.find(bundle, new Path(relativeImagePath), null);
        if(url != null) {
          desc = ImageDescriptor.createFromURL(url);
        }
    }
    return desc.createImage();
  }
}
