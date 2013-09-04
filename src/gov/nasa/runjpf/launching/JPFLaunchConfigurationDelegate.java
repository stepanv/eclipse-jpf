package gov.nasa.runjpf.launching;

import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.EclipseJPFLauncher;
import gov.nasa.runjpf.internal.launching.JPFDebugger;
import gov.nasa.runjpf.tab.JPFCommonTab;
import gov.nasa.runjpf.tab.JPFDebugTab.JDWPInstallation;
import gov.nasa.runjpf.tab.JPFDebugTab.JDWPInstallations;
import gov.nasa.runjpf.tab.JPFSettings;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

import com.sun.jdi.connect.Connector.Argument;

public class JPFLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate implements
    ILaunchConfigurationDelegate {

  private void conditionallyAddOrOverride(List<String> programArgs, boolean override, String param, String value) {
    if (value == null || "".equals(value)) {
      return;
    }
    if (override) {
      programArgs.add(new StringBuilder("+").append(param).append("=").append(value).toString());
    } else {
      programArgs.add(new StringBuilder("++").append(param).append("=").append(value).append(",").toString());
    }
  }
  
  @Override
  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
    String jpfFile = configuration.getAttribute(JPFCommonTab.JPF_FILE_LOCATION, "");
    boolean debugBothVMs = configuration.getAttribute(JPFCommonTab.JPF_DEBUG_BOTHVMS, false);
    boolean debugJPFInsteadOfTheProgram = configuration.getAttribute(JPFCommonTab.JPF_DEBUG_JPF_INSTEADOFPROGRAM, false);
//    String listenerClass = configuration.getAttribute(JPFCommonTab.JPF_OPT_LISTENER, "");
//    String searchClass = configuration.getAttribute(JPFCommonTab.JPF_OPT_SEARCH, "");
//    String targetClass = configuration.getAttribute(JPFCommonTab.JPF_OPT_TARGET, "");
    boolean override = configuration.getAttribute(JPFCommonTab.JPF_OPT_OVERRIDE_INSTEADOFADD, false);
    
    // /*
    // * for those terminate by our self .
    // *
    // * @see #terminateOldRJRLauncher
    // */
    // if (/* run the validation */) {
    // throw new CoreException(
    // new Status(
    // IStatus.ERROR,
    // Plugin.PLUGIN_ID,
    // 01,
    // " Invalid run configuration , please check the configuration ",
    // null));
    // }

    // addSourcesLookupProjectsFromMavenIfExist(configuration);

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

      String jpfRunPath;
      try {
        EclipseJPFLauncher eclipseJpfLauncher = new EclipseJPFLauncher();
        File siteProperties = eclipseJpfLauncher.lookupSiteProperties();
        File jpfRunJar = eclipseJpfLauncher.lookupRunJpfJar(siteProperties);
        jpfRunPath = jpfRunJar.getAbsolutePath();
      } catch (NullPointerException npe) {
        EclipseJPF.logError("JPF was not sucessfully found.", npe);
        throw new CoreException(new Status(IStatus.ERROR, EclipseJPF.PLUGIN_ID, "JPF was not found", npe));
      }

      List<String> classpath = new LinkedList<>();
      classpath.add(jpfRunPath);
      
      if (ILaunchManager.DEBUG_MODE.equals(mode)) {
        int selectedJdwpInstallation = configuration.getAttribute(JPFCommonTab.JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX, -1);
        
        if (selectedJdwpInstallation == -1) {
          throw new CoreException(new Status(IStatus.ERROR, EclipseJPF.PLUGIN_ID, "JDWP was not selected - this shouldn't happended"));
        }
        
        if (selectedJdwpInstallation == JDWPInstallations.DEFAULT_INSTALLATION_INDEX) {
          // using embedded jdwp
          classpath.add(JDWPInstallation.EMBEDDED.getClasspathFile().getAbsolutePath());
        } // else JPF will resolve jpf-jdwp by itself 
      } 
      VMRunnerConfiguration runConfig = new VMRunnerConfiguration(EclipseJPF.JPF_MAIN_CLASS, classpath.toArray(new String[classpath.size()]));

      List<String> programArgs = new ArrayList<String>(Arrays.asList("+shell.port=4242", jpfFile));
      programArgs.addAll(Arrays.asList(execArgs.getProgramArgumentsArray()));
      
      @SuppressWarnings({ "unchecked" })
      Map<String, String> dynamicMap = configuration.getAttribute(JPFSettings.ATTR_JPF_DYNAMICCONFIG, Collections.<String, String>emptyMap());
      
      for (String key : dynamicMap.keySet()) {
        String value = dynamicMap.get(key);
        
        if (value.contains(JPFCommonTab.UNIQUE_ID_PLACEHOLDER)) {
          String uniqueId = UUID.randomUUID().toString();
          value = value.replace(JPFCommonTab.UNIQUE_ID_PLACEHOLDER, uniqueId);
        }
        
        programArgs.add(new StringBuilder("+").append(key).append("=").append(value).toString());
      }
      // TODO
//      conditionallyAddOrOverride(programArgs, override, "target", targetClass);
//      conditionallyAddOrOverride(programArgs, override, "listener", listenerClass);
//      conditionallyAddOrOverride(programArgs, override, "search.class", searchClass);
      
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

      // done the verification phase
      monitor.worked(1);
      monitor.subTask("Creating source locator");
      // set the default source locator if required
      setDefaultSourceLocator(launch, configuration);

      launch.getSourceLocator();
      monitor.worked(1);

      
      synchronized (configuration) {
        // terminateOldRJRLauncher(configuration, launch);
        // Launch the configuration - 1 unit of work
        // getVMRunner(configuration, mode)
        // .run(runConfig, launch, monitor);
        IVMRunner runner;
        if (ILaunchManager.DEBUG_MODE.equals(mode) && (debugBothVMs || !debugJPFInsteadOfTheProgram)) {
          IVMInstall vm = verifyVMInstall(configuration);
          runner = new JPFDebugger(vm, debugBothVMs);
        } else {
          runner = getVMRunner(configuration, mode);
        }
        runner.run(runConfig, launch, monitor);

        // registerRJRLauncher(configuration, launch);
      }

      // check for cancellation
      if (monitor.isCanceled())
        return;

    } finally {
      monitor.done();
    }
  }

}
