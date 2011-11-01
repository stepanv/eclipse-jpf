package gov.nasa.runjpf.wizard;

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
    return true;
  }

}
