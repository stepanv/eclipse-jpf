package gov.nasa.runjpf.wizard;

import gov.nasa.jpf.template.CreateProject;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class NewJPFProjectWizard extends Wizard implements INewWizard {

  NewJPFProjectPage mainPage;
  
  public NewJPFProjectWizard() {
    setWindowTitle("JPF Project Wizard");
  }
  
  @Override
  public void addPages(){
    super.addPages();
    
    mainPage = new NewJPFProjectPage("New JPF Project");
    mainPage.setTitle("New JPF Project");
    mainPage.setDescription("Create a new JPF project");
    
    addPage(mainPage);
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    // TODO Auto-generated method stub
  }

  @Override
  public boolean performFinish() {
    String projectName = mainPage.getProjectName();
    String projectLocation = mainPage.getProjectLocation();
    File projectDir = new File(projectLocation, projectName);
    
    String[] depProjects = mainPage.getDependencyProjects();
    String[] depJars = mainPage.getDependencyJars();
    
    String[] args = new String[ 1 + depProjects.length + depJars.length];
    args[0] = projectDir.getAbsolutePath();
    
    int i=1;
    for (int j=0; j<depProjects.length; j++){
      args[i++] = depProjects[j];
    }
    for (int j=0; j<depJars.length; j++){
      args[i++] = depJars[j];
    }
    
    try {
      CreateProject.main(args);
      openProject( projectName, projectLocation);
            
    } catch (Throwable t){
      System.err.println("JPF project creation failed: " + t);
      t.printStackTrace();
    }
    
    return true;
  }

  private void openProject (String projectName, String projectLoc){
    IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);

    if (!newProject.exists()) {
      try {
        newProject.create(null);
        if (!newProject.isOpen()) {
          newProject.open(null);
        }
      } catch (CoreException e) {
        e.printStackTrace();
      }
    }
  }

}
