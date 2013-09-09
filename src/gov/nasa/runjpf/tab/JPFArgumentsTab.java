package gov.nasa.runjpf.tab;

import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.EclipseJPFLauncher;

import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

public class JPFArgumentsTab extends JavaArgumentsTab {

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy config) {
    super.setDefaults(config);
    
    String vmArgs = Platform.getPreferencesService().getString(EclipseJPF.BUNDLE_SYMBOLIC, EclipseJPFLauncher.VM_ARGS, "", null);
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);
    
    String programArgs = Platform.getPreferencesService().getString(EclipseJPF.BUNDLE_SYMBOLIC, EclipseJPFLauncher.ARGS, "", null);
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, programArgs);
    
  }

  @Override
  public String getName() {
    return "JPF Arguments";
  }
  

}
