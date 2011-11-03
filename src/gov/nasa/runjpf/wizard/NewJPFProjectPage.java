package gov.nasa.runjpf.wizard;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;

import gov.nasa.jpf.Config;
import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.EclipseJPFLauncher;
import gov.nasa.runjpf.util.ListDialog;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.dialogs.IDEResourceInfoUtils;
import org.eclipse.ui.internal.ide.dialogs.ProjectContentsLocationArea.IErrorMessageReporter;

public class NewJPFProjectPage extends WizardPage {

  Text projectNameField;
  Button defaultLocButton;
  Button browseLocButton;
  Text projectPathField;
  List depProjectsList;
  List depJarsList;

  String lastSitePath;
  java.util.List<String> definedProjects;
  
  public NewJPFProjectPage(String pageName) {
    super(pageName);
    setPageComplete(false);
  }

  @Override
  public void createControl (Composite parent){
    initializeDialogUnits(parent);
    
    Composite composite = createPageContent(parent);

    setPageComplete(validatePage());
    
    setErrorMessage(null);
    setMessage(null);
    
    setControl(composite);
    Dialog.applyDialogFont(composite);    
  }
  
  private Composite createPageContent (final Composite parent){
    int margin = 10;
    int margin2 = 15;
    FormData fd;
    GridLayout gridLayout;
    Button button;
    Group group;
    List list;
    Composite c;
    
    Composite composite = new Composite(parent, SWT.NULL);
    composite.setLayout( new FormLayout());
    
    //--- the project name group
    Label projectLabel = new Label( composite, SWT.NONE);
    projectLabel.setText("Project name:");
    fd = new FormData();
    fd.left = new FormAttachment( 0, margin);
    fd.top = new FormAttachment( 0, margin);
    projectLabel.setLayoutData( fd);
    
    projectNameField = new Text(composite, SWT.BORDER);
    fd = new FormData();
    fd.left = new FormAttachment( projectLabel, margin);
    fd.top = new FormAttachment( 0, margin);
    fd.right = new FormAttachment( 100, -margin);
    projectNameField.setLayoutData( fd);
    
    projectNameField.setText("jpf-");
    projectNameField.addListener( SWT.Modify, new Listener(){
      public void handleEvent(Event e) {
        setPageComplete(validatePage());
      }
    });
        
    //--- the project location group
    defaultLocButton = button = new Button(composite, SWT.CHECK | SWT.RIGHT);
    button.setText("use default workspace");
    button.setSelection(true);
    fd = new FormData();
    fd.left = new FormAttachment( 0, margin);
    fd.top = new FormAttachment( projectLabel, margin2);
    button.setLayoutData( fd);
    
    button.setSelection(true);
    button.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        boolean useDefaults = defaultLocButton.getSelection();
        if (useDefaults) {
          projectPathField.setText(getDefaultPathDisplayString());
          projectPathField.setEnabled(false);
          browseLocButton.setEnabled(false);
        } else {
          projectPathField.setText("");
          projectPathField.setEnabled(true);
          browseLocButton.setEnabled(true);
        }
      }
    });

    Label locLabel = new Label( composite, SWT.NONE);
    locLabel.setText("Location:");
    fd = new FormData();
    fd.left = new FormAttachment( 0, margin);
    fd.top = new FormAttachment( defaultLocButton, margin);
    locLabel.setLayoutData( fd);

    browseLocButton = button = new Button( composite, SWT.PUSH);
    button.setText("Browse..");
    fd = new FormData();
    fd.right = new FormAttachment(100, -margin);
    fd.top = new FormAttachment( locLabel, -5, SWT.TOP);
    button.setLayoutData(fd);
    
    browseLocButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
          handleLocationBrowseButtonPressed();
      }
    });
    browseLocButton.setEnabled(false);
    
    projectPathField = new Text(composite, SWT.BORDER);
    fd = new FormData();
    fd.left = new FormAttachment(locLabel, margin);
    fd.right = new FormAttachment(browseLocButton, -margin);
    fd.top = new FormAttachment( defaultLocButton, margin);
    projectPathField.setLayoutData( fd);
    projectPathField.setText(getDefaultPathDisplayString());
    projectPathField.setEnabled(false);
    
    //--- dependency projects
    Group projGroup = group = new Group( composite, SWT.SHADOW_ETCHED_OUT);
    group.setText("Project Dependencies");
    group.setLayout( new FormLayout());
    fd = new FormData();
    fd.left = new FormAttachment(0, margin);
    fd.right = new FormAttachment(100, -margin);
    fd.top = new FormAttachment( locLabel, margin2);
    //fd.bottom = new FormAttachment(100, -margin);
    group.setLayoutData( fd);
    
    c = new Composite( group, SWT.NONE);
    gridLayout = new GridLayout();
    gridLayout.marginHeight = gridLayout.marginWidth = 0;
    c.setLayout(gridLayout);
    fd = new FormData();
    fd.right = new FormAttachment(100, -margin);
    fd.top = new FormAttachment( 0, margin);
    c.setLayoutData(fd);
    
    button = new Button( c, SWT.PUSH);
    button.setText("Add..");
    button.setLayoutData( new GridData( GridData.FILL_BOTH));
    button.addSelectionListener( new SelectionAdapter(){
      @Override
      public void widgetSelected( SelectionEvent event){
        java.util.List<String> projects = getSiteProjects();
        if (projects.isEmpty()){
          setErrorMessage("no projects defined in site.properties");
          return;
        }
        ListDialog dialog = new ListDialog(getShell());
        dialog.setTitle("Select Dependency Project");
        dialog.setMessage("projects defined in: " + getSitePropertiesPath());
        dialog.setItems( getSiteProjects());
        String[] selectedProjects = dialog.open();
        if (selectedProjects != null){
          for (String proj : selectedProjects){
            if (depProjectsList.indexOf(proj) >= 0) {
              setErrorMessage("project already selected");
            } else {
              depProjectsList.add(proj);
            }
          }
        }
      }
    });

    button = new Button( c, SWT.PUSH);
    button.setText("Remove");
    button.setLayoutData( new GridData( GridData.FILL_BOTH));
    button.addSelectionListener( new SelectionAdapter(){
      @Override
      public void widgetSelected( SelectionEvent event){
        int[] indices = depProjectsList.getSelectionIndices();
        depProjectsList.remove( indices);
      }
    });
    
    button = new Button( c, SWT.PUSH);
    button.setText("Up");
    button.setLayoutData( new GridData( GridData.FILL_BOTH));
    button.addSelectionListener( new SelectionAdapter(){
      @Override
      public void widgetSelected( SelectionEvent event){
        int index = depProjectsList.getSelectionIndex();
        if (index > 0){
          String project = depProjectsList.getItem(index);
          depProjectsList.remove(index);
          index--;
          depProjectsList.add( project, index);
          depProjectsList.setSelection(index);         
        }
      }
    });
    
    button = new Button( c, SWT.PUSH);
    button.setText("Down");
    button.setLayoutData( new GridData( GridData.FILL_BOTH));
    button.addSelectionListener( new SelectionAdapter(){
      @Override
      public void widgetSelected( SelectionEvent event){
        int index = depProjectsList.getSelectionIndex();
        int n = depProjectsList.getItemCount()-1;
        if (index < n){
          String project = depProjectsList.getItem(index);
          depProjectsList.remove(index);
          index++;
          depProjectsList.add( project, index);
          depProjectsList.setSelection(index);         
        }
      }
    });
    
    depProjectsList = list = new List( group, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
    fd = new FormData();
    fd.left = new FormAttachment(0, margin);
    fd.right = new FormAttachment(c, -margin);
    fd.top = new FormAttachment( 0, margin);
    fd.bottom = new FormAttachment( 100, -margin);
    list.setLayoutData(fd);

    //--- dependency libraries
    Group jarGroup = group = new Group( composite, SWT.SHADOW_ETCHED_OUT);
    group.setText("Jar Dependencies");
    group.setLayout( new FormLayout());
    fd = new FormData();
    fd.left = new FormAttachment(0, margin);
    fd.right = new FormAttachment(100, -margin);
    fd.top = new FormAttachment( projGroup, margin2);
    fd.bottom = new FormAttachment(100, -margin);
    group.setLayoutData( fd);
 
    c = new Composite( group, SWT.NONE);
    gridLayout = new GridLayout();
    gridLayout.marginHeight = gridLayout.marginWidth = 0;
    c.setLayout(gridLayout);
    fd = new FormData();
    fd.right = new FormAttachment(100, -margin);
    fd.top = new FormAttachment( 0, margin);
    c.setLayoutData(fd);
    
    button = new Button( c, SWT.PUSH);
    button.setText("Add..");
    button.setLayoutData( new GridData( GridData.FILL_BOTH));
    button.addSelectionListener( new SelectionAdapter(){
      @Override
      public void widgetSelected( SelectionEvent event){
        FileDialog dialog = new FileDialog(getShell());
        dialog.setFilterExtensions( new String[] {"*.jar"});
        String selectedJar = dialog.open();
        if (selectedJar != null){
          if (depJarsList.indexOf(selectedJar) >= 0){
            setErrorMessage("jar already selected");
            return;
          }
          depJarsList.add( selectedJar);
        }
      }
    });

    button = new Button( c, SWT.PUSH);
    button.setText("Remove");
    button.setLayoutData( new GridData( GridData.FILL_BOTH));
    button.addSelectionListener( new SelectionAdapter(){
      @Override
      public void widgetSelected( SelectionEvent event){
        int[] indices = depJarsList.getSelectionIndices();
        depJarsList.remove( indices);
      }
    });

    depJarsList = list = new List( group, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
    fd = new FormData();
    fd.left = new FormAttachment(0, margin);
    fd.right = new FormAttachment(c, -margin);
    fd.top = new FormAttachment( 0, margin);
    fd.bottom = new FormAttachment( 100, -margin);
    list.setLayoutData(fd);
    
    return composite;
  }
  
  private void handleLocationBrowseButtonPressed() {
    String selectedDirectory = null;
    String dirName = projectPathField.getText();

    if (!dirName.isEmpty()) {
      IFileInfo info;
      info = IDEResourceInfoUtils.getFileInfo(dirName);
      if (info == null || !(info.exists())) {
        dirName = "";
      }
    }

    DirectoryDialog dialog = new DirectoryDialog( projectPathField.getShell());
    dialog.setMessage("Choose Project Location");

    dialog.setFilterPath(dirName);
    selectedDirectory = dialog.open();

    if (selectedDirectory != null){
      projectPathField.setText(selectedDirectory);
    }
  }

  
  private String getDefaultPathDisplayString() {
    return Platform.getLocation().toOSString();
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

  public IProject getProjectHandle() {
    return ResourcesPlugin.getWorkspace().getRoot().getProject(projectNameField.getText());
  }

  //--- site.properties processing
  
  protected String getSitePropertiesPath() {
    return EclipseJPF.getDefault().getPluginPreferences().getString(EclipseJPFLauncher.SITE_PROPERTIES_PATH);
  }
  
  protected java.util.List<String> getSiteProjects(){
    String sitePath = getSitePropertiesPath();
    if (sitePath == null){
      setErrorMessage("no site.properties");
      return null;
    }
    
    if (sitePath.equals(lastSitePath)){
      return definedProjects;
    }
    
    lastSitePath = sitePath;
    File file = new File( getSitePropertiesPath());
    
    try {
      FileReader fr = new FileReader(file);
      Config config = new Config(fr);
      java.util.List<String> projects = new ArrayList<String>();
      
      for (String projId : config.getEntrySequence()){
        if ("extensions".equals(projId)){
          // we have to filter this out in case there is only a single project in
          // the list, in which case we find a jpf.properties under its value
          continue;
        }
        
        String v = config.getString(projId);
        File projDir = new File(v);
        
        if (projDir.isDirectory()){
          File propFile = new File(projDir, "jpf.properties");
          if (propFile.isFile()){
            projects.add(projId);
          }
        }
      }
      
      definedProjects = projects;
      return projects;
      
    } catch (FileNotFoundException e) {
      setErrorMessage( "no site.properties found");
      return null;
    }
  }
  
  
  //--- page content validation
  
  protected boolean validatePage() {
    IWorkspace workspace = IDEWorkbenchPlugin.getPluginWorkspace();

    String projectFieldContents = projectNameField.getText();
    if (projectFieldContents.equals("")) {
      setErrorMessage(null);
      setMessage("no project name");
      return false;
    }

    IStatus nameStatus = workspace.validateName(projectFieldContents, IResource.PROJECT);
    if (!nameStatus.isOK()) {
      setErrorMessage(nameStatus.getMessage());
      return false;
    }

    IProject handle = getProjectHandle();
    if (handle.exists()) {
      setErrorMessage("project exists");
      return false;
    }

    String validLocationMessage = checkValidLocation();
    if (validLocationMessage != null) { // no destination location
      setErrorMessage(validLocationMessage);
      return false;
    }

    setErrorMessage(null);
    setMessage(null);
    return true;
  }
  
  public String checkValidLocation() {
    String locationFieldContents = projectPathField.getText();
    if (locationFieldContents.length() == 0) {
        return "no project location specified";
    }

    return null;
  }
  
}
