package gov.nasa.runjpf.tab.internal;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.util.JPFSiteUtils;
import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.EclipseJPFLauncher;
import gov.nasa.runjpf.tab.JPFSettingsTab;

import java.io.File;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

public class LookupConfigHelper extends Config {
  /** */
  private static final long serialVersionUID = 2968226266594218025L;

  public LookupConfigHelper() {
    super((String) null);
  }

  public String getPathArgPublic(String[] args, String key) {
    return getPathArg(args, key);
  }
  
  public static String programArguments(ILaunchConfiguration configuration) {
    String programArguments = "";
    try {
      programArguments = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, "");
      programArguments = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(programArguments);
    } catch (CoreException e) {
      // no program args .. we're fine with that
    }
    return programArguments;
  }

  public static Config defaultConfigFactory(ILaunchConfiguration configuration, String appPropPath) {
    String sitePath = null;
    // Check whether site is overridden from the command line
    String arguments = programArguments(configuration);
    LookupConfigHelper tmpLookupConfig = new LookupConfigHelper();
    sitePath = tmpLookupConfig.getPathArgPublic(DebugPlugin.parseArguments(arguments), "site");
    
    if (sitePath == null) {
      // site is not overridden from the command line
  
      if (appPropPath != null) {
        // check whether the app prop file contains a site definition
        sitePath = JPFSiteUtils.getMatchFromFile(appPropPath, "site");
      }
      
      if (sitePath == null) {
        // get it from Eclipse
        EclipseJPFLauncher siteLookup = new EclipseJPFLauncher();
        File siteProperties = siteLookup.lookupSiteProperties();
        if (siteProperties != null) {
          sitePath = siteProperties.getAbsolutePath();
        } else {
          throw new IllegalStateException("No site properties found!");
        }
      }
    }
    return new Config(new String[] { "+site=" + sitePath });
  }
  
  public static Config programArgumentsConfigFactory(ILaunchConfiguration configuration) {
    LookupConfigHelper config = new LookupConfigHelper();
    config.loadArgs(DebugPlugin.parseArguments(programArguments(configuration)));
    return config;
  }
  
  @SuppressWarnings("unchecked")
  public static void reloadConfig(ILaunchConfiguration configuration, String configAttribute, Config config) {
    try {
      Map<String, String> mapConfig = configuration.getAttribute(configAttribute, (Map<String, String>)null);
      mapConfig.clear();
      for (Object key : config.keySet()) {
        if (key instanceof String) {
          mapConfig.put((String)key, (String)config.get(key));
        }
      }
    } catch (CoreException e) {
     EclipseJPF.logError("Cannot reload configuration", e);
     // we don't care more
    }
  }

  public static Config defaultConfigFactory(ILaunchConfiguration configuration) {
    return defaultConfigFactory(configuration, appPropPath(configuration));
  }
  
  private static String appPropPath(ILaunchConfiguration configuration) {
    String appPropPath = null;
    try {
      appPropPath = configuration.getAttribute(JPFSettingsTab.JPF_FILE_LOCATION, (String)null);
    } catch (CoreException e) {
      // we're fine
    }
    return appPropPath;
  }
  
  public static Config appConfigFactory(ILaunchConfiguration configuration) {
    return appConfigFactory(configuration, appPropPath(configuration));
  }
  public static Config appConfigFactory(ILaunchConfiguration configuration, String appPropPath) {
    return new Config(appPropPath);
  }
}