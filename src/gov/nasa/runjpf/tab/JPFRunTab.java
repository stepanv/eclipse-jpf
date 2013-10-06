package gov.nasa.runjpf.tab;

import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.EclipseJPFLauncher;
import gov.nasa.runjpf.internal.resources.FilteredFileSelectionDialog;
import gov.nasa.runjpf.internal.ui.ExtensionInstallations;
import gov.nasa.runjpf.tab.internal.LookupConfigHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * <p>
 * This is a GUI SWT Eclipse launch configuration Tab for Java PathFinder
 * Verification launch action.<br/>
 * The intention of this tab is to provide a user with an easy way to choose
 * some handy JPF options as well as to specify what to execute (verify).
 * </p>
 * 
 * @author stepan
 * 
 */
@SuppressWarnings("restriction")
public class JPFRunTab extends CommonJPFTab {

  private static final String FLUSHED_COMMENT = "" + System.lineSeparator() + "# This is dynamic configuration by the Eclipse-JPF plugin"
      + System.lineSeparator();

  private static final String REQUIRED_LIBRARY = "lib/jpf.jar";
  private static final String EXTENSION_PROJECT = "jpf-core";

  public static final String UNIQUE_ID_PLACEHOLDER = "{UNIQUE_ID}";

  public static final ExtensionInstallations jpfInstallations = ExtensionInstallations.factory(REQUIRED_LIBRARY);

  private Text textMainAppFileLocation;
  private Text textMainTarget;
  private Button radioMainAppFileSelected;
  private Button radioMainMethodClass;
  private Button buttonMainAppendDynamicProperties;
  private Button buttonMainBrowseWorkspace;
  private Button buttonMainBrowseFilesystem;
  private Button buttonMainSearchMainClass;
  private Button checkMainStopInAppMain;
  private Button checkMainStopOnPropertyViolation;
  private Button checkMainStopInJpfMain;

  private Text textOptListenerClass;
  private Text textOptSearchClass;
  private Text textOptShellPort;
  private Button checkOptShellEnabled;

  private Text textTraceFile;
  private Button radioTraceStore;
  private Button radioTraceReplay;
  private Button radioTraceNoTrace;
  private Button buttonTraceBrowse;
  private Button buttonTraceBrowseWorkspace;

  private Combo comboJpfInstallation;
  private Button buttonJpfRuntimeReset;

  private IType listenerType;
  private IType searchType;
  private IType targetType;

  private String createPlaceholderedTraceFile() {
    String commonDir = Platform.getPreferencesService().getString(EclipseJPF.BUNDLE_SYMBOLIC, EclipseJPFLauncher.COMMON_DIR, "", null);
    return commonDir + File.separatorChar + "trace-" + UNIQUE_ID_PLACEHOLDER + ".txt";
  }

  /**
   * Main group selection and modification handler.
   * 
   * @param isJpfFile
   *          whether JPF file was selected or a main class
   */
  private void runJpfSelected(boolean isJpfFile) {
    buttonMainAppendDynamicProperties.setEnabled(isJpfFile);
    buttonMainBrowseFilesystem.setEnabled(isJpfFile);
    buttonMainBrowseWorkspace.setEnabled(isJpfFile);
    textMainAppFileLocation.setEnabled(isJpfFile);

    textMainTarget.setEnabled(!isJpfFile);
    buttonMainSearchMainClass.setEnabled(!isJpfFile);

    updateLaunchConfigurationDialog();
  }

  /**
   * Trace selection changed.
   */
  private void traceChanged() {
    boolean isNoTrace = radioTraceNoTrace.getSelection();
    textTraceFile.setEnabled(!isNoTrace);
    buttonTraceBrowse.setEnabled(!isNoTrace);
    buttonTraceBrowseWorkspace.setEnabled(!isNoTrace);
    updateLaunchConfigurationDialog();
  }

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

    setControl(comp2);

    Composite comp = comp2;
    GridLayout gl_comp2 = new GridLayout(1, false);
    comp2.setLayout(gl_comp2);

    Group grpJpfExecution = new Group(comp2, SWT.NONE);
    grpJpfExecution.setText("JPF Execution");
    grpJpfExecution.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
    grpJpfExecution.setLayout(new GridLayout(5, false));

    radioMainAppFileSelected = new Button(grpJpfExecution, SWT.RADIO);
    radioMainAppFileSelected.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        runJpfSelected(radioMainAppFileSelected.getSelection());
      }

    });
    radioMainAppFileSelected.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
    radioMainAppFileSelected.setText("Run a .jpf file:");
    new Label(grpJpfExecution, SWT.NONE);
    new Label(grpJpfExecution, SWT.NONE);
    new Label(grpJpfExecution, SWT.NONE);

    Label lblFile = new Label(grpJpfExecution, SWT.NONE);
    lblFile.setText("File:");

    textMainAppFileLocation = new Text(grpJpfExecution, SWT.BORDER);
    textMainAppFileLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
    textMainAppFileLocation.addModifyListener(updatedListener);
    textMainAppFileLocation.setBounds(10, 35, 524, 21);
    new Label(grpJpfExecution, SWT.NONE);
    new Label(grpJpfExecution, SWT.NONE);

    buttonMainAppendDynamicProperties = new Button(grpJpfExecution, SWT.NONE);
    buttonMainAppendDynamicProperties.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent selectionEvent) {
        ILaunchConfiguration configuration = getCurrentLaunchConfiguration();
        String fileString = textMainAppFileLocation.getText();
        FileWriter fileWrite = null;
        try {
          @SuppressWarnings("unchecked")
          Map<String, String> config = configuration.getAttribute(JPFOverviewTab.ATTR_JPF_DYNAMICCONFIG,
                                                                  Collections.<String, String> emptyMap());

          fileWrite = new FileWriter(fileString, true);

          fileWrite.append(FLUSHED_COMMENT);
          for (String key : config.keySet()) {
            fileWrite.append(new StringBuilder(key).append(" = ").append(config.get(key)).append(System.lineSeparator()));
          }
          fileWrite.close();
        } catch (CoreException | IOException e) {
          setErrorMessage("Cannot store dynamic properties in the file: '" + fileString + "'");
          EclipseJPF.logError("Error occurred while saving dynamic properties to the file '" + fileString + "'", e);
        } finally {
          if (fileWrite != null) {
            try {
              fileWrite.close();
            } catch (IOException e) {
              // we don't care ...
            }
          }
        }
      }
    });
    buttonMainAppendDynamicProperties.setText("&Append dynamic properties into this file");
    new Label(grpJpfExecution, SWT.NONE);

    Composite basicConfiguraionComposite = new Composite(grpJpfExecution, SWT.NONE);
    basicConfiguraionComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
    GridLayout gl_basicConfiguraionComposite = new GridLayout(1, false);
    gl_basicConfiguraionComposite.marginHeight = 0;
    gl_basicConfiguraionComposite.marginWidth = 0;
    basicConfiguraionComposite.setLayout(gl_basicConfiguraionComposite);
    basicConfiguraionComposite.setFont(comp.getFont());

    Composite composite_1 = new Composite(basicConfiguraionComposite, SWT.NONE);
    composite_1.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    GridLayout gl_composite_1 = new GridLayout(2, false);
    gl_composite_1.marginHeight = 0;
    gl_composite_1.marginWidth = 0;
    composite_1.setLayout(gl_composite_1);

    buttonMainBrowseWorkspace = new Button(composite_1, SWT.NONE);
    buttonMainBrowseWorkspace.setSize(109, 25);
    buttonMainBrowseWorkspace.setText("Browse &workspace");

    buttonMainBrowseFilesystem = new Button(composite_1, SWT.NONE);
    buttonMainBrowseFilesystem.setText("Browse &filesystem");
    buttonMainBrowseFilesystem.setBounds(540, 33, 106, 25);

    buttonMainBrowseFilesystem.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
        dialog.setFilterExtensions(new String[] { "*.jpf" });
        if (getJpfFileLocation().length() > 0) {
          dialog.setFileName(getJpfFileLocation());
        }
        String file = dialog.open();
        if (file != null) {
          file = file.trim();
          if (file.length() > 0) {
            textMainAppFileLocation.setText(file);
            updateLaunchConfigurationDialog();
            setDirty(true);
          }
        }
      }
    });
    buttonMainBrowseWorkspace.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        FilteredFileSelectionDialog dialog = new FilteredFileSelectionDialog(
            "JPF File Selection", "Choose a .jpf file", new String[] { "jpf" }); //$NON-NLS-1$

        int buttonId = dialog.open();
        if (buttonId == IDialogConstants.OK_ID) {
          Object[] resource = dialog.getResult();
          if (resource != null && resource.length > 0) {
            if (resource[0] instanceof IFile) {
              String filePath = ((IFile) resource[0]).getLocation().toString();
              textMainAppFileLocation.setText(filePath);
              setDirty(true);
              updateLaunchConfigurationDialog();
            }
          }
        }
      }
    });

    radioMainMethodClass = new Button(grpJpfExecution, SWT.RADIO);
    radioMainMethodClass.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        runJpfSelected(!radioMainMethodClass.getSelection());
      }
    });
    radioMainMethodClass.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
    radioMainMethodClass.setText("Run a main class:");
    new Label(grpJpfExecution, SWT.NONE);
    new Label(grpJpfExecution, SWT.NONE);
    new Label(grpJpfExecution, SWT.NONE);

    Label lblTarget = new Label(grpJpfExecution, SWT.NONE);
    lblTarget.setText("Target:");

    textMainTarget = new Text(grpJpfExecution, SWT.BORDER);
    textMainTarget.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
    textMainTarget.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        updateLaunchConfigurationDialog();
      }
    });
    new Label(grpJpfExecution, SWT.NONE);
    new Label(grpJpfExecution, SWT.NONE);

    Composite composite_2 = new Composite(grpJpfExecution, SWT.NONE);
    composite_2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
    GridLayout glComposite = new GridLayout(1, false);
    glComposite.marginHeight = 0;
    glComposite.marginWidth = 0;
    composite_2.setLayout(glComposite);
    new Label(grpJpfExecution, SWT.NONE);

    buttonMainSearchMainClass = new Button(grpJpfExecution, SWT.NONE);
    buttonMainSearchMainClass.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    buttonMainSearchMainClass.setText("Search...");
    
    checkMainStopOnPropertyViolation = new Button(grpJpfExecution, SWT.CHECK);
    checkMainStopOnPropertyViolation.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 5, 1));
    checkMainStopOnPropertyViolation.setText("Stop on property violation");
    checkMainStopOnPropertyViolation.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateLaunchConfigurationDialog();
      }
    });
    
    checkMainStopInAppMain = new Button(grpJpfExecution, SWT.CHECK);
    checkMainStopInAppMain.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 5, 1));
    checkMainStopInAppMain.setText("Stop in application main");
    checkMainStopInAppMain.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateLaunchConfigurationDialog();
      }
    });
    
    checkMainStopInJpfMain = new Button(grpJpfExecution, SWT.CHECK);
    checkMainStopInJpfMain.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 5, 1));
    checkMainStopInJpfMain.setText("Stop in JPF main");
    checkMainStopInJpfMain.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateLaunchConfigurationDialog();
      }
    });
    
    buttonMainSearchMainClass.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        targetType = handleSearchMainClassButtonSelected(textMainTarget, targetType);
      }
    });

    Group groupOpt = new Group(comp, SWT.NONE);
    groupOpt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    groupOpt.setText("Common JPF settings");
    groupOpt.setLayout(new GridLayout(5, false));

    Label labelListener = new Label(groupOpt, SWT.NONE);
    labelListener.setText("Listener:");

    textOptListenerClass = new Text(groupOpt, SWT.BORDER);
    textOptListenerClass.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
    textOptListenerClass.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        updateLaunchConfigurationDialog();
      }
    });

    Button buttonListenerSearch = new Button(groupOpt, SWT.NONE);
    buttonListenerSearch.setText("Search...");
    buttonListenerSearch.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        listenerType = handleSupertypeSearchButtonSelected("gov.nasa.jpf.JPFListener", textOptListenerClass, listenerType);
        updateLaunchConfigurationDialog();
      }
    });

    Label labelSearch = new Label(groupOpt, SWT.NONE);
    labelSearch.setText("Search:");

    textOptSearchClass = new Text(groupOpt, SWT.BORDER);
    textOptSearchClass.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
    textOptSearchClass.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        updateLaunchConfigurationDialog();
      }
    });

    Button searchSearchButton = new Button(groupOpt, SWT.NONE);
    searchSearchButton.setText("Search...");
    searchSearchButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        searchType = handleSupertypeSearchButtonSelected("gov.nasa.jpf.search.Search", textOptSearchClass, searchType);
        updateLaunchConfigurationDialog();
      }
    });

    checkOptShellEnabled = new Button(groupOpt, SWT.CHECK);
    checkOptShellEnabled.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    checkOptShellEnabled.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        boolean isShellEnabled = checkOptShellEnabled.getSelection();
        textOptShellPort.setEnabled(isShellEnabled);
        updateLaunchConfigurationDialog();
      }
    });
    checkOptShellEnabled.setText("Enable shell on port:");

    textOptShellPort = new Text(groupOpt, SWT.BORDER);
    textOptShellPort.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    new Label(groupOpt, SWT.NONE);
    new Label(groupOpt, SWT.NONE);

    Group groupTrace = new Group(comp2, SWT.NONE);
    groupTrace.setLayout(new GridLayout(3, false));
    groupTrace.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    groupTrace.setText("Trace");

    Composite composite = new Composite(groupTrace, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
    composite.setLayout(new GridLayout(3, false));

    radioTraceNoTrace = new Button(composite, SWT.RADIO);
    radioTraceNoTrace.setText("No Trace");

    radioTraceStore = new Button(composite, SWT.RADIO);
    radioTraceStore.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        traceChanged();
      }
    });
    radioTraceStore.setBounds(0, 0, 90, 16);
    radioTraceStore.setText("Store");

    radioTraceReplay = new Button(composite, SWT.RADIO);
    radioTraceReplay.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        traceChanged();
      }
    });
    radioTraceReplay.setText("Replay");

    final Label labelTraceFile = new Label(groupTrace, SWT.CHECK);
    labelTraceFile.setText("Trace File:");

    textTraceFile = new Text(groupTrace, SWT.BORDER);
    textTraceFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    new Label(groupTrace, SWT.NONE);
    new Label(groupTrace, SWT.NONE);

    Composite composite_3 = new Composite(groupTrace, SWT.NONE);
    GridLayout gl_composite_3 = new GridLayout(2, false);
    gl_composite_3.marginHeight = 0;
    gl_composite_3.marginWidth = 0;
    composite_3.setLayout(gl_composite_3);
    composite_3.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 1, 1));

    buttonTraceBrowseWorkspace = new Button(composite_3, SWT.NONE);
    buttonTraceBrowseWorkspace.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        FilteredFileSelectionDialog dialog = new FilteredFileSelectionDialog("Trace File Selection", "Choose a file", null /* no filtering */); //$NON-NLS-1$

        int buttonId = dialog.open();
        if (buttonId == IDialogConstants.OK_ID) {
          Object[] resource = dialog.getResult();
          if (resource != null && resource.length > 0) {
            if (resource[0] instanceof IFile) {
              String filePath = ((IFile) resource[0]).getLocation().toString();
              textTraceFile.setText(filePath);
              updateLaunchConfigurationDialog();
            }
          }
        }
      }
    });
    buttonTraceBrowseWorkspace.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    buttonTraceBrowseWorkspace.setText("Browse workspace");

    buttonTraceBrowse = new Button(composite_3, SWT.NONE);
    buttonTraceBrowse.setText("Browse filesystem");

    buttonTraceBrowse.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
        dialog.setFilterExtensions(new String[] { "*.*" });
        if (getJpfFileLocation().length() > 0) {
          dialog.setFileName(getJpfFileLocation());
        }
        String file = dialog.open();
        if (file != null) {
          file = file.trim();
          if (file.length() > 0) {
            textTraceFile.setText(file);
            updateLaunchConfigurationDialog();
          }
        }
      }
    });

    radioTraceNoTrace.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        boolean traceEnabled = !radioTraceNoTrace.getSelection();
        buttonTraceBrowse.setEnabled(traceEnabled);
        buttonTraceBrowseWorkspace.setEnabled(traceEnabled);
        labelTraceFile.setEnabled(traceEnabled);
        textTraceFile.setEnabled(traceEnabled);
        updateLaunchConfigurationDialog();
      }
    });

    runtime(comp2);
  }

  /** Append graphic objects to the runtime */
  protected void runtimeAppend(Composite parent) {
    // empty implementation for subclasses;
  }

  /**
   * Prepend graphic object to the Runtime.
   * 
   * @param parent
   *          The parent object
   */
  protected void runtimePrepend(Composite parent) {
    // empty implementation for subclasses;
  }

  private void runtime(Composite parent) {
    runtimePrepend(parent);

    Group groupRuntime = new Group(parent, SWT.NONE);
    groupRuntime.setLayout(new GridLayout(3, false));
    groupRuntime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    groupRuntime.setText("Runtime");

    Label labelJpfRuntime = new Label(groupRuntime, SWT.NONE);
    labelJpfRuntime.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    labelJpfRuntime.setText("JPF:");

    comboJpfInstallation = SWTFactory.createCombo(groupRuntime, SWT.DROP_DOWN | SWT.READ_ONLY, 1, null);

    buttonJpfRuntimeReset = new Button(groupRuntime, SWT.NONE);
    buttonJpfRuntimeReset.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        jpfInstallations.reset(REQUIRED_LIBRARY);
        initializeExtensionInstallations(getCurrentLaunchConfiguration(), jpfInstallations, comboJpfInstallation,
                                         JPF_ATTR_RUNTIME_JPF_INSTALLATIONINDEX, EXTENSION_PROJECT);
        updateLaunchConfigurationDialog();
      }
    });
    buttonJpfRuntimeReset.setText("Reset");
    comboJpfInstallation.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateLaunchConfigurationDialog();
        // setStatus(OK_STATUS);
        // firePropertyChange();
      }
    });

    runtimeAppend(groupRuntime);
  }

  private static String defaultProperty(Map<String, String> map, String property, String defValue) {
    if (map == null || !map.containsKey(property)) {
      return defValue;
    }
    return (String) map.get(property);
  }

  /**
   * Default settings for this tab.
   * 
   * @param configuration
   *          Launch configuration
   * @param projectName
   *          Project name or null (is used for working dir etc..)
   * @param jpfFile
   *          JPF Application property file or null
   */
  public static void initDefaultConfiguration(ILaunchConfigurationWorkingCopy configuration, String projectName, IFile jpfFile) {

    // set a unique id
    configuration.setAttribute(JPF_ATTR_LAUNCHID, JPF_ATTR_LAUNCHID);
    
    configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, EclipseJPF.JPF_MAIN_CLASS);
    configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
    

    if (jpfFile != null) {
      String jpfFileAbsolutePath = jpfFile.getLocation().toFile().getAbsolutePath();
      configuration.setAttribute(JPF_ATTR_MAIN_JPFFILELOCATION, jpfFileAbsolutePath);
    } else {
      configuration.setAttribute(JPF_ATTR_MAIN_JPFFILELOCATION, "");
    }

    configuration.setAttribute(JPF_ATTR_TRACE_STOREINSTEADOFREPLAY, false);
    configuration.setAttribute(JPF_ATTR_TRACE_ENABLED, false);

    // now we still don't know what is better to pick
    configuration.setAttribute(JPF_ATTR_RUNTIME_JPF_INSTALLATIONINDEX, -1);

    try {
      @SuppressWarnings("unchecked")
      Map<String, String> map = configuration.getAttribute(JPFOverviewTab.ATTR_JPF_APPCONFIG, Collections.EMPTY_MAP);

      configuration.setAttribute(JPFRunTab.JPF_ATTR_OPT_LISTENER, defaultProperty(map, "listener", ""));
      configuration.setAttribute(JPFRunTab.JPF_ATTR_OPT_SEARCH, defaultProperty(map, "search.class", ""));
      configuration.setAttribute(JPFRunTab.JPF_ATTR_MAIN_JPFTARGET, defaultProperty(map, "target", ""));

    } catch (CoreException e) {
      EclipseJPF.logError("Unable to store the dynamic configuration to the standard fields", e);
    }

  }

  /**
   * Set text from the launch configuration for the given attribute name.
   * 
   * @param configuration
   *          Launch configuration
   * @param text
   *          The text field to set
   * @param attributeName
   *          The attribute name
   * @throws CoreException
   *           if {@link ILaunchConfiguration#getAttribute(String, String)}
   *           throws an error.
   */
  protected void setText(ILaunchConfiguration configuration, Text text, String attributeName) throws CoreException {
    text.setText(configuration.getAttribute(attributeName, ""));
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {

    try {
      textMainAppFileLocation.setText(configuration.getAttribute(JPF_ATTR_MAIN_JPFFILELOCATION, ""));
      setText(configuration, textOptListenerClass, JPFRunTab.JPF_ATTR_OPT_LISTENER);
      setText(configuration, textOptSearchClass, JPFRunTab.JPF_ATTR_OPT_SEARCH);
      setText(configuration, textMainTarget, JPFRunTab.JPF_ATTR_MAIN_JPFTARGET);

      checkOptShellEnabled.setSelection(configuration.getAttribute(JPF_ATTR_OPT_SHELLENABLED, true));
      int defaultShellPort = Platform.getPreferencesService().getInt(EclipseJPF.BUNDLE_SYMBOLIC, EclipseJPFLauncher.PORT,
                                                                     EclipseJPFLauncher.DEFAULT_PORT, null);
      textOptShellPort.setText(String.valueOf(configuration.getAttribute(JPF_ATTR_OPT_SHELLPORT, defaultShellPort)));
      textOptShellPort.setEnabled(configuration.getAttribute(JPF_ATTR_OPT_SHELLENABLED, true));

      boolean traceEnabled = configuration.getAttribute(JPF_ATTR_TRACE_ENABLED, false);

      radioTraceNoTrace.setSelection(!traceEnabled);
      radioTraceReplay.setSelection(traceEnabled && !configuration.getAttribute(JPF_ATTR_TRACE_STOREINSTEADOFREPLAY, false));
      radioTraceStore.setSelection(traceEnabled && configuration.getAttribute(JPF_ATTR_TRACE_STOREINSTEADOFREPLAY, false));
      textTraceFile.setEnabled(traceEnabled);
      buttonTraceBrowse.setEnabled(traceEnabled);
      buttonTraceBrowseWorkspace.setEnabled(traceEnabled);
      textTraceFile.setText(configuration.getAttribute(JPF_ATTR_TRACE_FILE, createPlaceholderedTraceFile()));

      boolean jpfFileSelected = configuration.getAttribute(JPF_ATTR_MAIN_JPFFILESELECTED, true);
      runJpfSelected(jpfFileSelected);
      radioMainMethodClass.setSelection(!jpfFileSelected);
      radioMainAppFileSelected.setSelection(jpfFileSelected);
      
      checkMainStopInAppMain.setSelection(configuration.getAttribute(JPF_ATTR_MAIN_STOPINMAIN, false));
      checkMainStopOnPropertyViolation.setSelection(configuration.getAttribute(JPF_ATTR_MAIN_STOPONPROPERTYVIOLATION, false));
      checkMainStopInJpfMain.setSelection(configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_STOP_IN_MAIN, false));

      initializeExtensionInstallations(configuration, jpfInstallations, comboJpfInstallation, JPF_ATTR_RUNTIME_JPF_INSTALLATIONINDEX,
                                       EXTENSION_PROJECT);

    } catch (CoreException e) {
      EclipseJPF.logError("Error during the JPF initialization form", e);
      MessageDialog.openError(getShell(), "Error during the initialization of the tab!", e.getMessage());
      return;
    }

    super.initializeFrom(configuration);
  }

  String getJpfFileLocation() {
    return textMainAppFileLocation.getText().trim();
  }

  @Override
  public String getName() {
    return "JPF Run";
  }

  private String addListener(String originalListener, String newListener) {
    if (originalListener == null || "".equals(originalListener)) {
      return newListener;
    }
    if (originalListener.contains(newListener)) {
      return originalListener;
    }
    return originalListener + "," + newListener;
  }

  // private String removeListener(String originalListener, String
  // removalListener) {
  // if (originalListener == null) {
  // return null;
  // }
  // if (originalListener.contains(removalListener)) {
  // // T O D O this is completely wrong!!!
  // int startIndex = originalListener.indexOf(removalListener);
  // String result = originalListener.substring(0, startIndex);
  // int advancedIndex = startIndex + removalListener.length() + 1;
  // if (originalListener.length() >= advancedIndex) {
  // String append = originalListener.substring(startIndex +
  // removalListener.length() + 1);
  //
  // if (result.endsWith(",") && append.startsWith(",")) {
  // result += append.substring(1);
  // } else {
  // result += append;
  // }
  // } else {
  // if (result.endsWith(",")) {
  // result = result.substring(0, result.length() - 1);
  // }
  // }
  // return result;
  // }
  // return originalListener;
  // }

  public boolean attributeEquals(ILaunchConfiguration configuration, String stringAttributeName, String valueCandidate) {
    if (valueCandidate == null) {
      return false;
    }
    try {
      String currentValue = configuration.getAttribute(stringAttributeName, "");
      return currentValue.trim().equals(valueCandidate.trim());
    } catch (CoreException e) {
      EclipseJPF.logError("Accessing " + stringAttributeName + " triggered an error!", e);
      return false;
    }
  }

  private void performApplyDynamicConfiguration(ILaunchConfigurationWorkingCopy configuration) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, String> map = configuration.getAttribute(JPFOverviewTab.ATTR_JPF_DYNAMICCONFIG, Collections.EMPTY_MAP);

      String listenerString = "";
      map.remove("trace.file");
      map.remove("choice.use_trace");
      map.remove("listener");
      map.remove("search.class");
      map.remove("target");
      map.remove("shell.port");

      if (!radioTraceNoTrace.getSelection()) {
        // we're tracing
        String traceFileName = textTraceFile.getText().trim();
        if (radioTraceStore.getSelection()) {
          // we're storing a trace

          // let's substitute the generated random string into the unique
          // placeholder
          if (traceFileName.contains(JPFRunTab.UNIQUE_ID_PLACEHOLDER)) {
            String uniqueId = UUID.randomUUID().toString();
            traceFileName = traceFileName.replace(JPFRunTab.UNIQUE_ID_PLACEHOLDER, uniqueId);
            textTraceFile.setText(traceFileName);
          }
          map.put("trace.file", traceFileName);

          listenerString = addListener(listenerString, ".listener.TraceStorer");

        } else if (radioTraceReplay.getSelection()) {
          // we're replaying a trace
          map.put("choice.use_trace", traceFileName);

          listenerString = addListener(listenerString, ".listener.ChoiceSelector");
        } else {
          throw new IllegalStateException("Shouldn't occur");
        }
        configuration.setAttribute(JPF_ATTR_TRACE_FILE, traceFileName);
      }

      if (checkOptShellEnabled.getSelection()) {
        map.put("shell.port", textOptShellPort.getText());
      }

      if (!isApplicationProperty(configuration, "listener", textOptListenerClass.getText())) {
        listenerString = addListener(listenerString, textOptListenerClass.getText().trim());
      }
      if (!Objects.equals("", listenerString)) {
        map.put("listener", listenerString);
      }
      putIfNotApplicationPropertyAndNotEmpty(configuration, map, "search.class", textOptSearchClass.getText());
      putIfNotApplicationPropertyAndNotEmpty(configuration, map, "target", textMainTarget.getText());

    } catch (CoreException e) {
      EclipseJPF.logError("Cannot reset dynamic configuration properties!", e);
    }
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    generateImplicitProject(configuration);

    configuration.setAttribute(JPF_ATTR_OPT_SHELLENABLED, checkOptShellEnabled.getSelection());
    // port is already validated
    int portShell = Integer.parseInt(textOptShellPort.getText());
    configuration.setAttribute(JPF_ATTR_OPT_SHELLPORT, portShell);

    if (!attributeEquals(configuration, JPF_ATTR_MAIN_JPFFILELOCATION, textMainAppFileLocation.getText())) {
      // jpf file location has changed
      configuration.setAttribute(JPF_ATTR_MAIN_JPFFILELOCATION, textMainAppFileLocation.getText());

      // reload app config
      LookupConfigHelper.reloadConfig(configuration, JPFOverviewTab.ATTR_JPF_APPCONFIG, LookupConfigHelper.appConfigFactory(configuration));

      // reload jpf installations
      initializeExtensionInstallations(configuration, jpfInstallations, comboJpfInstallation, JPF_ATTR_RUNTIME_JPF_INSTALLATIONINDEX,
                                       EXTENSION_PROJECT);
    }

    configuration.setAttribute(JPF_ATTR_TRACE_ENABLED, !radioTraceNoTrace.getSelection());
    configuration.setAttribute(JPF_ATTR_TRACE_STOREINSTEADOFREPLAY, radioTraceStore.getSelection());

    configuration.setAttribute(JPF_ATTR_OPT_LISTENER, textOptListenerClass.getText().trim());
    configuration.setAttribute(JPF_ATTR_OPT_SEARCH, textOptSearchClass.getText().trim());
    configuration.setAttribute(JPF_ATTR_MAIN_JPFTARGET, textMainTarget.getText().trim());

    int selectedJpfInstallation = comboJpfInstallation.getSelectionIndex();
    configuration.setAttribute(JPF_ATTR_RUNTIME_JPF_INSTALLATIONINDEX, selectedJpfInstallation);

    if (selectedJpfInstallation == ExtensionInstallations.EMBEDDED_INSTALLATION_INDEX) {
      // using embedded JPF
      String classpath = jpfInstallations.getEmbedded().classpath(File.pathSeparator);
      configuration.setAttribute(JPF_ATTR_RUNTIME_JPF_EMBEDDEDCLASSPATH, classpath);
    } else {
      // clear it
      configuration.removeAttribute(JPF_ATTR_RUNTIME_JPF_EMBEDDEDCLASSPATH);
    }

    configuration.setAttribute(JPF_ATTR_MAIN_JPFFILESELECTED, radioMainAppFileSelected.getSelection());
    
    configuration.setAttribute(JPF_ATTR_MAIN_STOPINMAIN, checkMainStopInAppMain.getSelection());
    configuration.setAttribute(JPF_ATTR_MAIN_STOPONPROPERTYVIOLATION, checkMainStopOnPropertyViolation.getSelection());
    configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_STOP_IN_MAIN, checkMainStopInJpfMain.getSelection());

    performApplyDynamicConfiguration(configuration);
  }

  /**
   * Generates an implicit project according to the given configuration. <br/>
   * The implicit project is used in default working directory for example.
   * 
   * @param configuration
   */
  private void generateImplicitProject(ILaunchConfigurationWorkingCopy configuration) {
    IProject implicitProject = null;

    for (IType type : new IType[] { searchType, listenerType, targetType }) {
      if (type == null || type.getJavaProject() == null) {
        continue;
      }
      implicitProject = type.getJavaProject().getProject();
    }

    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    URI jpfFileUri = new File(textMainAppFileLocation.getText()).toURI();

    IFile[] iFiles = root.findFilesForLocationURI(jpfFileUri);
    for (int i = iFiles.length - 1; i >= 0; --i) {
      if (iFiles[i].getProject() == null) {
        continue;
      }
      implicitProject = iFiles[i].getProject();
    }

    if (implicitProject != null) {
      configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, implicitProject.getName());
    }
  }

  /**
   * Tests whether given file exists
   * 
   * @param filename
   *          Filename of the file
   * @return true or false
   */
  private static boolean testFileExists(String filename) {
    File file = new File(filename);
    return file.isFile() && file.exists();
  }

  /** The icon for this tab */
  private static final Image icon = createImage("icons/service_manager.png");

  @Override
  public Image getImage() {
    return icon;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.debug.ui.AbstractLaunchConfigurationTab#isValid(org.eclipse
   * .debug.core.ILaunchConfiguration)
   */
  @Override
  final public boolean isValid(ILaunchConfiguration config) {
    setErrorMessage(null);
    setMessage(null);
    setWarningMessage(null);
    String jpfFileString = textMainAppFileLocation.getText();
    String targetString = textMainTarget.getText();
    if (Objects.equals("", jpfFileString) && Objects.equals("", targetString)) {
      setErrorMessage("Either JPF File (*.jpf) or Target class has to be specified!");
      return false;
    }

    if (radioMainAppFileSelected.getSelection()) {
      if (!"".equals(jpfFileString) && !jpfFileString.toLowerCase().endsWith(".jpf")) {
        // this is here because we provide just .jpf files in the workspace
        // browser and as such we want to be consistent
        setErrorMessage("JPF File (*.jpf) must end with .jpf extension!");
        return false;
      }
      if (radioMainAppFileSelected.getSelection() && !testFileExists(jpfFileString)) {
        setErrorMessage("Provided JPF file: '" + jpfFileString + "' doesn't exist!");
        return false;
      }
    } else {
      if ("".equals(textMainTarget.getText())) {
        setErrorMessage("Specified target class must not be empty!");
        return false;
      }
    }
    String traceFileString = textTraceFile.getText();
    if (radioTraceReplay.getSelection() && !testFileExists(traceFileString)) {
      setErrorMessage("Provided trace file: '" + traceFileString + "' doesn't exist!");
      return false;
    }
    if (checkOptShellEnabled.getSelection()) {
      int port;
      try {
        port = Integer.parseInt(textOptShellPort.getText());
      } catch (NumberFormatException e) {
        setErrorMessage("Provided port number cannot be converted to integer: " + e.getMessage());
        return false;
      }
      if (port < 0) {
        // let's do not care about the upper bound cause I don't know if there
        // are platforms that support different number than the normal one
        setErrorMessage("Provided port is invalid");
        return false;
      }
    }

    if (comboJpfInstallation.getSelectionIndex() == ExtensionInstallations.EMBEDDED_INSTALLATION_INDEX) {
      // selected embedded

      if (!jpfInstallations.get(ExtensionInstallations.EMBEDDED_INSTALLATION_INDEX).isValid()) {
        setErrorMessage("Embedded JPF installation in error due to: "
            + jpfInstallations.get(ExtensionInstallations.EMBEDDED_INSTALLATION_INDEX).toString());
        return false;
      }

      if (jpfInstallations.size() > 1) {
        // we have other than embedded jdwps
        setWarningMessage("If embedded JPF is used it is likely, it will interfere with locally installed jpf-core extension.");
      }
    }
    if (jpfInstallations.size() > 2) {
      // we have other than embedded jdwps
      setWarningMessage("Multiple JPF extensions found. It is likely, there will be some classpath issues.");
    }

    if (!isPostValid(config)) {
      return false;
    }
    return super.isValid(config);
  }

  /**
   * Post-validation for subtypes.
   * 
   * @param config
   *          Launch configuration
   * @return whether it's valid or invalid
   */
  protected boolean isPostValid(ILaunchConfiguration config) {
    return true;
    // for subtypes
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    initDefaultConfiguration(configuration, null, null);
  }
}
