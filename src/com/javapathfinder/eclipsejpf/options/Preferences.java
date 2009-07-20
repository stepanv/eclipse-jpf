package com.javapathfinder.eclipsejpf.options;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.javapathfinder.eclipsejpf.Activator;
import com.javapathfinder.eclipsejpf.RunJPF;

public class Preferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{
	
	public Preferences() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Set the properties for Eclipse JPF");
	}

	@Override
	protected void createFieldEditors() {
		addField(new StringFieldEditor(RunJPF.VM_ARGS, "JPF's Host &VM Arguements", getFieldEditorParent()));
		
	}

	@Override
	public void init(IWorkbench workbench) {
		
	}

}
