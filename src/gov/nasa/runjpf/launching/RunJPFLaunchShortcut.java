package gov.nasa.runjpf.launching;

import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.RunJPF;
import gov.nasa.runjpf.tab.CommonJPFTab;
import gov.nasa.runjpf.tab.JPFArgumentsTab;
import gov.nasa.runjpf.tab.JPFClasspathTab;
import gov.nasa.runjpf.tab.JPFOverviewTab;
import gov.nasa.runjpf.tab.JPFRunTab;
import gov.nasa.runjpf.tab.JPFSourceLookupTab;
import gov.nasa.runjpf.util.LaunchUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * <p>
 * This shortcut is used when user clicks on <i>Run as</i> or <i>Debug as</i>
 * sub-option of the context menu of the file (for instance a file in a
 * <i>Project Explorer</i>).
 * </p>
 * <p>
 * It will look for any associated already existing launch configurations and
 * then it is decided whether the verification (the run) should be directly
 * triggered or the <i>Launch configuration dialog</i> is shown.<br/>
 * It is also possible to skip this shortcut and open the <i>Launch
 * configuration dialog</i> (see {@link JPFLaunchConfigurationTabGroup})
 * directly which is done if user opens the <i>Run Configurations...</i> or the
 * <i>Debug Configurations...</i> dialogs.
 * 
 * @author stepan
 * 
 */
public class RunJPFLaunchShortcut implements ILaunchShortcut, IExecutableExtension {

  private static final String JPF_CONFIGURATION_TYPE_STRING = "eclipse-jpf.launching.runJpf";
  /**
   * whether to show the launch configuration dialog or to start the
   * verification directly
   */
  private boolean showDialog = false;
  private boolean legacy = false;

  @Override
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    if ("WITH_DIALOG".equals(data)) { //$NON-NLS-1$
      this.showDialog = true;
    } else if ("LEGACY".equals(data)) {
      this.legacy  = true;
    }
  }

  @Override
  public void launch(ISelection selection, String mode) {
    launch(LaunchUtils.getSelectedResource(selection), mode);
  }

  @Override
  public void launch(IEditorPart editor, String mode) {
    launch(LaunchUtils.getFile(editor.getEditorInput()), mode);
  }

  /**
   * A convenience method for launching of JPF apps.<br/>
   * It either displays launch configuration dialog with all the tabs specified
   * in {@link JPFLaunchConfigurationTabGroup} or let the
   * {@link JPFLaunchConfigurationDelegate} launch the process directly using
   * the default or already stored configuration.
   * 
   * @param resource
   *          A resource to launch JPF with. This resource determines which
   *          configuration to use.
   * @param mode
   *          debug or run mode
   */
  public void launch(IResource resource, String mode) {
    if (legacy) {
      runLegacy(resource);
      return;
    }
    
    ILaunchConfiguration configuration = findOrCreateLaunchConfiguration(resource);

    if (configuration == null) {
      return;
    }

    if (showDialog) {
      ILaunchGroup group = DebugUITools.getLaunchGroup(configuration, mode);
      DebugUITools.openLaunchConfigurationDialog(getActiveShell(), configuration, group.getIdentifier(), null);
    } else {
      DebugUITools.launch(configuration, mode);
    }
  }

  private void runLegacy(IResource resource) {
    if (resource != null && resource instanceof IFile) {
      new RunJPF((IFile)resource).schedule();
    }
  }

  /**
   * Finds or creates or lets user to pick a configuration for the given
   * resource.
   * 
   * @param resource
   *          The resource the launch configuration should be associated with
   * @return a launch configuration or <tt>null</tt> to cancel.
   */
  private ILaunchConfiguration findOrCreateLaunchConfiguration(IResource resource) {
    List<ILaunchConfiguration> candidates = launchConfigurationCandidates(resource);
    if (candidates.size() <= 0) {
      return createConfiguration(resource);
    }
    if (candidates.size() > 1) {
      return chooseConfiguration(candidates);
    }
    return candidates.get(0);
  }

  /**
   * Returns a configuration from the given collection of configurations that
   * should be launched, or <code>null</code> to cancel. Default implementation
   * opens a selection dialog that allows the user to choose one of the
   * specified launch configurations. Returns the chosen configuration, or
   * <code>null</code> if the user cancels.
   * 
   * @param configList
   *          list of configurations to choose from
   * @return configuration to launch or <code>null</code> to cancel
   */
  protected ILaunchConfiguration chooseConfiguration(List<ILaunchConfiguration> configList) {
    IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
    ElementListSelectionDialog dialog = new ElementListSelectionDialog(getActiveShell(), labelProvider);
    dialog.setElements(configList.toArray());
    dialog.setTitle("JPF Launch Configurations");
    dialog.setMessage("&Select existing configuration:");
    dialog.setMultipleSelection(false);
    int result = dialog.open();
    labelProvider.dispose();
    if (result == Window.OK) {
      return (ILaunchConfiguration) dialog.getFirstResult();
    }
    return null;
  }

  /**
   * Get all launch configuration candidates for the given resource
   * 
   * @param resource
   *          The resource that determines a set of launch configurations
   * @return list of all launch configuration candidates
   */
  public List<ILaunchConfiguration> launchConfigurationCandidates(IResource resource) {
    List<ILaunchConfiguration> candidates = new ArrayList<>();

    if (resource instanceof IFile) {
      File selectedFile = ((IFile) resource).getLocation().toFile();

      try {
        ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations();
        for (ILaunchConfiguration config : configs) {

          if (isJpfRunConfiguration(config)) {
            String appPropFileName = config.getAttribute(JPFRunTab.JPF_ATTR_MAIN_JPFFILELOCATION, "");
            File foundFile = new File(appPropFileName);
            if (foundFile.equals(selectedFile)) {
              candidates.add(config);
            }
          }
        }
      } catch (CoreException e) {
      }
    }
    return candidates;
  }

  /**
   * Checks whether given configuration is JPF launch configuration.
   * 
   * @param configuration
   *          A configuration to test.
   * @return true or false
   * @throws CoreException
   */
  private boolean isJpfRunConfiguration(ILaunchConfiguration configuration) throws CoreException {
    String mainType = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "");
    String jpfId = configuration.getAttribute(CommonJPFTab.JPF_ATTR_LAUNCHID, "");
    return EclipseJPF.JPF_MAIN_CLASS.equals(mainType) && CommonJPFTab.JPF_ATTR_LAUNCHID.equals(jpfId);
  }

  private ILaunchManager getLaunchManager() {
    return DebugPlugin.getDefault().getLaunchManager();
  }

  private ILaunchConfigurationType getConfigurationType() {
    return getLaunchManager().getLaunchConfigurationType(JPF_CONFIGURATION_TYPE_STRING);
  }

  /**
   * Conditionally adds the given project as a source to the working copy so
   * that JPF <tt>sourcepath</tt> can be populated from it.
   * 
   * @param project
   *          The project to add.
   * @param workingCopy
   *          Where to add the given project.
   */
  private void addProjectAsSourceLookupAndSourcepath(IProject project, ILaunchConfigurationWorkingCopy workingCopy) {
    try {
      @SuppressWarnings("restriction")
      ISourceLookupDirector sourceLookupDirector = new org.eclipse.jdt.internal.launching.JavaSourceLookupDirector();
      String initMemento = workingCopy.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, (String) null);
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
      Map<String, String> dynamicConfig = workingCopy.getAttribute(JPFOverviewTab.ATTR_JPF_DYNAMICCONFIG, new HashMap<>());
      dynamicConfig.put("sourcepath", JPFSourceLookupTab.generateSourcepath(existingContainers));

    } catch (Exception e) {
      EclipseJPF.logError("Cannot add resources", e);
    }
  }

  /**
   * Add the container to the given containers provided it's not there already.
   * 
   * @param container
   *          The container to add conditionally
   * @param containers
   *          Where to add the container
   * @return The same as object as <tt>containers</tt> if nothing changed or
   *         enlarged one.
   */
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

  /**
   * Creates the <i>JPF Launch Configuration</i> with all default values based
   * on the file used as a contract.
   * 
   * @param type
   * @return
   */
  public ILaunchConfiguration createConfiguration(IResource type) {
    ILaunchConfiguration config = null;

    if (type != null && type instanceof IFile) {
      try {
        ILaunchConfigurationType configType = getConfigurationType();
        String launchConfigName = getLaunchManager().generateLaunchConfigurationName(((IFile) type).getName());
        ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, launchConfigName);

        JPFOverviewTab.initDefaultConfiguration(wc, type.getProject().getName(), (IFile) type);
        JPFRunTab.initDefaultConfiguration(wc, type.getProject().getName(), (IFile) type);
        JPFArgumentsTab.defaults(wc);

        addProjectAsSourceLookupAndSourcepath(type.getProject(), wc);
        addProjectToClasspath(wc);

        // set mapped resource , let next time we could execute this
        // directly from menuitem.
        wc.setMappedResources(new IResource[] { type });
        config = wc.doSave();
      } catch (CoreException exception) {
        showError(exception.getStatus().getMessage());
      }
    }
    return config;
  }

  /**
   * Add the default set of projects that are stored by JDT in the working copy
   * configuration to the classpath provided they are java projects.
   * 
   * @param wc
   *          The working copy configuration
   */
  private void addProjectToClasspath(ILaunchConfigurationWorkingCopy wc) {

    try {
      IRuntimeClasspathEntry[] entries = JavaRuntime.computeUnresolvedRuntimeClasspath(wc);
      entries = JavaRuntime.resolveRuntimeClasspath(entries, wc);

      // add the result to the dynamic config
      @SuppressWarnings("unchecked")
      Map<String, String> dynamicConfig = wc.getAttribute(JPFOverviewTab.ATTR_JPF_DYNAMICCONFIG, new HashMap<>());
      dynamicConfig.put("classpath", JPFClasspathTab.generateClasspath(wc, entries));

    } catch (CoreException e) {
      EclipseJPF.logError("A problem occurred while generating the JPF classpath!", e);
      // we don't have to propagate the problem further
    }

  }

  /**
   * Show an error windows to the user.
   * 
   * @param message
   *          The message to show.
   */
  private void showError(String message) {
    MessageDialog.openError(getActiveShell(), "Error during JPF Verification startup!", message);
  }

  /**
   * Get active shell or null.
   * 
   * @return Active shell or null
   */
  private Shell getActiveShell() {
    IWorkbenchWindow win = EclipseJPF.getDefault().getWorkbench().getActiveWorkbenchWindow();

    if (win == null)
      return null;

    return win.getShell();
  }

  /**
   * Whether to show the dialog.
   * 
   * @param showDialog
   *          True or False
   */
  public void setShowDialog(boolean showDialog) {
    this.showDialog = showDialog;
  }

}
