package gov.nasa.runjpf.internal.ui;

import gov.nasa.runjpf.EclipseJPF;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

public class ExtensionInstallation {

  public static final String EMBEDDED = "Embedded";
  public static ExtensionInstallation embeddedExtensionFactory(String[] classpathElements) {
    List<String> requiredFiles = new LinkedList<>();
    for (String relativePath : classpathElements) {
      try {
        requiredFiles.add(locateEmbeddedFile(relativePath).getAbsolutePath());
      } catch (URISyntaxException | IOException e) {
        EclipseJPF.logError("Cannot locate embedded runtime", e);
        return new ExtensionInstallation(EMBEDDED, Collections.<String>emptyList(), "Exception occurred: " + e.getMessage(), false);
      }
    }
    
    String pseudoPath = "";
    if (requiredFiles.size() > 0) {
      String path = requiredFiles.get(0);
      int trimLength = 70;
      if (path.length() - trimLength >= 0) {
        path = "... " + path.substring(path.length() - trimLength, path.length());
      }
      pseudoPath = path;
    }
  
    return new ExtensionInstallation(EMBEDDED, requiredFiles, pseudoPath, true);
  }
  
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
      if (!file.isFile()) {
        throw new FileNotFoundException("File: " + file + " is not a file at URL: " + fileURL);
      }
      return file;
  }
  
  private String friendlyName = null;
  private String pseudoPath = "";
  private List<String> classpathFiles = Collections.<String>emptyList();
  private boolean isValid = true;
  
  public ExtensionInstallation(String friendlyName, String pseudoPath) {
    this.friendlyName = friendlyName;
    this.pseudoPath = pseudoPath;
  }
  
  public ExtensionInstallation(String friendlyName, List<String> classpathFiles, String pseudoPath, boolean isValid) {
    this.friendlyName = friendlyName;
    this.classpathFiles = classpathFiles;
    this.isValid = isValid;
    this.pseudoPath = pseudoPath;
  }
  
	@Override
	public String toString() {
		return new StringBuilder(friendlyName).append(" (location: ").append(pseudoPath).append(")").toString();
	}

  public String classpath(String delimiter) {
    String classpath = "";
    for (String filePath : classpathFiles) {
      classpath += filePath + delimiter;
    }
    return classpath;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ExtensionInstallation)) {
      return false;
    }
    ExtensionInstallation other = (ExtensionInstallation)obj;
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
    return isValid;
  }

}