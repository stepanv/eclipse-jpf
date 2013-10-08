package gov.nasa.runjpf.launching;

import gov.nasa.runjpf.tab.JPFArgumentsTab;
import gov.nasa.runjpf.tab.JPFClasspathTab;
import gov.nasa.runjpf.tab.JPFOverviewTab;
import gov.nasa.runjpf.tab.JPFRunTab;
import gov.nasa.runjpf.tab.JPFSourceLookupTab;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;

/**
 * The tab group that facilities all the tabs JPF launch configuration dialog
 * consists of.
 * 
 * @author stepan
 * 
 */
public class JPFLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup implements
    org.eclipse.debug.ui.ILaunchConfigurationTabGroup {

  @Override
  public void createTabs(ILaunchConfigurationDialog arg0, String arg1) {
    boolean debug = ILaunchManager.DEBUG_MODE.equals(arg0.getMode());
    ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] { new JPFRunTab(debug),
        new JPFOverviewTab(debug), new JPFArgumentsTab(), new JPFClasspathTab(), new JavaJRETab(), new JPFSourceLookupTab(),
        new EnvironmentTab(), new CommonTab() };
    setTabs(tabs);
  }
}
