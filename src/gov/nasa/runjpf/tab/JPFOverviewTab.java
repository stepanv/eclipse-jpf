package gov.nasa.runjpf.tab;

import gov.nasa.jpf.Config;
import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.internal.launching.JPFRunner;
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
 
  private boolean debug;

  public JPFOverviewTab(boolean debug) {
    this.debug = debug;
  }
  
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
    labelListener.setToolTipText("The additional listener prepended to the configuration.\r\nThis option is rather a demonstration of Eclipse capabilitites than something that is intended to be used daily and heavily.");
    labelListener.setText("Listener:");

    textOptListenerClass = new Text(groupOpt, SWT.BORDER);
    textOptListenerClass.setToolTipText("The listener class.\r\nThis class must be available on the classpath. See \"jpf-core.native_classpath\" property.");
    textOptListenerClass.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
    textOptListenerClass.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        updateLaunchConfigurationDialog();
      }
    });

    Button buttonListenerSearch = new Button(groupOpt, SWT.NONE);
    buttonListenerSearch.setToolTipText("Select a listener class from the Eclipse indexed classes.\r\nThe listener class must implement the VMListener interface otherwise it's not shown.");
    buttonListenerSearch.setText("Search...");
    buttonListenerSearch.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        listenerType = handleSupertypeSearchButtonSelected("gov.nasa.jpf.JPFListener", textOptListenerClass, listenerType);
        updateLaunchConfigurationDialog();
      }
    });

    Label labelSearch = new Label(groupOpt, SWT.NONE);
    labelSearch.setToolTipText("The search class to override a search class from the configuration.\r\nThis option is rather a demonstration of Eclipse capabilitites than something that is intended to be used daily and heavily.");
    labelSearch.setText("Search:");

    textOptSearchClass = new Text(groupOpt, SWT.BORDER);
    textOptSearchClass.setToolTipText("The search class.\r\nThis class must be available on the classpath. See \"jpf-core.native_classpath\" property.");
    textOptSearchClass.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
    textOptSearchClass.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        updateLaunchConfigurationDialog();
      }
    });

    Button searchSearchButton = new Button(groupOpt, SWT.NONE);
    searchSearchButton.setToolTipText("Select a search class from the Eclipse indexed classes.\r\nThe listener class must implement the SearchListener interface otherwise it's not shown.");
    searchSearchButton.setText("Search...");
    searchSearchButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        searchType = handleSupertypeSearchButtonSelected("gov.nasa.jpf.search.Search", textOptSearchClass, searchType);
        updateLaunchConfigurationDialog();
      }
    });

    checkOptShellEnabled = new Button(groupOpt, SWT.CHECK);
    checkOptShellEnabled.setToolTipText("Whether to enable the shell on the given port.\r\nThis option is here for backwards compatibility.");
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
    textOptShellPort.setToolTipText("The port number between 1 - 65535");
    GridData gd_textOptShellPort = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
    gd_textOptShellPort.widthHint = 66;
    textOptShellPort.setLayoutData(gd_textOptShellPort);
    textOptShellPort.setOrientation(SWT.RIGHT_TO_LEFT);
    textOptShellPort.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        updateLaunchConfigurationDialog();
      }
    });
    new Label(groupOpt, SWT.NONE);
    new Label(groupOpt, SWT.NONE);

    createConfigTable(mainComposite);

    Composite composite = new Composite(mainComposite, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    GridLayout gl_composite = new GridLayout(1, false);
    gl_composite.marginWidth = 0;
    composite.setLayout(gl_composite);

    Group grpGeneratedPseudoCommand = new Group(composite, SWT.NONE);
    grpGeneratedPseudoCommand.setToolTipText("The command line string that will be used for the verification.\r\nNote that some information is not present untill the process is really created and therefore this string cannot be complete.\r\nNote that the properties are applied in the following order:\r\n1. The default properties that come from the site properties\r\n2. The application properties that come from the .jpf file\r\n3. The JPF properties specified as program arguments\r\n4. The dynamic properties that are created by this launch configuration");
    grpGeneratedPseudoCommand.setText("Generated pseudo command line");
    grpGeneratedPseudoCommand.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
    grpGeneratedPseudoCommand.setLayout(new GridLayout(2, false));

    textGeneratedCommandLine = new Text(grpGeneratedPseudoCommand, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
    GridData gd_textGeneratedCommandLine = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
    gd_textGeneratedCommandLine.heightHint = 109;
    textGeneratedCommandLine.setLayoutData(gd_textGeneratedCommandLine);
    textGeneratedCommandLine.setEditable(false);
  }

  /**
   * Creates and configures the table that displayed the key/value pairs that
   * comprise the config.
   * 
   * @param parent
   *          the composite in which the table should be created
   */
  protected void createConfigTable(Composite parent) {
    Font font = parent.getFont();
    // Create table composite
    Composite tableComposite = SWTFactory.createComposite(parent, font, 1, 1, GridData.FILL_BOTH, 0, 0);
    tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));
    final Composite comp = tableComposite;

    Group grpJpfPropertiesOverview = new Group(tableComposite, SWT.NONE);
    grpJpfPropertiesOverview.setToolTipText("JPF properties overview displays all the properties that will be effective for the JPF verification.\r\nNote that the properties are applied in the following order:\r\n1. The default properties that come from the site properties\r\n2. The application properties that come from the .jpf file\r\n3. The JPF properties specified as program arguments\r\n4. The dynamic properties that are created by this launch configuration");
    grpJpfPropertiesOverview.setText("JPF properties overview");
    grpJpfPropertiesOverview.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
    grpJpfPropertiesOverview.setLayout(new GridLayout(1, false));

    Composite checkComposite = SWTFactory
        .createComposite(grpJpfPropertiesOverview, mainComposite.getFont(), 1, 1, GridData.FILL_BOTH, 0, 0);
    GridData gd_checkComposite = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
    gd_checkComposite.heightHint = 19;
    checkComposite.setLayoutData(gd_checkComposite);
    GridLayout gridLayout = (GridLayout) checkComposite.getLayout();
    gridLayout.numColumns = 4;

    checkDynamicProperties = new Button(checkComposite, SWT.CHECK);
    checkDynamicProperties.setToolTipText("Whether to show the properties that are dynamically created by this launch configuration.\r\nThis option is useful especially for an inspection of how this Eclipse plugin sets up the JPF.");
    checkDynamicProperties.setText("Show dynamic properties");
    checkDynamicProperties.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateConfig(getCurrentLaunchConfiguration());
      }
    });

    checkCmdargsProperties = new Button(checkComposite, SWT.CHECK);
    checkCmdargsProperties.setToolTipText("Whether to show the JPF properties specified as program arguments in the JPF Arguments tab.\r\nNote that JPF property must match \"+property=value\" format.\r\nThis table doesn't indicate whether a property overrides, prepends or appends.");
    checkCmdargsProperties.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateConfig(getCurrentLaunchConfiguration());
      }
    });
    checkCmdargsProperties.setText("Show cmd args properties");

    checkAppProperties = new Button(checkComposite, SWT.CHECK);
    checkAppProperties.setToolTipText("Whether to show the JPF properties from the .jpf application property file.");
    checkAppProperties.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateConfig(getCurrentLaunchConfiguration());
      }
    });
    checkAppProperties.setText("Show app properties");

    checkDefaultProperties = new Button(checkComposite, SWT.CHECK);
    checkDefaultProperties.setToolTipText("Whether to show the JPF properties that come through the site properties file.");
    checkDefaultProperties.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateConfig(getCurrentLaunchConfiguration());
      }
    });
    checkDefaultProperties.setText("Show default properties");

    // Create table
    configTable = new TableViewer(grpJpfPropertiesOverview, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
    Table table = configTable.getTable();
    table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
    table.setLayout(new GridLayout());
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
    table.setFont(font);
    configTable.setLabelProvider(new ExtendedPropertyLabelProvider());
    configTable.setContentProvider(new ExtendedPropertyContentProvider(checkAppProperties, checkCmdargsProperties, checkDefaultProperties,
        checkDynamicProperties, CONFIG_TO_NAME_MAP));
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
      textGeneratedCommandLine.setVisible(false);
      JPFLaunchConfigurationDelegate jpfDelegate = new JPFLaunchConfigurationDelegate();
      VMRunnerConfiguration runConfig = jpfDelegate.createRunConfig(configuration);
      JPFRunner runner = new JPFRunner(jpfDelegate.verifyVMInstall(configuration));
      String exec = runner.constructProgramString(runConfig);

      List<String> arguments = new LinkedList<>();
      arguments.add(exec);
      arguments.add("-classpath");
      arguments.add(StringUtils.join(runConfig.getClassPath(), File.pathSeparator));
      arguments.addAll(Arrays.asList(runConfig.getVMArguments()));
      arguments.add(runConfig.getClassToLaunch());
      arguments.addAll(Arrays.asList(runConfig.getProgramArguments()));

      int topLine = textGeneratedCommandLine.getTopIndex();
      textGeneratedCommandLine.setText(StringUtils.join(arguments, "\n"));
      textGeneratedCommandLine.setTopIndex(topLine);
    } catch (CoreException e) {
      EclipseJPF.logError("Cannot update command line text", e);
    } finally {
      textGeneratedCommandLine.setVisible(true);
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

    try {
      int portShell = Integer.parseInt(textOptShellPort.getText());
      configuration.setAttribute(JPF_ATTR_OPT_SHELLPORT, portShell);
    } catch (NumberFormatException e) {
      // lets do not modify the stored value (will be verified in next step
      // anyway)
    }
    configuration.setAttribute(JPF_ATTR_OPT_SHELLENABLED, checkOptShellEnabled.getSelection());

    // store the dynamic configuration into the launch configuration
    storeDynamicConfiguration(configuration, debug);

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
    setErrorMessage(null);
    setMessage(null);
    setWarningMessage(null);

    if (checkOptShellEnabled.getSelection()) {
      int port;
      try {
        port = Integer.parseInt(textOptShellPort.getText());
      } catch (NumberFormatException e) {
        setErrorMessage("Provided port number cannot be converted to integer: " + e.getMessage());
        return false;
      }
      if (port <= 0 || port > (Short.MAX_VALUE - Short.MIN_VALUE)) {
        setErrorMessage("Provided port number is invalid");
        return false;
      }
    }
    return true;
  }

}
