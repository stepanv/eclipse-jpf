package gov.nasa.runjpf.launching;

import gov.nasa.runjpf.tab.JPFArgumentsTab;
import gov.nasa.runjpf.tab.JPFClasspathTab;
import gov.nasa.runjpf.tab.JPFCommonTab;
import gov.nasa.runjpf.tab.JPFDebugTab;
import gov.nasa.runjpf.tab.JPFSettingsTab;
import gov.nasa.runjpf.tab.JPFSourceLookupTab;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;

public class JPFLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup implements
    org.eclipse.debug.ui.ILaunchConfigurationTabGroup {

  @Override
  public void createTabs(ILaunchConfigurationDialog arg0, String arg1) {

    JPFCommonTab jpfTab;
    if (ILaunchManager.DEBUG_MODE.equals(arg0.getMode())) {
      jpfTab = new JPFDebugTab();
    } else {
      jpfTab = new JPFCommonTab();
    }
    ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] { jpfTab, new JPFSettingsTab(), new JPFArgumentsTab(), new JPFClasspathTab(), new JavaJRETab(),
        new JPFSourceLookupTab(), new EnvironmentTab(), new CommonTab() };
    setTabs(tabs);
  }
}
