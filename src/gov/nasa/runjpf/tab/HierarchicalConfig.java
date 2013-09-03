package gov.nasa.runjpf.tab;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.util.JPFSiteUtils;

import java.io.File;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

public class HierarchicalConfig extends Config {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  List<InnerHiararchicalConfig> configList = new LinkedList<InnerHiararchicalConfig>();

  public List<InnerHiararchicalConfig> getConfigList() {
    return configList;
  }

  abstract class InnerHiararchicalConfig extends Config {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String configName;


    public String getConfigName() {
      return configName;
    }
    
    String getSitePropertiesLocation(String[] args, String appPropPath){
      String path = getPathArg(args, "site");

      if (path == null){
        // look into the app properties
        // NOTE: we might want to drop this in the future because it constitutes
        // a cyclic properties file dependency
        if (appPropPath != null){
          path = JPFSiteUtils.getMatchFromFile(appPropPath,"site");
        }

        if (path == null) {
          File siteProps = JPFSiteUtils.getStandardSiteProperties();
          if (siteProps != null){
            path = siteProps.getAbsolutePath();
          }
        }
      }
      
      put("jpf.site", path);

      return path;
    }

    public InnerHiararchicalConfig(String configName) {
      super((String)null);
      
      this.configName = configName;
      
      initialize();
    }
    
    protected abstract void initialize();
    
    List<HierarchicalProperty> getSortedProperties() {
      
      List<HierarchicalProperty> result = new LinkedList<HierarchicalProperty>();

      // just how much do you have to do to get a printout with keys in alphabetical order :<
      TreeSet<String> kset = new TreeSet<String>();
      for (Enumeration<?> e = propertyNames(); e.hasMoreElements();) {
        Object k = e.nextElement();
        if (k instanceof String) {
          kset.add( (String)k);
        }
      }

      for (String key : kset) {
        String val = getProperty(key);
        result.add(new HierarchicalProperty(key, val, configName));
      }

      return result;
    }
    
  }
  
  public class HierarchicalProperty {
    public final String property;
    public final String configName;
    public final String value;
    
    public HierarchicalProperty(String property, String value, String configName) {
      this.property = property;
      this.configName = configName;
      this.value = value;
    }
  }
  
  String getAppPropertiesLocation(String[] args){
    String path = null;

    path = getPathArg(args, "app");
    if (path == null){
      // see if the first free arg is a *.jpf
      path = getAppArg(args);
    }
    
    put("jpf.app", path);

    return path;
  }
  
  HierarchicalConfig() {
    this("");
  }
  HierarchicalConfig(String appPropertiesInput) {
    super((String)null);
    final String[] a = new String[]{appPropertiesInput};
    final String appProperties = getAppPropertiesLocation(a);
    
    configList.add(new InnerHiararchicalConfig("Properties as Arguments") {

      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      @Override
      public void initialize() {
        loadArgs(a);
      }
      
    });
    
    configList.add(new InnerHiararchicalConfig("Application properties") {

      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      @Override
      public void initialize() {
      //--- the application properties
        if (appProperties != null){
          loadProperties( appProperties);
        }
      }
      
    });
    
    configList.add(new InnerHiararchicalConfig("Project properties") {

      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      @Override
      public void initialize() {
        //--- get the project properties from current dir + site configured extensions
        loadProjectProperties();
      }
      
    });
    
    configList.add(new InnerHiararchicalConfig("Site properties") {

      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      @Override
      public void initialize() {
        String appProperties = getAppPropertiesLocation(a);

        //--- the site properties
        String siteProperties = getSitePropertiesLocation( a, appProperties);
        if (siteProperties != null){
          loadProperties( siteProperties);
        }
      }
      
    });
  
  }

  @Override
  public String getProperty(String key) {
    String value = null;
    for (Config config : configList) {
      value = config.getProperty(key);
      if (value != null) {
        break;
      }
    }
    return value;
  }
  
  public HierarchicalProperty getHierarchicalProperty(String key) {
    HierarchicalProperty result = null;
    for (InnerHiararchicalConfig config : configList) {
      String value = config.getProperty(key);
      if (value != null) {
        return new HierarchicalProperty(key, value, config.getConfigName());
      }
    }
    
    return null;
    
  }

  @Override
  public synchronized int size() {
    int size = 0;
    for (InnerHiararchicalConfig config : configList) {
      size += config.size();
    }
    return size;
  }

  @Override
  public synchronized boolean isEmpty() {
    return size() > 0;
  }
  
  

}
