/**
 * 
 */
package gov.nasa.runjpf.internal.breakpoints;

import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.internal.debug.core.breakpoints.JavaExceptionBreakpoint;

/**
 * The class {@link SuspendVMExceptionBreakpoint} preserves all the
 * functionality of {@link JavaExceptionBreakpoint} except for the suspend
 * policy behavior which is always <i>Suspend the whole VM</i> in this class.
 * 
 * @author stepan
 * 
 */
@SuppressWarnings("restriction")
public class SuspendVMExceptionBreakpoint extends JavaExceptionBreakpoint {

  public SuspendVMExceptionBreakpoint() {
    super();
  }

  /**
   * Creates and returns an exception breakpoint for the given (throwable) type.
   * Caught and uncaught specify where the exception should cause thread
   * suspensions - that is, in caught and/or uncaught locations. Checked
   * indicates if the given exception is a checked exception.
   * 
   * @param resource
   *          the resource on which to create the associated breakpoint marker
   * @param exceptionName
   *          the fully qualified name of the exception for which to create the
   *          breakpoint
   * @param caught
   *          whether to suspend in caught locations
   * @param uncaught
   *          whether to suspend in uncaught locations
   * @param checked
   *          whether the exception is a checked exception
   * @param add
   *          whether to add this breakpoint to the breakpoint manager
   * @return a Java exception breakpoint
   * @exception DebugException
   *              if unable to create the associated marker due to a lower level
   *              exception.
   */
  public SuspendVMExceptionBreakpoint(IResource resource, String exceptionName, boolean caught, boolean uncaught, boolean checked,
                                      boolean add, Map<String, Object> attributes) throws DebugException {
    super(resource, exceptionName, caught, uncaught, checked, add, attributes);
  }

  @Override
  protected int getDefaultSuspendPolicy() {
    return SUSPEND_VM;
  }

}
