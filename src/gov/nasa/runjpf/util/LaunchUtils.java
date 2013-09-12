package gov.nasa.runjpf.util;

import gov.nasa.runjpf.EclipseJPF;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Utility class for getting the file that is clicked on.
 * 
 */
public class LaunchUtils {

  /* it's a util class */
  private LaunchUtils() {
  };

  /**
   * This converts a selection (which is selected by user mouse click) into a
   * resource.<br/>
   * A common usage would be to call this method when user selects a file in a
   * tree explorer.
   * 
   * @param selection
   *          Where user clicked.
   * @return A {@link IResource} instance.
   */
  public static IResource getSelectedResource(ISelection selection) {

    if (selection instanceof TreeSelection) {// could be project explorer
      Object selectedElement = ((TreeSelection) selection).getFirstElement();

      if (selectedElement instanceof IJavaElement) {
        return ((IJavaElement) selectedElement).getResource();
      } else if (selectedElement instanceof IResource)
        return (IResource) selectedElement;
      else if (selectedElement instanceof IProject) {
        try {
          return ((IProject) selectedElement).members()[0];
        } catch (CoreException e) {
          EclipseJPF.logError("An error occured while getting members of selection!", e);
          return null;
        }
      }
    }

    return null;
  }

  /**
   * Get a file that is opened in the editor.<br/>
   * A common usage would be to get a file that is opened in the editor and user
   * uses a right mouse click in the opened editor.
   * 
   * @param editorInput
   *          An editor input
   * @return The {@link IFile} instance that is opened in the editor.
   */
  public static IFile getFile(IEditorInput editorInput) {
    FileEditorInput fileEditorInput = (FileEditorInput) editorInput.getAdapter(FileEditorInput.class);

    if (fileEditorInput == null || fileEditorInput.getFile() == null) {
      return null;
    }
    return fileEditorInput.getFile();
  }
}
