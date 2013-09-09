package gov.nasa.runjpf.tab;

import gov.nasa.jpf.Config;
import gov.nasa.runjpf.internal.ui.ClassSearchEngine;
import gov.nasa.runjpf.internal.ui.ExtensionInstallation;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
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
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Text;

@SuppressWarnings("restriction")
public abstract class AbstractJPFTab extends JavaLaunchTab {
  
  public static final String JPF_FILE_LOCATION = "JPF_FILE";
  public static final String JPF_DEBUG_BOTHVMS = "JPF_DEBUG_VM";
  public static final String JPF_DEBUG_JPF_INSTEADOFPROGRAM = "JPF_DEBUG_JPF_INSTEADOFPROGRAM";
  
//  public static final String JPF_OPT_TARGET = "JPF_OPT_TARGET";
//  public static final String JPF_OPT_SEARCH = "JPF_OPT_SEARCH";
//  public static final String JPF_OPT_LISTENER = "JPF_OPT_LISTENER";
//  public static final String JPF_OPT_OVERRIDE_INSTEADOFADD = "JPF_OPT_OVERRIDE_INSTEADOFADD";
  
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
  
  protected Map<String, File> getSiteProjects(Config config) {
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

protected void lookupLocalInstallation(List<ExtensionInstallation> jdwpInstallations, String appJpfFile, String extension) {
  Config config;
  if (appJpfFile != null) {
    config = new Config(new String[] {appJpfFile});
  } else {
    config = new Config(new String[] {});
  }
  
//  String sitePath = getSitePropertiesPath();
//  if (sitePath == null) {
//    setErrorMessage("no site.properties");
//    return null;
//  }
//
//  File file = new File(sitePath);
  
  Map<String, File> projects = getSiteProjects(config);
  if (projects.containsKey(extension)) {
    String pseudoPath = projects.get(extension).getAbsolutePath();
    ExtensionInstallation localJdwpInstallation = new ExtensionInstallation("Locally installed as "+extension+" extension", pseudoPath);
    if (!jdwpInstallations.contains(localJdwpInstallation)) {
      jdwpInstallations.add(localJdwpInstallation);
    }
  }
}
}
