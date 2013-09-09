package gov.nasa.runjpf.internal.launching;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.internal.launching.StandardVMRunner;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;

@SuppressWarnings("restriction")
public class JPFRunner extends StandardVMRunner {

  public JPFRunner(IVMInstall vmInstance) {
    super(vmInstance);
  }
  
  @Override
  protected Map<String, String> getDefaultProcessMap() {
    return jpfProcessDefaultMap();
  }
  
  static Map<String, String> jpfProcessDefaultMap() {
    Map<String, String> map = new HashMap<String, String>();
    map.put(IProcess.ATTR_PROCESS_TYPE, "gov.nasa.jpf.ui.jpfProcess");
    return map;
  }

}
