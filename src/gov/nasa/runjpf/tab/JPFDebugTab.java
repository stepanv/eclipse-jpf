package gov.nasa.runjpf.tab;

import gov.nasa.runjpf.EclipseJPF;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

public class JPFDebugTab extends JPFCommonTab {

  private Button btnDebugBothTargets;
  private Button btnDebugJpfItself;
  private Button btnDebugTheProgram;

  /**
   * @wbp.parser.entryPoint
   */
  @Override
  public void createControl(Composite parent) {

    Composite comp2 = new Composite(parent, SWT.NONE);
    comp2.setFont(parent.getFont());

    GridData gd = new GridData(1);
    gd.horizontalAlignment = SWT.FILL;
    gd.grabExcessHorizontalSpace = true;
    gd.horizontalSpan = GridData.FILL_BOTH;
    comp2.setLayoutData(gd);

    GridLayout layout = new GridLayout(1, false);
    layout.verticalSpacing = 0;
    layout.horizontalSpacing = 0;
    comp2.setLayout(layout);

    super.createControl(comp2);

    postCreateControl(comp2);

    setControl(comp2);
  }

  public void postCreateControl(Composite comp3) {

    Group grpExperimentalSetting = new Group(comp3, SWT.NONE);
    grpExperimentalSetting.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    grpExperimentalSetting.setText("Experimental settings");
    grpExperimentalSetting.setLayout(new GridLayout(2, false));

    btnDebugTheProgram = new Button(grpExperimentalSetting, SWT.RADIO);
    btnDebugTheProgram.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateLaunchConfigurationDialog();
      }
    });
    btnDebugTheProgram.setText("Debug the program being verified in JPF");

    btnDebugBothTargets = new Button(grpExperimentalSetting, SWT.CHECK);
    btnDebugBothTargets.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 2));
    btnDebugBothTargets.setText("Debug both the underlaying VM and the program (can lead to deadlocks)");

    btnDebugJpfItself = new Button(grpExperimentalSetting, SWT.RADIO);
    btnDebugJpfItself.setText("Debug JPF itself");
    btnDebugJpfItself.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        updateLaunchConfigurationDialog();
      }
    });

    btnDebugBothTargets.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {

        boolean readioChoicesEnabled = !btnDebugBothTargets.getSelection();
        btnDebugJpfItself.setEnabled(readioChoicesEnabled);
        btnDebugTheProgram.setEnabled(readioChoicesEnabled);
        updateLaunchConfigurationDialog();
      }
    });

    return;

  }

  public static void initDefaultConfiguration(ILaunchConfigurationWorkingCopy configuration, String projectName, String launchConfigName, IFile jpfFile) {
    JPFCommonTab.initDefaultConfiguration(configuration, projectName, launchConfigName, jpfFile);
    configuration.setAttribute(JPF_DEBUG_BOTHVMS, false);
    configuration.setAttribute(JPF_DEBUG_JPF_INSTEADOFPROGRAM, false);
  }

  public void initializeFrom(ILaunchConfiguration configuration) {

    try {
      btnDebugBothTargets.setSelection(configuration.getAttribute(JPF_DEBUG_BOTHVMS, false));
      btnDebugJpfItself.setSelection(configuration.getAttribute(JPF_DEBUG_JPF_INSTEADOFPROGRAM, false));
      btnDebugTheProgram.setSelection(!btnDebugJpfItself.getSelection());

      boolean readioChoicesEnabled = !btnDebugBothTargets.getSelection();
      btnDebugJpfItself.setEnabled(readioChoicesEnabled);
      btnDebugTheProgram.setEnabled(readioChoicesEnabled);
    } catch (CoreException e) {
      EclipseJPF.logError("Error during the JPF initialization form", e);
    }

    super.initializeFrom(configuration);
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    super.performApply(configuration);
    configuration.setAttribute(JPF_DEBUG_BOTHVMS, btnDebugBothTargets.getSelection());
    configuration.setAttribute(JPF_DEBUG_JPF_INSTEADOFPROGRAM, btnDebugJpfItself.getSelection());
  }

}
