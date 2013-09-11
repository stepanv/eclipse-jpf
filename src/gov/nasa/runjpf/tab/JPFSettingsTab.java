package gov.nasa.runjpf.tab;

import gov.nasa.jpf.Config;
import gov.nasa.runjpf.tab.internal.ExtendedPropertyContentProvider;
import gov.nasa.runjpf.tab.internal.ExtendedPropertyLabelProvider;
import gov.nasa.runjpf.tab.internal.LookupConfigHelper;
import gov.nasa.runjpf.tab.internal.TableSorter;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

@SuppressWarnings("restriction")
public class JPFSettingsTab extends AbstractJPFTab {

  public static final String ATTR_JPF_DEFAULTCONFIG = "ATTR_JPF_CONFIG";
  public static final String ATTR_JPF_APPCONFIG = "ATTR_JPF_APPCONFIG";
  public static final String ATTR_JPF_DYNAMICCONFIG = "ATTR_JPF_DYNAMICCONFIG";
  public static final String ATTR_JPF_CMDARGSCONFIG = "ATTR_JPF_CMDARGSCONFIG";

  static final Map<String, String> CONFIG_TO_NAME_MAP = new HashMap<String, String>();
  private static final String ATTR_JPF_SETTINGS_DYNAMICSELECTED = "ATTR_JPF_SETTINGS_DYNAMICSELECTED";
  private static final String ATTR_JPF_SETTINGS_CMDARGSSELECTED = "ATTR_JPF_SETTINGS_CMDARGSSELECTED";
  private static final String ATTR_JPF_SETTINGS_DEFAULTSELECTED = "ATTR_JPF_SETTINGS_DEFAULTSELECTED";
  private static final String ATTR_JPF_SETTINGS_APPPROPSSELECTED = "ATTR_JPF_SETTINGS_APPPROPSSELECTED";

  static {
    CONFIG_TO_NAME_MAP.put(ATTR_JPF_DEFAULTCONFIG, "Default properties");
    CONFIG_TO_NAME_MAP.put(ATTR_JPF_APPCONFIG, "Application properties");
    CONFIG_TO_NAME_MAP.put(ATTR_JPF_DYNAMICCONFIG, "Dynamic properties");
    CONFIG_TO_NAME_MAP.put(ATTR_JPF_CMDARGSCONFIG, "Command line arguments properties");
  }

  private Text jpfFileLocationText;

  // private Table table;
  private TableViewer configTable;
  private Composite mainComposite;

  Button checkAppProperties;

  Button checkDynamicProperties;

  Button checkDefaultProperties;
  Button checkCmdargsProperties;

  /**
   * @wbp.parser.entryPoint
   */
  @Override
  public void createControl(Composite parent) {

    mainComposite = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL);
    setControl(mainComposite);

    createConfigTable(mainComposite);
  }

  /**
   * Creates and configures the table that displayed the key/value pairs that
   * comprise the config.
   * 
   * @param parent
   *          the composite in which the table should be created
   */
  protected void createConfigTable(Composite parent) {

    SWTFactory.createLabel(parent, "JPF properties to &set:", 2);
    Font font = parent.getFont();

    Composite checkComposite = SWTFactory.createComposite(mainComposite, mainComposite.getFont(), 1, 1, GridData.FILL_BOTH, 0, 0);
    checkComposite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
    GridLayout gridLayout = (GridLayout) checkComposite.getLayout();
    gridLayout.numColumns = 4;

    checkDynamicProperties = new Button(checkComposite, SWT.CHECK);
    checkDynamicProperties.setText("Show dynamic properties");
    checkDynamicProperties.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateConfig(getCurrentLaunchConfiguration());
      }
    });

    checkCmdargsProperties = new Button(checkComposite, SWT.CHECK);
    checkCmdargsProperties.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateConfig(getCurrentLaunchConfiguration());
      }
    });
    checkCmdargsProperties.setText("Show cmd args properties");

    checkAppProperties = new Button(checkComposite, SWT.CHECK);
    checkAppProperties.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateConfig(getCurrentLaunchConfiguration());
      }
    });
    checkAppProperties.setText("Show app properties");

    checkDefaultProperties = new Button(checkComposite, SWT.CHECK);
    checkDefaultProperties.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateConfig(getCurrentLaunchConfiguration());
      }
    });
    checkDefaultProperties.setText("Show default properties");
    new Label(mainComposite, SWT.NONE);
    // Create table composite
    Composite tableComposite = SWTFactory.createComposite(parent, font, 1, 1, GridData.FILL_BOTH, 0, 0);
    tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));
    // Create table
    configTable = new TableViewer(tableComposite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
    Table table = configTable.getTable();
    table.setLayout(new GridLayout());
    table.setLayoutData(new GridData(GridData.FILL_BOTH));
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
    table.setFont(font);
    configTable.setContentProvider(new ExtendedPropertyContentProvider(checkAppProperties, checkCmdargsProperties, checkDefaultProperties,
        checkDynamicProperties, CONFIG_TO_NAME_MAP));
    configTable.setLabelProvider(new ExtendedPropertyLabelProvider());
    // environmentTable.setColumnProperties(new String[] {P_VARIABLE, P_VALUE});
    configTable.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        // handleTableSelectionChanged(event);
      }
    });
    configTable.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        if (!configTable.getSelection().isEmpty()) {
          // handleEnvEditButtonSelected();
        }
      }
    });
    // Create columns

    final TableColumn tc1 = new TableColumn(table, SWT.NONE, 0);
    tc1.setText("Property");
    final TableColumn tc2 = new TableColumn(table, SWT.NONE, 1);
    tc2.setText("Value");
    final TableColumn tc3 = new TableColumn(table, SWT.NONE, 2);
    tc3.setText("Properties source");

    final Table tref = table;
    final Composite comp = tableComposite;

    tableComposite.addControlListener(new ControlAdapter() {
      public void controlResized(ControlEvent e) {
        Rectangle area = comp.getClientArea();
        Point size = tref.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        ScrollBar vBar = tref.getVerticalBar();
        int width = area.width - tref.computeTrim(0, 0, 0, 0).width - 2;
        if (size.y > area.height + tref.getHeaderHeight()) {
          Point vBarSize = vBar.getSize();
          width -= vBarSize.x;
        }
        Point oldSize = tref.getSize();
        if (oldSize.x > area.width) {
          tc3.setWidth(width / 6 - 1);
          int remainingWidth = width - tc3.getWidth();
          tc1.setWidth(remainingWidth / 2 - 1);
          tc2.setWidth(remainingWidth - tc1.getWidth());
          tref.setSize(area.width, area.height);
        } else {
          tref.setSize(area.width, area.height);
          tc3.setWidth(width / 6 - 1);
          int remainingWidth = width - tc3.getWidth();
          tc1.setWidth(remainingWidth / 2 - 1);
          tc2.setWidth(remainingWidth - tc1.getWidth());
        }
      }
    });

    new TableSorter(configTable);
  }

//  String getSitePropertiesLocation(String[] args, String appPropPath) {
//    String path = getPathArg(args, "site");
//
//    if (path == null) {
//      // look into the app properties
//      // NOTE: we might want to drop this in the future because it constitutes
//      // a cyclic properties file dependency
//      if (appPropPath != null) {
//        path = JPFSiteUtils.getMatchFromFile(appPropPath, "site");
//      }
//
//      if (path == null) {
//        File siteProps = JPFSiteUtils.getStandardSiteProperties();
//        if (siteProps != null) {
//          path = siteProps.getAbsolutePath();
//        }
//      }
//    }
//
//    put("jpf.site", path);
//
//    return path;
//  }

  public static void initDefaultConfiguration(ILaunchConfigurationWorkingCopy configuration, String projectName, IFile jpfFile) {
    String appPropPath = null;
    if (jpfFile != null && jpfFile.getLocation() != null && jpfFile.getLocation().toFile() != null && jpfFile.getLocation().toFile().exists()) {
      appPropPath = jpfFile.getLocation().toFile().getAbsolutePath();
    }
    
    // empty config
    configuration.setAttribute(ATTR_JPF_DYNAMICCONFIG, new Config((String) null));
    configuration.setAttribute(ATTR_JPF_DEFAULTCONFIG, LookupConfigHelper.defaultConfigFactory(configuration, appPropPath));
    configuration.setAttribute(ATTR_JPF_APPCONFIG, LookupConfigHelper.appConfigFactory(configuration, appPropPath));
    configuration.setAttribute(ATTR_JPF_CMDARGSCONFIG, LookupConfigHelper.programArgumentsConfigFactory(configuration));

  }
  
  private static final Image icon = createImage("icons/search.png");
  
  @Override
  public Image getImage() {
    return icon;
  }

  protected void setText(ILaunchConfiguration configuration, Text text, String attribute) throws CoreException {
    text.setText(configuration.getAttribute(attribute, ""));
  }

  /**
   * Updates the environment table for the given launch configuration
   * 
   * @param configuration
   *          the configuration to use as input for the backing table
   */
  protected void updateConfig(ILaunchConfiguration configuration) {
    configTable.setInput(configuration);
    updateLaunchConfigurationDialog();
  }

  public void initializeFrom(ILaunchConfiguration configuration) {

    try {
      checkDynamicProperties.setSelection(configuration.getAttribute(ATTR_JPF_SETTINGS_DYNAMICSELECTED, true));
      checkCmdargsProperties.setSelection(configuration.getAttribute(ATTR_JPF_SETTINGS_CMDARGSSELECTED, true));
      checkDefaultProperties.setSelection(configuration.getAttribute(ATTR_JPF_SETTINGS_DEFAULTSELECTED, false));
      checkAppProperties.setSelection(configuration.getAttribute(ATTR_JPF_SETTINGS_APPPROPSSELECTED, false));
    } catch (CoreException e1) {
      // this should not happened
      throw new IllegalStateException("Programmers fatal error...", e1);
    }

    LookupConfigHelper.reloadConfig(configuration, ATTR_JPF_CMDARGSCONFIG, LookupConfigHelper.programArgumentsConfigFactory(configuration));
    LookupConfigHelper.reloadConfig(configuration, ATTR_JPF_DEFAULTCONFIG, LookupConfigHelper.defaultConfigFactory(configuration));
    LookupConfigHelper.reloadConfig(configuration, ATTR_JPF_APPCONFIG, LookupConfigHelper.appConfigFactory(configuration));
   
    updateConfig(configuration);

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
    configuration.setAttribute(ATTR_JPF_SETTINGS_DYNAMICSELECTED, checkDynamicProperties.getSelection());
    configuration.setAttribute(ATTR_JPF_SETTINGS_CMDARGSSELECTED, checkCmdargsProperties.getSelection());
    configuration.setAttribute(ATTR_JPF_SETTINGS_DEFAULTSELECTED, checkDefaultProperties.getSelection());
    configuration.setAttribute(ATTR_JPF_SETTINGS_APPPROPSSELECTED, checkAppProperties.getSelection());

    // IProject implicitProject = null;

    // TableItem[] items = environmentTable.getTable().getItems();
    // Map map = new HashMap(items.length);
    // for (int i = 0; i < items.length; i++)
    // {
    // ExtendedProperty var = (ExtendedProperty) items[i].getData();
    // map.put(var.getName(), var.getValue());
    // }
    // if (map.size() == 0) {
    // configuration.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES,
    // (Map) null);
    // } else {
    // configuration.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES,
    // map);
    // }
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    initDefaultConfiguration(configuration, null, null);
  }
}
