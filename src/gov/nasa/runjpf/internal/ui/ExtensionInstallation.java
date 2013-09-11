package gov.nasa.runjpf.internal.ui;

import gov.nasa.runjpf.EclipseJPF;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/**
 * Class that represents JPF extension for a reference from this plug-in.
 * 
 * @author stepan
 * 
 */
public class ExtensionInstallation {

  public static final String EMBEDDED = "Embedded";

  private static final int EXTENSION_PSEUDOPATH_TRIM = 70;

  private String friendlyName = null;
  private String pseudoPath = "";
  private List<String> classpathFiles = Collections.<String> emptyList();
  private boolean valid = true;

  /**
   * Constructor for standard locally installed JPF extensions.
   * 
   * @param friendlyName
   *          Friendly name that is used as a whole in the GUI as a identifier.
   * @param pseudoPath
   *          A friendly identification of a path to the installation (doesn't
   *          have to be fully qualified)
   */
  public ExtensionInstallation(String friendlyName, String pseudoPath) {
    this.friendlyName = friendlyName;
    this.pseudoPath = pseudoPath;
  }

  /**
   * Constructor for embedded extensions.
   * 
   * @param friendlyName
   *          Friendly name that is used as a whole in the GUI as a identifier.
   * @param pseudoPath
   *          A friendly identification of a path to the installation (doesn't
   *          have to be fully qualified)
   * @param classpathFiles
   *          List of classpath file required by this extension during runtime
   * @param isValid
   *          whether this extension is not invalid
   */
  private ExtensionInstallation(String friendlyName, String pseudoPath, List<String> classpathFiles, boolean isValid) {
    this(friendlyName, pseudoPath);
    this.classpathFiles = classpathFiles;
    this.valid = isValid;
  }

  @Override
  public String toString() {
    return new StringBuilder(friendlyName).append(" (location: ").append(pseudoPath).append(")").toString();
  }

  /**
   * Generate the system dependent classpath using the provided path delimiter.
   * 
   * @param delimiter
   *          The path delimiter.
   * @return Joined classpath entries into one string.
   */
  public String classpath(String delimiter) {
    return StringUtils.join(classpathFiles, delimiter);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ExtensionInstallation)) {
      return false;
    }
    ExtensionInstallation other = (ExtensionInstallation) obj;
    if (!other.pseudoPath.equals(pseudoPath)) {
      return false;
    }
    if (!other.friendlyName.equals(friendlyName)) {
      return false;
    }
    if (classpathFiles != null && other.classpathFiles == null) {
      return false;
    }
    if (classpathFiles == null && other.classpathFiles != null) {
      return false;
    }
    if (classpathFiles == null && other.classpathFiles == null) {
      return true;
    }
    return true;
  }

  public boolean isValid() {
    return valid;
  }

  /**
   * Creates an embedded extension from the given classpath.
   * 
   * @param classpathElements
   *          List of classpath entries that this extensions requires for
   *          runtime
   * @return an {@link ExtensionInstallation} instance of embedded extension.
   */
  public static ExtensionInstallation embeddedExtensionFactory(String[] classpathElements) {
    List<String> requiredFiles = new LinkedList<>();
    for (String relativePath : classpathElements) {
      try {
        requiredFiles.add(locateEmbeddedFile(relativePath).getAbsolutePath());
      } catch (URISyntaxException | IOException e) {
        EclipseJPF.logError("Cannot locate embedded runtime", e);
        return new ExtensionInstallation(EMBEDDED, "Exception occurred: " + e.getMessage(), Collections.<String> emptyList(), false);
      }
    }

    String pseudoPath = "";
    if (requiredFiles.size() > 0) {
      String path = requiredFiles.get(0);
      if (path.length() - EXTENSION_PSEUDOPATH_TRIM >= 0) {
        path = "... " + path.substring(path.length() - EXTENSION_PSEUDOPATH_TRIM, path.length());
      }
      pseudoPath = path;
    }

    return new ExtensionInstallation(EMBEDDED, pseudoPath, requiredFiles, true);
  }

  /**
   * Locates a file within this plug-in bundle for the given relative path.<br/>
   * Note that this may throw lot of errors as it requires this plug-in to be
   * unpacked when installed in Eclipse. (see MANIFEST file for related
   * configuration.)
   * 
   * @param relativePath
   *          relative path within this plug-in where the desired file is
   *          located
   * @return A {@link File} instance that is positively a file.
   * @throws IOException
   *           In case there is a problem with the file at the relative path.
   * @throws URISyntaxException
   *           If {@link File#toURI()} has a problem - shouldn't happen at all.
   */
  private static File locateEmbeddedFile(String relativePath) throws IOException, URISyntaxException {
    Bundle bundle = Platform.getBundle(EclipseJPF.BUNDLE_SYMBOLIC);
    Path path = new Path(relativePath);
    URL clientFileURL = FileLocator.find(bundle, path, null);
    if (clientFileURL == null) {
      throw new FileNotFoundException("Unable to locate: " + relativePath + " within the Eclipse plugin bundle!");
    }
    URL fileURL = FileLocator.resolve(clientFileURL);
    File file = new File(fileURL.toURI());
    if (clientFileURL.equals(fileURL)) {
      throw new FileNotFoundException("Unable to locate: " + relativePath + " at URL: " + clientFileURL);
    }
    if (!file.isFile() || !file.exists()) {
      throw new FileNotFoundException("File: " + file + " is not a file at URL: " + fileURL);
    }
    if (!file.canRead()) {
      throw new AccessDeniedException("File: " + file + " is not accessible at URL: " + fileURL);
    }
    return file;
  }

}