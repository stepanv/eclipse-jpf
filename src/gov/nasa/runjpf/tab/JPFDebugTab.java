package gov.nasa.runjpf.tab;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nasa.jpf.Config;
import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.EclipseJPFLauncher;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jdt.internal.debug.ui.actions.ControlAccessibleListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;

public class JPFDebugTab extends JPFCommonTab {

  private Button btnDebugBothTargets;
  private Button btnDebugJpfItself;
  private Button btnDebugTheProgram;
  private Group grpRuntime;
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

    postCreateControl(comp2);

    setControl(comp2);
    
    grpRuntime = new Group(comp2, SWT.NONE);
    grpRuntime.setLayout(new GridLayout(1, false));
    grpRuntime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    grpRuntime.setText("Runtime");
    
    
    fCombo = SWTFactory.createCombo(grpRuntime, SWT.DROP_DOWN | SWT.READ_ONLY, 1, null);
    //ControlAccessibleListener.addListener(fCombo, fSpecificButton.getText());
    fCombo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
//        setStatus(OK_STATUS);
//        firePropertyChange();
      }
    });
    String[] jdwps = (String[]) jdwpInstallations.toArray();
    fCombo.setItems(jdwps);
    fCombo.setVisibleItemCount(Math.min(jdwps.length, 20));
    
    String[] names = new String[] {"sdlfj", "sdlfkj", "dfdddd", "SDSS"};
  }
  
  public static class JDWPInstallation {
    public static final JDWPInstallation EMBEDED = new JDWPInstallation("Embeded", "Eclipse plugin-in");
    private String friendlyName;
    private String pseudoPath;
    
    JDWPInstallation(String friendlyName, String pseudoPath) {
      this.friendlyName = friendlyName;
      this.pseudoPath = pseudoPath;
    }
  }
  
  List<JDWPInstallation> jdwpInstallations = new ArrayList<>();
  
  private void populateJdwpInstallations(ILaunchConfiguration configuration) throws CoreException {
    jdwpInstallations.add(JDWPInstallation.EMBEDED);
    lookupLocalJdwpInstallation(jdwpInstallations, configuration.getAttribute(JPF_FILE_LOCATION, ""));
  }
  
  private void lookupLocalJdwpInstallation(List<JDWPInstallation> jdwpInstallations, String appJpfFile) {
    Config config;
    if (appJpfFile != null) {
      config = new Config(new String[] {appJpfFile});
    } else {
      config = new Config(new String[] {});
    }
    
//    String sitePath = getSitePropertiesPath();
//    if (sitePath == null) {
//      setErrorMessage("no site.properties");
//      return null;
//    }
//
//    File file = new File(sitePath);
    
    Map<String, File> projects = getSiteProjects(config);
    if (projects.containsKey("jpf-jdwp")) {
      String pseudoPath = projects.get("jpf-jdwp").getAbsolutePath();
      jdwpInstallations.add(new JDWPInstallation("Locally installed as an Extension", pseudoPath));
    }
  }
  
  protected String getSitePropertiesPath() {
    return EclipseJPF.getDefault().getPluginPreferences().getString(EclipseJPFLauncher.SITE_PROPERTIES_PATH);
  }

  protected Map<String, File> getSiteProjects(Config config) {
      Map<String, File> projects = new HashMap<>();

      for (String projId : config.getEntrySequence()) {
        if ("extensions".equals(projId)) {
          // we have to filter this out in case there is only a single project
          // in
          // the list, in which case we find a jpf.properties under its value
          continue;
        }

        String v = config.getString(projId);
        File projDir = new File(v);

        if (projDir.isDirectory()) {
          File propFile = new File(projDir, "jpf.properties");
          if (propFile.isFile()) {
            projects.put(projId, propFile);
          }
        }
      }
      return projects;
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

  public static void initDefaultConfiguration(ILaunchConfigurationWorkingCopy configuration, String projectName, IFile jpfFile) {
    JPFCommonTab.initDefaultConfiguration(configuration, projectName, jpfFile);
    configuration.setAttribute(JPF_DEBUG_BOTHVMS, false);
    configuration.setAttribute(JPF_DEBUG_JPF_INSTEADOFPROGRAM, false);
  }

  public void initializeFrom(ILaunchConfiguration configuration) {

    try {
      populateJdwpInstallations(configuration);
      
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
  
  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    super.setDefaults(configuration);
  }
}
