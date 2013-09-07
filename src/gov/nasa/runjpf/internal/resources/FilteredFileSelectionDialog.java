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
 * @author Alexey Prybytkouski
 * @author stepan
 */
public class FilteredFileSelectionDialog extends ElementTreeSelectionDialog {

  private String[] filteredExtensions;

  private static class ExtensionFilterFileContentenProvider implements ITreeContentProvider {
    String[] extensions;

    public ExtensionFilterFileContentenProvider(String[] extensions) {
      super();
      this.extensions = extensions;
    }

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
              // recursively detect whether the folder contain a file with desired extensions
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

    public Object getParent(Object element) {
      return ((IResource) element).getParent();
    }

    public boolean hasChildren(Object element) {
      return element instanceof IContainer;
    }

    public Object[] getElements(Object input) {
      return (Object[]) input;
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
  };

  private static final IStatus OK = new Status(IStatus.OK, EclipseJPF.PLUGIN_ID, 0, "", null);
  private static final IStatus ERROR = new Status(IStatus.ERROR, EclipseJPF.PLUGIN_ID, 0, "", null);

  /*
   * Validator
   */
  private ISelectionStatusValidator validator = new ISelectionStatusValidator() {
    public IStatus validate(Object[] selection) {
      return selection.length == 1 && selection[0] instanceof IFile && validExtension(((IFile) selection[0]).getFileExtension()) ? OK
          : ERROR;
    }
  };

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

  public FilteredFileSelectionDialog(String title, String message, String[] type) {
    this(Display.getDefault().getActiveShell(), WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider(),
        new ExtensionFilterFileContentenProvider(type));
    this.filteredExtensions = type;

    setComparator(new FileViewerComparator());
    setTitle(title);
    setMessage(message);

    setInput(computeInput());
    setValidator(validator);
  }

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