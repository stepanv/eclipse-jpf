package gov.nasa.runjpf.launching;

import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

public class JavaPathFinderLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate implements ILaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		System.out.println("here");
		
		String jpfFile = configuration.getAttribute(JPFRunTab.JPF_FILE_LOCATION, "");
		
		System.out.println("JPF File: " + jpfFile);
		
//		/*
//		 * for those terminate by our self .
//		 *
//		 * @see #terminateOldRJRLauncher
//		 */
//		if (/* run the validation */) {
//			throw new CoreException(
//					new Status(
//							IStatus.ERROR,
//							Plugin.PLUGIN_ID,
//							01,
//							" Invalid run configuration , please check the configuration ",
//							null));
//		}

//		addSourcesLookupProjectsFromMavenIfExist(configuration);

		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		monitor.beginTask(
				MessageFormat.format("{0}...", configuration.getName()), 3); //$NON-NLS-1$

		// check for cancellation
		if (monitor.isCanceled())
			return;

		try {
			monitor.subTask("verifying installation");

			// Program & VM arguments
			ExecutionArguments execArgs = new ExecutionArguments(
					getVMArguments(configuration),
					getProgramArguments(configuration));

			
			VMRunnerConfiguration runConfig = new VMRunnerConfiguration(
					"gov.nasa.jpf.tool.RunJPF",
					new String[] {"C:/Users/jd39686/../../apps/SVN_WorkingCopy/devel/gov.nasa.jpf.core/build/RunJPF.jar"});

//			runConfig.setProgramArguments(execArgs.getProgramArgumentsArray());
			
			runConfig.setProgramArguments(new String[] {"+shell.port=4242", jpfFile});

			// Environment variables
			runConfig.setEnvironment(getEnvironment(configuration));

//			boolean debug = ILaunchManager.DEBUG_MODE.equals(mode);
//			runConfig.setVMArguments(getRuntimeArguments(configuration,
//					execArgs.getVMArgumentsArray(),debug));

//			runConfig
//					.setWorkingDirectory(getWorkingDirectoryAbsolutePath(configuration));
			runConfig
					.setVMSpecificAttributesMap(getVMSpecificAttributesMap(configuration));

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
//				terminateOldRJRLauncher(configuration, launch);
				// Launch the configuration - 1 unit of work
//				getVMRunner(configuration, mode)
//						.run(runConfig, launch, monitor);
				IVMRunner runner;
				if (ILaunchManager.DEBUG_MODE.equals(mode)) {
					IVMInstall vm = verifyVMInstall(configuration);
					runner = new JPFDebugger(vm);
					
				} else {
					runner = getVMRunner(configuration, mode);
				}
				runner.run(runConfig, launch, monitor);
				
//				registerRJRLauncher(configuration, launch);
			}

			// check for cancellation
			if (monitor.isCanceled())
				return;

		} finally {
			monitor.done();
		}
		
		
		
	}

}
