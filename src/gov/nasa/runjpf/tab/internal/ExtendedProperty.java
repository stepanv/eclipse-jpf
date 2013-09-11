package gov.nasa.runjpf.tab.internal;

import gov.nasa.jpf.Config;

/**
 * Extended property class provides access to more information of the
 * {@link Config} properties.
 * 
 * @author stepan
 * 
 */
class ExtendedProperty {
  private String property;

  private String configName;
  private String value;

  /**
   * Creates extended property.
   * 
   * @param property
   *          Property name (key)
   * @param value
   *          Property value
   * @param configName
   *          Property {@link Config} origin
   */
  public ExtendedProperty(String property, String value, String configName) {
    this.property = property;
    this.configName = configName;
    this.value = value;
  }

  /**
   * Get a string for a specified column.
   * 
   * @param column
   *          Column number (starting with 0)
   * @return string for the given column
   */
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