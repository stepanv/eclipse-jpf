package gov.nasa.runjpf.launching;

import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.tab.JPFClasspathTab;
import gov.nasa.runjpf.tab.JPFDebugTab;
import gov.nasa.runjpf.tab.JPFRunTab;
import gov.nasa.runjpf.tab.JPFSettingsTab;
import gov.nasa.runjpf.tab.JPFSourceLookupTab;
import gov.nasa.runjpf.util.ProjectUtil;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.DefaultSourceContainer;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.debug.ui.classpath.ClasspathModel;
import org.eclipse.jdt.internal.launching.JavaSourceLookupDirector;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;

@SuppressWarnings("restriction")
public class RunJPFLaunchShortcut implements ILaunchShortcut, IExecutableExtension {

  private static final String JPF_CONFIGURATION_TYPE_STRING = "eclipse-jpf.launching.runJpf";
  private boolean showDialog = false;

  /**
   * @param showDialog
   *          the showDialog to set
   */
  public void setShowDialog(boolean showDialog) {
    this.showDialog = showDialog;
  }

  @Override
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    if ("WITH_DIALOG".equals(data)) { //$NON-NLS-1$
      this.showDialog = true;
    }
  }

  private ILaunchConfiguration findOrCreateLaunchConfiguration(IResource ir) {
    ILaunchConfiguration launchConfiguration = findLaunchConfiguration(ir);
    if (launchConfiguration == null) {
      launchConfiguration = createConfiguration(ir);
    }
    return launchConfiguration;
  }

  @Override
  public void launch(ISelection selection, String mode) {
    IResource ir = getLaunchableResource(selection);
    launch(findOrCreateLaunchConfiguration(ir), mode);
  }

  @Override
  public void launch(IEditorPart editor, String mode) {
    launch(findOrCreateLaunchConfiguration(getLaunchableResource(editor)), mode);
  }

  public ILaunchConfiguration findLaunchConfiguration(IResource type) {
    if (type == null)
      return null;

    if (type instanceof IFile) {
      File selectedFile = ((IFile) type).getLocation().toFile();

      try {
        ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations();
        for (ILaunchConfiguration config : configs) {

          if (isJpfRunConfiguration(config)) {
            String currentProejctName = config.getAttribute(JPFRunTab.JPF_FILE_LOCATION, "");
            File foundFile = new File(currentProejctName);
            if (foundFile.equals(selectedFile)) {
              return config;
            }
          }
        }
      } catch (CoreException e) {
      }
    }
    return null;
  }

  private boolean isJpfRunConfiguration(ILaunchConfiguration config) throws CoreException {
    String mainType = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "");
    return EclipseJPF.JPF_MAIN_CLASS.equals(mainType);
  }

  private ILaunchManager getLaunchManager() {
    return DebugPlugin.getDefault().getLaunchManager();
  }

  public ILaunchConfigurationType getConfigurationType() {
    return getLaunchManager().getLaunchConfigurationType(JPF_CONFIGURATION_TYPE_STRING);
  }

  /**
   * Conditionally adds the given project to the working copy.
   * 
   * @param project
   *          The project to add.
   * @param workingCopy
   *          Where to add the given project.
   */
  private void addProjectAsSourceLookupAndSourcepath(IProject project, ILaunchConfigurationWorkingCopy workingCopy) {
    try {
      ISourceLookupDirector sourceLookupDirector = new JavaSourceLookupDirector();
      String initMemento = workingCopy.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, (String)null);
      if (initMemento != null) {
        sourceLookupDirector.initializeFromMemento(initMemento);
      }

      // check default source container
      ISourceContainer[] existingContainers = conditionallyAddIfNotPresent(new DefaultSourceContainer(),
                                                                           sourceLookupDirector.getSourceContainers());

      // handle projects in current workspace
      existingContainers = conditionallyAddIfNotPresent(new JavaProjectSourceContainer(JavaCore.create(project)), existingContainers);

      sourceLookupDirector.setSourceContainers(existingContainers);
      workingCopy.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, sourceLookupDirector.getMemento());
      
      // add the result to the dynamic config
      @SuppressWarnings("unchecked")
      Map<String, String> dynamicConfig = workingCopy.getAttribute(JPFSettingsTab.ATTR_JPF_DYNAMICCONFIG, new HashMap<>());
      dynamicConfig.put("sourcepath", JPFSourceLookupTab.generateSourcepath(existingContainers));

    } catch (Exception e) {
      EclipseJPF.logError("Cannot add resources", e);
    }
  }

  private ISourceContainer[] conditionallyAddIfNotPresent(ISourceContainer container, ISourceContainer[] containers) {
    if (!isPresent(container, containers)) {
      ISourceContainer[] enlargedContainers = Arrays.copyOf(containers, containers.length + 1);
      enlargedContainers[containers.length] = container;
      return enlargedContainers;
    }
    return containers;
  }

  /**
   * if containers contains target source container
   * 
   * @param testedContainer
   *          The Container to be tested
   * @param containers
   *          The set of containers where tested container is looked for
   * 
   * @return True if the tested container is present in the given ones
   */
  private boolean isPresent(ISourceContainer testedContainer, ISourceContainer[] containers) {
    if (testedContainer == null) {
      return false;
    }

    String testedName = testedContainer.getName();
    String testedTypeId = testedContainer.getType().getId();
    for (ISourceContainer currentContainer : containers) {
      String name = currentContainer.getName();
      String typeId = currentContainer.getType().getId();
      if (name.equals(testedName) && typeId.equals(testedTypeId)) {
        return true;
      }
    }
    return false;
  }

  public ILaunchConfiguration createConfiguration(IResource type) {
    if (type == null)
      return null;

    if (type instanceof IFile) {
      String typeName = ((IFile) type).getName();

      ILaunchConfiguration config = null;
      ILaunchConfigurationWorkingCopy wc = null;
      try {
        ILaunchConfigurationType configType = getConfigurationType();

        String launchConfigName = getLaunchManager().generateLaunchConfigurationName(typeName);

        wc = configType.newInstance(null, launchConfigName);
        
        JPFSettingsTab.initDefaultConfiguration(wc, type.getProject().getName(), (IFile)type);
        JPFRunTab.initDefaultConfiguration(wc, type.getProject().getName(), (IFile)type);
        JPFDebugTab.initDefaultConfiguration(wc, type.getProject().getName(), (IFile)type);

        addProjectAsSourceLookupAndSourcepath(type.getProject(), wc);
        addProjectToClasspath(wc);
        
        // set mapped resource , let next time we could execute this
        // directly from menuitem.
        wc.setMappedResources(new IResource[] { type });
        config = wc.doSave();
      } catch (CoreException exception) {
        showError(exception.getStatus().getMessage());
      }
      return config;
    }
    return null;
  }

  private void addProjectToClasspath(ILaunchConfigurationWorkingCopy wc) {

    try {
      IRuntimeClasspathEntry[] entries = JavaRuntime.computeUnresolvedRuntimeClasspath(wc);
      entries = JavaRuntime.resolveRuntimeClasspath(entries, wc);
      
   // add the result to the dynamic config
      @SuppressWarnings("unchecked")
      Map<String, String> dynamicConfig = wc.getAttribute(JPFSettingsTab.ATTR_JPF_DYNAMICCONFIG, new HashMap<>());
      dynamicConfig.put("classpath", JPFClasspathTab.generateClasspath(wc, entries));
      
    } catch (CoreException e) {
      EclipseJPF.logError("A problem occurred while generating the JPF classpath!", e);
      // we don't have to propagate the problem further
    }

  }

  private void showError(String message) {
    MessageDialog.openError(getActiveShell(), "Error when startup JPF Verification", message);
  }

  private Shell getActiveShell() {
    IWorkbenchWindow win = EclipseJPF.getDefault().getWorkbench().getActiveWorkbenchWindow();

    if (win == null)
      return null;

    return win.getShell();
  }

  public void launch(ILaunchConfiguration launchConfiguration, String mode) {
    if (launchConfiguration == null) {
      return;
    }

    if (showDialog) {
      DebugUITools.saveBeforeLaunch();
      ILaunchGroup group = DebugUITools.getLaunchGroup(launchConfiguration, mode);
      DebugUITools.openLaunchConfigurationDialog(getActiveShell(), launchConfiguration, group.getIdentifier(), null);
    } else {
      DebugUITools.launch(launchConfiguration, mode);
    }
  }

  public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
    ILaunchConfiguration launchconf = findLaunchConfiguration(getLaunchableResource(selection));
    if (launchconf == null)
      return null;
    return new ILaunchConfiguration[] { launchconf };
  }

  public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editorpart) {
    ILaunchConfiguration launchconf = findLaunchConfiguration(getLaunchableResource(editorpart));
    if (launchconf == null)
      return null;
    return new ILaunchConfiguration[] { launchconf };
  }

  public IResource getLaunchableResource(ISelection selection) {
    return getLaunchableResource(ProjectUtil.getSelectedResource(selection));
  }

  public IResource getLaunchableResource(IEditorPart editorpart) {
    return getLaunchableResource(ProjectUtil.getFile(editorpart.getEditorInput()));
  }

  private IResource getLaunchableResource(IResource ir) {
    if (ir == null)
      return null;

    return ir;
  }

}
