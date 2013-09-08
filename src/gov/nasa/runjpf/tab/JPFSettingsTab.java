package gov.nasa.runjpf.tab;

import gov.nasa.jpf.Config;
import gov.nasa.runjpf.EclipseJPF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsMessages;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
  
  @SuppressWarnings("unchecked")
  private static final Map<String, String> CONFIG_TO_NAME_MAP = new HashMap<String, String>();
  
  
  static {
    CONFIG_TO_NAME_MAP.put(ATTR_JPF_DEFAULTCONFIG, "Default properties");
    CONFIG_TO_NAME_MAP.put(ATTR_JPF_APPCONFIG, "Application properties");
    CONFIG_TO_NAME_MAP.put(ATTR_JPF_DYNAMICCONFIG, "Dynamic properties");
    CONFIG_TO_NAME_MAP.put(ATTR_JPF_CMDARGSCONFIG, "Command line arguments properties");
  }

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
  private TableViewer configTable;
  private Button envAddButton;
  private Button envSelectButton;
  private Button envEditButton;
  private Button envRemoveButton;
  private Composite mainComposite;

  private Button checkAppProperties;

  private Button checkDynamicProperties;

  private Button checkDefaultProperties;
  private Button checkCmdargsProperties;
  
  /**
   * @wbp.parser.entryPoint
   */
  @Override
  public void createControl(Composite parent) {

    
    mainComposite = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL);
    setControl(mainComposite);
    
    createConfigTable(mainComposite);
    createTableButtons(mainComposite);
    
    checkDynamicProperties.setSelection(true);
    checkCmdargsProperties.setSelection(true);

  }
  
  /**
   * Creates and configures the table that displayed the key/value
   * pairs that comprise the config.
   * @param parent the composite in which the table should be created
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
   configTable.setContentProvider(new HierarchicalPropertyContentProvider());
   configTable.setLabelProvider(new HierarchicalPropertyLabelProvider());
  //  environmentTable.setColumnProperties(new String[] {P_VARIABLE, P_VALUE});
    configTable.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        //handleTableSelectionChanged(event);
      }
    });
    configTable.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        if (!configTable.getSelection().isEmpty()) {
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
    tc3.setText("Properties source");
    
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

  public static void initDefaultConfiguration(ILaunchConfigurationWorkingCopy configuration, String projectName, IFile jpfFile) {
    
    Config config = new Config(new String[] {});
    configuration.setAttribute(ATTR_JPF_DEFAULTCONFIG, config);
    
    Config appConfig;
    if (jpfFile != null) {
      appConfig = new Config(jpfFile.getLocation().toFile().getAbsolutePath());
    } else {
      // empty config
      appConfig = new Config((String)null);
    }
    configuration.setAttribute(ATTR_JPF_APPCONFIG, appConfig);
    
    // empty config
    Config dynamicConfig = new Config((String)null);
    configuration.setAttribute(ATTR_JPF_DYNAMICCONFIG, dynamicConfig);
    
    initializeCmdArgs(configuration);
    
  }

  private static void initializeCmdArgs(ILaunchConfigurationWorkingCopy configuration) {
    Config cmdArgsConfig = new ConfigCmdArgs().publicLoadArgs(ConfigCmdArgs.programArguments(configuration));
    configuration.setAttribute(ATTR_JPF_CMDARGSCONFIG, cmdArgsConfig);
  }
  
  private static class ConfigCmdArgs extends Config {
    public ConfigCmdArgs() {
      super((String)null);
    }
    
    private static String programArguments(ILaunchConfiguration configuration) {
      String programArguments = "";
      try {
        programArguments = configuration.getAttribute(
            IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, "");
      } catch (CoreException e) {
        // no program args .. we're fine with that
      }
      return programArguments;
    }

    public Config publicLoadArgs(String programArguments) {
      String[] programArgumentsArray = DebugPlugin.parseArguments(programArguments);
      loadArgs(programArgumentsArray);
      return this;
    }
    public static void reloadArgs(ILaunchConfiguration configuration, Map<String, String> map) {
      Config newConfig = new ConfigCmdArgs().publicLoadArgs(programArguments(configuration));
      map.clear();
      for (Object key : newConfig.keySet()) {
        if (key instanceof String) {
          map.put((String)key, (String)newConfig.get(key));
        }
      }
    }
  }

  protected void setText(ILaunchConfiguration configuration, Text text, String attribute) throws CoreException {
    text.setText(configuration.getAttribute(attribute, ""));
  }
  
  /**
   * Updates the environment table for the given launch configuration
   * @param configuration the configuration to use as input for the backing table
   */
  protected void updateConfig(ILaunchConfiguration configuration) {
    configTable.setInput(configuration);
  }
  
  @SuppressWarnings("unchecked")
  public void initializeFrom(ILaunchConfiguration configuration) {
    
    try {
      ConfigCmdArgs.reloadArgs(configuration, configuration.getAttribute(ATTR_JPF_CMDARGSCONFIG, Collections.<String,String>emptyMap()));
    } catch (CoreException e) {
      // if reload is not successful we don't care
      EclipseJPF.logError("Config Command Arguments reload not successful", e);
    }
    updateConfig(configuration);

    super.initializeFrom(configuration);
  }
  
  private static class ExtendedProperty {
    private String property;
    public String getProperty() {
      return property;
    }

    public void setProperty(String property) {
      this.property = property;
    }

    public String getConfigName() {
      return configName;
    }

    public void setConfigName(String configName) {
      this.configName = configName;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    private String configName;
    private String value;
    
    public ExtendedProperty(String property, String value, String configName) {
      this.property = property;
      this.configName = configName;
      this.value = value;
    }
  }
  
  /**
   * Content provider for the config table
   */
  protected class HierarchicalPropertyContentProvider implements IStructuredContentProvider {
    public Object[] getElements(Object inputElement) {
      List<ExtendedProperty> elements = new ArrayList<ExtendedProperty>();
      ILaunchConfiguration config = (ILaunchConfiguration) inputElement;
      
      List<String> attributes = new LinkedList<String>();
      if (checkDefaultProperties.getSelection()) {
        attributes.add(ATTR_JPF_DEFAULTCONFIG);
      }
      if (checkCmdargsProperties.getSelection()) {
        attributes.add(ATTR_JPF_CMDARGSCONFIG);
      }
      if (checkAppProperties.getSelection()) {
        attributes.add(ATTR_JPF_APPCONFIG);
      }
      if (checkDynamicProperties.getSelection()) {
        attributes.add(ATTR_JPF_DYNAMICCONFIG);
      }
      for (String attribute : attributes) {
        try {
          @SuppressWarnings("unchecked")
          Map<String, String> m = config.getAttribute(attribute, (Map<String, String>) Collections.<String, String>emptyMap());
        
          if (m != null && !m.isEmpty()) {
            String friendlyName = CONFIG_TO_NAME_MAP.get(attribute);
            for (String key : m.keySet()) {
              elements.add(new ExtendedProperty(key, m.get(key), friendlyName));
            }
          }
        
        } catch (CoreException e) {
          DebugUIPlugin.log(new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), IStatus.ERROR, "Error reading configuration", e)); //$NON-NLS-1$
        }
      }
      
      return elements.toArray();
    }
    public void dispose() {
    }
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      if (newInput == null){
        return;
      }
      if (viewer instanceof TableViewer){
        TableViewer tableViewer= (TableViewer) viewer;
        if (tableViewer.getTable().isDisposed()) {
          return;
        }
        tableViewer.setComparator(new ViewerComparator() {
          public int compare(Viewer iviewer, Object e1, Object e2) {
            if (e1 == null) {
              return -1;
            } else if (e2 == null) {
              return 1;
            } else {
              return ((ExtendedProperty)e1).getProperty().compareToIgnoreCase(((ExtendedProperty)e2).getProperty());
            }
          }
        });
      }
    }
  }
  
  /**
   * Label provider for the config table
   */
  public class HierarchicalPropertyLabelProvider extends LabelProvider implements ITableLabelProvider {
    public String getColumnText(Object element, int columnIndex)  {
      String result = null;
      if (element != null) {
        ExtendedProperty var = (ExtendedProperty) element;
        switch (columnIndex) {
          case 0: // variable
            result = var.getProperty();
            break;
          case 1: // value
            result = var.getValue();
            break;
          case 2: // location
            result = var.getConfigName();
            break;
        }
      }
      return result;
    }
    
    public Image getColumnImage(Object element, int columnIndex) {
      if (columnIndex == 0) {
        return DebugPluginImages.getImage(IDebugUIConstants.IMG_OBJS_ENV_VAR);
      }
      return null;
    }
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
//    IProject implicitProject = null;
    
//    TableItem[] items = environmentTable.getTable().getItems();
//    Map map = new HashMap(items.length);
//    for (int i = 0; i < items.length; i++)
//    {
//      ExtendedProperty var = (ExtendedProperty) items[i].getData();
//      map.put(var.getName(), var.getValue());
//    } 
//    if (map.size() == 0) {
//      configuration.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, (Map) null);
//    } else {
//      configuration.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, map);
//    }
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    initDefaultConfiguration(configuration, null, null);
  }
}
