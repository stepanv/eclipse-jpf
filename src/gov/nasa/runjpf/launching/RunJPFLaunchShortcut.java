package gov.nasa.runjpf.launching;

import java.io.File;

import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.util.ProjectUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;


public class RunJPFLaunchShortcut implements ILaunchShortcut, IExecutableExtension {

	private static final String JPF_CONFIGURATION_TYPE_STRING = "eclipse-jpf.launching.runJpf";
	private boolean showDialog = false;

	/**
	 * @param showDialog the showDialog to set
	 */
	public void setShowDialog(boolean showDialog) {
		this.showDialog = showDialog;
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
	    if("WITH_DIALOG".equals(data)) { //$NON-NLS-1$
	      this.showDialog  = true;
	    }
	  }
	
	private ILaunchConfiguration findOrCreateLaunchConfiguration(IResource ir) {
		ILaunchConfiguration launchConfiguration = findLaunchConfiguration(ir);
		if (launchConfiguration == null) {
			launchConfiguration = createConfiguration(ir);
		}
		return launchConfiguration;
	}
	
	@Override
	public void launch(ISelection selection, String mode) {
		IResource ir = getLaunchableResource(selection);
		launch(findOrCreateLaunchConfiguration(ir), mode);
	}

	@Override
	public void launch(IEditorPart editor, String mode) {
		launch(findOrCreateLaunchConfiguration(getLaunchableResource(editor)), mode);
	}
	
	public ILaunchConfiguration findLaunchConfiguration(IResource type) {
		if(type == null ) return null;
		
		if (type instanceof IFile) {
			File selectedFile = ((IFile)type).getLocation().toFile();
			
			try {
				ILaunchConfiguration[] configs = DebugPlugin.getDefault().
					getLaunchManager().getLaunchConfigurations();
				for (ILaunchConfiguration config : configs) {
	
					if(isJpfRunConfiguration(config)){
						String currentProejctName = config.getAttribute(JPFRunTab.JPF_FILE_LOCATION, "");
						File foundFile = new File(currentProejctName);
						if (foundFile.equals(selectedFile)) {
							return config;
						}
					}
				}
			} catch (CoreException e) {
			}
		}
		return null;
	}

	private boolean isJpfRunConfiguration(ILaunchConfiguration config) throws CoreException{
		String mainType = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,"");
		return	EclipseJPF.JPF_MAIN_CLASS.equals(mainType);
	}

	private ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}

	public ILaunchConfigurationType getConfigurationType(){
		return getLaunchManager().getLaunchConfigurationType(JPF_CONFIGURATION_TYPE_STRING);
	}
	public ILaunchConfiguration createConfiguration(IResource type){
		if( type == null ) return null;
		
		if (type instanceof IFile) {
			String typeName = ((IFile)type).getName();

		ILaunchConfiguration config = null;
		ILaunchConfigurationWorkingCopy wc = null;
		try {
			ILaunchConfigurationType configType = getConfigurationType();

				String launchConfigName = getLaunchManager().
					generateLaunchConfigurationName(typeName);

				wc = configType.newInstance(null, launchConfigName);

				JPFRunTab.initDefaultConfiguration(wc, null, launchConfigName);
				
				wc.setAttribute(
						JPFRunTab.JPF_FILE_LOCATION, ((IFile)type).getLocation().toFile().getAbsolutePath());

				//set mapped resource , let next time we could execute this directly from menuitem.
				wc.setMappedResources(new IResource[] {type});
				config = wc.doSave();
		} catch (CoreException exception) {
			showError( exception.getStatus().getMessage());
		}
		return config;
		}
		return null;
	}

	private void showError(String message){
		MessageDialog.openError(getActiveShell(),"Error when startup JPF Verification",
				message);
	}
	private Shell getActiveShell(){
		IWorkbenchWindow win = EclipseJPF.getDefault().getWorkbench().getActiveWorkbenchWindow();

		if(win ==null) return null;

		return win.getShell();
	}
	

	public void launch(ILaunchConfiguration launchConfiguration, String mode){
		if (launchConfiguration == null) {
			return;
		}
		
		if(showDialog) {
	      DebugUITools.saveBeforeLaunch();
	      ILaunchGroup group = DebugUITools.getLaunchGroup(launchConfiguration, mode);
	      DebugUITools.openLaunchConfigurationDialog(getActiveShell(), launchConfiguration,
	    		  group.getIdentifier(), null);
	    } else {
	      DebugUITools.launch(launchConfiguration, mode);
	    }
	}

	public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
		ILaunchConfiguration launchconf = findLaunchConfiguration(getLaunchableResource(selection));
		if(launchconf == null) return null;
		return new ILaunchConfiguration[]{launchconf};
	}

	public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editorpart) {
		ILaunchConfiguration launchconf = findLaunchConfiguration(getLaunchableResource(editorpart));
		if(launchconf == null) return null;
		return new ILaunchConfiguration[]{launchconf};
	}

	public IResource getLaunchableResource(ISelection selection) {
		return getLaunchableResource(ProjectUtil.getSelectedResource(selection));
	}

	public IResource getLaunchableResource(IEditorPart editorpart) {
		return getLaunchableResource(ProjectUtil.getFile(editorpart
				.getEditorInput()));
	}

	private IResource getLaunchableResource(IResource ir) {
		if (ir == null )
			return null;

		return ir;
	}



}
