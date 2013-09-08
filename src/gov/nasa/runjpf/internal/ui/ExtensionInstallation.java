package gov.nasa.runjpf.internal.ui;

import gov.nasa.runjpf.EclipseJPF;

import java.io.File;
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
  
  
  public static List<File> generateClasspathEmbedded(String[] classpathElements) {
    List<File> requiredFiles = new LinkedList<>();
    for (String relativePath : classpathElements) {
      requiredFiles.add(locateEmbeddedFile(relativePath));
    }
    return requiredFiles;
  }
  
  private static File locateEmbeddedFile(String relativePath) {
    try {
      Bundle bundle = Platform.getBundle(EclipseJPF.BUNDLE_SYMBOLIC);
      Path path = new Path(relativePath);
      URL clientFileURL = FileLocator.find(bundle, path, null);
      URL fileURL = FileLocator.resolve(clientFileURL);
      return new File(fileURL.toURI());
    } catch (URISyntaxException | IOException e) {
      EclipseJPF.logError("Cannot locate embedded runtime", e);
      return new File("");
    }
  }
  
  private String friendlyName = null;
  private String pseudoPath = "";
  private List<File> classpathFiles = Collections.<File>emptyList();
  
  public ExtensionInstallation(String friendlyName, String pseudoPath) {
    this.friendlyName = friendlyName;
    this.pseudoPath = pseudoPath;
  }
  
  public ExtensionInstallation(String friendlyName, List<File> classpathFiles) {
    this.friendlyName = friendlyName;
    this.classpathFiles = classpathFiles;
    
    if (classpathFiles.size() > 0) {
      String path = classpathFiles.get(0).getAbsolutePath();
      int trimLength = 70;
      if (path.length() - trimLength >= 0) {
        path = "... " + path.substring(path.length() - trimLength, path.length());
      }
      this.pseudoPath = path;
    }
  }

	@Override
	public String toString() {
		return new StringBuilder(friendlyName).append(" (location: ").append(pseudoPath).append(")").toString();
	}

  public String classpath(String delimiter) {
    String classpath = "";
    for (File file : classpathFiles) {
      classpath += file.getAbsolutePath() + delimiter;
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
  
  
}