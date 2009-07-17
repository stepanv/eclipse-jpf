package com.javapathfinder.eclipsejpf;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;


public class VerifyActionDelegate implements IObjectActionDelegate {
	
	private IFile file = null;

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {}

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
