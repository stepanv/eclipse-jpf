package gov.nasa.runjpf.tab;

import gov.nasa.runjpf.EclipseJPF;
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
public class JPFDebugTab extends JPFRunTab {

  private Button checkDebugBothTargets;
  private Button radioDebugJpfItself;
  private Button radioDebugTheProgram;
  private Combo comboJdwp;
  private Label labelJdwp;
  private Button buttonJdwpReset;

  private static final String[] REQUIRED_LIBRARIES = new String[] { "lib/jpf-jdwp.jar", "lib/slf4j-api-1.7.5.jar",
      "lib/slf4j-nop-1.7.5.jar" };
  private static final String EXTENSION_STRING = "jpf-jdwp";
  public static final ExtensionInstallations jdwpInstallations = ExtensionInstallations.factory(REQUIRED_LIBRARIES);

  /**
   * @wbp.parser.entryPoint
   */
  public void createControl(Composite parent) {
    super.createControl(parent);
  }

  @Override
  protected void runtimeAppend(Composite comp2) {

    labelJdwp = new Label(comp2, SWT.NONE);
    labelJdwp.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    labelJdwp.setText("JDWP:");

    comboJdwp = SWTFactory.createCombo(comp2, SWT.DROP_DOWN | SWT.READ_ONLY, 1, null);

    buttonJdwpReset = new Button(comp2, SWT.NONE);
    buttonJdwpReset.setText("Reset");
    buttonJdwpReset.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        jdwpInstallations.reset(REQUIRED_LIBRARIES);
        initializeExtensionInstallations(getCurrentLaunchConfiguration(), jdwpInstallations, comboJdwp,
                                         JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX, EXTENSION_STRING);
        updateLaunchConfigurationDialog();
      }
    });
    comboJdwp.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateLaunchConfigurationDialog();
      }
    });
  }

  @Override
  protected void runtimePrepend(Composite parent) {

    Group grpExperimentalSetting = new Group(parent, SWT.NONE);
    grpExperimentalSetting.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    grpExperimentalSetting.setText("Experimental settings");
    grpExperimentalSetting.setLayout(new GridLayout(2, false));

    radioDebugTheProgram = new Button(grpExperimentalSetting, SWT.RADIO);
    radioDebugTheProgram.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateLaunchConfigurationDialog();
      }
    });
    radioDebugTheProgram.setText("Debug the program being verified in JPF");

    checkDebugBothTargets = new Button(grpExperimentalSetting, SWT.CHECK);
    checkDebugBothTargets.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 2));
    checkDebugBothTargets.setText("Debug both the underlaying VM and the program (can lead to deadlocks)");

    radioDebugJpfItself = new Button(grpExperimentalSetting, SWT.RADIO);
    radioDebugJpfItself.setText("Debug JPF itself");
    radioDebugJpfItself.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        updateLaunchConfigurationDialog();
      }
    });

    checkDebugBothTargets.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {

        boolean readioChoicesEnabled = !checkDebugBothTargets.getSelection();
        radioDebugJpfItself.setEnabled(readioChoicesEnabled);
        radioDebugTheProgram.setEnabled(readioChoicesEnabled);
        updateLaunchConfigurationDialog();
      }
    });

  }

  /**
   * The default initialization of this tab.
   * 
   * @param configuration
   *          The configuration
   * @param projectName
   *          The project name
   * @param jpfFile
   *          APF Application properties file (*.jpf)
   */
  public static void initDefaultConfiguration(ILaunchConfigurationWorkingCopy configuration, String projectName, IFile jpfFile) {
    JPFRunTab.initDefaultConfiguration(configuration, projectName, jpfFile);

    // it's better to not use the embedded one if normal extension is detected
    configuration.setAttribute(JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX, -1);
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {

    try {
      lookupLocalInstallation(configuration, jdwpInstallations, EXTENSION_STRING);

      checkDebugBothTargets.setSelection(configuration.getAttribute(JPF_ATTR_DEBUG_DEBUGBOTHVMS, false));
      radioDebugJpfItself.setSelection(configuration.getAttribute(JPF_ATTR_DEBUG_DEBUGJPFINSTEADOFPROGRAM, false));
      radioDebugTheProgram.setSelection(!radioDebugJpfItself.getSelection());

      boolean readioChoicesEnabled = !checkDebugBothTargets.getSelection();
      radioDebugJpfItself.setEnabled(readioChoicesEnabled);
      radioDebugTheProgram.setEnabled(readioChoicesEnabled);

      initializeExtensionInstallations(configuration, jdwpInstallations, comboJdwp, JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX, EXTENSION_STRING);

    } catch (CoreException e) {
      EclipseJPF.logError("Error during the JPF initialization form", e);
    }

    super.initializeFrom(configuration);
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    super.performApply(configuration);

    boolean debugBothVms = checkDebugBothTargets.getSelection();
    boolean debugJpfItself = radioDebugJpfItself.getSelection();

    configuration.setAttribute(JPF_ATTR_DEBUG_DEBUGBOTHVMS, debugBothVms);
    configuration.setAttribute(JPF_ATTR_DEBUG_DEBUGJPFINSTEADOFPROGRAM, debugJpfItself);
    configuration.setAttribute(JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX, comboJdwp.getSelectionIndex());

    try {
      @SuppressWarnings("unchecked")
      Map<String, String> dynMapConfig = configuration.getAttribute(JPFSettingsTab.ATTR_JPF_DYNAMICCONFIG,
                                                                    Collections.<String, String> emptyMap());
      // we're using +jpf-core.native_classpath only here so we can safely
      // remove it
      // TODO move this to JPFSettings and wipe it always
      dynMapConfig.remove("+jpf-core.native_classpath");

      if (!debugJpfItself || debugBothVms) {
        // we're debugging the program itself

        int selectedJdwpInstallation = configuration.getAttribute(JPFRunTab.JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX, -1);

        if (selectedJdwpInstallation == -1) {
          EclipseJPF.logError("Obtained incorret jdwp installation index");
        } else if (selectedJdwpInstallation == ExtensionInstallations.EMBEDDED_INSTALLATION_INDEX) {
          // using embedded jdwp
          String classpath = jdwpInstallations.getEmbedded().classpath(File.pathSeparator);
          dynMapConfig.put("+jpf-core.native_classpath", classpath);
        } // nothing changes
      }
    } catch (CoreException e) {
      EclipseJPF.logError("Cannot store debug configuration into the launch configuration!", e);
    }

  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    initDefaultConfiguration(configuration, null, null);
    super.setDefaults(configuration);
  }

  @Override
  public boolean isPostValid(ILaunchConfiguration config) {
    if (comboJdwp.getSelectionIndex() == ExtensionInstallations.EMBEDDED_INSTALLATION_INDEX) {
      if (!jdwpInstallations.get(ExtensionInstallations.EMBEDDED_INSTALLATION_INDEX).isValid()) {
        setErrorMessage("Embedded JDWP installation in error due to: "
            + jdwpInstallations.get(ExtensionInstallations.EMBEDDED_INSTALLATION_INDEX).toString());
        return false;
      }
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
