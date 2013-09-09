package gov.nasa.runjpf.options;

import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.EclipseJPFLauncher;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * The <code>Preferences</code> class represents the PreferencePage for this
 * plugin. All of the keys for the preferences stored can be retrived from the
 * {@link EclipseJPFLauncher} class.
 * 
 * @author sandro
 * 
 */
public class Preferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  public Preferences() {
    super(GRID);
    setPreferenceStore(EclipseJPF.getDefault().getPreferenceStore());
    setDescription("Set the properties for Eclipse-JPF");
  }

  @Override
  protected void createFieldEditors() {
    IntegerFieldEditor port = new IntegerFieldEditor(EclipseJPFLauncher.PORT, "Shell Port Number:", getFieldEditorParent());
    FileFieldEditor sitePropertiesPath = new FileFieldEditor(EclipseJPFLauncher.SITE_PROPERTIES_PATH, "Path to site.properties",
        getFieldEditorParent());
    StringFieldEditor args = new StringFieldEditor(EclipseJPFLauncher.ARGS, "JPF's Arguments", getFieldEditorParent());
    StringFieldEditor vm_args = new StringFieldEditor(EclipseJPFLauncher.VM_ARGS, "JPF's Host &VM Arguements", getFieldEditorParent());

    addField(port);
    addField(sitePropertiesPath);
    addField(args);
    addField(vm_args);
    
    addField(new StringFieldEditor(EclipseJPFLauncher.COMMON_DIR, "Common dir for traces", getFieldEditorParent()));
  }

  @Override
  public void init(IWorkbench workbench) {

  }

}
