package gov.nasa.runjpf.tab;

import gov.nasa.runjpf.tab.HierarchicalConfig.HierarchicalProperty;
import gov.nasa.runjpf.tab.HierarchicalConfig.InnerHiararchicalConfig;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsMessages;
import org.eclipse.debug.ui.EnvironmentTab.EnvironmentVariableLabelProvider;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.launcher.DebugTypeSelectionDialog;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jdt.internal.debug.ui.launcher.MainMethodSearchEngine;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;

public class JPFSettings extends AbstractJPFTab {

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
  private Text text_1;
 // private Table table;
  private Table table_1;
  private TableViewer environmentTable;
  private Button envAddButton;
  private Button envSelectButton;
  private Button envEditButton;
  private Button envRemoveButton;
  
  /**
   * @wbp.parser.entryPoint
   */
  @Override
  public void createControl(Composite parent) {

    
    Composite mainComposite = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL);
    setControl(mainComposite);
//    Composite mainComposit = new Composite(parent, SWT.NONE);
//    mainComposit.setFont(parent.getFont());
//
//    GridData gd = new GridData(1);
//    gd.horizontalAlignment = SWT.FILL;
//    gd.grabExcessHorizontalSpace = true;
//    gd.horizontalSpan = GridData.FILL_BOTH;
//    mainComposit.setLayoutData(gd);
//
//    
//
//    GridLayout glMainComposit = new GridLayout(1, false);
//    glMainComposit.marginHeight = 0;
//    glMainComposit.marginWidth = 0;
//    mainComposit.setLayout(glMainComposit);
    
    
    
    createEnvironmentTable(mainComposite);
    createTableButtons(mainComposite);

//    Group grpSettings = new Group(mainComposit, SWT.NONE);
//    grpSettings.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
//    grpSettings.setText("Settings");
//    grpSettings.setLayout(new GridLayout(2, false));
    
//    Composite composite_2 = new Composite(grpSettings, SWT.NONE);
//    RowLayout rl_composite_2 = new RowLayout(SWT.HORIZONTAL);
//    rl_composite_2.spacing = 8;
//    composite_2.setLayout(rl_composite_2);
//    
//    Button btnShowDefaultProperties = new Button(composite_2, SWT.CHECK);
//    btnShowDefaultProperties.setText("Show default properties");
//    
//    Button btnShowSiteProperties = new Button(composite_2, SWT.CHECK);
//    btnShowSiteProperties.setText("Show site properties");
//    
//    Button btnShowAppProperties = new Button(composite_2, SWT.CHECK);
//    btnShowAppProperties.setText("Show app properties");
//    
//    Button btnShowDynamicProperties = new Button(composite_2, SWT.CHECK);
//    btnShowDynamicProperties.setText("Show dynamic properties");
//    
//    Composite composite_1 = new Composite(grpSettings, SWT.NONE);
//    composite_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
//    composite_1.setLayout(new GridLayout(2, false));
//    composite_1.setBounds(0, 0, 64, 64);
//    
//    Composite composite = new Composite(composite_1, SWT.V_SCROLL);
//    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 3));
//    composite.setLayout(new GridLayout(1, false));
//    
//    table = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
//    table.setHeaderVisible(true);
//    table.setLinesVisible(true);
//    
//    TableColumn tblclmnProperty = new TableColumn(table, SWT.NONE);
//    tblclmnProperty.setWidth(200);
//    tblclmnProperty.setText("Property");
//    
//    TableColumn tblclmnValue = new TableColumn(table, SWT.NONE);
//    tblclmnValue.setWidth(200);
//    tblclmnValue.setText("Value");
//    
//    TableItem tableItem = new TableItem(table, SWT.NONE);
//    tableItem.setText(new String[] {"foo","ar"});
//    
//    TableItem tableItem_1 = new TableItem(table, SWT.NONE);
//    tableItem_1.setText("New TableItem");
//    
//    TableColumn tblclmnPropertyLocation = new TableColumn(table, SWT.NONE);
//    tblclmnPropertyLocation.setWidth(100);
//    tblclmnPropertyLocation.setText("Property location");
//    
//    ScrolledComposite scrolledComposite = new ScrolledComposite(composite, SWT.BORDER | SWT.V_SCROLL);
//    scrolledComposite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 4));
//    scrolledComposite.setExpandVertical(true);
//    scrolledComposite.setAlwaysShowScrollBars(true);
//    scrolledComposite.setMinHeight(200);
//    scrolledComposite.setExpandHorizontal(true);
//    
//    table_1 = new Table(scrolledComposite, SWT.BORDER | SWT.FULL_SELECTION);
//    table_1.setHeaderVisible(true);
//    table_1.setLinesVisible(true);
//    
//    TableColumn tblclmnFoo = new TableColumn(table_1, SWT.NONE);
//    tblclmnFoo.setWidth(100);
//    tblclmnFoo.setText("foo");
//    
//    TableColumn tblclmnBar = new TableColumn(table_1, SWT.NONE);
//    tblclmnBar.setWidth(100);
//    tblclmnBar.setText("bar");
//    
//    TableItem tableItem_2 = new TableItem(table_1, SWT.NONE);
//    tableItem_2.setText("New TableItem");
//    
//    TableItem tableItem_3 = new TableItem(table_1, SWT.NONE);
//    tableItem_3.setText("New TableItem");
//    
//    TableItem tableItem_4 = new TableItem(table_1, SWT.NONE);
//    tableItem_4.setText("New TableItem");
//    scrolledComposite.setContent(table_1);
//    scrolledComposite.setMinSize(table_1.computeSize(SWT.DEFAULT, SWT.DEFAULT));
//    
//    Button btnAdd = new Button(composite_1, SWT.NONE);
//    btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
//    btnAdd.setText("Add");
//    
//    Button btnNewButton = new Button(composite_1, SWT.NONE);
//    btnNewButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
//    btnNewButton.addSelectionListener(new SelectionAdapter() {
//      @Override
//      public void widgetSelected(SelectionEvent e) {
//      }
//    });
//    btnNewButton.setText("Delete");
//    
//    Button btnEdit = new Button(composite_1, SWT.NONE);
//    btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
//    btnEdit.setText("Edit");

   
    
//    Button btnFoo = new Button(grpSettings, SWT.NONE);
//    btnFoo.setText("foo");
//    
//    Button btnBar = new Button(grpSettings, SWT.NONE);
//    btnBar.setText("bar");
  }
  
  /**
   * Creates and configures the table that displayed the key/value
   * pairs that comprise the environment.
   * @param parent the composite in which the table should be created
   */
  protected void createEnvironmentTable(Composite parent) {
    
//    Composite comp2 = new Composite(parent, SWT.NONE);
//    comp2.setFont(parent.getFont());
//
//    GridData gd = new GridData(1);
//    gd.horizontalAlignment = SWT.FILL;
//    gd.grabExcessHorizontalSpace = true;
//    gd.horizontalSpan = GridData.FILL_BOTH;
//    comp2.setLayoutData(gd);
//
//    GridLayout gl_comp2 = new GridLayout(1, false);
//    gl_comp2.marginHeight = 0;
//    gl_comp2.marginWidth = 0;
//    comp2.setLayout(gl_comp2);
    
//    parent = comp2;
    SWTFactory.createLabel(parent, LaunchConfigurationsMessages.EnvironmentTab_Environment_variables_to_set__3, 2);
    Font font = parent.getFont();
    // Create table composite
    Composite tableComposite = SWTFactory.createComposite(parent, font, 1, 1, GridData.FILL_BOTH, 0, 0);
    tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));
    // Create table
    environmentTable = new TableViewer(tableComposite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
    Table table = environmentTable.getTable();
    table.setLayout(new GridLayout());
    table.setLayoutData(new GridData(GridData.FILL_BOTH));
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
    table.setFont(font);
   // environmentTable.setContentProvider(new EnvironmentVariableContentProvider());
   // environmentTable.setLabelProvider(new EnvironmentVariableLabelProvider());
  //  environmentTable.setColumnProperties(new String[] {P_VARIABLE, P_VALUE});
    environmentTable.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        //handleTableSelectionChanged(event);
      }
    });
    environmentTable.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        if (!environmentTable.getSelection().isEmpty()) {
          //handleEnvEditButtonSelected();
        }
      }
    });
    // Create columns
    
    final TableColumn tc1 = new TableColumn(table, SWT.NONE, 0);
    tc1.setText("Property");
    final TableColumn tc2 = new TableColumn(table, SWT.NONE, 1);
    tc2.setText("Value");
    final TableColumn tc3 = new TableColumn(table, SWT.NONE, 2);
    tc3.setText("Properties location");
    
    final Table tref = table;
    final Composite comp = tableComposite;
    
    tableComposite.addControlListener(new ControlAdapter() {
      public void controlResized(ControlEvent e) {
        Rectangle area = comp.getClientArea();
        Point size = tref.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        ScrollBar vBar = tref.getVerticalBar();
        int width = area.width - tref.computeTrim(0,0,0,0).width - 2;
        if (size.y > area.height + tref.getHeaderHeight()) {
          Point vBarSize = vBar.getSize();
          width -= vBarSize.x;
        }
        Point oldSize = tref.getSize();
        if (oldSize.x > area.width) {
          tc3.setWidth(width/6-1);
          int remainingWidth = width - tc3.getWidth();
          tc1.setWidth(remainingWidth/2-1);
          tc2.setWidth(remainingWidth - tc1.getWidth());
          tref.setSize(area.width, area.height);
        } else {
          tref.setSize(area.width, area.height);
          tc3.setWidth(width/6-1);
          int remainingWidth = width - tc3.getWidth();
          tc1.setWidth(remainingWidth/2-1);
          tc2.setWidth(remainingWidth - tc1.getWidth());
        }
      }
    });
  }
  
  /**
   * Creates the add/edit/remove buttons for the environment table
   * @param parent the composite in which the buttons should be created
   */
  protected void createTableButtons(Composite parent) {
    // Create button composite
    Composite buttonComposite = SWTFactory.createComposite(parent, parent.getFont(), 1, 1, GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END, 0, 0);

    // Create buttons
    envAddButton = createPushButton(buttonComposite, LaunchConfigurationsMessages.EnvironmentTab_New_4, null); 
    envAddButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
        //handleEnvAddButtonSelected();
      }
    });
    envSelectButton = createPushButton(buttonComposite, LaunchConfigurationsMessages.EnvironmentTab_18, null); 
    envSelectButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
        //handleEnvSelectButtonSelected();
      }
    });
    envEditButton = createPushButton(buttonComposite, LaunchConfigurationsMessages.EnvironmentTab_Edit_5, null); 
    envEditButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
        //handleEnvEditButtonSelected();
      }
    });
    envEditButton.setEnabled(false);
    envRemoveButton = createPushButton(buttonComposite, LaunchConfigurationsMessages.EnvironmentTab_Remove_6, null); 
    envRemoveButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
        //handleEnvRemoveButtonSelected();
      }
    });
    envRemoveButton.setEnabled(false);
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

   
  }

  protected void setText(ILaunchConfiguration configuration, Text text, String attribute) throws CoreException {
    text.setText(configuration.getAttribute(attribute, ""));
  }
  
  public void initializeFrom(ILaunchConfiguration configuration) {
    HierarchicalConfig hierarchicalConfig = new HierarchicalConfig();
    
    
    
    for (InnerHiararchicalConfig config : hierarchicalConfig.getConfigList()) {
      for (HierarchicalProperty property : config.getSortedProperties()) {
        TableItem tableItem = new TableItem(environmentTable.getTable(), SWT.NONE);
        tableItem.setText(new String[] {property.property, property.value, property.configName});
      }
    }

//    try {
//      
//    } catch (CoreException e) {
//      EclipseJPF.logError("Error during the JPF initialization form", e);
//    }

    super.initializeFrom(configuration);
  }

  String getJpfFileLocation() {
    return jpfFileLocationText.getText().trim();
  }

  @Override
  public String getName() {
    return "JPF Settings";
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    IProject implicitProject = null;
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy arg0) {
    System.out.println("DEFAULTS");
  }
}
