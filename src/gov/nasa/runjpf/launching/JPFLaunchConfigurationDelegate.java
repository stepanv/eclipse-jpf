package gov.nasa.runjpf.launching;

import gov.nasa.jpf.Config;
import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.EclipseJPFLauncher;
import gov.nasa.runjpf.internal.breakpoints.SuspendVMExceptionBreakpoint;
import gov.nasa.runjpf.internal.launching.JPFAllDebugger;
import gov.nasa.runjpf.internal.launching.JPFDebugger;
import gov.nasa.runjpf.internal.launching.JPFRunner;
import gov.nasa.runjpf.tab.CommonJPFTab;
import gov.nasa.runjpf.tab.JPFOverviewTab;
import gov.nasa.runjpf.tab.JPFRunTab;
import gov.nasa.runjpf.tab.internal.LookupConfigHelper;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaExceptionBreakpoint;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;
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
 * The sole execution is performed by {@link JPFRunner} or
 * {@link JPFAllDebugger} depending on whether the debug mode is on.
 * </p>
 * 
 * @author stepan
 * 
 */
public class JPFLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate implements ILaunchConfigurationDelegate {

  /** The set of JPF properties that are automatically prepended */
  private static Set<String> PROPERTIES_TO_PREPEND = new HashSet<>();
  static {
    PROPERTIES_TO_PREPEND.add("listener");
    PROPERTIES_TO_PREPEND.add("sourcepath");
    PROPERTIES_TO_PREPEND.add("classpath");
  }

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

      VMRunnerConfiguration runConfig = createRunConfig(configuration);

      // check for cancellation
      if (monitor.isCanceled())
        return;

      cleanupPropertyViolationBreakpoints();

      // stop in main or in app main or stop on property violation - conditional
      // preparation
      conditionallyConfigureAsDebugHandler(configuration);

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

  /**
   * Creates <i>Run Configuration</i> from the current <i>Launch
   * Configuration</i>
   * 
   * @param configuration
   *          The Launch configuration to create the Run configuration from.
   * @return The created Run Configuration
   * @throws CoreException
   *           If something bad deep down happens.
   */
  public VMRunnerConfiguration createRunConfig(ILaunchConfiguration configuration) throws CoreException {
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

    VMRunnerConfiguration runConfig = new VMRunnerConfiguration(EclipseJPF.JPF_MAIN_CLASS, classpath.toArray(new String[classpath.size()]));

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
      if (PROPERTIES_TO_PREPEND.contains(key)) {
        programArgs.add(new StringBuilder("++").append(key).append('=').append(value).append(',').toString());
      } else {
        programArgs.add(new StringBuilder("+").append(key).append('=').append(value).toString());
      }
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
    return runConfig;
  }

  /**
   * Conditionally adds <code>this</code> instance as a debug event listener so
   * that some JDWP commands can be sent to the debuggee when the target starts.
   * 
   * @param configuration
   *          The launch configuration.
   * @throws CoreException
   *           If an error occurs.
   */
  private void conditionallyConfigureAsDebugHandler(ILaunchConfiguration configuration) throws CoreException {
    if (isStopInAppMain(configuration) || isStopInMain(configuration) || isStopOnPropertyViolation(configuration)) {
      // This listener does not remove itself from the debug plug-in
      // as an event listener (there is no dispose notification for
      // launch delegates). However, since there is only one delegate
      // instantiated per config type, this is tolerable.
      DebugPlugin.getDefault().addDebugEventListener(this);
    }

  }

  /**
   * Get the main class which is an entry point of the application to be
   * verified by the JPF.
   * 
   * @param configuration
   *          The launch configuration.
   * @throws CoreException
   *           If an error occurs.
   */
  public String getJPFMainTypeName(ILaunchConfiguration configuration) throws CoreException {

    String mainType = null;

    for (String configAttrName : new String[] { JPFOverviewTab.ATTR_JPF_DYNAMICCONFIG, JPFOverviewTab.ATTR_JPF_APPCONFIG,
        JPFOverviewTab.ATTR_JPF_CMDARGSCONFIG }) {
      @SuppressWarnings({ "unchecked" })
      Map<String, String> dynamicMap = configuration.getAttribute(configAttrName, Collections.<String, String> emptyMap());
      if (dynamicMap.containsKey("target")) {
        mainType = dynamicMap.get("target");
        return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(mainType);
      }
    }

    return null;
  }

  /**
   * Handles the both "stop-in-main" and "stop-on-property-violation" options.
   * 
   * @param events
   *          the debug events.
   * @see org.eclipse.debug.core.IDebugEventSetListener#handleDebugEvents(DebugEvent[])
   */
  public void handleDebugEvents(DebugEvent[] events) {
    for (int i = 0; i < events.length; i++) {
      DebugEvent event = events[i];
      if (event.getKind() == DebugEvent.CREATE && event.getSource() instanceof IJavaDebugTarget) {
        IJavaDebugTarget target = (IJavaDebugTarget) event.getSource();
        ILaunch launch = target.getLaunch();
        if (launch != null) {
          ILaunchConfiguration configuration = launch.getLaunchConfiguration();
          if (configuration != null) {
            try {
              if (isStopOnPropertyViolation(configuration)) {
                Map<String, Object> map = new HashMap<String, Object>(10);
                map.put(CommonJPFTab.JPF_ATTR_MAIN_STOPONPROPERTYVIOLATION, CommonJPFTab.JPF_ATTR_MAIN_STOPONPROPERTYVIOLATION);
                IJavaExceptionBreakpoint eee = new SuspendVMExceptionBreakpoint(ResourcesPlugin.getWorkspace().getRoot(),
                    "gov.nasa.jpf.jdwp.exception.special.NoPropertyViolationException", true, true, false, true, map);

                eee.setPersisted(false);
                target.breakpointAdded(eee);
              }
              if (isStopInAppMain(configuration)) {
                String mainType = getJPFMainTypeName(configuration);
                if (mainType != null) {
                  Map<String, Object> map = new HashMap<String, Object>();
                  map.put(CommonJPFTab.JPF_ATTR_MAIN_STOPINMAIN, CommonJPFTab.JPF_ATTR_MAIN_STOPINMAIN);
                  IJavaMethodBreakpoint bp = JDIDebugModel.createMethodBreakpoint(ResourcesPlugin.getWorkspace().getRoot(), mainType,
                                                                                  "main", //$NON-NLS-1$
                                                                                  "([Ljava/lang/String;)V", //$NON-NLS-1$
                                                                                  true, false, false, -1, -1, -1, 1, false, map);
                  bp.setPersisted(false);
                  target.breakpointAdded(bp);

                }
              }
              if (isStopInMain(configuration)) {
                String mainType = getMainTypeName(configuration);
                if (mainType != null) {
                  Map<String, Object> map = new HashMap<String, Object>();
                  map.put(IJavaLaunchConfigurationConstants.ATTR_STOP_IN_MAIN, IJavaLaunchConfigurationConstants.ATTR_STOP_IN_MAIN);
                  IJavaMethodBreakpoint bp = JDIDebugModel.createMethodBreakpoint(ResourcesPlugin.getWorkspace().getRoot(), mainType,
                                                                                  "main", //$NON-NLS-1$
                                                                                  "([Ljava/lang/String;)V", //$NON-NLS-1$
                                                                                  true, false, false, -1, -1, -1, 1, false, map);
                  bp.setPersisted(false);
                  target.breakpointAdded(bp);

                }
              }
              DebugPlugin.getDefault().removeDebugEventListener(this);
            } catch (CoreException e) {
              EclipseJPF.logError("An error occurred during a creation of synthetic breakpoints.", e);
            }
          }
        }
      }
    }
  }

  /**
   * Removes all <i>Property Violation Breakpoints</i> from the debugger.
   */
  @SuppressWarnings("restriction")
  private void cleanupPropertyViolationBreakpoints() {
    // remove all bp
    DebugPlugin plugin = DebugPlugin.getDefault();
    List<IBreakpoint> bpsRemove = new ArrayList<>();
    if (plugin != null) {
      for (IBreakpoint iBreakpoint : plugin.getBreakpointManager().getBreakpoints()) {
        if (iBreakpoint instanceof SuspendVMExceptionBreakpoint) {
          SuspendVMExceptionBreakpoint b = (SuspendVMExceptionBreakpoint) iBreakpoint;
          if ("gov.nasa.jpf.jdwp.exception.special.NoPropertyViolationException".equals(b.getExceptionTypeName())) {
            bpsRemove.add(b);
          }
        }
      }
      for (IBreakpoint iBreakpoint : bpsRemove) {
        try {
          plugin.getBreakpointManager().removeBreakpoint(iBreakpoint, true);
        } catch (CoreException e) {
          EclipseJPF.logInfo("Cannot remove breakpoint: " + iBreakpoint + ". Exception: " + e.getMessage());
          // trying to go further
        }
      }
    }
  }

  /**
   * Whether the debug functionality to stop in main is enabled.
   * 
   * @param configuration
   *          The launch configuration.
   * @return True or false.
   * @throws CoreException
   *           If an error occurs.
   */
  private boolean isStopInAppMain(ILaunchConfiguration configuration) throws CoreException {
    return configuration.getAttribute(CommonJPFTab.JPF_ATTR_MAIN_STOPINMAIN, false);
  }

  /**
   * Whether the debug functionality to stop on property violation is enabled.
   * 
   * @param configuration
   *          The launch configuration.
   * @return True or false.
   * @throws CoreException
   *           If an error occurs.
   */
  protected boolean isStopOnPropertyViolation(ILaunchConfiguration configuration) throws CoreException {
    return configuration.getAttribute(CommonJPFTab.JPF_ATTR_MAIN_STOPONPROPERTYVIOLATION, false);
  }

}
