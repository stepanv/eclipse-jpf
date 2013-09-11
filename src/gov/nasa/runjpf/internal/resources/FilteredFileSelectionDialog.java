package gov.nasa.runjpf.internal.resources;

import gov.nasa.runjpf.EclipseJPF;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Workspace relative extension based filtering file selection dialog.<br/>
 * 
 * @see <a
 *      href="http://stackoverflow.com/questions/14750712/swt-component-for-choose-file-only-from-workspace">http://stackoverflow.com/questions/14750712/swt-component-for-choose-file-only-from-workspace</a>
 * @author Alexey Prybytkouski
 * @author stepan
 */
public class FilteredFileSelectionDialog extends ElementTreeSelectionDialog {

  private String[] filteredExtensions;

  /**
   * This content provided recursively hides subfolders if they don't contain
   * desired extensions.
   * 
   * @author stepan
   * 
   */
  private static class ExtensionFilterFileContentenProvider implements ITreeContentProvider {
    String[] extensions;

    /**
     * Creates a content provider with enabled filtering for the provided
     * extensions.<br/>
     * 
     * 
     * @param extensions
     *          An array of extensions (without a dot) such as:
     *          <tt>new String[] {"txt", "properties", "java"}</tt> or null if
     *          no filtering.
     */
    public ExtensionFilterFileContentenProvider(String[] extensions) {
      super();
      this.extensions = extensions;
    }

    @Override
    public Object[] getChildren(Object element) {
      if (element instanceof IContainer) {
        try {
          List<Object> objects = new LinkedList<>();
          for (IResource resource : ((IContainer) element).members()) {
            if (resource instanceof IFile) {
              // filter the extension too
              IFile file = (IFile) resource;
              String extension = file.getFileExtension();

              if (validExtension(extensions, extension)) {
                objects.add(resource);
              }
            } else if (resource instanceof IFolder) {
              // recursively detect whether the folder contain a file with
              // desired extensions
              Object[] children = getChildren(resource);
              if (children != null && children.length > 0) {
                objects.add(resource);
              }
            } else {
              objects.add(resource);
            }
          }
          return objects.toArray();
        } catch (CoreException e) {
        }
      }
      return null;
    }

    @Override
    public Object getParent(Object element) {
      return ((IResource) element).getParent();
    }

    @Override
    public boolean hasChildren(Object element) {
      return element instanceof IContainer;
    }

    @Override
    public Object[] getElements(Object input) {
      return (Object[]) input;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
  };

  private static final IStatus OK = new Status(IStatus.OK, EclipseJPF.PLUGIN_ID, 0, "", null);
  private static final IStatus ERROR = new Status(IStatus.ERROR, EclipseJPF.PLUGIN_ID, 0, "", null);

  /**
   * Validator
   */
  private ISelectionStatusValidator validator = new ISelectionStatusValidator() {
    public IStatus validate(Object[] selection) {
      return selection.length == 1 && selection[0] instanceof IFile && validExtension(((IFile) selection[0]).getFileExtension()) ? OK
          : ERROR;
    }
  };

  /**
   * This view comparator gives the priority to the folders the be shown before
   * files.
   * 
   * @author stepan
   * 
   */
  private static class FileViewerComparator extends ViewerComparator {
    @Override
    public int category(Object element) {
      if (element instanceof IFile) {
        // files are returned after directories
        return 1;
      }
      return 0;
    }
  }

  /**
   * Convenient constructor for easy of use.
   * 
   * @param title
   *          Title of the dialog window.
   * @param message
   *          Message to show above the file tree selection subwindow.
   * @param extensions
   *          Extensions to be shown or null for no filtering
   */
  public FilteredFileSelectionDialog(String title, String message, String[] extensions) {
    this(Display.getDefault().getActiveShell(), WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider(),
        new ExtensionFilterFileContentenProvider(extensions));
    this.filteredExtensions = extensions;

    setComparator(new FileViewerComparator());
    setTitle(title);
    setMessage(message);

    setInput(computeInput());
    setValidator(validator);
  }

  /**
   * Generic constructor.
   */
  public FilteredFileSelectionDialog(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider) {
    super(parent, labelProvider, contentProvider);
  }

  /*
   * Show projects
   */
  private Object[] computeInput() {
    /*
     * Refresh projects tree.
     */
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    for (int i = 0; i < projects.length; i++) {
      try {
        projects[i].refreshLocal(IResource.DEPTH_INFINITE, null);
      } catch (CoreException e) {
        e.printStackTrace();
      }
    }

    try {
      ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_ONE, null);
    } catch (CoreException e) {
    }
    List<IProject> openProjects = new ArrayList<IProject>(projects.length);
    for (int i = 0; i < projects.length; i++) {
      if (projects[i].isOpen()) {
        openProjects.add(projects[i]);
      }
    }
    return openProjects.toArray();
  }

  /*
   * Check file extension
   */
  private boolean validExtension(String name) {
    return validExtension(filteredExtensions, name);
  }

  private static boolean validExtension(String[] filteredExtensions, String extension) {
    if (extension == null) {
      // file has no extension
      if (filteredExtensions != null && filteredExtensions.length > 0) {
        // filtered extensions were provided
        return false;
      } else {
        // filtered extensions were not provided
        return true;
      }
    }
    if (extension.equals("*") || filteredExtensions == null) {
      return true;
    }

    for (int i = 0; i < filteredExtensions.length; i++) {
      if (filteredExtensions[i].equals(extension)) {
        return true;
      }
    }
    return false;
  }
}