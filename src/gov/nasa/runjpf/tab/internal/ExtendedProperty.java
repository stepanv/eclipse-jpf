package gov.nasa.runjpf.tab.internal;

class ExtendedProperty {
  private String property;
  public String getProperty() {
    return property;
  }

  public void setProperty(String property) {
    this.property = property;
  }

  public String getConfigName() {
    return configName;
  }

  public void setConfigName(String configName) {
    this.configName = configName;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  private String configName;
  private String value;
  
  public ExtendedProperty(String property, String value, String configName) {
    this.property = property;
    this.configName = configName;
    this.value = value;
  }

  public String get(int column) {
    switch (column) {
    case 0:
      return property;
    case 1:
      return value;
    case 2:
      return configName;
    default:
      throw new IllegalStateException("Implementation error! Wrong number of columns!");
    }
    
  }
}