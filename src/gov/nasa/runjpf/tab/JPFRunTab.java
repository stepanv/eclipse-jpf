package gov.nasa.runjpf.tab;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class JPFRunTab extends JPFCommonTab {

  /**
   * @wbp.parser.entryPoint
   */
  @Override
  public void createControl(Composite parent) {

    Composite comp2 = new Composite(parent, SWT.NONE);
    comp2.setFont(parent.getFont());

    GridData gd = new GridData(1);
    gd.horizontalSpan = GridData.FILL_BOTH;
    comp2.setLayoutData(gd);

    GridLayout layout = new GridLayout(1, false);
    layout.verticalSpacing = 0;
    comp2.setLayout(layout);

    super.createControl(comp2);

    postCreateControl(comp2);

    setControl(comp2);
  }

  public void postCreateControl(Composite comp3) {


    return;

  }

  public static void initDefaultConfiguration(ILaunchConfigurationWorkingCopy configuration, String projectName, IFile jpfFile) {
    JPFCommonTab.initDefaultConfiguration(configuration, projectName, jpfFile);
  }

  public void initializeFrom(ILaunchConfiguration configuration) {

//    try {
//    } catch (CoreException e) {
//      EclipseJPF.logError("Error during the JPF initialization form", e);
//    }

    super.initializeFrom(configuration);
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    super.performApply(configuration);
  }
  
  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    super.setDefaults(configuration);
    initDefaultConfiguration(configuration, null, null);
  }

}
