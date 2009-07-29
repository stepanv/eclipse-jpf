/**
 * 
 */
package com.javapathfinder.eclipsejpf;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * @author sandro
 *
 */
public class RunJPF extends Job {
   
  private static final String JOB_NAME = "Verify...";
  private IFile file;
  
  public RunJPF(IFile file){
    super(JOB_NAME);
    this.file = file;
  }

  @Override
  /**
   * Do the work of setting up and executing the verify.
   */
  protected IStatus run(IProgressMonitor monitor) {
    IPath path = ResourcesPlugin.getWorkspace().getRoot().getLocation().append(file.getFullPath());
        
    EclipseJPFLauncher launcher = new EclipseJPFLauncher();
    try {
      launcher.launch(path.toFile());
    } catch (IOException e) {
       EclipseJPF.logError("Selected Config file was not found.");
    }    
    return Status.OK_STATUS;
  }
}




