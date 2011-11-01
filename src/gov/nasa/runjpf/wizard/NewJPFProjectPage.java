package gov.nasa.runjpf.wizard;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ui.dialogs.WorkingSetGroup;
import org.eclipse.ui.internal.ide.dialogs.ProjectContentsLocationArea;
import org.eclipse.ui.internal.ide.dialogs.ProjectContentsLocationArea.IErrorMessageReporter;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.dialogs.ProjectContentsLocationArea.IErrorMessageReporter;

public class NewJPFProjectPage extends WizardPage {

  private static final int SIZING_TEXT_FIELD_WIDTH = 250;
  private static String initialProjectFieldValue = "jpf-";

  Text projectNameField;

  private ProjectContentsLocationArea locationArea;
  private WorkingSetGroup workingSetGroup;

  public NewJPFProjectPage(String pageName) {
    super(pageName);
    setPageComplete(false);
  }

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);

    initializeDialogUnits(parent);

    composite.setLayout(new GridLayout());
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));

    createProjectNameGroup(composite);
    locationArea = new ProjectContentsLocationArea(getErrorReporter(),
        composite);

    // Scale the button based on the rest of the dialog
    setButtonLayoutData(locationArea.getBrowseButton());

    setPageComplete(validatePage());
    // Show description on opening
    setErrorMessage(null);
    setMessage(null);
    setControl(composite);
    Dialog.applyDialogFont(composite);
  }

  private final void createProjectNameGroup(Composite parent) {
    // project specification group
    Composite projectGroup = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    projectGroup.setLayout(layout);
    projectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    // new project label
    Label projectLabel = new Label(projectGroup, SWT.NONE);
    projectLabel
        .setText(IDEWorkbenchMessages.WizardNewProjectCreationPage_nameLabel);
    projectLabel.setFont(parent.getFont());

    // new project name entry field
    projectNameField = new Text(projectGroup, SWT.BORDER);
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    data.widthHint = SIZING_TEXT_FIELD_WIDTH;
    projectNameField.setLayoutData(data);
    projectNameField.setFont(parent.getFont());

    // Set the initial value first before listener
    // to avoid handling an event during the creation.
    if (initialProjectFieldValue != null) {
      projectNameField.setText(initialProjectFieldValue);
    }

    Listener nameModifyListener = new Listener() {
      public void handleEvent(Event e) {
        setLocationForSelection();
        boolean valid = validatePage();
        setPageComplete(valid);

      }
    };
    projectNameField.addListener(SWT.Modify, nameModifyListener);
  }

  void setLocationForSelection() {
    locationArea.updateProjectName(getProjectNameFieldValue());
  }
  
  private IErrorMessageReporter getErrorReporter() {
    return new IErrorMessageReporter() {
      @Override
      public void reportError(String errorMessage, boolean infoOnly) {
        setErrorMessage(errorMessage);
        boolean valid = errorMessage == null;
        if (valid) {
          valid = validatePage();
        }

        setPageComplete(valid);
      }
    };
  }

  private String getProjectNameFieldValue() {
    if (projectNameField == null) {
      return "";
    }
    return projectNameField.getText().trim();
  }

  public String getProjectName() {
    if (projectNameField == null) {
      return initialProjectFieldValue;
    }

    return getProjectNameFieldValue();
  }

  public IProject getProjectHandle() {
    return ResourcesPlugin.getWorkspace().getRoot()
        .getProject(getProjectName());
  }

  protected boolean validatePage() {
    IWorkspace workspace = IDEWorkbenchPlugin.getPluginWorkspace();

    String projectFieldContents = getProjectNameFieldValue();
    if (projectFieldContents.equals("")) { //$NON-NLS-1$
      setErrorMessage(null);
      setMessage(IDEWorkbenchMessages.WizardNewProjectCreationPage_projectNameEmpty);
      return false;
    }

    IStatus nameStatus = workspace.validateName(projectFieldContents,
        IResource.PROJECT);
    if (!nameStatus.isOK()) {
      setErrorMessage(nameStatus.getMessage());
      return false;
    }

    IProject handle = getProjectHandle();
    if (handle.exists()) {
      setErrorMessage(IDEWorkbenchMessages.WizardNewProjectCreationPage_projectExistsMessage);
      return false;
    }

    IProject project = ResourcesPlugin.getWorkspace().getRoot()
        .getProject(getProjectNameFieldValue());
    locationArea.setExistingProject(project);

    String validLocationMessage = locationArea.checkValidLocation();
    if (validLocationMessage != null) { // there is no destination location
                                        // given
      setErrorMessage(validLocationMessage);
      return false;
    }

    setErrorMessage(null);
    setMessage(null);
    return true;
  }
}
