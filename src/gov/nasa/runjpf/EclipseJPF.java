package gov.nasa.runjpf;

import java.util.HashMap;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.ITextEditor;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class EclipseJPF extends AbstractUIPlugin {

	/**
	 *  The plug-in ID
	 */
	public static final String PLUGIN_ID = "RunJPF";

	// The shared instance
	private static EclipseJPF plugin;
	
	public EclipseJPF() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static EclipseJPF getDefault() {
		return plugin;
	}

	  /**
	   * Convenience method to log any type of message.
	   * The code for this log will be Status.OK
	   */
	  public static void log(int severity, String message, Throwable exception) {
	    getDefault().getLog().log(new Status(severity, PLUGIN_ID, Status.OK,
	                                         message, exception == null ? new Throwable() : exception));
	  }

	  /**
	   * Convenience method to log plugin information.
	   */
	  public static void logInfo(String message) {
	    log(Status.INFO, message == null ? "null" : message, null);
	  }

	  /**
	   * Convenience method to log Warnings without an exception This call is
	   * exactly equivalent to logWarning(message, null)
	   * @param message - the message to include with the warning
	   */
	  public static void logWarning(String message) {
	    logWarning(message, null);
	  }

	  /**
	   * Convenience method to log Warnings along with an exception @param message
	   * the message to include with the warning
	   * @param exception - the exception to include with the warning
	   */
	  public static void logWarning(String message, Exception exception) {
	    log(Status.WARNING, message, exception);
	  }

	  /**
	   * Convenience method to log errors
	   * @param message the message to display with this error
	   * @param exception the exception to associate with this error.
	   */
	  public static void logError(String message, Throwable exception) {
	    log(Status.ERROR, message, exception);
	  }
	  

	  /**
	   * Convenience method to log errors
	   * @param message the message to display with this error
	   */
	  public static void logError(String message) {
	    logError(message, new Exception());
	  }
	  
	  private static HashMap<String, IFileStore> links = new HashMap<String, IFileStore>();
	  /*
	   * Given a path, this method returns a IFileStore representing the file at 
	   * this path. Returns null if the underlying resource does not exist.
	   * This method uses memoization to optimize results. 
	   */
	  public static IFileStore getIFileStore(String filepath){    
	    if (links.containsKey(filepath)){
	      return links.get(filepath);
	    }
	    
	    IFileStore s = EFS.getLocalFileSystem().getStore(new Path(filepath));
	    
	    if (s.fetchInfo().exists() == false)
	      s = null;
	    
	    links.put(filepath,  s);
	    return s;
	  }
	  
	  public static void openLink(final IFile file, final int line){
	    Display.getDefault().syncExec(new Runnable() {
        public void run() {
          IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName());
          IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
          try {
            IEditorPart part = page.openEditor(new FileEditorInput(file), desc.getId());
            int num = line - 1;
            if (num < 0)
              return;
            if (part instanceof ITextEditor){
              ITextEditor editor = (ITextEditor) part;
              IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());      
              IRegion fileLine = doc.getLineInformation(num);
              editor.selectAndReveal(fileLine.getOffset(), fileLine.getLength());
            }
          } catch (PartInitException e) {
            logError("Could not create editor for: "+file.getName(), e);
          } catch (BadLocationException e) {
            logError("Bad location for line number: " + line +" in file: "+file.getName(), e);
          }
        }
	    });
	    
	  }
	  
	  private static HashMap<String, IFile> filecache = new HashMap<String, IFile>(); 
	  public static IFile getIFile(String file){
	    if (filecache.containsKey(file))
	      return filecache.get(file);
	    for (IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()){
	      try {
	        if (p.getNature(JavaCore.NATURE_ID) != null){
	          IJavaProject j = JavaCore.create(p);
	          for(IClasspathEntry cpe : j.getResolvedClasspath(true)){
	            if (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE){
	              IPath path = cpe.getPath().append(file);
	              IFile f = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
	              if (f != null && f.exists()){
	                filecache.put(file, f);
	                return f;
	              }
	              
	            }
	          }
	        }
	      } catch (CoreException e) {
	        logError("Could not check nature of project: " + p.getName());
	      }
	    }
	    filecache.put(file, null);
      return null;
	  }
	  
	  
	  public static void openExternalLink(final String filepath, final int line){
      openExternalLink(getIFileStore(filepath), line);
    }
	  /*
	   * Attempts to open the file given by filepath to the given line.
	   */
	  public static void openExternalLink(final IFileStore file, final int line){
	    Display.getDefault().syncExec(new Runnable() {
	      public void run() {
	        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	        try{
	          IEditorPart part = IDE.openEditorOnFileStore(page, file);
	          int num = line -1;
	          if (num < 0)
	            return;
	          if (part instanceof ITextEditor){
	            ITextEditor editor = (ITextEditor) part;
	            IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());      
	            IRegion fileLine = doc.getLineInformation(num);
	            editor.selectAndReveal(fileLine.getOffset(), fileLine.getLength());
	          }        
	        }catch(PartInitException e){
	          EclipseJPF.logError("PartInitException", e);
	        } catch (BadLocationException e) {
	          EclipseJPF.logError("BadLocationException", e);
	        }
	      }
	    });
	  }
	  
	  final public static String JPF_MAIN_CLASS = "gov.nasa.jpf.tool.RunJPF";
	  

}
