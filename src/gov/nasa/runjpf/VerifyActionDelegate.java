package gov.nasa.runjpf;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * The <code>VerifyActionDelegate</code> class is called by eclipse-jpf plugin
 * when the user selects "Verify..." from the menu.
 * @author sandro
 *
 */
public class VerifyActionDelegate implements IObjectActionDelegate {
	
	private IFile file = null;

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {}

	/**
	 * Schedules a RunJPF job to be run
	 * @see RunJPF
	 */
	@Override
	public void run(IAction action) {
		if (file == null)
			return;
		new RunJPF(file).schedule();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (!(selection instanceof TreeSelection))
		      return;
		TreeSelection s = (TreeSelection) selection;
		file = (IFile) s.getFirstElement();  
		
	}

}
