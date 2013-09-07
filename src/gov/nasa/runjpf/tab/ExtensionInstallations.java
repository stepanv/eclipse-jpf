package gov.nasa.runjpf.tab;

import java.util.ArrayList;
import java.util.List;

public class ExtensionInstallations extends ArrayList<ExtensionInstallation> implements List<ExtensionInstallation> {
  /**	 */
  private static final long serialVersionUID = 1L;
  
  public static final int EMBEDDED_INSTALLATION_INDEX = 0;
  public static final int NONEMBEDDED_INSTALLATION_INDEX = 1;
  
  public ExtensionInstallations(ExtensionInstallation embedded) {
    add(EMBEDDED_INSTALLATION_INDEX, embedded);
  }
  
  public ExtensionInstallation getEmbedded() {
    return get(EMBEDDED_INSTALLATION_INDEX);
  }

  public String[] toStringArray(String[] array) {
    if (array.length < this.size()) {
      throw new UnsupportedOperationException("The array specified must have a good size!");
    }
    int i = 0;
    for (ExtensionInstallation extensionInstallation : this) {
      array[i++] = extensionInstallation.toString();
    }
    return array;
  }

  public int getDefaultInstallationIndex() {
    if (size() > 1) {
      return NONEMBEDDED_INSTALLATION_INDEX;
    } else {
      return EMBEDDED_INSTALLATION_INDEX;
    }
  }
}