package gov.nasa.runjpf.internal.launching;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.internal.launching.LaunchingMessages;
import org.eclipse.jdt.internal.launching.LaunchingPlugin;
import org.eclipse.jdt.internal.launching.LibraryInfo;
import org.eclipse.jdt.internal.launching.StandardVM;
import org.eclipse.jdt.internal.launching.StandardVMDebugger;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.ListeningConnector;

@SuppressWarnings("restriction")
public class JPFDebugger extends StandardVMDebugger {

  private boolean debugVM;

  public JPFDebugger(IVMInstall vmInstance, boolean debugVM) {
    super(vmInstance);
    this.debugVM = debugVM;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jdt.launching.IVMRunner#run(org.eclipse.jdt.launching.
   * VMRunnerConfiguration, org.eclipse.debug.core.ILaunch,
   * org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public void run(VMRunnerConfiguration config, ILaunch launch, IProgressMonitor monitor) throws CoreException {

    if (monitor == null) {
      monitor = new NullProgressMonitor();
    }

    IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
    subMonitor.beginTask(LaunchingMessages.StandardVMDebugger_Launching_VM____1, 4);
    subMonitor.subTask(LaunchingMessages.StandardVMDebugger_Finding_free_socket____2);

    int port = SocketUtil.findFreePort();
    if (port == -1) {
      abort(LaunchingMessages.StandardVMDebugger_Could_not_find_a_free_socket_for_the_debugger_1, null,
            IJavaLaunchConfigurationConstants.ERR_NO_SOCKET_AVAILABLE);
    }
    int portDebugVM = 0;
    if (debugVM) {
      portDebugVM = SocketUtil.findFreePort();
      if (portDebugVM == -1) {
        abort(LaunchingMessages.StandardVMDebugger_Could_not_find_a_free_socket_for_the_debugger_1, null,
          IJavaLaunchConfigurationConstants.ERR_NO_SOCKET_AVAILABLE);
      }
    }

    subMonitor.worked(1);

    // check for cancellation
    if (monitor.isCanceled()) {
      return;
    }

    subMonitor.subTask(LaunchingMessages.StandardVMDebugger_Constructing_command_line____3);

    String program = constructProgramString(config);

    List<String> arguments = new ArrayList<String>(12);

    arguments.add(program);

    if (debugVM) {
      if (fVMInstance instanceof StandardVM && ((StandardVM) fVMInstance).getDebugArgs() != null) {
        String debugArgString = ((StandardVM) fVMInstance).getDebugArgs()
            .replaceAll("\\Q" + StandardVM.VAR_PORT + "\\E", Integer.toString(portDebugVM)); //$NON-NLS-1$ //$NON-NLS-2$
        String[] debugArgs = DebugPlugin.parseArguments(debugArgString);
        for (int i = 0; i < debugArgs.length; i++) {
          arguments.add(debugArgs[i]);
        }
      } else {
        // VM arguments are the first thing after the java program so that users
        // can specify
        // options like '-client' & '-server' which are required to be the first
        // options
        double version = getJavaVersion();
        if (version < 1.5) {
          arguments.add("-Xdebug"); //$NON-NLS-1$
          arguments.add("-Xnoagent"); //$NON-NLS-1$
        }
  
        // check if java 1.4 or greater
        if (version < 1.4) {
          arguments.add("-Djava.compiler=NONE"); //$NON-NLS-1$
        }
        if (version < 1.5) {
          arguments.add("-Xrunjdwp:transport=dt_socket,suspend=y,address=localhost:" + portDebugVM); //$NON-NLS-1$
        } else {
          arguments.add("-agentlib:jdwp=transport=dt_socket,suspend=y,address=localhost:" + portDebugVM); //$NON-NLS-1$
        }
  
      }
    }

    String[] allVMArgs = combineVmArgs(config, fVMInstance);
    addArguments(ensureEncoding(launch, allVMArgs), arguments);
    addBootClassPathArguments(arguments, config);

    String[] cp = config.getClassPath();
    int cpidx = -1;
    if (cp.length > 0) {
      cpidx = arguments.size();
      arguments.add("-classpath"); //$NON-NLS-1$
      arguments.add(convertClassPath(cp));
    }

    arguments.add(config.getClassToLaunch());
    addArguments(config.getProgramArguments(), arguments);

    addArguments(new String[] { "++listener=gov.nasa.jpf.jdwp.JDWPListener,",
        "+jpf-jdwp.jdwp=transport=dt_socket,server=n,suspend=y,address=" + port }, arguments);

    // With the newer VMs and no backwards compatibility we have to always
    // prepend the current env path (only the runtime one)
    // with a 'corrected' path that points to the location to load the debug
    // dlls from, this location is of the standard JDK installation
    // format: <jdk path>/jre/bin
    String[] envp = prependJREPath(config.getEnvironment(), new Path(program));

    String[] newenvp = checkClasspathPrivate(arguments, cp, envp);
    if (newenvp != null) {
      envp = newenvp;
      arguments.remove(cpidx);
      arguments.remove(cpidx);
    }

    String[] cmdLine = new String[arguments.size()];
    arguments.toArray(cmdLine);

    // check for cancellation
    if (monitor.isCanceled()) {
      return;
    }

    subMonitor.worked(1);
    subMonitor.subTask(LaunchingMessages.StandardVMDebugger_Starting_virtual_machine____4);

    ListeningConnector connector = getConnector();
    if (connector == null) {
      abort(LaunchingMessages.StandardVMDebugger_Couldn__t_find_an_appropriate_debug_connector_2, null,
            IJavaLaunchConfigurationConstants.ERR_CONNECTOR_NOT_AVAILABLE);
    }
    Map<String, Connector.Argument> map = connector.defaultArguments();

    specifyArguments(map, port);
    
    ListeningConnector connectorDebugVM = null;
    Map<String, Connector.Argument> mapDebugVM = null;
    if (debugVM) {
      connectorDebugVM = getConnector();
      if (connectorDebugVM == null) {
        abort(LaunchingMessages.StandardVMDebugger_Couldn__t_find_an_appropriate_debug_connector_2, null,
              IJavaLaunchConfigurationConstants.ERR_CONNECTOR_NOT_AVAILABLE);
      }
      mapDebugVM = connectorDebugVM.defaultArguments();
  
      specifyArguments(mapDebugVM, portDebugVM);
    }
    Process p = null;
    try {
      try {
        // check for cancellation
        if (monitor.isCanceled()) {
          return;
        }

        connector.startListening(map);
        if (debugVM) {
          connectorDebugVM.startListening(mapDebugVM);
        }

        File workingDir = getWorkingDir(config);
        p = exec(cmdLine, workingDir, envp);
        if (p == null) {
          return;
        }

        // check for cancellation
        if (monitor.isCanceled()) {
          p.destroy();
          return;
        }

        String timestamp = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(new Date(System.currentTimeMillis()));
        IProcess process = newProcess(launch, p, renderProcessLabel(cmdLine, timestamp), getDefaultProcessMap());
        process.setAttribute(IProcess.ATTR_CMDLINE, renderCommandLine(cmdLine));
        subMonitor.worked(1);
        subMonitor.subTask(LaunchingMessages.StandardVMDebugger_Establishing_debug_connection____5);
        boolean retry = false;
        do {
          try {
            ConnectRunnable runnableDebugVM = null;
            if (debugVM) {
              runnableDebugVM = new ConnectRunnable(connectorDebugVM, mapDebugVM);
              Thread connectThreadDebugVM = new Thread(runnableDebugVM, "Listening Connector DebugVM"); //$NON-NLS-1$
              connectThreadDebugVM.setDaemon(true);
              connectThreadDebugVM.start();
              while (connectThreadDebugVM.isAlive()) {
                if (monitor.isCanceled()) {
                  try {
                    connectorDebugVM.stopListening(mapDebugVM);
                  } catch (IOException ioe) {
                    // expected
                  }
                  p.destroy();
                  return;
                }
                try {
                  p.exitValue();
                  // process has terminated - stop waiting for a connection
                  try {
                    connectorDebugVM.stopListening(mapDebugVM);
                  } catch (IOException e) {
                    // expected
                  }
                  checkErrorMessage(process);
                } catch (IllegalThreadStateException e) {
                  // expected while process is alive
                }
                try {
                  Thread.sleep(100);
                } catch (InterruptedException e) {
                }
              }
              
              Exception ex = runnableDebugVM.getException();
              if (ex instanceof IllegalConnectorArgumentsException) {
                throw (IllegalConnectorArgumentsException) ex;
              }
              if (ex instanceof InterruptedIOException) {
                throw (InterruptedIOException) ex;
              }
              if (ex instanceof IOException) {
                throw (IOException) ex;
              }
              
              VirtualMachine vmDebugVM = runnableDebugVM.getVirtualMachine();
              if (vmDebugVM != null) {
                createDebugTarget(config, launch, portDebugVM, process, vmDebugVM);
              }
            }
            
            ConnectRunnable runnable = new ConnectRunnable(connector, map);
            Thread connectThread = new Thread(runnable, "Listening Connector"); //$NON-NLS-1$
            connectThread.setDaemon(true);
            connectThread.start();
            while (connectThread.isAlive()) {
              if (monitor.isCanceled()) {
                try {
                  connector.stopListening(map);
                } catch (IOException ioe) {
                  // expected
                }
                p.destroy();
                return;
              }
              try {
                p.exitValue();
                // process has terminated - stop waiting for a connection
                try {
                  connector.stopListening(map);
                } catch (IOException e) {
                  // expected
                }
                checkErrorMessage(process);
              } catch (IllegalThreadStateException e) {
                // expected while process is alive
              }
              try {
                Thread.sleep(100);
              } catch (InterruptedException e) {
              }
            }

            Exception ex = runnable.getException();
            if (ex instanceof IllegalConnectorArgumentsException) {
              throw (IllegalConnectorArgumentsException) ex;
            }
            if (ex instanceof InterruptedIOException) {
              throw (InterruptedIOException) ex;
            }
            if (ex instanceof IOException) {
              throw (IOException) ex;
            }

            VirtualMachine vm = runnable.getVirtualMachine();
            if (vm != null) {
              createDebugTarget(config, launch, port, process, vm);
              subMonitor.worked(1);
              subMonitor.done();
            }
            return;
          } catch (InterruptedIOException e) {
            checkErrorMessage(process);

            // timeout, consult status handler if there is one
            IStatus status = new Status(IStatus.ERROR, LaunchingPlugin.getUniqueIdentifier(),
                IJavaLaunchConfigurationConstants.ERR_VM_CONNECT_TIMEOUT, "", e); //$NON-NLS-1$
            IStatusHandler handler = DebugPlugin.getDefault().getStatusHandler(status);

            retry = false;
            if (handler == null) {
              // if there is no handler, throw the exception
              throw new CoreException(status);
            }
            Object result = handler.handleStatus(status, this);
            if (result instanceof Boolean) {
              retry = ((Boolean) result).booleanValue();
            }
          }
        } while (retry);
      } finally {
        connector.stopListening(map);
        if (debugVM) {
          connectorDebugVM.stopListening(mapDebugVM);
        }
      }
    } catch (IOException e) {
      abort(LaunchingMessages.StandardVMDebugger_Couldn__t_connect_to_VM_4, e, IJavaLaunchConfigurationConstants.ERR_CONNECTION_FAILED);
    } catch (IllegalConnectorArgumentsException e) {
      abort(LaunchingMessages.StandardVMDebugger_Couldn__t_connect_to_VM_5, e, IJavaLaunchConfigurationConstants.ERR_CONNECTION_FAILED);
    }
    if (p != null) {
      p.destroy();
    }
  }

  /**
   * Used to attach to a VM in a separate thread, to allow for cancellation and
   * detect that the associated System process died before the connect occurred.
   */
  class ConnectRunnable implements Runnable {

    private VirtualMachine fVirtualMachine = null;
    private ListeningConnector fConnector = null;
    private Map<String, Connector.Argument> fConnectionMap = null;
    private Exception fException = null;

    /**
     * Constructs a runnable to connect to a VM via the given connector with the
     * given connection arguments.
     * 
     * @param connector
     *          the connector to use
     * @param map
     *          the argument map
     */
    public ConnectRunnable(ListeningConnector connector, Map<String, Connector.Argument> map) {
      fConnector = connector;
      fConnectionMap = map;
    }

    public void run() {
      try {
        fVirtualMachine = fConnector.accept(fConnectionMap);
      } catch (IOException e) {
        fException = e;
      } catch (IllegalConnectorArgumentsException e) {
        fException = e;
      }
    }

    /**
     * Returns the VM that was attached to, or <code>null</code> if none.
     * 
     * @return the VM that was attached to, or <code>null</code> if none
     */
    public VirtualMachine getVirtualMachine() {
      return fVirtualMachine;
    }

    /**
     * Returns any exception that occurred while attaching, or <code>null</code>
     * .
     * 
     * @return IOException or IllegalConnectorArgumentsException
     */
    public Exception getException() {
      return fException;
    }
  }

  /**
   * Checks to see if the command / classpath needs to be shortened for Windows.
   * Returns the modified environment or <code>null</code> if no changes are
   * needed.
   * 
   * @param args
   *          the raw arguments from the runner
   * @param cp
   *          the raw classpath from the runner configuration
   * @param env
   *          the current environment
   * @return the modified environment or <code>null</code> if no changes were
   *         made
   * @sine 3.6.200
   */
  String[] checkClasspathPrivate(List<String> args, String[] cp, String[] env) {
    if (Platform.getOS().equals(Platform.OS_WIN32)) {
      // count the complete command length
      int size = 0;
      for (String arg : args) {
        if (arg != null) {
          size += arg.length();
        }
      }
      // greater than 32767 is a no-go
      // see
      // http://msdn.microsoft.com/en-us/library/windows/desktop/ms682425(v=vs.85).aspx
      if (size > 32767) {
        StringBuffer newcp = new StringBuffer("CLASSPATH="); //$NON-NLS-1$
        for (int i = 0; i < cp.length; i++) {
          newcp.append(cp[i]);
          newcp.append(File.pathSeparatorChar);
        }
        String[] newenvp = null;
        int index = -1;
        if (env == null) {
          @SuppressWarnings("unchecked")
          Map<String, String> nenv = DebugPlugin.getDefault().getLaunchManager().getNativeEnvironment();
          Entry<String, String> entry = null;
          newenvp = new String[nenv.size()];
          int idx = 0;
          for (Iterator<Entry<String, String>> i = nenv.entrySet().iterator(); i.hasNext();) {
            entry = i.next();
            String value = entry.getValue();
            if (value == null) {
              value = ""; //$NON-NLS-1$
            }
            String key = entry.getKey();
            if (key.equalsIgnoreCase("CLASSPATH")) { //$NON-NLS-1$
              index = idx;
            }
            newenvp[idx] = key + '=' + value;
            idx++;
          }
        } else {
          newenvp = env;
          index = getCPIndexPrivate(newenvp);
        }
        if (index < 0) {
          String[] newenv = new String[newenvp.length + 1];
          System.arraycopy(newenvp, 0, newenv, 0, newenvp.length);
          newenv[newenvp.length] = newcp.toString();
          return newenv;
        }
        newenvp[index] = newcp.toString();
        return newenvp;
      }
    }
    return null;
  }

  /**
   * Returns the index in the given array for the CLASSPATH variable
   * 
   * @param env
   *          the environment array or <code>null</code>
   * @return -1 or the index of the CLASSPATH variable
   * @since 3.6.200
   */
  int getCPIndexPrivate(String[] env) {
    if (env != null) {
      for (int i = 0; i < env.length; i++) {
        if (env[i].regionMatches(true, 0, "CLASSPATH=", 0, 10)) { //$NON-NLS-1$
          return i;
        }
      }
    }
    return -1;
  }

  /**
   * Returns the version of the current VM in use
   * 
   * @return the VM version
   */
  private double getJavaVersion() {
    String version = null;
    if (fVMInstance instanceof IVMInstall2) {
      version = ((IVMInstall2) fVMInstance).getJavaVersion();
    } else {
      LibraryInfo libInfo = LaunchingPlugin.getLibraryInfo(fVMInstance.getInstallLocation().getAbsolutePath());
      if (libInfo == null) {
        return 0D;
      }
      version = libInfo.getVersion();
    }
    if (version == null) {
      // unknown version
      return 0D;
    }
    int index = version.indexOf("."); //$NON-NLS-1$
    int nextIndex = version.indexOf(".", index + 1); //$NON-NLS-1$
    try {
      if (index > 0 && nextIndex > index) {
        return Double.parseDouble(version.substring(0, nextIndex));
      }
      return Double.parseDouble(version);
    } catch (NumberFormatException e) {
      return 0D;
    }

  }

}
