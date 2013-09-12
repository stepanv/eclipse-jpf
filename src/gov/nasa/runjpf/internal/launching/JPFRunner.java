package gov.nasa.runjpf.internal.launching;

import gov.nasa.runjpf.JPFProcessConsoleTracker;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.internal.launching.StandardVMRunner;
import org.eclipse.jdt.launching.IVMInstall;

/**
 * JPF runner that overrides on top of standard runner functionality the process
 * identification so that it can be used by JPF specific console trackers.
 * 
 * @author stepan
 * 
 */
@SuppressWarnings("restriction")
public class JPFRunner extends StandardVMRunner {

  /**
   * Creates {@link JPFRunner} instance
   * 
   * @param vmInstance
   */
  public JPFRunner(IVMInstall vmInstance) {
    super(vmInstance);
  }

  /**
   * This is how the {@link StandardVMRunner} is forced to use the JPF specific
   * process identification.<br/>
   * This is used by the {@link JPFProcessConsoleTracker}.
   */
  @Override
  protected Map<String, String> getDefaultProcessMap() {
    return jpfProcessDefaultMap();
  }

  /**
   * Creates process map for JPF processes with new ID
   * <tt>gov.nasa.jpf.ui.jpfProcess</tt>.
   * 
   * @return The map
   */
  static Map<String, String> jpfProcessDefaultMap() {
    Map<String, String> map = new HashMap<String, String>();
    map.put(IProcess.ATTR_PROCESS_TYPE, "gov.nasa.jpf.ui.jpfProcess");
    return map;
  }

}
