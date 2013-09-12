package gov.nasa.runjpf.tab;

import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.EclipseJPFLauncher;

import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

/**
 * <p>
 * This class just renames the standard Java launch <i>Arguments</i> tab. And
 * adds some defaults to it.
 * </p>
 * <p>
 * Even though the super class is marked as no-extendible it seems it works well.
 * </p>
 * 
 * @author stepan
 * 
 */
public class JPFArgumentsTab extends JavaArgumentsTab {

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy config) {
    super.setDefaults(config);

    defaults(config);
  }

  /**
   * Set the default values using the JPF default preferences.
   * 
   * @param configuration
   *          The Launch configuration
   */
  public static void defaults(ILaunchConfigurationWorkingCopy configuration) {
    String vmArgs = Platform.getPreferencesService().getString(EclipseJPF.BUNDLE_SYMBOLIC, EclipseJPFLauncher.VM_ARGS, "", null);
    configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);

    String programArgs = Platform.getPreferencesService().getString(EclipseJPF.BUNDLE_SYMBOLIC, EclipseJPFLauncher.ARGS, "", null);
    configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, programArgs);
  }

  @Override
  public String getName() {
    return "JPF Arguments";
  }
}
