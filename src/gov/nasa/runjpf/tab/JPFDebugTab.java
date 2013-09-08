package gov.nasa.runjpf.tab;

import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.EclipseJPFLauncher;
import gov.nasa.runjpf.internal.ui.ExtensionInstallation;
import gov.nasa.runjpf.internal.ui.ExtensionInstallations;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

@SuppressWarnings("restriction")
public class JPFDebugTab extends JPFCommonTab {

  
  private Button btnDebugBothTargets;
  private Button btnDebugJpfItself;
  private Button btnDebugTheProgram;
  private Combo fCombo;

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

    setControl(comp2);
   
  }

  protected void runtimeAppend(Composite comp2) {
    
    lblJdwp = new Label(comp2, SWT.NONE);
    lblJdwp.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    lblJdwp.setText("JDWP:");
    
    
    fCombo = SWTFactory.createCombo(comp2, SWT.DROP_DOWN | SWT.READ_ONLY, 1, null);
    
    btnConfigure = new Button(comp2, SWT.NONE);
    btnConfigure.setText("Configure");
    //ControlAccessibleListener.addListener(fCombo, fSpecificButton.getText());
    fCombo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateLaunchConfigurationDialog();
//        setStatus(OK_STATUS);
//        firePropertyChange();
      }
    });
  }
  
  // TODO put them to an appropriate place
  public static final ExtensionInstallations jdwpInstallations = new ExtensionInstallations(new ExtensionInstallation("Embedded", ExtensionInstallation.generateClasspathEmbedded(new String[] { "lib/jpf-jdwp.jar", "lib/slf4j-api-1.7.5.jar", "lib/slf4j-nop-1.7.5.jar" })));
  
  private Label lblJdwp;
  private Button btnConfigure;
  
  
  protected String getSitePropertiesPath() {
    return EclipseJPF.getDefault().getPluginPreferences().getString(EclipseJPFLauncher.SITE_PROPERTIES_PATH);
  }

  
  
  @Override
  protected void runtimePrepend(Composite parent) {
  
    Group grpExperimentalSetting = new Group(parent, SWT.NONE);
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

  public static void initDefaultConfiguration(ILaunchConfigurationWorkingCopy configuration, String projectName, IFile jpfFile) {
    JPFCommonTab.initDefaultConfiguration(configuration, projectName, jpfFile);
    configuration.setAttribute(JPF_DEBUG_BOTHVMS, false);
    configuration.setAttribute(JPF_DEBUG_JPF_INSTEADOFPROGRAM, false);
    
    // TODO it's better to not use the embedded one if normal extension is detected
    configuration.setAttribute(JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX, -1);
  }

  public void initializeFrom(ILaunchConfiguration configuration) {

    try {
      lookupLocalInstallation(jdwpInstallations, configuration.getAttribute(JPF_FILE_LOCATION, ""), "jpf-jdwp");
      
      btnDebugBothTargets.setSelection(configuration.getAttribute(JPF_DEBUG_BOTHVMS, false));
      btnDebugJpfItself.setSelection(configuration.getAttribute(JPF_DEBUG_JPF_INSTEADOFPROGRAM, false));
      btnDebugTheProgram.setSelection(!btnDebugJpfItself.getSelection());

      boolean readioChoicesEnabled = !btnDebugBothTargets.getSelection();
      btnDebugJpfItself.setEnabled(readioChoicesEnabled);
      btnDebugTheProgram.setEnabled(readioChoicesEnabled);
      
      String[] jdwps = (String[]) jdwpInstallations.toStringArray(new String[jdwpInstallations.size()]);
      fCombo.setItems(jdwps);
      fCombo.setVisibleItemCount(Math.min(jdwps.length, 20));
      fCombo.select(jdwpInstallations.getDefaultInstallationIndex());
      
      if (configuration.getAttribute(JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX, -1) == -1) {
        // this is the first initialization ever
        fCombo.select(jdwpInstallations.getDefaultInstallationIndex());
      } else {
        fCombo.select(configuration.getAttribute(JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX, ExtensionInstallations.EMBEDDED_INSTALLATION_INDEX));
      }
      
    } catch (CoreException e) {
      EclipseJPF.logError("Error during the JPF initialization form", e);
    }

    super.initializeFrom(configuration);
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    super.performApply(configuration);
    
    boolean debugBothVms = btnDebugBothTargets.getSelection();
    boolean debugJpfItself = btnDebugJpfItself.getSelection();
    
    configuration.setAttribute(JPF_DEBUG_BOTHVMS, debugBothVms);
    configuration.setAttribute(JPF_DEBUG_JPF_INSTEADOFPROGRAM, debugJpfItself);
    configuration.setAttribute(JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX, fCombo.getSelectionIndex());
    
    try {
      @SuppressWarnings("unchecked")
      Map<String, String> dynMapConfig = configuration.getAttribute(JPFSettingsTab.ATTR_JPF_DYNAMICCONFIG, Collections.<String,String>emptyMap());
      // we're using +jpf-core.native_classpath only here so we can safely remove it
      // TODO move this to JPFSettings and wipe it always
      dynMapConfig.remove("+jpf-core.native_classpath");
      
      if (!debugJpfItself || debugBothVms) {
        // we're debugging the program itself
        
        int selectedJdwpInstallation = configuration.getAttribute(
            JPFCommonTab.JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX, -1);

        if (selectedJdwpInstallation == -1) {
          EclipseJPF.logError("Obtained incorret jdwp installation index");
        } else if (selectedJdwpInstallation == ExtensionInstallations.EMBEDDED_INSTALLATION_INDEX) {
          // using embedded jdwp
          String classpath = jdwpInstallations.getEmbedded().classpath(File.pathSeparator);
          dynMapConfig.put("+jpf-core.native_classpath", classpath);
        } // nothing changes
      }
    } catch (CoreException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
  }
  
  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    initDefaultConfiguration(configuration, null, null);
    super.setDefaults(configuration);
  }

  @Override
  public boolean isValid(ILaunchConfiguration config) {
    setErrorMessage(null);
    setMessage(null);
    setWarningMessage(null);
    if (fCombo.getSelectionIndex() == ExtensionInstallations.EMBEDDED_INSTALLATION_INDEX) {
      // selected embedded
      if (jdwpInstallations.size() > 1) {
        // we have other than embedded jdwps
        setWarningMessage("If embedded JDWP is used it is likely, it will interfere with locally installed jpf-jdwp extension.");
      }
    }
    if (jdwpInstallations.size() > 2) {
      // we have other than embedded jdwps
      setWarningMessage("Multiple JDWP extensions found. It is likely, there will be some classpath issues.");
    }
    return super.isValid(config);
  }
}
