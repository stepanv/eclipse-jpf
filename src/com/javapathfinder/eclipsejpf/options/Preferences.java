package com.javapathfinder.eclipsejpf.options;

import java.io.File;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.javapathfinder.eclipsejpf.EclipseJPF;
import com.javapathfinder.eclipsejpf.EclipseJPFLauncher;

public class Preferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{
	
	public Preferences() {
		super(GRID);
		setPreferenceStore(EclipseJPF.getDefault().getPreferenceStore());
		setDescription("Set the properties for Eclipse JPF");
	}

	@Override
	protected void createFieldEditors() {
	  String[][] options = {
	                          {"Use included JPF binary (Default)",EclipseJPFLauncher.INTERNAL},
	                          {"Use JPF installation described by " + 
	                            System.getProperty("user.home") + 
	                            File.separator + ".jpf" + File.separator + "site.properties", 
	                            EclipseJPFLauncher.EXTERNAL
	                           }
	                       };
	  RadioGroupFieldEditor runMode = new RadioGroupFieldEditor(
	      EclipseJPFLauncher.RUN_MODE,"Set JPF's Run Mode", 
	      1, 
	      options,
	      getFieldEditorParent());
	  
	  addField(runMode);
		addField(new StringFieldEditor(
		    EclipseJPFLauncher.VM_ARGS, 
		    "JPF's Host &VM Arguements", 
		    getFieldEditorParent()));
	}

	@Override
	public void init(IWorkbench workbench) {
		
	}

}
