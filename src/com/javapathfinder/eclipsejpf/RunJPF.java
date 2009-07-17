/**
 * 
 */
package com.javapathfinder.eclipsejpf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedHashSet;
import java.util.Properties;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * @author sandro
 *
 */
public class RunJPF extends Job {
  
  private static final String JOB_NAME = "Verify...";
  private static final String separator = ",";
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
    StringWriter sw = new StringWriter();
    PrintWriter log = new PrintWriter(sw);
   
    //Find JPF   
    Properties siteproperties = new Properties();
    String sitePropertyLocation = System.getProperty("user.home") + File.separator + ".jpf"  + File.separator + "site.properties";
    try {
      siteproperties.load(new FileReader( sitePropertyLocation ));
    } catch (FileNotFoundException e1) {
      log.println("Warning: expected to find site.properties file at: " + sitePropertyLocation);
    } catch (IOException e1) {
      e1.printStackTrace(log);
    }
    
    //Try grabbing JPF from the jpf.core property set in ${user.home}/.jpf/site.properties
    String jpf_command = getJPFCommand(siteproperties.getProperty("jpf.core"));
    if (jpf_command == null){
      log.println("Warning: could not find jpf from 'jpf.core' setting. jpf.core=" + siteproperties.getProperty("jpf.core", null));
      log.println("Warning: attempting to find JPF from within the current classpath");
      String[] paths = System.getProperty("java.class.path", "").split(File.pathSeparator);
      for(String path : paths){
        String java_command = getJPFCommand(path);
        if (java_command != null)
          break;
      }
    }
    
    //Can't find JPF!
    if (jpf_command == null){
      log.println("JPF Couldn't be found!");
      displayErrorInConsole(sw.toString());
      return Status.OK_STATUS;
    }
    
    //Load up the selected *.jpf file.
    Properties prop = new Properties();
    try {
      prop.load(file.getContents());
    } catch (IOException e) {
      e.printStackTrace();
      return Status.OK_STATUS;
    } catch (CoreException e) {
      e.printStackTrace();
      return Status.OK_STATUS;
    }
    
    if (prop.getProperty("target") == null){
      log.println("No target specified.");
      displayErrorInConsole(sw.toString());
      return Status.OK_STATUS;
    }
    
    //Get the paths required
    String vmpath = prop.getProperty("classpath") == null ? getClasspath() : null;
    String sourcepath = prop.getProperty("sourcepath") == null ? getSourcepath() : null;
    
    //Build the command    
    StringBuffer command = new StringBuffer(jpf_command);
    if (vmpath != null)
      command.append(" +classpath=").append(vmpath);
    if (sourcepath != null)
      command.append(" +sourcepath=").append(sourcepath);
    command.append(" ").append(file.getLocation());
    
    //Startup the JPF process
    Thread jpfKiller = null;
    try {
      final Process jpf = Runtime.getRuntime().exec(command.toString());
      
      //Make sure that we kill the thread if this VM exists. (Most probably from eclipse closing)
      jpfKiller = new Thread(){
        public void run(){
          jpf.destroy();
        }
      };
      Runtime.getRuntime().addShutdownHook(jpfKiller);
      
      //Handle JPF's IO
      MessageConsole io = new MessageConsole("JPF", null);
      ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { io });
      MessageConsoleStream stream = io.newMessageStream();
      PrintWriter print = new PrintWriter(stream, true);
      if (!sw.toString().isEmpty())
        print.println(sw.toString());
      print.println("Executing command: " + command.toString());
      print.println("------------------------ start JPF with config file: " + file.getName());    
      new IORedirector(jpf.getInputStream(), stream).start();
      new IORedirector(jpf.getErrorStream(), stream).start();
      new ShellListener().start();
      jpf.waitFor();
      print.println("------------------------  exit JPF");
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }finally{
      if (jpfKiller != null)
        Runtime.getRuntime().removeShutdownHook(jpfKiller);
    }
    
    return Status.OK_STATUS;
  }
  
  private String getJPFCommand(String path){
    if (path == null || path.isEmpty())
      return null;

    File file = new File(path);
    if (!file.exists())
      return null;
    
    if (file.isFile()){
      if (file.getName().equals("jpf.jar") || file.getName().equals("jpf-launch.jar"))
        return "java -jar " + file.getAbsolutePath();
      else
        return null;
    }
    
    //By this point the file has to be a directory.   
    File dist = new File(file, "dist");
    if (dist.isDirectory()){
      for(File child : dist.listFiles()){
        String command = getJPFCommand(child.getAbsolutePath());
        if (command != null)
          return command;
      }
    }
    
    //Now check for the build folder
    File main = new File(file, "build" + File.separator + "main");
    if ( new File(main, "gov" + File.separator + "nasa" + File.separator + "jpf" + File.separator + "Main.class").isFile()){
      return "java -cp " + main.getAbsolutePath() + " gov.nasa.jpf.Main";
    }
    
    return null;
  }
  
  public String getClasspath(){
    IJavaProject project = JavaCore.create(file.getProject());
    LinkedHashSet<IPath> paths = new LinkedHashSet<IPath>();
    
    // append the default output folder
    IPath defOutputFolder;
    try {
      defOutputFolder = project.getOutputLocation();
      if (defOutputFolder != null) {
        paths.add(defOutputFolder);
      }
      
      // look for libraries and source root specific output folders
      for (IClasspathEntry e : project.getResolvedClasspath(true)) {
        IPath ePath = null;
        
        switch ( e.getContentKind()) {
        case IClasspathEntry.CPE_LIBRARY: 
          ePath = e.getPath(); break;
        case IClasspathEntry.CPE_SOURCE:
          ePath = e.getOutputLocation(); break;
        }
        
        if (ePath != null && !paths.contains(ePath)) {
          paths.add(ePath);
        }
      }
      
      //convert our IPaths into Strings separated by commas
      StringBuffer classpath = new StringBuffer();
      for (IPath path : paths) {
        String absPath = getAbsolutePath(project, path).toOSString();
        if (classpath.length() > 0) {
          classpath.append(separator);
        }
        classpath.append(absPath);
      }
      return classpath.toString();
    } catch (JavaModelException e) {
     JPFError("Project output folder does not exist.");
    }
    
    return "";
  }
  
  public String getSourcepath(){
    IJavaProject project = JavaCore.create(file.getProject());
    StringBuilder sourcepath = new StringBuilder();
    IClasspathEntry[] paths;
    
    try {
      paths = project.getResolvedClasspath(true);
    } catch (JavaModelException e) {
      JPFError("Could not retrieve project classpaths.",e);
      return "";
    }
    
    for(IClasspathEntry entry : paths){
      if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE){  
        sourcepath.append(getAbsolutePath(project, entry.getPath()));
        sourcepath.append(separator);
      }else if (entry.getSourceAttachmentPath() != null){
        IPath path = entry.getSourceAttachmentPath();
        if (path.getFileExtension() == null){ //null for a directory
          sourcepath.append(path);
          sourcepath.append(separator);
        }
      }
    }
    if (sourcepath.length() > 0)
      sourcepath.setLength(sourcepath.length() - 1); //remove that trailing separator
    return sourcepath.toString();
  }
  
  private static IPath getAbsolutePath(IJavaProject project, IPath relative){
    IPath path = ResourcesPlugin.getWorkspace().getRoot().getLocation();
    path = path.append(relative);
    return path;
  }
  
  private static void displayErrorInConsole(String error){
    MessageConsole io = new MessageConsole("JPF", null);
    ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { io });
    MessageConsoleStream stream = io.newMessageStream();
    PrintWriter print = new PrintWriter(stream, true);
    print.println(error);
    //Exceptions get thrown if the streams are closed. Leave this to eclipse I suppose
  }
  
  private static void JPFError(String message){
    Activator.logError(message);
  }
  
  private static void JPFError(String message, Exception e){
    JPFError(message +  " : "  + e);
  }
}

//Funnels the output from the process into the console
class IORedirector extends Thread {

  private PrintWriter out;
  private BufferedReader in;

  public IORedirector(final InputStream in, final OutputStream out) {
    this.in = new BufferedReader(new InputStreamReader(in));
    this.out = new PrintWriter(out, true);
  }

  @Override
  public void run() {
    try {
      String s;
      while ((s = in.readLine()) != null) {
        out.println(s);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }
}

/*
 * Listens for messages from the Shell on port 8000 
 */
class ShellListener extends Thread{
    @Override
    public void run() {
      Socket socket = null;
      //We aren't sure when the port is going to open (if it ever does) so keep
      //on trying until we get a hit.
      while (socket == null) {
        try {
          socket = new Socket(InetAddress.getLocalHost(), 8000);
        } catch (IOException io) {
          try {
            Thread.sleep(500);
          } catch (InterruptedException ex) {
            ex.printStackTrace();
          }
        }
      }
      try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String input = null;
        while ((input = reader.readLine()) != null) {
          final String[] location = input.split(":");
          Display.getDefault().syncExec(new Runnable() {
            public void run() {
              int lineNumber = new Integer(location[1]);
              String path = location[0];
              IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
              try{
                IFileStore file = EFS.getLocalFileSystem().getStore(new Path(path));
                IEditorPart part = IDE.openEditorOnFileStore(page, file);
                if ( lineNumber > 0 && part instanceof ITextEditor){
                  ITextEditor editor = (ITextEditor) part;
                  IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
                  
                  IRegion line = doc.getLineInformation(lineNumber - 1);
                  editor.selectAndReveal(line.getOffset(), line.getLength());
                }
                
              }catch(PartInitException e){
                Activator.logError("PartInitException", e);
              } catch (BadLocationException e) {
                Activator.logError("BadLocationException", e);
              }
            }
          });
        }
      } catch (SocketException se){
        //Probably nothing to worry about. Just the socket closing.
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
}
