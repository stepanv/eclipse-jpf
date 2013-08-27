package gov.nasa.runjpf.launching;

import gov.nasa.runjpf.tab.JPFCommonTab;
import gov.nasa.runjpf.tab.JPFDebugTab;
import gov.nasa.runjpf.tab.JPFRunTab;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;

public class JPFLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup implements
    org.eclipse.debug.ui.ILaunchConfigurationTabGroup {

  @Override
  public void createTabs(ILaunchConfigurationDialog arg0, String arg1) {

    JPFCommonTab jpfTab;
    if (ILaunchManager.DEBUG_MODE.equals(arg0.getMode())) {
      jpfTab = new JPFDebugTab();
    } else {
      jpfTab = new JPFRunTab();
    }
    ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] { jpfTab, new JavaArgumentsTab(), new JavaJRETab(),
        new SourceLookupTab(), new EnvironmentTab(), new CommonTab() };
    setTabs(tabs);
  }
}
