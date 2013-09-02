package gov.nasa.runjpf.tab;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.util.IntSet;
import gov.nasa.runjpf.EclipseJPF;

import java.io.File;
import java.lang.ref.Reference;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaLaunchTab;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.launcher.DebugTypeSelectionDialog;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jdt.internal.debug.ui.launcher.MainMethodSearchEngine;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.window.Window;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.layout.RowLayout;
import swing2swt.layout.FlowLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.custom.TableCursor;

public class JPFSettings extends JavaLaunchTab {
  public static final String JPF_FILE_LOCATION = "JPF_FILE";
  public static final String JPF_DEBUG_BOTHVMS = "JPF_DEBUG_VM";
  public static final String JPF_DEBUG_JPF_INSTEADOFPROGRAM = "JPF_DEBUG_JPF_INSTEADOFPROGRAM";
  
  public static final String JPF_OPT_TARGET = "JPF_OPT_TARGET";
  public static final String JPF_OPT_SEARCH = "JPF_OPT_SEARCH";
  public static final String JPF_OPT_LISTENER = "JPF_OPT_LISTENER";
  public static final String JPF_OPT_OVERRIDE_INSTEADOFADD = "JPF_OPT_OVERRIDE_INSTEADOFADD";
  

  private Text jpfFileLocationText;

  /**
   * If it's modified , just update the configuration directly.
   */
  private class UpdateModfiyListener implements ModifyListener {
    public void modifyText(ModifyEvent e) {
      updateLaunchConfigurationDialog();
    }
  }

  private UpdateModfiyListener updatedListener = new UpdateModfiyListener();
  private Text listenerText;
  private Text searchText;
  private Text targetText;
  private IType listenerType;
  private IType searchType;
  private IType targetType;
  private Button radioAppend;
  private Button radioOverride;
  private Text text_1;
  private Table table;
  
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

    Group grpSettings = new Group(comp2, SWT.NONE);
    grpSettings.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    grpSettings.setText("Settings");
    grpSettings.setLayout(new GridLayout(1, false));
    
    Composite composite_2 = new Composite(grpSettings, SWT.NONE);
    RowLayout rl_composite_2 = new RowLayout(SWT.HORIZONTAL);
    rl_composite_2.spacing = 8;
    composite_2.setLayout(rl_composite_2);
    
    Button btnShowDefaultProperties = new Button(composite_2, SWT.CHECK);
    btnShowDefaultProperties.setText("Show default properties");
    
    Button btnShowSiteProperties = new Button(composite_2, SWT.CHECK);
    btnShowSiteProperties.setText("Show site properties");
    
    Button btnShowAppProperties = new Button(composite_2, SWT.CHECK);
    btnShowAppProperties.setText("Show app properties");
    
    Button btnShowDynamicProperties = new Button(composite_2, SWT.CHECK);
    btnShowDynamicProperties.setText("Show dynamic properties");
    
    Composite composite_1 = new Composite(grpSettings, SWT.NONE);
    composite_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    composite_1.setLayout(new GridLayout(2, false));
    composite_1.setBounds(0, 0, 64, 64);
    
    table = new Table(composite_1, SWT.BORDER | SWT.FULL_SELECTION);
    table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 3));
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
    
    TableColumn tblclmnProperty = new TableColumn(table, SWT.NONE);
    tblclmnProperty.setWidth(200);
    tblclmnProperty.setText("Property");
    
    TableColumn tblclmnValue = new TableColumn(table, SWT.NONE);
    tblclmnValue.setWidth(200);
    tblclmnValue.setText("Value");
    
    TableItem tableItem = new TableItem(table, SWT.NONE);
    tableItem.setText("EMPTY TABLE FOO");
    
    TableItem tableItem_1 = new TableItem(table, SWT.NONE);
    tableItem_1.setText("New TableItem");
    
    TableColumn tblclmnPropertyLocation = new TableColumn(table, SWT.NONE);
    tblclmnPropertyLocation.setWidth(100);
    tblclmnPropertyLocation.setText("Property location");
    
    Button btnAdd = new Button(composite_1, SWT.NONE);
    btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnAdd.setText("Add");
    
    Button btnNewButton = new Button(composite_1, SWT.NONE);
    btnNewButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnNewButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
      }
    });
    btnNewButton.setText("Delete");
    
    Button btnEdit = new Button(composite_1, SWT.NONE);
    btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnEdit.setText("Edit");
    radioOverride.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        updateLaunchConfigurationDialog();
      }
    });

  }

  abstract private class InlineSearcher {
    abstract IType[] search() throws InvocationTargetException, InterruptedException;
  }

  protected IType handleSupertypeSearchButtonSelected(final String supertype, Text text, IType originalType) {
    return handleSearchButtonSelected(new InlineSearcher() {
      @Override
      IType[] search() throws InvocationTargetException, InterruptedException {
        ClassSearchEngine engine = new ClassSearchEngine();
        return engine.searchClasses(getLaunchConfigurationDialog(), simpleSearchScope(), true, supertype);
      }
    }, text, originalType);
  }

  protected IType handleSearchMainClassButtonSelected(Text text, IType originalType) {
    return handleSearchButtonSelected(new InlineSearcher() {
      @Override
      IType[] search() throws InvocationTargetException, InterruptedException {
        MainMethodSearchEngine engine = new MainMethodSearchEngine();
        return engine.searchMainMethods(getLaunchConfigurationDialog(), simpleSearchScope(), true);
      }
    }, text, originalType);
  }

  protected IJavaSearchScope simpleSearchScope() {
    IJavaElement[] elements = null;
    IJavaModel model = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
    if (model != null) {
      try {
        elements = model.getJavaProjects();
      } catch (JavaModelException e) {
        JDIDebugUIPlugin.log(e);
      }
    }

    if (elements == null) {
      elements = new IJavaElement[] {};
    }

    return SearchEngine.createJavaSearchScope(elements, 1);
  }

  /**
   * Show a dialog that lists all main types
   */
  protected IType handleSearchButtonSelected(InlineSearcher searcher, Text text, IType originalType) {

    IType[] types = null;
    try {
      types = searcher.search();

    } catch (InvocationTargetException e) {
      setErrorMessage(e.getMessage());
      return null;
    } catch (InterruptedException e) {
      setErrorMessage(e.getMessage());
      return null;
    }
    DebugTypeSelectionDialog mmsd = new DebugTypeSelectionDialog(getShell(), types, LauncherMessages.JavaMainTab_Choose_Main_Type_11);
    if (mmsd.open() == Window.CANCEL) {
      return null;
    }
    Object[] results = mmsd.getResult();
    IType type = (IType) results[0];
    if (type != null) {
      text.setText(type.getFullyQualifiedName());
      return type;
    }
    return originalType;
  }

  public static void initDefaultConfiguration(ILaunchConfigurationWorkingCopy configuration, String projectName, String launchConfigName, IFile jpfFile) {

    configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, EclipseJPF.JPF_MAIN_CLASS);
    configuration.setAttribute(JPF_FILE_LOCATION, "");
    configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
   
    String jpfFileAbsolutePath = jpfFile.getLocation().toFile().getAbsolutePath();
    configuration.setAttribute(JPFSettings.JPF_FILE_LOCATION, jpfFileAbsolutePath);
    
    // TODO get the configuration from the JPF
    // listener, target .. and other stuff
    Config config = new Config(new String[] {jpfFileAbsolutePath});
    
    configuration.setAttribute(JPFSettings.JPF_OPT_LISTENER, config.getProperty("listener", ""));
    configuration.setAttribute(JPFSettings.JPF_OPT_SEARCH, config.getProperty("search.class", ""));
    configuration.setAttribute(JPFSettings.JPF_OPT_TARGET, config.getProperty("target", ""));
   
  }

  protected void setText(ILaunchConfiguration configuration, Text text, String attribute) throws CoreException {
    text.setText(configuration.getAttribute(attribute, ""));
  }
  
  public void initializeFrom(ILaunchConfiguration configuration) {

    try {
      jpfFileLocationText.setText(configuration.getAttribute(JPF_FILE_LOCATION, ""));
      setText(configuration, listenerText, JPF_OPT_LISTENER);
      setText(configuration, searchText, JPF_OPT_SEARCH);
      setText(configuration, targetText, JPF_OPT_TARGET);
      
      boolean override = configuration.getAttribute(JPF_OPT_OVERRIDE_INSTEADOFADD, false);
      radioOverride.setSelection(override);
      radioAppend.setSelection(!override);
      
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
    
    configuration.setAttribute(JPF_FILE_LOCATION, jpfFileLocationText.getText());
    configuration.setAttribute(JPF_OPT_TARGET, targetText.getText());
    configuration.setAttribute(JPF_OPT_SEARCH, searchText.getText());
    configuration.setAttribute(JPF_OPT_LISTENER, listenerText.getText());
    configuration.setAttribute(JPF_OPT_OVERRIDE_INSTEADOFADD, radioOverride.getSelection() && !radioAppend.getSelection());
    
    if (implicitProject != null) {
      configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, implicitProject.getName());
    }
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy arg0) {
    System.out.println("DEFAULTS");
  }
}
