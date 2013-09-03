package gov.nasa.runjpf.tab;

import gov.nasa.jpf.Config;
import gov.nasa.runjpf.EclipseJPF;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import junit.framework.Assert;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.junit.Test;

public class JPFCommonTab extends AbstractJPFTab {

  private static final String ATTRIBUTE_UNIQUE_PREFIX = "asdlkjasdfkj";
  
  public static final String JPF_ATTR_TRACE_STORE_INSTEADOF_REPLAY = ATTRIBUTE_UNIQUE_PREFIX + "JPF_TRACE_STORE";
  public static final String JPF_ATTR_TRACE_FILE = ATTRIBUTE_UNIQUE_PREFIX + "JPF_TRACE_FILE";
  public static final String JPF_ATTR_TRACE_ENABLED = ATTRIBUTE_UNIQUE_PREFIX + "JPF_TRACE_ENABLED";
  public static final String JPF_ATTR_TRACE_CUSTOMFILECHECKED = ATTRIBUTE_UNIQUE_PREFIX + "JPF_TRACE_CUSTOMFILE";
  public static final String JPF_ATTR_OPT_LISTENER = ATTRIBUTE_UNIQUE_PREFIX + "JPF_OPT_LISTENER";
  public static final String JPF_ATTR_OPT_SEARCH = ATTRIBUTE_UNIQUE_PREFIX + "JPF_OPT_SEARCH";
  public static final String JPF_ATTR_OPT_TARGET = ATTRIBUTE_UNIQUE_PREFIX + "JPF_OPT_TARGET";

  private Text jpfFileLocationText;

  private Text listenerText;
  private Text searchText;
  private Text targetText;
  private IType listenerType;
  private IType searchType;
  private IType targetType;
  private Button radioAppend;
  private Button radioOverride;
  private Text textTraceFile;

  private Button radioTraceStore;

  private Button radioTraceReplay;

  // TODO should use a string only - not to create the whole file
  private String lastTmpTraceFile;
  private String lastUserTraceFile = "";

  private Button radioTraceNoTrace;

  private Button checkTraceFile;

  private Button buttonTraceBrowse;
  
  private static final String TEMP_DIR_PATH;
  
  static {
    String tmpDirString;
    try {
      java.nio.file.Path tmpPath = Files.createTempFile("tmpdirlookup", ".tmp");
      File tmpDir = tmpPath.getParent().toFile();
      
      tmpDirString = tmpDir.getAbsolutePath();
      
    } catch (IOException e) {
      
      tmpDirString = System.getProperty("java.io.tmpdir");
      if (tmpDirString == null) {
        File[] roots = File.listRoots();
        if (roots != null && roots.length > 0) {
          tmpDirString = roots[0].getAbsolutePath();
        } else {
          throw new IllegalStateException("Unable to determine any directory as a temporary direcotyr");
        }
      }
    }
    TEMP_DIR_PATH = tmpDirString;
  }
  
  public JPFCommonTab() {
    lastTmpTraceFile = TEMP_DIR_PATH + File.separatorChar + "trace-{UNIQUE_ID}.txt";
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

    Group basicConfiguraionGroup = new Group(comp, SWT.NONE);
    basicConfiguraionGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    basicConfiguraionGroup.setText("JPF &File to execute (*.jpf):");
    basicConfiguraionGroup.setLayout(new GridLayout(4, false));
    basicConfiguraionGroup.setFont(comp.getFont());

    jpfFileLocationText = new Text(basicConfiguraionGroup, SWT.BORDER);
    jpfFileLocationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 3, 1));
    jpfFileLocationText.addModifyListener(updatedListener);
    jpfFileLocationText.setBounds(10, 35, 524, 21);

    Button button = new Button(basicConfiguraionGroup, SWT.NONE);
    button.setText("&Browse...");
    button.setBounds(540, 33, 71, 25);
    
    Button btnReload = new Button(basicConfiguraionGroup, SWT.NONE);
    btnReload.setText("Reload");
    
    Button button_1 = new Button(basicConfiguraionGroup, SWT.NONE);
    button_1.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
      }
    });
    button_1.setText("Store");
    new Label(basicConfiguraionGroup, SWT.NONE);
    new Label(basicConfiguraionGroup, SWT.NONE);

    button.addSelectionListener(new SelectionAdapter() {
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

    Group grpOverrideCommonJpf = new Group(comp, SWT.NONE);
    grpOverrideCommonJpf.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    grpOverrideCommonJpf.setText("Common JPF settings");
    grpOverrideCommonJpf.setLayout(new GridLayout(3, false));

    Label lblTarget = new Label(grpOverrideCommonJpf, SWT.NONE);
    lblTarget.setText("Target:");

    targetText = new Text(grpOverrideCommonJpf, SWT.BORDER);
    targetText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    targetText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateLaunchConfigurationDialog();
      }
    });

    Button btnSearch = new Button(grpOverrideCommonJpf, SWT.NONE);
    btnSearch.setText("Search...");
    btnSearch.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent e) {
      }

      public void widgetSelected(SelectionEvent e) {
        targetType = handleSearchMainClassButtonSelected(targetText, targetType);
      }
    });

    Label lblListener = new Label(grpOverrideCommonJpf, SWT.NONE);
    lblListener.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    lblListener.setText("Listener:");

    listenerText = new Text(grpOverrideCommonJpf, SWT.BORDER);
    listenerText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
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
    searchText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    searchText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateLaunchConfigurationDialog();
      }
    });

    Button searchSearchButton = new Button(grpOverrideCommonJpf, SWT.NONE);
    searchSearchButton.setText("Search...");
    searchSearchButton.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent e) {
      }

      public void widgetSelected(SelectionEvent e) {
        searchType = handleSupertypeSearchButtonSelected("gov.nasa.jpf.search.Search", searchText, searchType);
      }
    });

    Group grpInteraction = new Group(grpOverrideCommonJpf, SWT.NONE);
    GridData gd_grpInteraction = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
    gd_grpInteraction.widthHint = 283;
    grpInteraction.setLayoutData(gd_grpInteraction);
    grpInteraction.setText("Interaction with settings from *.jpf file");
    grpInteraction.setLayout(new GridLayout(2, false));

    radioAppend = new Button(grpInteraction, SWT.RADIO);
    radioAppend.setText("Add");
    radioAppend.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        updateLaunchConfigurationDialog();
      }
    });

    radioOverride = new Button(grpInteraction, SWT.RADIO);
    radioOverride.setText("Override");
    
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
        if (checkTraceFile.getSelection()) {
          textTraceFile.setText(lastUserTraceFile);
        } else {
          textTraceFile.setText(lastTmpTraceFile);
        }
        textTraceFile.setEnabled(checkTraceFile.getSelection());
        buttonTraceBrowse.setEnabled(checkTraceFile.getSelection());
        updateLaunchConfigurationDialog();
      }
    });
    radioTraceStore.setBounds(0, 0, 90, 16);
    radioTraceStore.setText("Store");
    
    radioTraceReplay = new Button(composite, SWT.RADIO);
    radioTraceReplay.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (checkTraceFile.getSelection()) {
          textTraceFile.setText(lastUserTraceFile);
        } else {
          textTraceFile.setText(lastTmpTraceFile);
        }
        textTraceFile.setEnabled(checkTraceFile.getSelection());
        buttonTraceBrowse.setEnabled(checkTraceFile.getSelection());
        updateLaunchConfigurationDialog();
      }
    });
    radioTraceReplay.setText("Replay");
    
    checkTraceFile = new Button(grpTrace, SWT.CHECK);
    checkTraceFile.setText("Trace File:");
    
    textTraceFile = new Text(grpTrace, SWT.BORDER);
    textTraceFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    
    buttonTraceBrowse = new Button(grpTrace, SWT.NONE);
    buttonTraceBrowse.setText("Browse...");
    
    radioTraceNoTrace.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        boolean traceEnabled = !radioTraceNoTrace.getSelection();
        buttonTraceBrowse.setEnabled(traceEnabled);
        checkTraceFile.setEnabled(traceEnabled);
        textTraceFile.setEnabled(traceEnabled);
        updateLaunchConfigurationDialog();
      }
    });
    
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
            lastUserTraceFile = file;
            textTraceFile.setText(file);
          }
        }
      }
    });
    
    checkTraceFile.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        textTraceFile.setEnabled(checkTraceFile.getSelection());
        buttonTraceBrowse.setEnabled(checkTraceFile.getSelection());
        if (checkTraceFile.getSelection()) {
          textTraceFile.setText(lastUserTraceFile);
        } else {
          textTraceFile.setText(lastTmpTraceFile);
        }
        updateLaunchConfigurationDialog();
      }
    });
    
    
    radioOverride.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        updateLaunchConfigurationDialog();
      }
    });

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
    
    configuration.setAttribute(JPF_ATTR_TRACE_FILE, "");
    configuration.setAttribute(JPF_ATTR_TRACE_STORE_INSTEADOF_REPLAY, false);
    configuration.setAttribute(JPF_ATTR_TRACE_CUSTOMFILECHECKED, false);
    configuration.setAttribute(JPF_ATTR_TRACE_ENABLED, false);
    
    // TODO get the configuration from the JPF
    // listener, target .. and other stuff
//    Config config = new Config(new String[] {jpfFileAbsolutePath});
    
    try {
      @SuppressWarnings("unchecked")
      Map<String, String> map = configuration.getAttribute(JPFSettings.ATTR_JPF_APPCONFIG, new HashMap<String, String>());
    
    
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
      
      boolean override = configuration.getAttribute(JPF_OPT_OVERRIDE_INSTEADOFADD, false);
      radioOverride.setSelection(override);
      radioAppend.setSelection(!override);
      
      
      
      
      boolean traceEnabled = configuration.getAttribute(JPF_ATTR_TRACE_ENABLED, false);
      
      radioTraceNoTrace.setSelection(!traceEnabled);
      radioTraceReplay.setSelection(traceEnabled && !configuration.getAttribute(JPF_ATTR_TRACE_STORE_INSTEADOF_REPLAY, false));
      radioTraceStore.setSelection(traceEnabled && configuration.getAttribute(JPF_ATTR_TRACE_STORE_INSTEADOF_REPLAY, false));
      checkTraceFile.setSelection(configuration.getAttribute(JPF_ATTR_TRACE_CUSTOMFILECHECKED, false));
      checkTraceFile.setEnabled(traceEnabled);
      textTraceFile.setEnabled(traceEnabled && configuration.getAttribute(JPF_ATTR_TRACE_CUSTOMFILECHECKED, false));
      buttonTraceBrowse.setEnabled(traceEnabled && configuration.getAttribute(JPF_ATTR_TRACE_CUSTOMFILECHECKED, false));
      
      String defaultFileText;
      if (checkTraceFile.getSelection()) {
        defaultFileText = "";
      } else {
        // TODO this should be generated right here
        defaultFileText = lastTmpTraceFile;
      }
      textTraceFile.setText(configuration.getAttribute(JPF_ATTR_TRACE_FILE, defaultFileText));
      
      
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
  private String removeListener(String originalListener, String removalListener) {
    if (originalListener == null) {
      return null;
    }
    if (originalListener.contains(removalListener)) {
      // TODO this is completely wrong!!!
      int startIndex = originalListener.indexOf(removalListener);
      String result = originalListener.substring(0, startIndex);
      int advancedIndex = startIndex + removalListener.length() + 1;
      if (originalListener.length() >= advancedIndex) {
        String append = originalListener.substring(startIndex + removalListener.length() + 1);
        
        if (result.endsWith(",") && append.startsWith(",")) {
          result += append.substring(1);
        } else {
          result += append;
        }
      } else {
        if (result.endsWith(",")) {
          result = result.substring(0, result.length() - 1);
        }
      }
      return result;
    }
    return originalListener;
  }
  
  @Test
  public void testRemove() {
    Assert.assertEquals("foo,ooo", removeListener("foo,bar,ooo", "bar"));
    Assert.assertEquals("foo,bar", removeListener("foo,bar,ooo", "ooo"));
    Assert.assertEquals("bar,ooo", removeListener("foo,bar,ooo", "foo"));
    Assert.assertEquals("bar,ooo", removeListener("bar,ooo", "foo"));
  }
  
  @Test
  public void testAdd() {
    Assert.assertEquals("bar,ooo,foo", addListener("bar,ooo", "foo"));
    Assert.assertEquals("bar,ooo", addListener("bar,ooo", "bar"));
    Assert.assertEquals("bar,ooo", addListener("bar,ooo", "ooo"));
  }
  
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
    try {
      if (!attributeEquals(JPF_FILE_LOCATION, configuration, jpfFileLocationText.getText())) {
        configuration.setAttribute(JPF_FILE_LOCATION, jpfFileLocationText.getText());
        
        // reload app config
        Config appConfig = new Config(jpfFileLocationText.getText());
        configuration.setAttribute(JPFSettings.ATTR_JPF_APPCONFIG, appConfig);
      }
      
      
      configuration.setAttribute(JPF_ATTR_TRACE_ENABLED, !radioTraceNoTrace.getSelection());
      configuration.setAttribute(JPF_ATTR_TRACE_STORE_INSTEADOF_REPLAY, radioTraceStore.getSelection());
      configuration.setAttribute(JPF_ATTR_TRACE_FILE, textTraceFile.getText());
      configuration.setAttribute(JPF_ATTR_TRACE_CUSTOMFILECHECKED, checkTraceFile.getSelection());
      
      configuration.setAttribute(JPF_ATTR_OPT_LISTENER, listenerText.getText().trim());
      configuration.setAttribute(JPF_ATTR_OPT_SEARCH, searchText.getText().trim());
      configuration.setAttribute(JPF_ATTR_OPT_TARGET, targetText.getText().trim());
    
      @SuppressWarnings("unchecked")
      Map<String, String> map = configuration.getAttribute(JPFSettings.ATTR_JPF_DYNAMICCONFIG, new HashMap<>());
    
      String listenerString = "";
      map.remove("trace.file");
      map.remove("choice.use_trace");
      map.remove("listener");
      map.remove("search.class");
      map.remove("target");
      
      if (!radioTraceNoTrace.getSelection()) {
        // we're tracing
        if (radioTraceStore.getSelection()) {
          // we're storing a trace
          map.put("trace.file", textTraceFile.getText().trim());
          
          listenerString = addListener(listenerString, ".listener.TraceStore");
          
        } else if (radioTraceReplay.getSelection()) {
          map.put("choice.use_trace", textTraceFile.getText().trim());
          
          listenerString = addListener(listenerString, ".listener.ChoiceSelector");
        } else {
          throw new IllegalStateException("Shouldn't occur");
        }
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
    
    configuration.setAttribute(JPF_OPT_OVERRIDE_INSTEADOFADD, radioOverride.getSelection() && !radioAppend.getSelection());
    
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
    Map<String, String> appMap = configuration.getAttribute(JPFSettings.ATTR_JPF_APPCONFIG, new HashMap<>());
    
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
  
  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    initDefaultConfiguration(configuration, null, null);
  }
}
