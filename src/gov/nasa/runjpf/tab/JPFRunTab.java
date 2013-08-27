package gov.nasa.runjpf.tab;

import gov.nasa.runjpf.EclipseJPF;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaLaunchTab;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.actions.ControlAccessibleListener;
import org.eclipse.jdt.internal.debug.ui.launcher.DebugTypeSelectionDialog;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

public class JPFRunTab extends JavaLaunchTab {
  public static final String JPF_FILE_LOCATION = "JPF_FILE";
  public static final String JPF_DEBUG_BOTHVMS = "JPF_DEBUG_VM";
  public static final String JPF_DEBUG_JPF_INSTEADOFPROGRAM = "JPF_DEBUG_JPF_INSTEADOFPROGRAM";
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
  private Group basicConfiguraionGroup;
  private Button listenerSearchButton;
  protected Text listenerText;
  private Text txtListener;
  private Text text;
  private Button btnDebugBothTargets;
  private Button btnDebugJpfItself;
  private Button btnDebugTheProgram;

  /**
   * @wbp.parser.entryPoint
   */
  @Override
  public void createControl(Composite parent) {
    Composite comp = new Composite(parent, SWT.NONE);
    comp.setFont(parent.getFont());

    GridData gd = new GridData(1);
    gd.horizontalSpan = GridData.FILL_BOTH;
    comp.setLayoutData(gd);

    GridLayout layout = new GridLayout(1, false);
    layout.verticalSpacing = 0;
    comp.setLayout(layout);

    createBasicConfigurationGroup(comp);

    setControl(comp);
    
    Group grpOverrideCommonJpf = new Group(comp, SWT.NONE);
    grpOverrideCommonJpf.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    grpOverrideCommonJpf.setText("Common JPF settings");
    grpOverrideCommonJpf.setLayout(new GridLayout(3, false));
    
    Label lblListener = new Label(grpOverrideCommonJpf, SWT.NONE);
    lblListener.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    lblListener.setText("Listener:");
    
    txtListener = new Text(grpOverrideCommonJpf, SWT.BORDER);
    txtListener.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    
    Button searchListenerButton = new Button(grpOverrideCommonJpf, SWT.NONE);
    searchListenerButton.setText("Search");
    
    Label lblSearch = new Label(grpOverrideCommonJpf, SWT.NONE);
    lblSearch.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    lblSearch.setText("Search:");
    
    text = new Text(grpOverrideCommonJpf, SWT.BORDER);
    text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    
    Button searchSearchButton = new Button(grpOverrideCommonJpf, SWT.NONE);
    searchSearchButton.setText("Search");
    
    Group grpInteraction = new Group(grpOverrideCommonJpf, SWT.NONE);
    grpInteraction.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
    grpInteraction.setText("Interaction");
    grpInteraction.setLayout(new GridLayout(2, false));
    
    Button btnOverride = new Button(grpInteraction, SWT.RADIO);
    btnOverride.setText("Override");
    
    Button btnAppend = new Button(grpInteraction, SWT.RADIO);
    btnAppend.setText("Append");
    
    Group grpExperimentalSetting = new Group(comp, SWT.NONE);
    grpExperimentalSetting.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    grpExperimentalSetting.setText("Experimental settings");
    grpExperimentalSetting.setLayout(new GridLayout(2, false));
    
    btnDebugTheProgram = new Button(grpExperimentalSetting, SWT.RADIO);
    btnDebugTheProgram.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateLaunchConfigurationDialog();
      }
    });
    btnDebugTheProgram.setText("Debug the program being verified in JPF");
    
    btnDebugBothTargets = new Button(grpExperimentalSetting, SWT.CHECK);
    btnDebugBothTargets.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 2));
    btnDebugBothTargets.setText("Debug both the underlaying VM and the program (can lead to deadlocks)");
    
    btnDebugJpfItself = new Button(grpExperimentalSetting, SWT.RADIO);
    btnDebugJpfItself.setText("Debug JPF itself");
    btnDebugJpfItself.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        updateLaunchConfigurationDialog();
      }
    });
    
    btnDebugBothTargets.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        
        boolean readioChoicesEnabled = !btnDebugBothTargets.getSelection();
        btnDebugJpfItself.setEnabled(readioChoicesEnabled);
        btnDebugTheProgram.setEnabled(readioChoicesEnabled);
        updateLaunchConfigurationDialog();
      }
    });

    return;

  }

  private void createBasicConfigurationGroup(Composite parent) {
    Font font = parent.getFont();

    /*
     * ---------------------------------------------------------------------
     */

    basicConfiguraionGroup = new Group(parent, SWT.NONE);
    basicConfiguraionGroup.setText("JPF Verification basic configuration");
    basicConfiguraionGroup.setLayout(new GridLayout(3, false));
    basicConfiguraionGroup.setLayoutData(createHFillGridData());
    basicConfiguraionGroup.setFont(font);

    Link link_1 = new Link(basicConfiguraionGroup, 0);
    link_1.setToolTipText("Open editor for user settings");
    link_1.setText("JPF &File to execute (*.jpf):");
    link_1.setBounds(10, 14, 370, 15);
    new Label(basicConfiguraionGroup, SWT.NONE);
    new Label(basicConfiguraionGroup, SWT.NONE);

    jpfFileLocationText = new Text(basicConfiguraionGroup, SWT.BORDER);
    jpfFileLocationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
    jpfFileLocationText.addModifyListener(updatedListener);
    jpfFileLocationText.setBounds(10, 35, 524, 21);

    Button button = new Button(basicConfiguraionGroup, SWT.NONE);
    button.setText("&Browse...");
    button.setBounds(540, 33, 71, 25);

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
    
    
    listenerText = SWTFactory.createSingleText(basicConfiguraionGroup, 1);
    listenerText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateLaunchConfigurationDialog();
      }
    });
    ControlAccessibleListener.addListener(listenerText, basicConfiguraionGroup.getText());
    
    listenerSearchButton = createPushButton(basicConfiguraionGroup, "Search...", null);
    new Label(basicConfiguraionGroup, SWT.NONE);
    new Label(basicConfiguraionGroup, SWT.NONE);
    listenerSearchButton.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent e) {
      }
      public void widgetSelected(SelectionEvent e) {
        handleSearchButtonSelected();
      }
    });
    
  }
  
  /**
   * Show a dialog that lists all main types
   */
  protected void handleSearchButtonSelected() {
//    IJavaProject project = getProject();
    IJavaElement[] elements = null;
//    if ((project == null) || !project.exists()) {
      IJavaModel model = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
      if (model != null) {
        try {
          elements = model.getJavaProjects();
        }
        catch (JavaModelException e) {JDIDebugUIPlugin.log(e);}
      }
//    }
//    else {
//      elements = new IJavaElement[]{project};
//    }
    if (elements == null) {
      elements = new IJavaElement[]{};
    }
//    int constraints = IJavaSearchScope.SOURCES;
//    constraints |= IJavaSearchScope.APPLICATION_LIBRARIES;
//    if (fSearchExternalJarsCheckButton.getSelection()) {
//      constraints |= IJavaSearchScope.SYSTEM_LIBRARIES;
//    }
//    IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(elements, constraints);
    IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(elements, 1);
    ListenerSearchEngine engine = new ListenerSearchEngine();
    IType[] types = null;
    try {
      //types = engine.searchMainMethods(getLaunchConfigurationDialog(), searchScope, fConsiderInheritedMainButton.getSelection());
      types = engine.searchMainMethods(getLaunchConfigurationDialog(), searchScope, true);
    }
    catch (InvocationTargetException e) {
      setErrorMessage(e.getMessage());
      return;
    }
    catch (InterruptedException e) {
      setErrorMessage(e.getMessage());
      return;
    }
    DebugTypeSelectionDialog mmsd = new DebugTypeSelectionDialog(getShell(), types, LauncherMessages.JavaMainTab_Choose_Main_Type_11); 
    if (mmsd.open() == Window.CANCEL) {
      return;
    }
    Object[] results = mmsd.getResult();  
    IType type = (IType)results[0];
    if (type != null) {
      listenerText.setText(type.getFullyQualifiedName());
      //fProjText.setText(type.getJavaProject().getElementName());
    }
  } 

  private GridData createHFillGridData() {
    GridData gd = new GridData();
    gd.horizontalAlignment = SWT.FILL;
    gd.grabExcessHorizontalSpace = true;
    return gd;
  }

  public static void initDefaultConfiguration(ILaunchConfigurationWorkingCopy configuration, String projectName, String launchConfigName) {

    configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, EclipseJPF.JPF_MAIN_CLASS);
    configuration.setAttribute(JPF_FILE_LOCATION, "");
    configuration.setAttribute(JPF_DEBUG_BOTHVMS, false);
    configuration.setAttribute(JPF_DEBUG_JPF_INSTEADOFPROGRAM, false);
  }

  public void initializeFrom(ILaunchConfiguration configuration) {

    try {
      jpfFileLocationText.setText(configuration.getAttribute(JPF_FILE_LOCATION, ""));
      btnDebugBothTargets.setSelection(configuration.getAttribute(JPF_DEBUG_BOTHVMS, false));
      btnDebugJpfItself.setSelection(configuration.getAttribute(JPF_DEBUG_JPF_INSTEADOFPROGRAM, false));
      btnDebugTheProgram.setSelection(!btnDebugJpfItself.getSelection());
      
      boolean readioChoicesEnabled = !btnDebugBothTargets.getSelection();
      btnDebugJpfItself.setEnabled(readioChoicesEnabled);
      btnDebugTheProgram.setEnabled(readioChoicesEnabled);
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
    configuration.setAttribute(JPF_FILE_LOCATION, jpfFileLocationText.getText());
    configuration.setAttribute(JPF_DEBUG_BOTHVMS, btnDebugBothTargets.getSelection());
    configuration.setAttribute(JPF_DEBUG_JPF_INSTEADOFPROGRAM, btnDebugJpfItself.getSelection());
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy arg0) {
    System.out.println("DEFAULTS");
  }
}
