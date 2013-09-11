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
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
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
import org.eclipse.jdt.internal.debug.ui.launcher.MainMethodSearchEngine;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

/**
 * Common tab for JPF tab implementations.
 * 
 * @author stepan
 * 
 */
@SuppressWarnings("restriction")
public abstract class AbstractJPFTab extends JavaLaunchTab {

  private static final String ATTRIBUTE_UNIQUE_PREFIX = "gov.nasa.jpf.eclipsejpf";

  /** JPF file *.jpf that is selected */
  public static final String JPF_ATTR_MAIN_JPFFILELOCATION = ATTRIBUTE_UNIQUE_PREFIX + "JPF_ATTR_MAIN_JPFFILELOCATION";
  /**
   * Java class with a main method that will be used as the main entry for the
   * JPF verification
   */
  public static final String JPF_ATTR_MAIN_JPFTARGET = ATTRIBUTE_UNIQUE_PREFIX + "JPF_ATTR_MAIN_JPFTARGET";
  /** Whether the JPF file is selected instead of the main class */
  public static final String JPF_ATTR_MAIN_JPFFILESELECTED = ATTRIBUTE_UNIQUE_PREFIX + "JPF_ATTR_MAIN_JPFFILESELECTED";

  /**
   * Whether to debug both VMs - JPF running in the JRE as well as the SuT
   * program running in JPF
   */
  public static final String JPF_ATTR_DEBUG_DEBUGBOTHVMS = ATTRIBUTE_UNIQUE_PREFIX + "JPF_ATTR_DEBUG_DEBUGBOTHVMS";
  /**
   * Whether to debug JPF running in the JRE instead of the program running in
   * JPF
   */
  public static final String JPF_ATTR_DEBUG_DEBUGJPFINSTEADOFPROGRAM = ATTRIBUTE_UNIQUE_PREFIX + "JPF_ATTR_DEBUG_DEBUGJPFINSTEADOFPROGRAM";

  /**
   * Whether to store the trace instead of replaying it.
   * 
   * @see AbstractJPFTab#JPF_ATTR_TRACE_ENABLED
   */
  public static final String JPF_ATTR_TRACE_STOREINSTEADOFREPLAY = ATTRIBUTE_UNIQUE_PREFIX + "JPF_ATTR_TRACE_STOREINSTEADOFREPLAY";
  /** Trace file location */
  public static final String JPF_ATTR_TRACE_FILE = ATTRIBUTE_UNIQUE_PREFIX + "JPF_ATTR_TRACE_FILE";
  /**
   * Whether trace is enabled
   * 
   * @see AbstractJPFTab#JPF_ATTR_TRACE_STOREINSTEADOFREPLAY
   */
  public static final String JPF_ATTR_TRACE_ENABLED = ATTRIBUTE_UNIQUE_PREFIX + "JPF_ATTR_TRACE_ENABLED";

  /**
   * Additional listener class that user can search for using the class search
   * dialog
   */
  public static final String JPF_ATTR_OPT_LISTENER = ATTRIBUTE_UNIQUE_PREFIX + "JPF_ATTR_OPT_LISTENER";
  /**
   * Additional search class that user can search for using the class search
   * dialog
   */
  public static final String JPF_ATTR_OPT_SEARCH = ATTRIBUTE_UNIQUE_PREFIX + "JPF_ATTR_OPT_SEARCH";
  /** Whether the shell port option is enabled */
  public static final String JPF_ATTR_OPT_SHELLENABLED = ATTRIBUTE_UNIQUE_PREFIX + "JPF_ATTR_OPT_SHELLENABLED";
  /** Shell port */
  public static final String JPF_ATTR_OPT_SHELLPORT = ATTRIBUTE_UNIQUE_PREFIX + "JPF_ATTR_OPT_SHELLPORT";

  /** Selected JDWP jpf extension installation index */
  public static final String JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX = ATTRIBUTE_UNIQUE_PREFIX + "JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX";
  /** Selected JPF Core extension installation index */
  public static final String JPF_ATTR_RUNTIME_JPF_INSTALLATIONINDEX = ATTRIBUTE_UNIQUE_PREFIX + "JPF_ATTR_RUNTIME_JPF_INSTALLATIONINDEX";
  /** The embedded JPF classpath */
  public static final String JPF_ATTR_RUNTIME_JPF_EMBEDDEDCLASSPATH = ATTRIBUTE_UNIQUE_PREFIX + "JPF_ATTR_RUNTIME_JPF_EMBEDDEDCLASSPATH";

  /**
   * If it's modified , just update the configuration directly.
   */
  private class UpdateModfiyListener implements ModifyListener {
    @Override
    public void modifyText(ModifyEvent e) {
      updateLaunchConfigurationDialog();
    }
  }

  protected UpdateModfiyListener updatedListener = new UpdateModfiyListener();

  /**
   * Lambda method like support class for class searching.
   * 
   * @author stepan
   * 
   */
  abstract private class InlineSearcher {
    abstract IType[] search() throws InvocationTargetException, InterruptedException;
  }

  /**
   * Search for all classes that extend a supertype recursively.
   * 
   * @param supertype
   *          The supertype that is the parent of all classes. Can be an
   *          interface too.
   * @param text
   *          Text field
   * @param originalType
   * @return Type of the selected class
   */
  protected IType handleSupertypeSearchButtonSelected(final String supertype, Text text, IType originalType) {
    return handleSearchButtonSelected(new InlineSearcher() {
      @Override
      IType[] search() throws InvocationTargetException, InterruptedException {
        ClassSearchEngine engine = new ClassSearchEngine();
        return engine.searchClasses(getLaunchConfigurationDialog(), simpleSearchScope(), true, supertype);
      }
    }, text, originalType, "Select a class.");
  }

  /**
   * Search for all main classes.
   * 
   * @param text
   *          Text field
   * @param originalType
   * @return Type of the selected class
   */
  protected IType handleSearchMainClassButtonSelected(Text text, IType originalType) {
    return handleSearchButtonSelected(new InlineSearcher() {
      @Override
      IType[] search() throws InvocationTargetException, InterruptedException {
        MainMethodSearchEngine engine = new MainMethodSearchEngine();
        return engine.searchMainMethods(getLaunchConfigurationDialog(), simpleSearchScope(), true);
      }
    }, text, originalType, "Select a main class.");
  }

  /**
   * Gets simple java search scope
   * 
   * @return
   */
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
   * Show a dialog that lists all classes according to what the searcher looks
   * up.
   * 
   * @param searcher
   *          The searching facility
   * @param text
   *          Where to put the user selected class
   * @param originalType
   *          The type of the selected class
   * @param dialogWindowText
   *          Text of the dialog window
   * @return Type of the selected class
   */
  protected IType handleSearchButtonSelected(InlineSearcher searcher, Text text, IType originalType, String dialogWindowText) {

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
    DebugTypeSelectionDialog mmsd = new DebugTypeSelectionDialog(getShell(), types, dialogWindowText);
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

  /**
   * Lookup local installation of the given JPF extension.
   * 
   * @param configuration
   *          The launch configuration
   * @param extensionInstallations
   *          where to put the results
   * @param extension
   *          The JPF extension to look up for
   */
  protected static void lookupLocalInstallation(ILaunchConfiguration configuration, ExtensionInstallations extensionInstallations,
                                                String extension) {

    // let's clear all non embedded installations
    while (extensionInstallations.size() > 1) {
      extensionInstallations.remove(1);
    }

    Config config = LookupConfigHelper.defaultConfigFactory(configuration);

    Map<String, File> projects = LookupConfigHelper.getSiteProjects(config);
    if (projects.containsKey(extension)) {
      String pseudoPath = projects.get(extension).getAbsolutePath();
      ExtensionInstallation localJdwpInstallation = new ExtensionInstallation("Locally installed as " + extension + " extension",
          pseudoPath);
      if (!extensionInstallations.contains(localJdwpInstallation)) {
        extensionInstallations.add(localJdwpInstallation);
      }
    }
  }

  /**
   * Initialize installations of the given JPF extension.
   * 
   * @param configuration
   *          Launch configuration
   * @param extensionInstallations
   *          Where to put the extensions
   * @param installationCombo
   *          An associated combo that is to be populated with the JPF
   *          extensions
   * @param installationIndexAttribute
   *          The attribute of the launch configuration that indexes the
   *          selected installation in the installation combo.
   * @param extension
   *          The JPF extension to initialize
   */
  protected void initializeExtensionInstallations(ILaunchConfiguration configuration, ExtensionInstallations extensionInstallations,
                                                  Combo installationCombo, String installationIndexAttribute, String extension) {
    lookupLocalInstallation(configuration, extensionInstallations, extension);

    String[] installations = (String[]) extensionInstallations.toStringArray(new String[extensionInstallations.size()]);
    installationCombo.setItems(installations);
    installationCombo.setVisibleItemCount(Math.min(installations.length, 20));
    try {
      if (configuration == null || configuration.getAttribute(installationIndexAttribute, -1) == -1) {
        // this is the first initialization ever
        installationCombo.select(extensionInstallations.defaultInstallationIndex());
      } else {
        installationCombo
            .select(configuration.getAttribute(installationIndexAttribute, ExtensionInstallations.EMBEDDED_INSTALLATION_INDEX));
      }
    } catch (CoreException e) {
      EclipseJPF.logError("Error occurred while getting the selected extension installation: " + extension, e);
    }
  }

  /**
   * Create an image for the given path
   * 
   * @param relativeImagePath
   *          Bundle relative path where the image is stored
   * @return Image instace
   */
  protected static Image createImage(String relativeImagePath) {
    ImageDescriptor desc = ImageDescriptor.getMissingImageDescriptor();
    Bundle bundle = Platform.getBundle(EclipseJPF.BUNDLE_SYMBOLIC);
    URL url = null;
    if (bundle != null) {
      url = FileLocator.find(bundle, new Path(relativeImagePath), null);
      if (url != null) {
        desc = ImageDescriptor.createFromURL(url);
      }
    }
    return desc.createImage();
  }

  /**
   * Whether the given property is from the application configuratio properties
   * file
   * 
   * @param configuration
   *          The launch configuration
   * @param key
   *          The property key
   * @param value
   *          The property value
   * @return whether it's true
   */
  protected static boolean isApplicationProperty(ILaunchConfigurationWorkingCopy configuration, String key, String value) {
    if (value == null) {
      return false;
    }

    try {
      @SuppressWarnings("unchecked")
      Map<String, String> appMap = configuration.getAttribute(JPFSettingsTab.ATTR_JPF_APPCONFIG, Collections.EMPTY_MAP);

      String appValue = (String) appMap.get(key);
      if (appValue != null && appValue.trim().equals(value.trim())) {
        return true;
      }
    } catch (CoreException e) {
      EclipseJPF.logError("Cannot get application config from the launch configuraiton", e);
    }
    return false;
  }

  /**
   * Put the given property to the given map provided the property is not from
   * the application configuration file.
   * 
   * @param configuration
   *          The launch configuration
   * @param map
   *          Where to put the property
   * @param key
   *          The key of the property
   * @param value
   *          The value of the property
   */
  protected static void putIfNotApplicationPropertyAndNotEmpty(ILaunchConfigurationWorkingCopy configuration, Map<String, String> map,
                                                               String key, String value) {
    if (value != null && !Objects.equals("", value) && !isApplicationProperty(configuration, key, value)) {
      map.put(key, value);
    }
  }
}
