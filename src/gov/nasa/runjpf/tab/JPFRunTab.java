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

  private Text textTraceFile;
  private Button radioTraceStore;
  private Button radioTraceReplay;
  private Button radioTraceNoTrace;
  private Button buttonTraceBrowse;
  private Button buttonTraceBrowseWorkspace;

  private Combo comboJpfInstallation;
  private Button buttonJpfRuntimeReset;

  private IType targetType;

  private Button radioDebugBothTargets;
  private Button radioDebugJpfItself;
  private Button radioDebugTheProgram;
  private Combo comboJdwp;
  private Label labelJdwp;
  private Button buttonJdwpReset;

  private boolean debug;

  /**
   * @param equals
   */
  public JPFRunTab(boolean debug) {
    this.debug = debug;
  }

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
    GridLayout gl_comp2 = new GridLayout(2, false);
    comp2.setLayout(gl_comp2);

    Group grpJpfExecution = new Group(comp2, SWT.NONE);
    grpJpfExecution.setToolTipText("The basic JPF execution options.");
    grpJpfExecution.setText("JPF Execution");
    grpJpfExecution.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
    grpJpfExecution.setLayout(new GridLayout(4, false));

    radioMainAppFileSelected = new Button(grpJpfExecution, SWT.RADIO);
    radioMainAppFileSelected.setToolTipText("Run the JPF verification based on the settings from the .jpf application property file.");
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

    Label lblFile = new Label(grpJpfExecution, SWT.NONE);
    lblFile.setText("File:");

    textMainAppFileLocation = new Text(grpJpfExecution, SWT.BORDER);
    textMainAppFileLocation.setToolTipText("The .jpf file to run the JPF verfication based on.\r\nThe properties from this file can be overriden by some dynamically generated properties by this launch configuration or by the properties specified as program arguments in the JPF arguments tab.");
    textMainAppFileLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
    textMainAppFileLocation.addModifyListener(updatedListener);
    textMainAppFileLocation.setBounds(10, 35, 524, 21);
    new Label(grpJpfExecution, SWT.NONE);
    new Label(grpJpfExecution, SWT.NONE);

    buttonMainAppendDynamicProperties = new Button(grpJpfExecution, SWT.NONE);
    buttonMainAppendDynamicProperties.setToolTipText("Append all the dynamically generated properties to this .jpf file. The dynamically generated properties reflect all the options selected in this launch configuration including source and classpath settings.\r\nThis option may be helpful for preserving of all the options so that the JPF verification can be run later with the same properties even without Eclipse.");
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
    buttonMainBrowseWorkspace.setToolTipText("Find the .jpf file in the workspace. Only folders that contain at least one .jpf file are shown.");
    buttonMainBrowseWorkspace.setSize(109, 25);
    buttonMainBrowseWorkspace.setText("Browse &workspace");

    buttonMainBrowseFilesystem = new Button(composite_1, SWT.NONE);
    buttonMainBrowseFilesystem.setToolTipText("Browse filesystem for the .jpf file.");
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
    radioMainMethodClass.setToolTipText("Run the JPF verification directly from this main class.");
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

    Label lblTarget = new Label(grpJpfExecution, SWT.NONE);
    lblTarget.setText("Target:");

    textMainTarget = new Text(grpJpfExecution, SWT.BORDER);
    textMainTarget.setToolTipText("The target class where to start the JPF verification from. This class must implement the main method.");
    textMainTarget.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
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

    buttonMainSearchMainClass = new Button(grpJpfExecution, SWT.NONE);
    buttonMainSearchMainClass.setToolTipText("Select a class from the Eclipse indexed classes. This class must implement the main method.");
    buttonMainSearchMainClass.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    buttonMainSearchMainClass.setText("Search...");

    buttonMainSearchMainClass.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        targetType = handleSearchMainClassButtonSelected(textMainTarget, targetType);
      }
    });

    Group grpStopOptions = new Group(comp2, SWT.NONE);
    grpStopOptions.setToolTipText("Additional options how to stop the VM execution.");
    GridData gd_grpStopOptions = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
    gd_grpStopOptions.widthHint = 303;
    grpStopOptions.setLayoutData(gd_grpStopOptions);
    grpStopOptions.setText("Stop options");
    grpStopOptions.setLayout(new GridLayout(1, false));
    grpStopOptions.setEnabled(debug);

    checkMainStopOnPropertyViolation = new Button(grpStopOptions, SWT.CHECK);
    checkMainStopOnPropertyViolation.setToolTipText("Stop the verification on a property violation notification.\r\n\r\nThis option is useful for an inspection of the program state right before the JPF verification reports a property violation such as deadlocks or uncheck exception throws.\r\n\r\nThe JPF is stopped as if an exception is thrown and appropriate exception breakpoint is set.\r\nAll the threads will be suspended. \r\nThe name of the thrown exception is \"gov.nasa.jpf.jdwp.exception.special.NoPropertyViolationException\" which is a synthetic exception loaded into the program in the moment of property violation notification.\r\n\r\nThe search class used in the JPF verification must notify the listeners about the property violation through \"SearchListener.propertyViolated(Search)\" method otherwise this option is ineffective.\r\n\r\nThis option is effective only if debugging the program being verified.");
    checkMainStopOnPropertyViolation.setText("Stop on property violation");
    checkMainStopOnPropertyViolation.setEnabled(debug);
    checkMainStopOnPropertyViolation.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateLaunchConfigurationDialog();
      }
    });

    checkMainStopInAppMain = new Button(grpStopOptions, SWT.CHECK);
    checkMainStopInAppMain.setToolTipText("Stop on entry into the main method that is used as a target for the JPF verification.\r\n\r\nThis option is effective only if debugging the program being verified.");
    checkMainStopInAppMain.setText("Stop in application main");
    checkMainStopInAppMain.setEnabled(debug);
    checkMainStopInAppMain.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateLaunchConfigurationDialog();
      }
    });

    checkMainStopInJpfMain = new Button(grpStopOptions, SWT.CHECK);
    checkMainStopInJpfMain.setToolTipText("Stop on entry of the main method of JPF.\r\n\r\nThis option is effective only if debugging JPF or both the program being verified and JPF itself.");
    checkMainStopInJpfMain.setText("Stop in JPF main");
    checkMainStopInJpfMain.setEnabled(debug);
    checkMainStopInJpfMain.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateLaunchConfigurationDialog();
      }
    });

    Group grpDebuggingOptions = new Group(comp2, SWT.NONE);
    grpDebuggingOptions.setToolTipText("The options how to the debugging.");
    grpDebuggingOptions.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    grpDebuggingOptions.setText("Debugging options");
    grpDebuggingOptions.setLayout(new GridLayout(1, false));
    grpDebuggingOptions.setEnabled(debug);

    radioDebugTheProgram = new Button(grpDebuggingOptions, SWT.RADIO);
    radioDebugTheProgram.setToolTipText("Debug the program being verified by the JPF.\r\n\r\nThe debugging uses JDWP implementation from the jpf-jdwp JPF extension. \r\nThis extension  should be declared in JPF site properties file but it's also possible to use an embedded version that comes bundled with this plugin.\r\n");
    radioDebugTheProgram.setSize(233, 16);
    radioDebugTheProgram.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        debugOptionChanged();
        updateLaunchConfigurationDialog();
      }
    });
    radioDebugTheProgram.setText("Debug the program being verified in JPF");
    radioDebugTheProgram.setEnabled(debug);

    radioDebugJpfItself = new Button(grpDebuggingOptions, SWT.RADIO);
    radioDebugJpfItself.setToolTipText("Debug JPF itself as if it was a normal java program.\r\nThis option comes handy when developing JPF extensions or for JPF troubleshooting.");
    radioDebugJpfItself.setEnabled(debug);
    radioDebugJpfItself.setText("Debug JPF itself");

    radioDebugBothTargets = new Button(grpDebuggingOptions, SWT.RADIO);
    radioDebugBothTargets.setToolTipText("An experimental feature!\r\nSome of other options/features may stop working!\r\n\r\nDebug both the JPF itself and the program being verified by JPF.\r\nThis option is good for a debugging of the JPF debugging.\r\n\r\nCan lead to serious issues in the debugger and consequently Eclipse termination.");
    radioDebugBothTargets.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        debugOptionChanged();
        updateLaunchConfigurationDialog();
      }
    });
    radioDebugBothTargets.setText("Debug both the JPF itself and the program being verified by JPF (experimental)");
    radioDebugBothTargets.setEnabled(debug);
    radioDebugJpfItself.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        debugOptionChanged();
        updateLaunchConfigurationDialog();
      }
    });

    Group groupTrace = new Group(comp2, SWT.NONE);
    groupTrace.setToolTipText("Run the JPF verification normally.\r\nNo trace is a default option.");
    groupTrace.setLayout(new GridLayout(3, false));
    groupTrace.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    groupTrace.setText("Trace");

    Composite composite = new Composite(groupTrace, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
    composite.setLayout(new GridLayout(3, false));

    radioTraceNoTrace = new Button(composite, SWT.RADIO);
    radioTraceNoTrace.setToolTipText("The default behavior.\r\nThe trace operations provided by this plugin are ineffective.");
    radioTraceNoTrace.setText("No Trace");

    radioTraceStore = new Button(composite, SWT.RADIO);
    radioTraceStore.setToolTipText("Store the trace using the listener \"gov.nasa.jpf.listener.TraceStorer\" into the file specified bellow.");
    radioTraceStore.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        traceChanged();
      }
    });
    radioTraceStore.setBounds(0, 0, 90, 16);
    radioTraceStore.setText("Store");

    radioTraceReplay = new Button(composite, SWT.RADIO);
    radioTraceReplay.setToolTipText("Replay the trace as recorded by the \"gov.nasa.jpf.listener.TraceStorer\" listener using the \"gov.nasa.jpf.listener.ChoiceSelector\" listener.\r\nOnly if this option is enabled, the debugging of the program being verified by the JPF can be fully supported.\r\nTo understand this limitation, please, refer to the documentation.");
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
    textTraceFile.setToolTipText("The location of the trace file where to record the trace path or what to replay.");
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
    buttonTraceBrowseWorkspace.setToolTipText("Browse the workspace for the trace file.");
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
    buttonTraceBrowse.setToolTipText("Browse the filesystem for the trace file.");
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

  private void runtime(Composite parent) {

    Group groupRuntime = new Group(parent, SWT.NONE);
    groupRuntime.setToolTipText("JPF Runtime options.");
    groupRuntime.setLayout(new GridLayout(3, false));
    groupRuntime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    groupRuntime.setText("Runtime");

    Label labelJpfRuntime = new Label(groupRuntime, SWT.NONE);
    labelJpfRuntime.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    labelJpfRuntime.setText("JPF:");

    comboJpfInstallation = SWTFactory.createCombo(groupRuntime, SWT.DROP_DOWN | SWT.READ_ONLY, 1, null);
    comboJpfInstallation.setToolTipText("The JPF runtime to use for the JPF verification.\r\nThe JPF runtime is dynamically looked up using the whole JPF configuration stack. For further information refer to the documentation.\r\nThe embedded version is intended to be used only by beginners who don't have jpf-core installed in their system.");

    buttonJpfRuntimeReset = new Button(groupRuntime, SWT.NONE);
    buttonJpfRuntimeReset.setToolTipText("Reload the JPF runtime location using the whole JPF configuration stack.");
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

    labelJdwp = new Label(groupRuntime, SWT.NONE);
    labelJdwp.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    labelJdwp.setText("JDWP:");

    comboJdwp = SWTFactory.createCombo(groupRuntime, SWT.DROP_DOWN | SWT.READ_ONLY, 1, null);
    comboJdwp.setToolTipText("The location of jpf-jdwp extension that is required for the debugging of the program being verified by the JPF.\r\nThe embedded version should work just fine as long as the jpf-core extension used for the JPF verification remains compatible with the API declared in the JPF in the time of packaging of this bundled JDWP.");
    comboJdwp.setEnabled(debug);

    buttonJdwpReset = new Button(groupRuntime, SWT.NONE);
    buttonJdwpReset.setToolTipText("Reload the JDWP extension location using the whole JPF configuration stack.");
    buttonJdwpReset.setText("Reset");
    buttonJdwpReset.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        jdwpInstallations.reset(JDWP_REQUIRED_LIBRARIES);
        initializeExtensionInstallations(getCurrentLaunchConfiguration(), jdwpInstallations, comboJdwp,
                                         JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX, JDWP_EXTENSION_STRING);
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
    // it's better to not use the embedded one if normal extension is detected
    configuration.setAttribute(JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX, -1);

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
      setText(configuration, textMainTarget, JPFRunTab.JPF_ATTR_MAIN_JPFTARGET);

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

    try {
      lookupLocalInstallation(configuration, jdwpInstallations, JDWP_EXTENSION_STRING);

      radioDebugBothTargets.setSelection(configuration.getAttribute(JPF_ATTR_DEBUG_DEBUGBOTHVMS, false));
      radioDebugJpfItself.setSelection(configuration.getAttribute(JPF_ATTR_DEBUG_DEBUGJPFINSTEADOFPROGRAM, false));
      radioDebugTheProgram.setSelection(!radioDebugJpfItself.getSelection() && !radioDebugBothTargets.getSelection());
      
      debugOptionChanged();

      initializeExtensionInstallations(configuration, jdwpInstallations, comboJdwp, JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX,
                                       JDWP_EXTENSION_STRING);

    } catch (CoreException e) {
      EclipseJPF.logError("Error during the JPF initialization form", e);
      MessageDialog.openError(getShell(), "Error during the initialization of the tab!", e.getMessage());
      return;
    }

    super.initializeFrom(configuration);
  }

  private void debugOptionChanged() {
    boolean debugJpfOnly = radioDebugJpfItself.getSelection();
    boolean debugProgramOnly = radioDebugTheProgram.getSelection();
    
    boolean debuggingProgram = debugProgramOnly || !debugJpfOnly;
    boolean debuggingJpf = !debugProgramOnly || debugJpfOnly;
    checkMainStopOnPropertyViolation.setEnabled(debuggingProgram && debug);
    checkMainStopInAppMain.setEnabled(debuggingProgram && debug);
    checkMainStopInJpfMain.setEnabled(debuggingJpf && debug);
    
    comboJdwp.setEnabled(debuggingProgram && debug);
    buttonJdwpReset.setEnabled(debuggingProgram && debug);
    
  }

  String getJpfFileLocation() {
    return textMainAppFileLocation.getText().trim();
  }

  @Override
  public String getName() {
    return "JPF Run";
  }

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

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    generateImplicitProject(configuration);

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

    String traceFileName = textTraceFile.getText();
    configuration.setAttribute(JPF_ATTR_TRACE_FILE, traceFileName);

    if (!radioTraceNoTrace.getSelection()) {
      // let's substitute the generated random string into the unique
      // placeholder
      if (traceFileName.contains(JPFRunTab.UNIQUE_ID_PLACEHOLDER)) {
        String uniqueId = UUID.randomUUID().toString();
        traceFileName = traceFileName.replace(JPFRunTab.UNIQUE_ID_PLACEHOLDER, uniqueId);
        textTraceFile.setText(traceFileName);
      }
    }

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

    configuration.setAttribute(JPF_ATTR_DEBUG_DEBUGBOTHVMS, radioDebugBothTargets.getSelection());
    configuration.setAttribute(JPF_ATTR_DEBUG_DEBUGJPFINSTEADOFPROGRAM, radioDebugJpfItself.getSelection());
    configuration.setAttribute(JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX, comboJdwp.getSelectionIndex());

    storeDynamicConfiguration(configuration, debug);

  }

  /**
   * Generates an implicit project according to the given configuration. <br/>
   * The implicit project is used in default working directory for example.
   * 
   * @param configuration
   */
  private void generateImplicitProject(ILaunchConfigurationWorkingCopy configuration) {
    IProject implicitProject = null;

    for (IType type : new IType[] { targetType }) {
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
