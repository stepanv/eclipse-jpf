package gov.nasa.runjpf.tab.internal;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.util.JPFSiteUtils;
import gov.nasa.runjpf.EclipseJPF;
import gov.nasa.runjpf.EclipseJPFLauncher;
import gov.nasa.runjpf.tab.JPFOverviewTab;
import gov.nasa.runjpf.wizard.NewJPFProjectPage;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

/**
 * This class facilities all the requirements this Eclipse plug-in has on the
 * JPF {@link Config} configuration class.<br/>
 * The problem with this {@link Config} is that it was too complicate to narrow
 * its implementation so that it
 * <ul>
 * <li>provides hierarchy based on the properties origin</li>
 * <li>exposes some encapsulated functionality to the outside</li>
 * <li>reimplements and reuse several parts of the code from it</li>
 * </ul>
 * This class also encapsulates following site properties lookup strategy as it
 * checks whether
 * <ol>
 * <li><tt>site</tt> property was specified as a command line argument</li>
 * <li>default application property file (.jpf) contains <tt>site</tt> property</li>
 * <li>site path is specified in the Eclipse JPF plugin configuration</li>
 * <li>site properties file is in a default location
 * <tt>(..home../.jpf/site.properties)</tt></li>
 * </ol>
 * 
 * @author stepan
 * 
 */
public class LookupConfigHelper {

  /* non instatiable helper */
  private LookupConfigHelper() {
  }

  /**
   * Private class that exposes some protected methods.
   * 
   * @author stepan
   * 
   */
  private static class LookupConfig extends Config {
    /** */
    private static final long serialVersionUID = 2968226266594218025L;

    /**
     * Creates an empty instance of {@link Config} that doesn't contain any
     * properties.
     */
    public LookupConfig() {
      super((String) null);
    }

    /**
     * Get a value of JPF like command argument provided property
     * <tt>+key=value</tt><br/>
     * Exposes hidden functionality of command line arguments parsing.
     * 
     * @param args
     *          Command line arguments
     * @param key
     *          Property key to look for
     * @return a value
     */
    public String getPathArgPublic(String[] args, String key) {
      return getPathArg(args, key);
    }

    /**
     * Exposes {@link Config#loadArgs(String[])} method.
     * 
     * @param cmdLineArgs
     */
    public void loadArgsPublic(String[] cmdLineArgs) {
      loadArgs(cmdLineArgs);
    }
  }

  /**
   * Get program arguments from the launch configuration that are managed by the
   * underlying JDT.
   * 
   * @param configuration
   *          Launch configuration
   * @return Raw program arguments or an empty string.
   */
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

  /**
   * Creates {@link Config} instance that contains ONLY site properties and
   * properties from linked extensions.<br/>
   * The configuration is looked up as specified in {@link LookupConfigHelper}
   * doc.<br/>
   * This constructor uses the application property path from the well known
   * location in the provided configuration.
   * 
   * @param configuration
   *          Launch configuration
   * @return {@link Config} instance
   */
  public static Config defaultConfigFactory(ILaunchConfiguration configuration) {
    return defaultConfigFactory(configuration, appPropPath(configuration));
  }

  /**
   * Creates {@link Config} instance that contains ONLY site properties and
   * properties from linked extensions. The configuration is looked up as
   * specified in {@link LookupConfigHelper} doc.<br/>
   * 
   * @param configuration
   *          Launch configuration
   * @param appPropPath
   *          Custom application properties file path
   * @return {@link Config} instance
   */
  public static Config defaultConfigFactory(ILaunchConfiguration configuration, String appPropPath) {
    String sitePath = null;
    // Check whether site is overridden from the command line
    String arguments = programArguments(configuration);
    LookupConfig tmpLookupConfig = new LookupConfig();
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
    // this is how we create a config from the specific site path
    return new Config(new String[] { "+site=" + sitePath });
  }

  /**
   * Creates {@link Config} instance that contains ONLY properties as specified
   * as command line arguments.
   * 
   * @param configuration
   *          Launch configuration
   * @return {@link Config} instance
   */
  public static Config programArgumentsConfigFactory(ILaunchConfiguration configuration) {
    LookupConfig config = new LookupConfig();
    config.loadArgsPublic(DebugPlugin.parseArguments(programArguments(configuration)));
    return config;
  }

  /**
   * Reloads and resets the content of a config stored in the configuration
   * under the given config attribute using the provided config.
   * 
   * @param configuration
   *          Launch configuration
   * @param configAttribute
   *          Config attribute
   * @param config
   *          The config that is used to reset the reseted config.
   */
  @SuppressWarnings("unchecked")
  public static void reloadConfig(ILaunchConfiguration configuration, String configAttribute, Config config) {
    try {
      Map<String, String> mapConfig = configuration.getAttribute(configAttribute, (Map<String, String>) null);
      mapConfig.clear();
      for (Object key : config.keySet()) {
        if (key instanceof String) {
          mapConfig.put((String) key, (String) config.get(key));
        }
      }
    } catch (CoreException e) {
      EclipseJPF.logError("Cannot reload configuration", e);
      // we don't care more
    }
  }

  private static String appPropPath(ILaunchConfiguration configuration) {
    String appPropPath = null;
    try {
      appPropPath = configuration.getAttribute(JPFOverviewTab.JPF_ATTR_MAIN_JPFFILELOCATION, (String) null);
    } catch (CoreException e) {
      // we're fine
    }
    return appPropPath;
  }

  /**
   * Creates application config that contains only properties from the
   * application properties file.<br/>
   * The path of the application property file is taken from the well-known
   * storage in the launch configuration.
   * 
   * @param configuration
   *          Launch configuration
   * @return {@link Config} instance.
   */
  public static Config appConfigFactory(ILaunchConfiguration configuration) {
    return appConfigFactory(configuration, appPropPath(configuration));
  }

  /**
   * Creates application config that contains only properties from the
   * application properties file.<br/>
   * 
   * 
   * @param configuration
   *          Launch configuration
   * @param appPropPath
   *          The path of the application property file.
   * @return {@link Config} instance.
   */
  public static Config appConfigFactory(ILaunchConfiguration configuration, String appPropPath) {
    if (appPropPath != null) {
      File appProp = new File(appPropPath);
      if (appProp.exists() && appProp.isFile() && appProp.canRead()) {
        return new Config(appPropPath);
      }
    }
    // behave as though no application property file was provided
    return new Config((String) null);
  }

  /**
   * Gets site project the same was as implemented in
   * {@link NewJPFProjectPage#getSiteProjects()}.
   * 
   * @param config
   *          The configuration {@link Config} instance to use.
   * @return Map of pairs <tt>&lt;projectId, {@link File} instance&gt;</tt>.
   */
  public static Map<String, File> getSiteProjects(Config config) {
    Map<String, File> projects = new HashMap<>();

    for (String projId : config.getEntrySequence()) {
      if ("extensions".equals(projId)) {
        // we have to filter this out in case there is only a single project
        // in
        // the list, in which case we find a jpf.properties under its value
        continue;
      }

      String v = config.getString(projId);
      if (v == null) {
        continue;
      }
      File projDir = new File(v);

      if (projDir.isDirectory()) {
        File propFile = new File(projDir, "jpf.properties");
        if (propFile.isFile()) {
          projects.put(projId, propFile);
        }
      }
    }
    return projects;
  }
}