/**
 * 
 */
package gov.nasa.runjpf;

import gov.nasa.runjpf.EclipseJPFLauncher.JPFKiller;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * The <code>RunJPF</code> class represents that Job that is executed to launch
 * JPF.
 * 
 * @author sandro
 * @see VerifyActionDelegate
 * @see EclipseJPFLauncher
 */
public class RunJPF extends Job {

  private static final String JOB_NAME = "Verify...";
  private IFile file;
  private volatile boolean isJPFRunning = false;

  public RunJPF(IFile file) {
    super(JOB_NAME);
    this.file = file;
  }

  @Override
  /**
   * Do the work of setting up and executing the verify. This method blocks until
   * the jpf process has finished running.
   * 
   */
  protected IStatus run(IProgressMonitor monitor) {

    EclipseJPFLauncher launcher = new EclipseJPFLauncher();
    final Process p = launcher.launch(file);
    JPFKiller killer = launcher.getKiller();

    // This is going to get beyond messy. But since there is no easy way to poll
    // if a process is running here we go

    // Runs until the jpf process terminates, then
    // sets isJPFRunning to false
    isJPFRunning = true;
    new Thread() {
      @Override
      public void run() {
        try {
          p.waitFor();
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } finally {
          isJPFRunning = false;
        }
      }
    }.start();

    while (isJPFRunning) {
      if (monitor.isCanceled()) {
        killer.run();
        isJPFRunning = false;
        return Status.CANCEL_STATUS;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return Status.OK_STATUS;
  }
}
