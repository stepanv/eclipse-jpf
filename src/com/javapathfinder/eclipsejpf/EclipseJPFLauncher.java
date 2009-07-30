package com.javapathfinder.eclipsejpf;

import java.io.File;
import java.io.PrintWriter;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.Path;
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

public class EclipseJPFLauncher extends JPFLauncher {
  
  public static final String VM_ARGS = "jpf.vm_args";
  public static final String ARGS = "jpf.args";
  public static final String SITE_PROPERTIES_PATH = "jpf.site_properties_path";
  public static final String PORT = "jpf.port"; 
  
  private PrintWriter out;
  
  public void launch(File file){
    //Handle JPF's IO
    MessageConsole io = new MessageConsole("JPF", null);
    ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { io });
    MessageConsoleStream stream = io.newMessageStream();
    out = new PrintWriter(stream, true);
    super.launch(file);
  }
  
  @Override
  protected PrintWriter getOutputStream() {
    return out;
  }

  @Override
  protected PrintWriter getErrorStream() {
    return out;
  }

  @Override
  protected void gotoSource(final String filepath, final int line) {
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try{
          IFileStore file = EFS.getLocalFileSystem().getStore(new Path(filepath));
          IEditorPart part = IDE.openEditorOnFileStore(page, file);
          if ( line > 0 && part instanceof ITextEditor){
            ITextEditor editor = (ITextEditor) part;
            IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());      
            IRegion fileLine = doc.getLineInformation(line);
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

  @Override
  protected int getPort() {
    return new Integer(EclipseJPF.getDefault().getPluginPreferences().getString(PORT));
  }

  @Override
  protected String getVMArgs(String def) {
    return EclipseJPF.getDefault().getPluginPreferences().getString(VM_ARGS);
  }

  @Override
  protected String getArgs(String def) {
    return EclipseJPF.getDefault().getPluginPreferences().getString(ARGS);
  }

  @Override
  protected String getSitePropertiesPath(String def) {
    return EclipseJPF.getDefault().getPluginPreferences().getString(SITE_PROPERTIES_PATH);
  }

}
