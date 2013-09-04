package gov.nasa.runjpf.tab;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nasa.jpf.Config;
import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.EclipseJPFLauncher;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
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
import org.osgi.framework.Bundle;

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
    grpRuntime.setLayout(new GridLayout(3, false));
    grpRuntime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    grpRuntime.setText("Runtime");
    
    lblJdwp = new Label(grpRuntime, SWT.NONE);
    lblJdwp.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    lblJdwp.setText("JDWP:");
    
    
    fCombo = SWTFactory.createCombo(grpRuntime, SWT.DROP_DOWN | SWT.READ_ONLY, 1, null);
    
    btnConfigure = new Button(grpRuntime, SWT.NONE);
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
  
  public static class JDWPInstallation {
    public static final JDWPInstallation EMBEDDED = new JDWPInstallation("Embedded", generateClasspathEmbedded());
    
    private static List<File> generateClasspathEmbedded() {
      List<File> requiredFiles = new LinkedList<>();
      for (String relativePath : new String[] { "lib/jpf-jdwp.jar", "lib/slf4j-api-1.7.5.jar", "lib/slf4j-nop-1.7.5.jar" }) {
        requiredFiles.add(locateEmbeddedFile(relativePath));
      }
      return requiredFiles;
    }
    
    private static File locateEmbeddedFile(String relativePath) {
      try {
        Bundle bundle = Platform.getBundle(EclipseJPF.BUNDLE_SYMBOLIC);
        Path path = new Path(relativePath);
        URL clientFileURL = FileLocator.find(bundle, path, null);
        URL fileURL = FileLocator.resolve(clientFileURL);
        return new File(fileURL.toURI());
      } catch (URISyntaxException | IOException e) {
        EclipseJPF.logError("Cannot locate embedded JPF JDWP", e);
        return new File("");
      }
    }
    
    private String friendlyName = null;
    private String pseudoPath = "";
    private List<File> classpathFiles = Collections.EMPTY_LIST;
    
    JDWPInstallation(String friendlyName, String pseudoPath) {
      this.friendlyName = friendlyName;
      this.pseudoPath = pseudoPath;
    }
    
    JDWPInstallation(String friendlyName, List<File> classpathFiles) {
      this.friendlyName = friendlyName;
      this.classpathFiles = classpathFiles;
      
      if (classpathFiles.size() > 0) {
        String path = classpathFiles.get(0).getAbsolutePath();
        int trimLength = 70;
        if (path.length() - trimLength >= 0) {
          path = "... " + path.substring(path.length() - trimLength, path.length());
        }
        this.pseudoPath = path;
      }
    }

  	@Override
  	public String toString() {
  		return new StringBuilder(friendlyName).append(" (location: ").append(pseudoPath).append(")").toString();
  	}

    public String classpath(String delimiter) {
      String classpath = "";
      for (File file : classpathFiles) {
        classpath += file.getAbsolutePath() + delimiter;
      }
      return classpath;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof JDWPInstallation)) {
        return false;
      }
      JDWPInstallation other = (JDWPInstallation)obj;
      if (!other.pseudoPath.equals(pseudoPath)) {
        return false;
      }
      if (!other.friendlyName.equals(friendlyName)) {
        return false;
      }
      if (classpathFiles != null && other.classpathFiles == null) {
        return false;
      }
      if (classpathFiles == null && other.classpathFiles != null) {
        return false;
      }
      if (classpathFiles == null && other.classpathFiles == null) {
        return true;
      }
      return true;
    }
    
    
  }
  
  public static class JDWPInstallations extends ArrayList<JDWPInstallation> implements List<JDWPInstallation> {
    /**	 */
    private static final long serialVersionUID = 1L;
    
    public static final int DEFAULT_INSTALLATION_INDEX = 0;
    public JDWPInstallations() {
      add(DEFAULT_INSTALLATION_INDEX, JDWPInstallation.EMBEDDED);
    }

    public String[] toStringArray(String[] array) {
      if (array.length < this.size()) {
        throw new UnsupportedOperationException("The array specified must have a good size!");
      }
      int i = 0;
      for (JDWPInstallation jdwpInstallation : this) {
        array[i++] = jdwpInstallation.toString();
      }
      return array;
    }
  }
  
  // TODO put them to an appropriate place
  public static final JDWPInstallations jdwpInstallations = new JDWPInstallations();
  private Label lblJdwp;
  private Button btnConfigure;
  
  private void populateJdwpInstallations(ILaunchConfiguration configuration) throws CoreException {
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
      JDWPInstallation localJdwpInstallation = new JDWPInstallation("Locally installed as jpf-jdwp extension", pseudoPath);
      if (!jdwpInstallations.contains(localJdwpInstallation)) {
        jdwpInstallations.add(localJdwpInstallation);
      }
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
        if (v == null) {
        	continue;
        }
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
    
    // TODO it's better to not use the embedded one if normal extension is detected
    configuration.setAttribute(JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX, JDWPInstallations.DEFAULT_INSTALLATION_INDEX);
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
      
      String[] jdwps = (String[]) jdwpInstallations.toStringArray(new String[jdwpInstallations.size()]);
      fCombo.setItems(jdwps);
      fCombo.setVisibleItemCount(Math.min(jdwps.length, 20));
      fCombo.select(configuration.getAttribute(JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX, JDWPInstallations.DEFAULT_INSTALLATION_INDEX));
      
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
    configuration.setAttribute(JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX, fCombo.getSelectionIndex());
    
    try {
      Map<String, String> dynMapConfig = configuration.getAttribute(JPFSettings.ATTR_JPF_DYNAMICCONFIG, Collections.EMPTY_MAP);
      // we're using +jpf-core.native_classpath only here so we can safely remove it
      // TODO move this to JPFSettings and wipe it always
      dynMapConfig.remove("+jpf-core.native_classpath");
      
      int selectedJdwpInstallation = configuration.getAttribute(JPFCommonTab.JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX, -1);
      
      if (selectedJdwpInstallation == -1) {
        EclipseJPF.logError("Obtained incorret jdwp installation index");
      } else if (selectedJdwpInstallation == JDWPInstallations.DEFAULT_INSTALLATION_INDEX) {
        // using embedded jdwp
        dynMapConfig.put("+jpf-core.native_classpath", JDWPInstallation.EMBEDDED.classpath(File.pathSeparator));
      } // nothing changes
      
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
    setWarningMessage(null);
    if (fCombo.getSelectionIndex() == JDWPInstallations.DEFAULT_INSTALLATION_INDEX) {
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
