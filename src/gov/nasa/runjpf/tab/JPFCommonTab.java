package gov.nasa.runjpf.tab;

import gov.nasa.runjpf.EclipseJPF;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
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

public class JPFCommonTab extends JavaLaunchTab {
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
  private Text listenerText;
  private Text searchText;
  private Text targetText;

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
    basicConfiguraionGroup.setLayout(new GridLayout(3, false));
    basicConfiguraionGroup.setFont(comp.getFont());

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
        handleSearchMainClassButtonSelected(targetText);
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
        handleSupertypeSearchButtonSelected("gov.nasa.jpf.JPFListener", listenerText);
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
        handleSupertypeSearchButtonSelected("gov.nasa.jpf.search.Search", searchText);
      }
    });

    Group grpInteraction = new Group(grpOverrideCommonJpf, SWT.NONE);
    GridData gd_grpInteraction = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
    gd_grpInteraction.widthHint = 283;
    grpInteraction.setLayoutData(gd_grpInteraction);
    grpInteraction.setText("Interaction with settings from *.jpf file");
    grpInteraction.setLayout(new GridLayout(2, false));

    Button btnAppend = new Button(grpInteraction, SWT.RADIO);
    btnAppend.setText("Add");

    Button btnOverride = new Button(grpInteraction, SWT.RADIO);
    btnOverride.setText("Override");

  }

  abstract private class InlineSearcher {
    abstract IType[] search() throws InvocationTargetException, InterruptedException;
  }

  protected void handleSupertypeSearchButtonSelected(final String supertype, Text text) {
    handleSearchButtonSelected(new InlineSearcher() {
      @Override
      IType[] search() throws InvocationTargetException, InterruptedException {
        ClassSearchEngine engine = new ClassSearchEngine();
        return engine.searchClasses(getLaunchConfigurationDialog(), simpleSearchScope(), true, supertype);
      }
    }, text);
  }

  protected void handleSearchMainClassButtonSelected(Text text) {
    handleSearchButtonSelected(new InlineSearcher() {
      @Override
      IType[] search() throws InvocationTargetException, InterruptedException {
        MainMethodSearchEngine engine = new MainMethodSearchEngine();
        return engine.searchMainMethods(getLaunchConfigurationDialog(), simpleSearchScope(), true);
      }
    }, text);
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
  protected void handleSearchButtonSelected(InlineSearcher searcher, Text text) {

    IType[] types = null;
    try {
      types = searcher.search();

    } catch (InvocationTargetException e) {
      setErrorMessage(e.getMessage());
      return;
    } catch (InterruptedException e) {
      setErrorMessage(e.getMessage());
      return;
    }
    DebugTypeSelectionDialog mmsd = new DebugTypeSelectionDialog(getShell(), types, LauncherMessages.JavaMainTab_Choose_Main_Type_11);
    if (mmsd.open() == Window.CANCEL) {
      return;
    }
    Object[] results = mmsd.getResult();
    IType type = (IType) results[0];
    if (type != null) {
      text.setText(type.getFullyQualifiedName());
      // fProjText.setText(type.getJavaProject().getElementName());
    }
  }

  public static void initDefaultConfiguration(ILaunchConfigurationWorkingCopy configuration, String projectName, String launchConfigName) {

    configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, EclipseJPF.JPF_MAIN_CLASS);
    configuration.setAttribute(JPF_FILE_LOCATION, "");
  }

  public void initializeFrom(ILaunchConfiguration configuration) {

    try {
      jpfFileLocationText.setText(configuration.getAttribute(JPF_FILE_LOCATION, ""));
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
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy arg0) {
    System.out.println("DEFAULTS");
  }
}
