package gov.nasa.runjpf.internal.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Extension Installations class encapsulates a list of available candidates for
 * a JPF extension.<br/>
 * Note that it is not intended to have a full sized list of all extensions that
 * may be located somewhere on the filesystem. This list should ultimately
 * provide 2 candidates at maximum. That is the embedded installation and one
 * more installation that JPF would use if it was started.
 * 
 * @author stepan
 * 
 */
public class ExtensionInstallations extends ArrayList<ExtensionInstallation> implements List<ExtensionInstallation> {

  /** generated uid */
  private static final long serialVersionUID = -5740362927543784157L;

  public static final int EMBEDDED_INSTALLATION_INDEX = 0;
  public static final int NONEMBEDDED_INSTALLATION_INDEX = 1;

  /**
   * A convenience factory for just one library.<br/>
   * For further information refer to
   * {@link ExtensionInstallations#factory(String[])}.
   * 
   * @param library
   *          a library
   * @return an instance of this class
   */
  public static ExtensionInstallations factory(String library) {
    return factory(new String[] { library });
  }

  /**
   * Factory that creates extension installations with one embedded extension
   * installation that consists from provided libraries.
   * 
   * @param libraries
   *          An array of libraries the embedded installation requires during
   *          runtime.
   * @return Instance of {@link ExtensionInstallations}.
   */
  public static ExtensionInstallations factory(String[] libraries) {
    return new ExtensionInstallations(ExtensionInstallation.embeddedExtensionFactory(libraries));
  }

  /**
   * A convenience method for reseting the installations.</br> For further
   * information refer to {@link ExtensionInstallations#reset(String[])} method.
   * 
   * @param library
   *          a library
   */
  public void reset(String library) {
    reset(new String[] { library });
  }

  /**
   * Resets this extension installations class and creates an embedded
   * installation for the given set of libraries.
   * 
   * @param libraries
   *          set of libraries the embedded installation requires during a
   *          runtime
   */
  public void reset(String[] libraries) {
    clear();
    add(EMBEDDED_INSTALLATION_INDEX, ExtensionInstallation.embeddedExtensionFactory(libraries));
  }

  /*
   * Only factories are designed to be used.
   */
  private ExtensionInstallations(ExtensionInstallation embedded) {
    super(3);
    add(EMBEDDED_INSTALLATION_INDEX, embedded);
  }

  /**
   * Get the embedded installation.
   * 
   * @return the embedded installation
   */
  public ExtensionInstallation getEmbedded() {
    return get(EMBEDDED_INSTALLATION_INDEX);
  }

  /**
   * A convenient method for populating GUI objects.
   * 
   * @param array
   *          String array to be filled with the extension installations
   * @return Populated the same array instance or new one if the size of the
   *         provided array wasn't big enough.
   */
  public String[] toStringArray(String[] array) {
    if (array.length < this.size()) {
      array = new String[this.size()];
    }
    int i = 0;
    for (ExtensionInstallation extensionInstallation : this) {
      array[i++] = extensionInstallation.toString();
    }
    return array;
  }

  /**
   * The default installation index to use.
   * 
   * @return an index
   */
  public int defaultInstallationIndex() {
    if (size() > 1) {
      return NONEMBEDDED_INSTALLATION_INDEX;
    } else {
      return EMBEDDED_INSTALLATION_INDEX;
    }
  }
}