package com.javapathfinder.eclipsejpf.options;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.javapathfinder.eclipsejpf.EclipseJPF;
import com.javapathfinder.eclipsejpf.EclipseJPFLauncher;

public class Preferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{
	
	public Preferences() {
		super(GRID);
		setPreferenceStore(EclipseJPF.getDefault().getPreferenceStore());
		setDescription("Set the properties for Eclipse-JPF");
	}

	@Override
	protected void createFieldEditors() {
	  IntegerFieldEditor port = new IntegerFieldEditor(EclipseJPFLauncher.PORT,"Shell Port Number:",getFieldEditorParent());
	  FileFieldEditor sitePropertiesPath = new FileFieldEditor(EclipseJPFLauncher.SITE_PROPERTIES_PATH, "Path to site.properties", getFieldEditorParent());
	  StringFieldEditor args = new StringFieldEditor(EclipseJPFLauncher.ARGS, "JPF's Arguments", getFieldEditorParent());
	  StringFieldEditor vm_args = new StringFieldEditor(EclipseJPFLauncher.VM_ARGS, "JPF's Host &VM Arguements",getFieldEditorParent());
	  
	  addField(port);
	  addField(sitePropertiesPath);
	  addField(args);
	  addField(vm_args);
	}

	@Override
	public void init(IWorkbench workbench) {
		
	}

}
