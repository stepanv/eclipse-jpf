package gov.nasa.runjpf.launching;

import gov.nasa.jpf.Config;
import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.EclipseJPFLauncher;
import gov.nasa.runjpf.internal.launching.JPFAllDebugger;
import gov.nasa.runjpf.internal.launching.JPFDebugger;
import gov.nasa.runjpf.internal.launching.JPFRunner;
import gov.nasa.runjpf.tab.JPFOverviewTab;
import gov.nasa.runjpf.tab.JPFRunTab;
import gov.nasa.runjpf.tab.internal.LookupConfigHelper;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaExceptionBreakpoint;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.launching.LaunchingPlugin;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

/**
 * <p>
 * This delegate transform the <i>Launch Configuration</i> (an instance of
 * {@link ILaunchConfiguration}) to match CLI accepted by JPF. <br/>
 * That is a process with arguments to be executed.
 * </p>
 * <p>
 * The sole execution is performed by {@link JPFRunner} or {@link JPFAllDebugger}
 * depending on whether the debug mode is on.
 * </p>
 * 
 * @author stepan
 * 
 */
public class JPFLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate implements ILaunchConfigurationDelegate {

  @Override
  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {

    if (monitor == null) {
      monitor = new NullProgressMonitor();
    }

    monitor.beginTask(MessageFormat.format("{0}...", configuration.getName()), 3); //$NON-NLS-1$

    // check for cancellation
    if (monitor.isCanceled())
      return;

    try {
      monitor.subTask("verifying installation");

      // Program & VM arguments
      ExecutionArguments execArgs = new ExecutionArguments(getVMArguments(configuration), getProgramArguments(configuration));

      List<String> classpath = new LinkedList<>();
      String embeddedJpfClasspath = configuration.getAttribute(JPFRunTab.JPF_ATTR_RUNTIME_JPF_EMBEDDEDCLASSPATH, (String) null);

      if (embeddedJpfClasspath != null) {
        // using embedded JPF
        classpath.add(embeddedJpfClasspath);
      } else {

        try {
          Config config = LookupConfigHelper.defaultConfigFactory(configuration);
          EclipseJPFLauncher eclipseJpfLauncher = new EclipseJPFLauncher();
          File siteProperties = new File(config.getString("jpf.site"));
          File jpfRunJar = eclipseJpfLauncher.lookupRunJpfJar(siteProperties);

          classpath.add(jpfRunJar.getAbsolutePath());
        } catch (NullPointerException npe) {
          EclipseJPF.logError("JPF was not sucessfully found.", npe);
          throw new CoreException(new Status(IStatus.ERROR, EclipseJPF.PLUGIN_ID, "JPF was not found", npe));
        }
      }

      // Create VM config

      VMRunnerConfiguration runConfig = new VMRunnerConfiguration(EclipseJPF.JPF_MAIN_CLASS,
          classpath.toArray(new String[classpath.size()]));

      List<String> programArgs = new ArrayList<String>();
      if (configuration.getAttribute(JPFRunTab.JPF_ATTR_MAIN_JPFFILESELECTED, true)) {
        programArgs.add(configuration.getAttribute(JPFRunTab.JPF_ATTR_MAIN_JPFFILELOCATION, "(this is an error) ??? .jpf"));
      } // else +target=some.Class is used
      programArgs.addAll(Arrays.asList(execArgs.getProgramArgumentsArray()));

      @SuppressWarnings({ "unchecked" })
      Map<String, String> dynamicMap = configuration.getAttribute(JPFOverviewTab.ATTR_JPF_DYNAMICCONFIG,
                                                                  Collections.<String, String> emptyMap());

      for (String key : dynamicMap.keySet()) {
        String value = dynamicMap.get(key);
        programArgs.add(new StringBuilder("+").append(key).append("=").append(value).toString());
      }

      runConfig.setProgramArguments(programArgs.toArray(new String[programArgs.size()]));

      // Environment variables
      runConfig.setEnvironment(getEnvironment(configuration));

      runConfig.setVMArguments(execArgs.getVMArgumentsArray());

      // runConfig
      if (verifyWorkingDirectory(configuration) != null) {
        runConfig.setWorkingDirectory(verifyWorkingDirectory(configuration).getAbsolutePath());
      }
      runConfig.setVMSpecificAttributesMap(getVMSpecificAttributesMap(configuration));

      // Boot path
      runConfig.setBootClassPath(getBootpath(configuration));

      // check for cancellation
      if (monitor.isCanceled())
        return;

      // stop in main
      prepareStopInMain(configuration);
      
      prepareStopOnPropertyViolation(configuration);

      // done the verification phase
      monitor.worked(1);
      monitor.subTask("Creating source locator");
      // set the default source locator if required
      setDefaultSourceLocator(launch, configuration);

      launch.getSourceLocator();
      monitor.worked(1);

      synchronized (configuration) {
        IVMRunner runner;
        IVMInstall vm = verifyVMInstall(configuration);
        boolean debugBothVMs = configuration.getAttribute(JPFRunTab.JPF_ATTR_DEBUG_DEBUGBOTHVMS, false);
        boolean debugJPFInsteadOfTheProgram = configuration.getAttribute(JPFRunTab.JPF_ATTR_DEBUG_DEBUGJPFINSTEADOFPROGRAM, false);
        
        if (ILaunchManager.DEBUG_MODE.equals(mode) && (debugBothVMs || !debugJPFInsteadOfTheProgram)) {
          runner = new JPFAllDebugger(vm, debugBothVMs);
        } else {
          if (ILaunchManager.DEBUG_MODE.equals(mode) && debugJPFInsteadOfTheProgram) {
            runner = new JPFDebugger(vm);
          } else {
            runner = new JPFRunner(vm);
          }
        }
        runner.run(runConfig, launch, monitor);
      }

      // check for cancellation
      if (monitor.isCanceled())
        return;

    } finally {
      monitor.done();
    }
  }
  
  protected void prepareStopOnPropertyViolation(ILaunchConfiguration configuration)
      throws CoreException {
    if (isStopOnPropertyViolation(configuration)) {
      // This listener does not remove itself from the debug plug-in
      // as an event listener (there is no dispose notification for
      // launch delegates). However, since there is only one delegate
      // instantiated per config type, this is tolerable.
      DebugPlugin.getDefault().addDebugEventListener(this);
    }
  }
  /**
   * Handles the "stop-in-main" option.
   * 
   * @param events
   *            the debug events.
   * @see org.eclipse.debug.core.IDebugEventSetListener#handleDebugEvents(DebugEvent[])
   */
  public void handleDebugEvents(DebugEvent[] events) {
    for (int i = 0; i < events.length; i++) {
      DebugEvent event = events[i];
      if (event.getKind() == DebugEvent.CREATE
          && event.getSource() instanceof IJavaDebugTarget) {
        IJavaDebugTarget target = (IJavaDebugTarget) event.getSource();
        ILaunch launch = target.getLaunch();
        if (launch != null) {
          ILaunchConfiguration configuration = launch
              .getLaunchConfiguration();
          if (configuration != null) {
            try {
              if (isStopOnPropertyViolation(configuration)) {
                Map<String, Object> map = new HashMap<String, Object>();
                IJavaExceptionBreakpoint ebp = JDIDebugModel.createExceptionBreakpoint(ResourcesPlugin
                              .getWorkspace()
                              .getRoot(), "*", false, false, false, true , map); //$NON-NLS-1$
                ebp.setPersisted(false);
                target.breakpointAdded(ebp);
              }
              if (isStopInMain(configuration)) {
                String mainType = getMainTypeName(configuration);
                if (mainType != null) {
                  Map<String, Object> map = new HashMap<String, Object>();
                  map
                      .put(
                          IJavaLaunchConfigurationConstants.ATTR_STOP_IN_MAIN,
                          IJavaLaunchConfigurationConstants.ATTR_STOP_IN_MAIN);
                  IJavaMethodBreakpoint bp = JDIDebugModel
                      .createMethodBreakpoint(
                          ResourcesPlugin
                              .getWorkspace()
                              .getRoot(),
                          mainType, "main", //$NON-NLS-1$
                          "([Ljava/lang/String;)V", //$NON-NLS-1$
                          true, false, false, -1, -1,
                          -1, 1, false, map); 
                  bp.setPersisted(false);
                  target.breakpointAdded(bp);
                  DebugPlugin.getDefault()
                      .removeDebugEventListener(this);
                }
              }
            } catch (CoreException e) {
              LaunchingPlugin.log(e);
            }
          }
        }
      }
    }
  }
  
  /**
   * @param configuration
   * @return
   */
  protected boolean isStopOnPropertyViolation(ILaunchConfiguration configuration) {
    return true;
  }
  

}
