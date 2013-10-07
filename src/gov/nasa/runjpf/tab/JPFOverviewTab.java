package gov.nasa.runjpf.tab;

import gov.nasa.jpf.Config;
import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.launching.JPFLaunchConfigurationDelegate;
import gov.nasa.runjpf.tab.internal.ExtendedPropertyContentProvider;
import gov.nasa.runjpf.tab.internal.ExtendedPropertyLabelProvider;
import gov.nasa.runjpf.tab.internal.LookupConfigHelper;
import gov.nasa.runjpf.tab.internal.TableSorter;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

/**
 * <p>
 * This is a GUI SWT Eclipse launch configuration Tab for Java PathFinder
 * Verification launch action.<br/>
 * The intention of this tab is to provide user an easy way to determine the
 * exact set of parameters, properties and settings JPF will be started with.
 * </p>
 * <p>
 * It would be cool if user can modify the values here, but that is completely
 * out of the scope of my GSoC 2013 project.
 * </p>
 * 
 * @author stepan
 * 
 */
@SuppressWarnings("restriction")
public class JPFOverviewTab extends CommonJPFTab {

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
  private Text textOptListenerClass;

  private IType listenerType;
  private IType searchType;
  private Text textOptSearchClass;
  private Button checkOptShellEnabled;
  private Text textOptShellPort;

  /**
   * @wbp.parser.entryPoint
   */
  @Override
  public void createControl(Composite parent) {

    mainComposite = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL);
    GridLayout gridLayout = (GridLayout) mainComposite.getLayout();
    gridLayout.numColumns = 1;
    setControl(mainComposite);

    Group groupOpt = new Group(mainComposite, SWT.NONE);
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

    createConfigTable(mainComposite);

    Composite composite = new Composite(mainComposite, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    GridLayout gl_composite = new GridLayout(2, false);
    gl_composite.marginWidth = 0;
    composite.setLayout(gl_composite);

    Label lblNewLabel = new Label(composite, SWT.NONE);
    lblNewLabel.setText("Generated command line:");
    new Label(composite, SWT.NONE);

    textGeneratedCommandLine = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
    GridData gd_text_1 = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    gd_text_1.heightHint = 113;
    textGeneratedCommandLine.setLayoutData(gd_text_1);
  }

  /**
   * Creates and configures the table that displayed the key/value pairs that
   * comprise the config.
   * 
   * @param parent
   *          the composite in which the table should be created
   */
  protected void createConfigTable(Composite parent) {

    Label label = SWTFactory.createLabel(parent, "JPF properties to &set:", 2);
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

  /**
   * Initialize the launch configuration with some defaults.
   * 
   * @param configuration
   *          The launch configuration to initialize
   * @param projectName
   *          The name of the project or null
   * @param jpfFile
   *          The JPF file to execute or null
   */
  public static void initDefaultConfiguration(ILaunchConfigurationWorkingCopy configuration, String projectName, IFile jpfFile) {
    String appPropPath = null;
    if (jpfFile != null && jpfFile.getLocation() != null && jpfFile.getLocation().toFile() != null
        && jpfFile.getLocation().toFile().exists()) {
      appPropPath = jpfFile.getLocation().toFile().getAbsolutePath();
    }

    // empty config
    configuration.setAttribute(ATTR_JPF_DYNAMICCONFIG, new Config((String) null));
    configuration.setAttribute(ATTR_JPF_DEFAULTCONFIG, LookupConfigHelper.defaultConfigFactory(configuration, appPropPath));
    configuration.setAttribute(ATTR_JPF_APPCONFIG, LookupConfigHelper.appConfigFactory(configuration, appPropPath));
    configuration.setAttribute(ATTR_JPF_CMDARGSCONFIG, LookupConfigHelper.programArgumentsConfigFactory(configuration));

  }

  private static final Image icon = createImage("icons/search.png");
  private Text textGeneratedCommandLine;

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
    updateConfigTable(configuration);
    updateLaunchConfigurationDialog();
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {

    try {
      setText(configuration, textOptListenerClass, JPFRunTab.JPF_ATTR_OPT_LISTENER);
      setText(configuration, textOptSearchClass, JPFRunTab.JPF_ATTR_OPT_SEARCH);

      checkOptShellEnabled.setSelection(configuration.getAttribute(JPF_ATTR_OPT_SHELLENABLED, true));
      textOptShellPort.setText(String.valueOf(configuration.getAttribute(JPF_ATTR_OPT_SHELLPORT, defaultShellPort())));
      textOptShellPort.setEnabled(configuration.getAttribute(JPF_ATTR_OPT_SHELLENABLED, true));

      checkDynamicProperties.setSelection(configuration.getAttribute(ATTR_JPF_SETTINGS_DYNAMICSELECTED, true));
      checkCmdargsProperties.setSelection(configuration.getAttribute(ATTR_JPF_SETTINGS_CMDARGSSELECTED, true));
      checkDefaultProperties.setSelection(configuration.getAttribute(ATTR_JPF_SETTINGS_DEFAULTSELECTED, false));
      checkAppProperties.setSelection(configuration.getAttribute(ATTR_JPF_SETTINGS_APPPROPSSELECTED, false));

      updateCommandLineText(configuration);

    } catch (CoreException e1) {
      // this should not happened
      throw new IllegalStateException("Programmer's fatal error...", e1);
    }

    LookupConfigHelper.reloadConfig(configuration, ATTR_JPF_CMDARGSCONFIG, LookupConfigHelper.programArgumentsConfigFactory(configuration));
    LookupConfigHelper.reloadConfig(configuration, ATTR_JPF_DEFAULTCONFIG, LookupConfigHelper.defaultConfigFactory(configuration));
    LookupConfigHelper.reloadConfig(configuration, ATTR_JPF_APPCONFIG, LookupConfigHelper.appConfigFactory(configuration));

    updateConfig(configuration);

    super.initializeFrom(configuration);
  }

  private void updateCommandLineText(ILaunchConfiguration configuration) {
    try {
      JPFLaunchConfigurationDelegate jpfDelegate = new JPFLaunchConfigurationDelegate();
      VMRunnerConfiguration runConfig = jpfDelegate.createRunConfig(configuration);

      List<String> arguments = new LinkedList<>();
      arguments.add("java");
      arguments.add("-classpath");
      arguments.add(StringUtils.join(runConfig.getClassPath(), File.pathSeparator));
      arguments.addAll(Arrays.asList(runConfig.getVMArguments()));
      arguments.add(runConfig.getClassToLaunch());
      arguments.addAll(Arrays.asList(runConfig.getProgramArguments()));

      textGeneratedCommandLine.setText(StringUtils.join(arguments, "\n"));
    } catch (CoreException e) {
      EclipseJPF.logError("Cannot update command line text", e);
    }
  }

  @Override
  public String getName() {
    return "JPF Overview";
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    configuration.setAttribute(ATTR_JPF_SETTINGS_DYNAMICSELECTED, checkDynamicProperties.getSelection());
    configuration.setAttribute(ATTR_JPF_SETTINGS_CMDARGSSELECTED, checkCmdargsProperties.getSelection());
    configuration.setAttribute(ATTR_JPF_SETTINGS_DEFAULTSELECTED, checkDefaultProperties.getSelection());
    configuration.setAttribute(ATTR_JPF_SETTINGS_APPPROPSSELECTED, checkAppProperties.getSelection());

    configuration.setAttribute(JPF_ATTR_OPT_LISTENER, textOptListenerClass.getText().trim());
    configuration.setAttribute(JPF_ATTR_OPT_SEARCH, textOptSearchClass.getText().trim());

    // port is already validated
    int portShell = Integer.parseInt(textOptShellPort.getText());
    configuration.setAttribute(JPF_ATTR_OPT_SHELLPORT, portShell);
    configuration.setAttribute(JPF_ATTR_OPT_SHELLENABLED, checkOptShellEnabled.getSelection());

    // store the dynamic configuration into the launch configuration
    storeDynamicConfiguration(configuration);

    // update the configuration table
    updateConfigTable(configuration);
    // update the generated command line text
    updateCommandLineText(configuration);
  }

  /**
   * Updates the configuration table with the values from the given launch
   * configuration.
   * 
   * @param configuration
   *          The launch configuration to update the configuration table with.
   */
  private void updateConfigTable(ILaunchConfiguration configuration) {
    configTable.setInput(configuration);
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    initDefaultConfiguration(configuration, null, null);
  }

  @Override
  public boolean isValid(ILaunchConfiguration launchConfig) {
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
    return true;
  }

}
