package gov.nasa.runjpf.tab.internal;

import gov.nasa.jpf.Config;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

public class ConfigCmdArgs extends Config {
  /**
   * 
   */
  private static final long serialVersionUID = -8153857880340843530L;
  public ConfigCmdArgs() {
    super((String)null);
  }
  
  public static String programArguments(ILaunchConfiguration configuration) {
    String programArguments = "";
    try {
      programArguments = configuration.getAttribute(
          IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, "");
    } catch (CoreException e) {
      // no program args .. we're fine with that
    }
    return programArguments;
  }

  public Config publicLoadArgs(String programArguments) {
    String[] programArgumentsArray = DebugPlugin.parseArguments(programArguments);
    loadArgs(programArgumentsArray);
    return this;
  }
  public static void reloadArgs(ILaunchConfiguration configuration, Map<String, String> map) {
    Config newConfig = new ConfigCmdArgs().publicLoadArgs(programArguments(configuration));
    map.clear();
    for (Object key : newConfig.keySet()) {
      if (key instanceof String) {
        map.put((String)key, (String)newConfig.get(key));
      }
    }
  }
}