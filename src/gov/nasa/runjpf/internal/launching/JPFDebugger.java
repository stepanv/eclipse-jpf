/**
 * 
 */
package gov.nasa.runjpf.internal.launching;

import gov.nasa.runjpf.JPFProcessConsoleTracker;

import java.util.Map;

import org.eclipse.jdt.internal.launching.StandardVMDebugger;
import org.eclipse.jdt.launching.IVMInstall;

/**
 * This is just for standard debugging that is a debugging of JPF as a java
 * program itself.<br/>
 * Just a custom JPF Specific console tracker is used thanks to
 * {@link JPFRunner#jpfProcessDefaultMap()}.
 * 
 * @author stepan
 * 
 */
@SuppressWarnings("restriction")
public class JPFDebugger extends StandardVMDebugger {

  /**
   * @param vmInstance
   */
  public JPFDebugger(IVMInstall vmInstance) {
    super(vmInstance);
  }

  /**
   * This is how the {@link StandardVMDebugger} is forced to use the JPF
   * specific process identification.<br/>
   * This is used by the {@link JPFProcessConsoleTracker}.
   */
  @Override
  protected Map<String, String> getDefaultProcessMap() {
    return JPFRunner.jpfProcessDefaultMap();
  }

}
