package gov.nasa.runjpf.launching;

import gov.nasa.runjpf.EclipseJPF;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaLaunchTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

public class JPFRunTab extends JavaLaunchTab {
	public static final String JPF_FILE_LOCATION = "JPF_FILE";
	private Text jpfFileLocationText;
	
	/**
	 * If it's modified , just update the configuration directly.
	 */
	private class UpdateModfiyListener implements ModifyListener {
		public void modifyText(ModifyEvent e) {
			updateLaunchConfigurationDialog();
		}
	}
	
	private UpdateModfiyListener updatedListener = new UpdateModfiyListener();
	private Group basicConfiguraionGroup;

	/**
	 * @wbp.parser.entryPoint
	 */
	@Override
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setFont(parent.getFont());

		GridData gd = new GridData(1);
		gd.horizontalSpan = GridData.FILL_BOTH;
		comp.setLayoutData(gd);

		GridLayout layout = new GridLayout(1, false);
		layout.verticalSpacing = 0;
		comp.setLayout(layout);
		
		createBasicConfigurationGroup(comp);
		
		setControl(comp);
		    
		return;
		
	}
	
	
	private void createBasicConfigurationGroup(Composite parent){
		Font font = parent.getFont();

		/*
		 * ---------------------------------------------------------------------
		 */

		basicConfiguraionGroup = new Group(parent, SWT.NONE);
		basicConfiguraionGroup.setText("JPF Verification basic configuration");
		basicConfiguraionGroup.setLayout(new GridLayout(3, false));
		basicConfiguraionGroup.setLayoutData(createHFillGridData());
		basicConfiguraionGroup.setFont(font);
 
		Link link_1 = new Link(basicConfiguraionGroup, 0);
		link_1.setToolTipText("Open editor for user settings");
		link_1.setText("JPF &File to execute (*.jpf):");
		link_1.setBounds(10, 14, 370, 15);
		new Label(basicConfiguraionGroup, SWT.NONE);
		new Label(basicConfiguraionGroup, SWT.NONE);
		
		jpfFileLocationText = new Text(basicConfiguraionGroup, SWT.BORDER);
		jpfFileLocationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
		jpfFileLocationText.addModifyListener(updatedListener);
		jpfFileLocationText.setBounds(10, 35, 524, 21);
		
		Button button = new Button(basicConfiguraionGroup, SWT.NONE);
		button.setText("&Browse...");
		button.setBounds(540, 33, 71, 25);
		
		button.addSelectionListener(new SelectionAdapter() {
		      public void widgetSelected(SelectionEvent e) {
		        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
		        dialog.setFilterExtensions(new String[] {"*.jpf"});
		        if(getJpfFileLocation().length() > 0) {
		          dialog.setFileName(getJpfFileLocation());
		        }
		        String file = dialog.open();
		        if(file != null) {
		          file = file.trim();
		          if(file.length() > 0) {
		            jpfFileLocationText.setText(file);
		            setDirty(true);
		          }
		        }
		      }
		    });
	}
	
	private GridData createHFillGridData() {
		GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = true;
		return gd;
	}

	
	public static void initDefaultConfiguration(
			ILaunchConfigurationWorkingCopy configuration, String projectName,
			String launchConfigName) {

		configuration.setAttribute(
				IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
				EclipseJPF.JPF_MAIN_CLASS);
		
		configuration.setAttribute(
				JPF_FILE_LOCATION, "");
	}

	public void initializeFrom(ILaunchConfiguration configuration) {

		try {
			jpfFileLocationText.setText(configuration.getAttribute(
					JPF_FILE_LOCATION, ""));
		} catch (CoreException e) {
			EclipseJPF.logError("Error during the JPF initialization form", e);
		}
		
		super.initializeFrom(configuration);
	}

	String getJpfFileLocation() {
	    return jpfFileLocationText.getText().trim();
	  }
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "JPF Run";
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(JPF_FILE_LOCATION,
				jpfFileLocationText.getText());
		
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy arg0) {
		System.out.println("DEFAULTS");
	}
}
