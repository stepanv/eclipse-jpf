/**
 * Copyright (C) 2009 United States Government as represented by the
 * Administrator of the National Aeronautics and Space Administration
 * (NASA).  All Rights Reserved.
 *
 * This software is distributed under the NASA Open Source Agreement
 * (NOSA), version 1.3.  The NOSA has been approved by the Open Source
 * Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
 * directory tree for the complete NOSA document.
 *
 * THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
 * KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
 * LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
 * SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
 * A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
 * THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
 * DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
 */
package gov.nasa.runjpf;

import java.io.PrintWriter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * The <code>EclipseJPFLauncher</code> class is used by the eclipse-jpf plugin
 * to launch a JPF process using preferences stored by this plugin.
 * 
 * @author Sandro Badame <a
 *         href="mailto:s.badame@gmail.com">S.badame@gmail.com</a>
 * 
 */
public class EclipseJPFLauncher extends JPFLauncher {

  /**
   * The key for the host VM arguments property
   */
  public static final String VM_ARGS = "jpf.vm_args";

  /**
   * The key for the JPF arguments property
   */
  public static final String ARGS = "jpf.args";

  /**
   * The key for the site properties file path
   */
  public static final String SITE_PROPERTIES_PATH = "jpf.site_properties_path";

  /**
   * The key for the port number property
   */
  public static final String PORT = "jpf.port";

  private PrintWriter out;
  private Process jpf;
  private JPFKiller killer;

  /**
   * Launches a JPF process with integration into the Eclipse IDE<br>
   * The following is done:
   * <ul>
   * <li>A new Message Console is created</li>
   * <li>THe jpf Killer is created help destroy the JPF process if needed</li>
   * </ul>
   * The Eclipse job management is done in {@link RunJPF}
   * 
   * @param file
   *          - the *.jpf configuration file to run by JPF
   * @return the JPF process
   */
  public Process launch(IFile file) {
    // Handle JPF's IO
    MessageConsole io = new MessageConsole(file.getName() + "(run)", null);
    io.addPatternMatchListener(new PatternMatchListener());
    ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { io });
    MessageConsoleStream stream = io.newMessageStream();
    out = new PrintWriter(stream, true);
    killer = new JPFKiller();

    IPath path = file.getLocation(); // we need the absolute path here
                                     // (IFile.getFullPath() isn't - it's
                                     // relative to the project)
    IPath workingDir = file.getProject().getLocation();
    if (workingDir == null)
      workingDir = new Path(System.getProperty("user.dir"));
    jpf = super.launch(path.toFile(), workingDir.toFile());
    return jpf;
  }

  /**
   * Returns a JPFKiller object used to cancel the current JPF launch
   * 
   * @return the jpf killer associated with the currently running JPF or null if
   *         the JPF process has already terminated.
   */
  public JPFKiller getKiller() {
    return killer;
  }

  /**
   * Returns the output stream connected to the message console
   * 
   * @return the output stream that is sent to the user to view
   */
  @Override
  protected PrintWriter getOutputStream() {
    return out;
  }

  /**
   * Returns the error stream connected to the message console
   * 
   * @return the error stream that is sent to the user to view
   */
  @Override
  protected PrintWriter getErrorStream() {
    return out;
  }

  /**
   * Opens the given file to the given line
   * 
   * @param filepath
   *          - the absolute path to the file to be opened
   * @param line
   *          - the line number in the file to be shown
   */
  @Override
  public void gotoSource(final String filepath, final int line) {
    EclipseJPF.openExternalLink(filepath, line);
  }

  /**
   * Return the port number to be used by the shell to allow for Shell<->IDE
   * communication.
   * 
   * @return - the port number specified by the user.
   * @see JPFLauncher#getPort()
   */
  @Override
  protected int getPort() {
    Integer port = EclipseJPF.getDefault().getPreferenceStore().getInt(PORT);
    return (int) (port != null ? port : DEFAULT_PORT);
  }

  /**
   * Returns the arguments for the host VM stored by plugin, configured by the
   * options pane
   * 
   * @param def
   *          - the value to return if no arguments are stored
   * @return the arguments for the host VM or def if no arguments for the host
   *         VM are stored
   */
  @Override
  protected String getVMArgs(String def) {
    return EclipseJPF.getDefault().getPluginPreferences().getString(VM_ARGS);
  }

  /**
   * Returns the arguments for JPF stored by plugin, configured by the options
   * pane
   * 
   * @param def
   *          - the value to return if no arguments are stored
   * @return the arguments for JPF or def if no arguments for the host VM are
   *         stored
   */
  @Override
  protected String getArgs(String def) {
    return EclipseJPF.getDefault().getPluginPreferences().getString(ARGS);
  }

  /**
   * Returns the absolute path to the site file<br>
   * Identical to
   * <code>getProperty(SITE_PROPERTIES_PATH, DEFAULT_SITE_PROPERTIES_PATH);</code>
   * 
   * @return - the requested site properties file or
   *         DEFAULT_SITE_PROPERTIES_PATH if none is specified
   * @see JPFLauncher#getSiteProperties()
   */
  @Override
  protected String getSiteProperties() {
    return EclipseJPF.getDefault().getPluginPreferences().getString(SITE_PROPERTIES_PATH);
  }

  /**
   * The <code>JPFKiller</code> class is used to help facilitate canceling a JPF
   * process
   * 
   * @author sandro
   */
  class JPFKiller extends Thread {

    /*
     * Creates an instance of JPFKiller and adds itself as a shutdown hook to
     * kill JPF.
     */
    public JPFKiller() {
      Runtime.getRuntime().addShutdownHook(this);
    }

    /**
     * Destroys the JPF thread, removes itself as a ShutdownHook and sets itself
     * to null.
     */
    @Override
    public void run() {
      if (jpf != null)
        jpf.destroy();

      try {
        if (killer != null)
          Runtime.getRuntime().removeShutdownHook(this);
      } catch (IllegalStateException e) {
        // Incase we are already shutting down
      }
      killer = null;
    }
  }

}
