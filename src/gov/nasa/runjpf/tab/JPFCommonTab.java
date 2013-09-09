package gov.nasa.runjpf.tab;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPFConfigException;
import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.EclipseJPFLauncher;
import gov.nasa.runjpf.internal.resources.FilteredFileSelectionDialog;
import gov.nasa.runjpf.internal.ui.ExtensionInstallation;
import gov.nasa.runjpf.internal.ui.ExtensionInstallations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
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
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.sun.jdi.IntegerValue;

@SuppressWarnings("restriction")
public class JPFCommonTab extends AbstractJPFTab {

  private static final String ATTRIBUTE_UNIQUE_PREFIX = "gov.nasa.jpf.runjpf-attributeprefix-";
  
  public static final String JPF_ATTR_TRACE_STORE_INSTEADOF_REPLAY = ATTRIBUTE_UNIQUE_PREFIX + "JPF_TRACE_STORE";
  public static final String JPF_ATTR_TRACE_FILE = ATTRIBUTE_UNIQUE_PREFIX + "JPF_TRACE_FILE";
  public static final String JPF_ATTR_TRACE_ENABLED = ATTRIBUTE_UNIQUE_PREFIX + "JPF_TRACE_ENABLED";
  public static final String JPF_ATTR_OPT_LISTENER = ATTRIBUTE_UNIQUE_PREFIX + "JPF_OPT_LISTENER";
  public static final String JPF_ATTR_OPT_SEARCH = ATTRIBUTE_UNIQUE_PREFIX + "JPF_OPT_SEARCH";
  public static final String JPF_ATTR_OPT_TARGET = ATTRIBUTE_UNIQUE_PREFIX + "JPF_OPT_TARGET";
  
  public static final String JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX = ATTRIBUTE_UNIQUE_PREFIX + "JPF_ATTR_DEBUG_JDWP_INSTALLATIONINDEX";
  public static final String JPF_ATTR_RUNTIME_JPF_INSTALLATIONINDEX = ATTRIBUTE_UNIQUE_PREFIX + "JPF_ATTR_RUNTIME_JPF_INSTALLATIONINDEX";
  public static final String JPF_ATTR_RUNTIME_JPF_EMBEDDEDCLASSPATH = ATTRIBUTE_UNIQUE_PREFIX + "JPF_ATTR_RUNTIME_JPF_EMBEDDEDCLASSPATH";


  private static final String FLUSHED_COMMENT = "" + System.lineSeparator() + "# This is flushed dynamic configuration by the Eclipse-JPF plugin" + System.lineSeparator();
  private Text jpfFileLocationText;

  private Text listenerText;
  private Text searchText;
  private Text targetText;
  private IType listenerType;
  private IType searchType;
  private IType targetType;
  private Text textTraceFile;

  private Button radioTraceStore;

  private Button radioTraceReplay;

  private Button radioTraceNoTrace;

  private Label lblTraceFile;

  private Button buttonTraceBrowse;

  private Group groupRuntime;

  private Label lblJpf;

  private Combo jpfCombo;

  private Button btnJpfReset;

  private Button buttonTraceBrowseWorkspace;

  private Button btnAppendDynamicProperties;

  private Button btnBrowseWorkspace;

  private Button btnbrowseFilesystem;

  private Button radioMainMethodClass;

  private Button btnSearch;

  private Button radioJpfFileSelected;

  private static final String REQUIRED_LIBRARY = "lib/jpf.jar";
  private static final String EXTENSION_PROJECT = "jpf-core";
  public static final ExtensionInstallations jpfInstallations = ExtensionInstallations.factory(REQUIRED_LIBRARY);
      
  public static final String UNIQUE_ID_PLACEHOLDER = "{UNIQUE_ID}";

  private static final String JPF_ATTR_RUNTIME_JPFFILESELECTED = "JPF_ATTR_RUNTIME_JPFFILESELECTED";

  private static final String JPF_ATTR_SHELL_ENABLED = "JPF_ATTR_SHELL_ENABLED";

  private static final String JPF_ATTR_SHELL_PORT = "JPR_ATTR_SHELL_PORT";
  private Button checkShellEnabled;
  private Text textShellPort;

  private String createPlaceholderedTraceFile() {
    String commonDir = Platform.getPreferencesService().getString(EclipseJPF.BUNDLE_SYMBOLIC, EclipseJPFLauncher.COMMON_DIR, "", null);;
    return commonDir + File.separatorChar + "trace-" + UNIQUE_ID_PLACEHOLDER + ".txt";
  }
  
  private void runJpfSelected(boolean isJpfFile) {
    btnAppendDynamicProperties.setEnabled(isJpfFile);
    btnbrowseFilesystem.setEnabled(isJpfFile);
    btnBrowseWorkspace.setEnabled(isJpfFile);
    jpfFileLocationText.setEnabled(isJpfFile);
    
    targetText.setEnabled(!isJpfFile);
    btnSearch.setEnabled(!isJpfFile);
    
    updateLaunchConfigurationDialog();
  }
  
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
    gl_comp2.marginHeight = 0;
    gl_comp2.marginWidth = 0;
    comp2.setLayout(gl_comp2);
    
    Group grpJpfExecution = new Group(comp2, SWT.NONE);
    grpJpfExecution.setText("JPF Execution");
    grpJpfExecution.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
    grpJpfExecution.setLayout(new GridLayout(5, false));
        
        radioJpfFileSelected = new Button(grpJpfExecution, SWT.RADIO);
        radioJpfFileSelected.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            runJpfSelected(radioJpfFileSelected.getSelection());
          }
          
        });
        radioJpfFileSelected.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
        radioJpfFileSelected.setText("Run a .jpf file:");
        new Label(grpJpfExecution, SWT.NONE);
                    new Label(grpJpfExecution, SWT.NONE);
                    new Label(grpJpfExecution, SWT.NONE);
                        
                        Label lblFile = new Label(grpJpfExecution, SWT.NONE);
                        lblFile.setText("File:");
                        
                            jpfFileLocationText = new Text(grpJpfExecution, SWT.BORDER);
                            jpfFileLocationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
                            jpfFileLocationText.addModifyListener(updatedListener);
                            jpfFileLocationText.setBounds(10, 35, 524, 21);
                    new Label(grpJpfExecution, SWT.NONE);
                    new Label(grpJpfExecution, SWT.NONE);
                    
                    btnAppendDynamicProperties = new Button(grpJpfExecution, SWT.NONE);
                    btnAppendDynamicProperties.addSelectionListener(new SelectionAdapter() {
                      @Override
                      public void widgetSelected(SelectionEvent selectionEvent) {
                        ILaunchConfiguration configuration = getCurrentLaunchConfiguration();
                        String fileString = jpfFileLocationText.getText();
                        FileWriter fileWrite = null;
                        try {
                          @SuppressWarnings("unchecked")
                          Map<String, String> config = configuration.getAttribute(JPFSettingsTab.ATTR_JPF_DYNAMICCONFIG, Collections.<String, String>emptyMap());
                          
                          fileWrite = new FileWriter(fileString, true);
                          
                          fileWrite.append(FLUSHED_COMMENT);
                          for (String key : config.keySet()) {
                            fileWrite.append(new StringBuilder(key).append(" = ").append(config.get(key)).append(System.lineSeparator()));
                          }
                          fileWrite.close();
                        } catch (CoreException | IOException e) {
                          setErrorMessage("Cannot store dynamic properties in the file: '" + fileString +"'");
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
                    btnAppendDynamicProperties.setText("&Append dynamic properties into this file");
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
                        
                        btnBrowseWorkspace = new Button(composite_1, SWT.NONE);
                        btnBrowseWorkspace.setSize(109, 25);
                        btnBrowseWorkspace.setText("Browse &workspace");
                        
                            btnbrowseFilesystem = new Button(composite_1, SWT.NONE);
                            btnbrowseFilesystem.setText("Browse &filesystem");
                            btnbrowseFilesystem.setBounds(540, 33, 106, 25);
                            

                            btnbrowseFilesystem.addSelectionListener(new SelectionAdapter() {
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
                                    jpfFileLocationText.setText(file);
                                    setDirty(true);
                                  }
                                }
                              }
                            });
                            btnBrowseWorkspace.addSelectionListener(new SelectionAdapter() {
                                public void widgetSelected(SelectionEvent e) {
                                	FilteredFileSelectionDialog dialog = new FilteredFileSelectionDialog("JPF File Selection","Choose a .jpf file", new String[] {"jpf"}); //$NON-NLS-1$

                                  int buttonId = dialog.open();
                                  if(buttonId == IDialogConstants.OK_ID) {
                                    Object[] resource = dialog.getResult();
                                    if(resource != null && resource.length > 0) {
                                    	if (resource[0] instanceof IFile) {
                                    	  String filePath = ((IFile) resource[0]).getLocation().toString();
//                String fileLoc = VariablesPlugin.getDefault().getStringVariableManager()
//                    .generateVariableExpression("workspace_loc", fileWorkspacePath); //$NON-NLS-1$
                                        jpfFileLocationText.setText(filePath);
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
                    
                        targetText = new Text(grpJpfExecution, SWT.BORDER);
                        targetText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
                        targetText.addModifyListener(new ModifyListener() {
                          public void modifyText(ModifyEvent e) {
                            updateLaunchConfigurationDialog();
                          }
                        });
                new Label(grpJpfExecution, SWT.NONE);
                                        new Label(grpJpfExecution, SWT.NONE);
                                        new Label(grpJpfExecution, SWT.NONE);
                                        new Label(grpJpfExecution, SWT.NONE);
                                        
                                        Composite composite_2 = new Composite(grpJpfExecution, SWT.NONE);
                                        composite_2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
                                        GridLayout gl_composite_2 = new GridLayout(1, false);
                                        gl_composite_2.marginHeight = 0;
                                        gl_composite_2.marginWidth = 0;
                                        composite_2.setLayout(gl_composite_2);
                                        
                                            btnSearch = new Button(composite_2, SWT.NONE);
                                            btnSearch.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
                                            btnSearch.setText("Search...");
                                            btnSearch.addSelectionListener(new SelectionListener() {
                                              public void widgetDefaultSelected(SelectionEvent e) {
                                              }

                                              public void widgetSelected(SelectionEvent e) {
                                                targetType = handleSearchMainClassButtonSelected(targetText, targetType);
                                              }
                                            });

    Group grpOverrideCommonJpf = new Group(comp, SWT.NONE);
    grpOverrideCommonJpf.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    grpOverrideCommonJpf.setText("Common JPF settings");
    grpOverrideCommonJpf.setLayout(new GridLayout(5, false));
        
            Label lblListener = new Label(grpOverrideCommonJpf, SWT.NONE);
            lblListener.setText("Listener:");
        
            listenerText = new Text(grpOverrideCommonJpf, SWT.BORDER);
            listenerText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
            listenerText.addModifyListener(new ModifyListener() {
              public void modifyText(ModifyEvent e) {
                updateLaunchConfigurationDialog();
              }
            });

    Button searchListenerButton = new Button(grpOverrideCommonJpf, SWT.NONE);
    searchListenerButton.setText("Search...");
    searchListenerButton.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent e) {
      }

      public void widgetSelected(SelectionEvent e) {
        listenerType = handleSupertypeSearchButtonSelected("gov.nasa.jpf.JPFListener", listenerText, listenerType);
        
      }
    });

    Label lblSearch = new Label(grpOverrideCommonJpf, SWT.NONE);
    lblSearch.setText("Search:");
        
            searchText = new Text(grpOverrideCommonJpf, SWT.BORDER);
            searchText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
            searchText.addModifyListener(new ModifyListener() {
              public void modifyText(ModifyEvent e) {
                updateLaunchConfigurationDialog();
              }
            });

    Button searchSearchButton = new Button(grpOverrideCommonJpf, SWT.NONE);
    searchSearchButton.setText("Search...");
    
    checkShellEnabled = new Button(grpOverrideCommonJpf, SWT.CHECK);
    checkShellEnabled.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    checkShellEnabled.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        boolean isShellEnabled = checkShellEnabled.getSelection();
        textShellPort.setEnabled(isShellEnabled);
        updateLaunchConfigurationDialog();
      }
    });
    checkShellEnabled.setText("Enable shell on port:");
    
    textShellPort = new Text(grpOverrideCommonJpf, SWT.BORDER);
    textShellPort.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    new Label(grpOverrideCommonJpf, SWT.NONE);
    new Label(grpOverrideCommonJpf, SWT.NONE);
    searchSearchButton.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent e) {
      }

      public void widgetSelected(SelectionEvent e) {
        searchType = handleSupertypeSearchButtonSelected("gov.nasa.jpf.search.Search", searchText, searchType);
      }
    });
    
    Group grpTrace = new Group(comp2, SWT.NONE);
    grpTrace.setLayout(new GridLayout(3, false));
    grpTrace.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    grpTrace.setText("Trace");
    
    Composite composite = new Composite(grpTrace, SWT.NONE);
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
    
    lblTraceFile = new Label(grpTrace, SWT.CHECK);
    lblTraceFile.setText("Trace File:");
    
    textTraceFile = new Text(grpTrace, SWT.BORDER);
    textTraceFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    new Label(grpTrace, SWT.NONE);
    new Label(grpTrace, SWT.NONE);
    
    Composite composite_3 = new Composite(grpTrace, SWT.NONE);
    GridLayout gl_composite_3 = new GridLayout(2, false);
    gl_composite_3.marginHeight = 0;
    gl_composite_3.marginWidth = 0;
    composite_3.setLayout(gl_composite_3);
    composite_3.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 1, 1));
    
    buttonTraceBrowseWorkspace = new Button(composite_3, SWT.NONE);
    buttonTraceBrowseWorkspace.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
          FilteredFileSelectionDialog dialog = new FilteredFileSelectionDialog("Trace File Selection","Choose a file", null /* no filtering */); //$NON-NLS-1$

          int buttonId = dialog.open();
          if(buttonId == IDialogConstants.OK_ID) {
            Object[] resource = dialog.getResult();
            if(resource != null && resource.length > 0) {
              if (resource[0] instanceof IFile) {
                String filePath = ((IFile) resource[0]).getLocation().toString();
//String fileLoc = VariablesPlugin.getDefault().getStringVariableManager()
//.generateVariableExpression("workspace_loc", fileWorkspacePath); //$NON-NLS-1$
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
        lblTraceFile.setEnabled(traceEnabled);
        textTraceFile.setEnabled(traceEnabled);
        updateLaunchConfigurationDialog();
      }
    });

    runtime(comp2);
  }
  
  protected void runtimeAppend(Composite parent) {
    // empty implementation for subclasses;
  }
  
  protected void runtimePrepend(Composite parent) {
 // empty implementation for subclasses;
  }
  private void runtime(Composite parent) {
    runtimePrepend(parent);
    
    groupRuntime = new Group(parent, SWT.NONE);
    groupRuntime.setLayout(new GridLayout(3, false));
    groupRuntime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    groupRuntime.setText("Runtime");
    
    lblJpf = new Label(groupRuntime, SWT.NONE);
    lblJpf.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    lblJpf.setText("JPF:");
    
    
    jpfCombo = SWTFactory.createCombo(groupRuntime, SWT.DROP_DOWN | SWT.READ_ONLY, 1, null);
    
    btnJpfReset = new Button(groupRuntime, SWT.NONE);
    btnJpfReset.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        jpfInstallations.reset(REQUIRED_LIBRARY);
        try {
          initializeExtensionInstallations(getCurrentLaunchConfiguration(), jpfInstallations, jpfCombo, JPF_ATTR_RUNTIME_JPF_INSTALLATIONINDEX, EXTENSION_PROJECT);
        } catch (CoreException e1) {
          // we don't care
        }
        updateLaunchConfigurationDialog();
      }
    });
    btnJpfReset.setText("Reset");
    //ControlAccessibleListener.addListener(fCombo, fSpecificButton.getText());
    jpfCombo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateLaunchConfigurationDialog();
//        setStatus(OK_STATUS);
//        firePropertyChange();
      }
    });
    
    runtimeAppend(groupRuntime);
  }
  private static String defaultProperty(Map<String,String> map, String property, String defValue) {
    if (map == null || !map.containsKey(property)) {
      return defValue;
    }
    return (String) map.get(property);
  }
  
  public static void initDefaultConfiguration(ILaunchConfigurationWorkingCopy configuration, String projectName, IFile jpfFile) {

    configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, EclipseJPF.JPF_MAIN_CLASS);
    configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
   
    if (jpfFile != null) {
      String jpfFileAbsolutePath = jpfFile.getLocation().toFile().getAbsolutePath();
      configuration.setAttribute(JPF_FILE_LOCATION, jpfFileAbsolutePath);
    } else {
      configuration.setAttribute(JPF_FILE_LOCATION, "");
    }
    
    configuration.setAttribute(JPF_ATTR_TRACE_STORE_INSTEADOF_REPLAY, false);
    configuration.setAttribute(JPF_ATTR_TRACE_ENABLED, false);
    
    // TODO it's better to not use the embedded one if normal extension is detected
    configuration.setAttribute(JPF_ATTR_RUNTIME_JPF_INSTALLATIONINDEX, -1);
    
    // TODO get the configuration from the JPF
    // listener, target .. and other stuff
//    Config config = new Config(new String[] {jpfFileAbsolutePath});
    
    try {
      @SuppressWarnings("unchecked")
      Map<String, String> map = configuration.getAttribute(JPFSettingsTab.ATTR_JPF_APPCONFIG, Collections.EMPTY_MAP);
    
    
    configuration.setAttribute(JPFCommonTab.JPF_ATTR_OPT_LISTENER, defaultProperty(map, "listener", ""));
    configuration.setAttribute(JPFCommonTab.JPF_ATTR_OPT_SEARCH, defaultProperty(map, "search.class", ""));
    configuration.setAttribute(JPFCommonTab.JPF_ATTR_OPT_TARGET, defaultProperty(map, "target", ""));
    
    
    
    } catch (CoreException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
   
  }

  protected void setText(ILaunchConfiguration configuration, Text text, String attributeName) throws CoreException {
    text.setText(configuration.getAttribute(attributeName, ""));
  }
  

  public void initializeFrom(ILaunchConfiguration configuration) {

    try {
      jpfFileLocationText.setText(configuration.getAttribute(JPF_FILE_LOCATION, ""));
      setText(configuration, listenerText, JPFCommonTab.JPF_ATTR_OPT_LISTENER);
      setText(configuration, searchText, JPFCommonTab.JPF_ATTR_OPT_SEARCH);
      setText(configuration, targetText, JPFCommonTab.JPF_ATTR_OPT_TARGET);
      
//      boolean override = configuration.getAttribute(JPF_OPT_OVERRIDE_INSTEADOFADD, false);
//      radioOverride.setSelection(override);
//      radioAppend.setSelection(!override);
//      
      
      checkShellEnabled.setSelection(configuration.getAttribute(JPF_ATTR_SHELL_ENABLED, true));
      int defaultShellPort = Platform.getPreferencesService().getInt(EclipseJPF.BUNDLE_SYMBOLIC, EclipseJPFLauncher.PORT, EclipseJPFLauncher.DEFAULT_PORT, null);
      textShellPort.setText(String.valueOf(configuration.getAttribute(JPF_ATTR_SHELL_PORT, defaultShellPort)));
      textShellPort.setEnabled(configuration.getAttribute(JPF_ATTR_SHELL_ENABLED, true));
      
      boolean traceEnabled = configuration.getAttribute(JPF_ATTR_TRACE_ENABLED, false);
      
      radioTraceNoTrace.setSelection(!traceEnabled);
      radioTraceReplay.setSelection(traceEnabled && !configuration.getAttribute(JPF_ATTR_TRACE_STORE_INSTEADOF_REPLAY, false));
      radioTraceStore.setSelection(traceEnabled && configuration.getAttribute(JPF_ATTR_TRACE_STORE_INSTEADOF_REPLAY, false));
      textTraceFile.setEnabled(traceEnabled);
      buttonTraceBrowse.setEnabled(traceEnabled);
      buttonTraceBrowseWorkspace.setEnabled(traceEnabled);
      textTraceFile.setText(configuration.getAttribute(JPF_ATTR_TRACE_FILE, createPlaceholderedTraceFile()));
      
      boolean jpfFileSelected = configuration.getAttribute(JPF_ATTR_RUNTIME_JPFFILESELECTED, true);
      runJpfSelected(jpfFileSelected);
      radioMainMethodClass.setSelection(!jpfFileSelected);
      radioJpfFileSelected.setSelection(jpfFileSelected);
      
      initializeExtensionInstallations(configuration, jpfInstallations, jpfCombo, JPF_ATTR_RUNTIME_JPF_INSTALLATIONINDEX, EXTENSION_PROJECT);
      
    } catch (CoreException e) {
      EclipseJPF.logError("Error during the JPF initialization form", e);
    }

    super.initializeFrom(configuration);
  }

  String getJpfFileLocation() {
    return jpfFileLocationText.getText().trim();
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
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
//  private String removeListener(String originalListener, String removalListener) {
//    if (originalListener == null) {
//      return null;
//    }
//    if (originalListener.contains(removalListener)) {
//      // TODO this is completely wrong!!!
//      int startIndex = originalListener.indexOf(removalListener);
//      String result = originalListener.substring(0, startIndex);
//      int advancedIndex = startIndex + removalListener.length() + 1;
//      if (originalListener.length() >= advancedIndex) {
//        String append = originalListener.substring(startIndex + removalListener.length() + 1);
//        
//        if (result.endsWith(",") && append.startsWith(",")) {
//          result += append.substring(1);
//        } else {
//          result += append;
//        }
//      } else {
//        if (result.endsWith(",")) {
//          result = result.substring(0, result.length() - 1);
//        }
//      }
//      return result;
//    }
//    return originalListener;
//  }
  
  public boolean attributeEquals(String attributeName, ILaunchConfiguration configuration, String valueCandidate) throws CoreException {
    if (valueCandidate == null) {
      return false;
    }
    String currentValue = configuration.getAttribute(attributeName, "");
    return currentValue.trim().equals(valueCandidate.trim());
  }
  
  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    IProject implicitProject = null;
    
    for (IType type : new IType[] {searchType, listenerType, targetType}) {
      if (type == null || type.getJavaProject() == null) {
        continue;
      }
      implicitProject = type.getJavaProject().getProject();
    }
    
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    URI jpfFileUri = new File(jpfFileLocationText.getText()).toURI();
    
    IFile[] iFiles = root.findFilesForLocationURI(jpfFileUri);
    for (int i = iFiles.length - 1; i >= 0; --i) {
      if (iFiles[i].getProject() == null) {
        continue;
      }
      implicitProject = iFiles[i].getProject();
    }
    
    configuration.setAttribute(JPF_ATTR_SHELL_ENABLED, checkShellEnabled.getSelection());
    // port is already validated
    int portShell = Integer.parseInt(textShellPort.getText());
    configuration.setAttribute(JPF_ATTR_SHELL_PORT, portShell);
    
    try {
      if (!attributeEquals(JPF_FILE_LOCATION, configuration, jpfFileLocationText.getText())) {
        configuration.setAttribute(JPF_FILE_LOCATION, jpfFileLocationText.getText());
        
        // reload app config
        try {
          Config appConfig = new Config(jpfFileLocationText.getText());
          configuration.setAttribute(JPFSettingsTab.ATTR_JPF_APPCONFIG, appConfig);
        } catch (JPFConfigException e) {
          configuration.setAttribute(JPFSettingsTab.ATTR_JPF_APPCONFIG, Collections.<String, String>emptyMap());
        }
        
      }
      
      
      configuration.setAttribute(JPF_ATTR_TRACE_ENABLED, !radioTraceNoTrace.getSelection());
      configuration.setAttribute(JPF_ATTR_TRACE_STORE_INSTEADOF_REPLAY, radioTraceStore.getSelection());

      configuration.setAttribute(JPF_ATTR_OPT_LISTENER, listenerText.getText().trim());
      configuration.setAttribute(JPF_ATTR_OPT_SEARCH, searchText.getText().trim());
      configuration.setAttribute(JPF_ATTR_OPT_TARGET, targetText.getText().trim());
      
      int selectedJpfInstallation = jpfCombo.getSelectionIndex();
      configuration.setAttribute(JPF_ATTR_RUNTIME_JPF_INSTALLATIONINDEX, selectedJpfInstallation);
      
      if (selectedJpfInstallation == ExtensionInstallations.EMBEDDED_INSTALLATION_INDEX) {
        // using embedded JPF
        
        // using embedded jdwp
        String classpath = jpfInstallations.getEmbedded().classpath(File.pathSeparator);
        configuration.setAttribute(JPF_ATTR_RUNTIME_JPF_EMBEDDEDCLASSPATH, classpath);
      } else {
        // clear it
        configuration.removeAttribute(JPF_ATTR_RUNTIME_JPF_EMBEDDEDCLASSPATH);
      }
      
      configuration.setAttribute(JPF_ATTR_RUNTIME_JPFFILESELECTED, radioJpfFileSelected.getSelection());
    
      @SuppressWarnings("unchecked")
      Map<String, String> map = configuration.getAttribute(JPFSettingsTab.ATTR_JPF_DYNAMICCONFIG, Collections.EMPTY_MAP);
    
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
          
          // let's substitute the generated random string into the unique placeholder
          if (traceFileName.contains(JPFCommonTab.UNIQUE_ID_PLACEHOLDER)) {
            String uniqueId = UUID.randomUUID().toString();
            traceFileName = traceFileName.replace(JPFCommonTab.UNIQUE_ID_PLACEHOLDER, uniqueId);
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
      
      if (checkShellEnabled.getSelection()) {
        map.put("shell.port", String.valueOf(portShell));
      }
      
      if (isDynamic(configuration, "listener", listenerText.getText())) {
        listenerString = addListener(listenerString, listenerText.getText().trim());
      }
      if (!Objects.equals("", listenerString)) {
        map.put("listener", listenerString);
      }
      putIfDynamicAndNotEmpty(configuration, map, "search.class", searchText.getText());
      putIfDynamicAndNotEmpty(configuration, map, "target", targetText.getText());
        
    } catch (CoreException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
//    configuration.setAttribute(JPF_OPT_TARGET, targetText.getText());
//    configuration.setAttribute(JPF_OPT_SEARCH, searchText.getText());
//    configuration.setAttribute(JPF_OPT_LISTENER, listenerText.getText());
    
//    configuration.setAttribute(JPF_OPT_OVERRIDE_INSTEADOFADD, radioOverride.getSelection() && !radioAppend.getSelection());
    
    if (implicitProject != null) {
      configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, implicitProject.getName());
    }
  }

  private boolean isDynamic(ILaunchConfigurationWorkingCopy configuration, String key, String value) throws CoreException {
    if (value == null) {
      return false;
    }
    
    // TODO look at other configs too
    @SuppressWarnings("unchecked")
    Map<String, String> appMap = configuration.getAttribute(JPFSettingsTab.ATTR_JPF_APPCONFIG, Collections.EMPTY_MAP);
    
    String appValue = (String) appMap.get(key);
    if (appValue != null && appValue.trim().equals(value.trim())) {
        return false;
    }
    return true;
  }
  private void putIfDynamicAndNotEmpty(ILaunchConfigurationWorkingCopy configuration, Map<String, String> map, String key, String value) throws CoreException {
    if (!Objects.equals("", value) && isDynamic(configuration, key, value)) {
      map.put(key, value);
    }
  }
  
  private static boolean testFileExists(String filename) {
    File file = new File(filename);
    return file.isFile();
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
   */
  @Override
  public boolean isValid(ILaunchConfiguration config) {
    String jpfFileString = jpfFileLocationText.getText();
    String targetString = targetText.getText();
    if (Objects.equals("", jpfFileString) && Objects.equals("", targetString)) {
      setErrorMessage("Either JPF File (*.jpf) or Target class has to be specified!");
      return false;
    }
    if (!"".equals(jpfFileString) && !jpfFileString.toLowerCase().endsWith(".jpf")) {
      // this is here because we provide just .jpf files in the workspace browser and as such we want to be consistent
      setErrorMessage("JPF File (*.jpf) must end with .jpf extension!");
      return false;
    }
    if (radioJpfFileSelected.getSelection() && !testFileExists(jpfFileString)) {
      setErrorMessage("Provided JPF file: '" + jpfFileString + "' doesn't exist!");
      return false;
    }
    String traceFileString = textTraceFile.getText();
    if (radioTraceReplay.getSelection() && !testFileExists(traceFileString)) {
      setErrorMessage("Provided trace file: '" + traceFileString + "' doesn't exist!");
      return false;
    }
    if (checkShellEnabled.getSelection()) {
      int port;
      try {
        port = Integer.parseInt(textShellPort.getText());
      } catch (NumberFormatException e) {
        setErrorMessage("Provided port number cannot be converted to integer: " + e.getMessage());
        return false;
      }
      if (port < 0) {
        // let's do not care about the upper bound cause I don't know if there are platforms that support different number than the normal one
        setErrorMessage("Provided port is invalid");
        return false;
      }
    }

    if (jpfCombo.getSelectionIndex() == ExtensionInstallations.EMBEDDED_INSTALLATION_INDEX) {
      // selected embedded
      
      if (!jpfInstallations.get(ExtensionInstallations.EMBEDDED_INSTALLATION_INDEX).isValid()) {
        setErrorMessage("Embedded JPF installation in error due to: " + jpfInstallations.get(ExtensionInstallations.EMBEDDED_INSTALLATION_INDEX).toString());
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
    return super.isValid(config);
  }
  
  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    initDefaultConfiguration(configuration, null, null);
  }
}
