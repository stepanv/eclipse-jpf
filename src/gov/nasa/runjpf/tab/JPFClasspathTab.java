package gov.nasa.runjpf.tab;

import gov.nasa.runjpf.EclipseJPF;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.internal.debug.ui.classpath.ClasspathEntry;
import org.eclipse.jdt.internal.debug.ui.classpath.ClasspathModel;
import org.eclipse.jdt.internal.debug.ui.classpath.IClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

/**
 * <p>
 * This class just adds update of the JPF Dynamic Config when the content of
 * this tab changes so that JPF CLI property <tt>classpath</tt> can reflect what
 * user chooses through the GUI. adds some defaults to it.
 * </p>
 * <p>
 * Even though th super class is marked as noextendable it seems it works well.
 * </p>
 * 
 * @author stepan
 * 
 */
@SuppressWarnings("restriction")
public class JPFClasspathTab extends JavaClasspathTab {

  /**
   * Get the user classpath.<br/>
   * Similar to {@link JavaClasspathTab#getCurrentClasspath()}.
   * 
   * @return an array of runtime user classpath entries
   */
  private IRuntimeClasspathEntry[] getUserClasspath() {
    IClasspathEntry[] entries = getModel().getEntries(ClasspathModel.USER);
    List<IRuntimeClasspathEntry> runtimeEntries = new ArrayList<IRuntimeClasspathEntry>(entries.length);
    for (IClasspathEntry entry : entries) {
      IRuntimeClasspathEntry runtimeEntry = null;
      if (entry instanceof ClasspathEntry) {
        runtimeEntry = ((ClasspathEntry) entry).getDelegate();
      } else if (entry instanceof IRuntimeClasspathEntry) {
        runtimeEntry = (IRuntimeClasspathEntry) entry;
      }
      if (runtimeEntry != null) {
        runtimeEntry.setClasspathProperty(IRuntimeClasspathEntry.USER_CLASSES);
        runtimeEntries.add(runtimeEntry);
      }
    }
    return runtimeEntries.toArray(new IRuntimeClasspathEntry[runtimeEntries.size()]);
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    super.performApply(configuration);

    // we also want to put classpath entries to the dynamic config
    try {
      @SuppressWarnings("unchecked")
      Map<String, String> dynamicConfig = configuration.getAttribute(JPFOverviewTab.ATTR_JPF_DYNAMICCONFIG, (Map<String, String>) null);

      if (dynamicConfig != null) {
        IRuntimeClasspathEntry[] entries = JavaRuntime.resolveRuntimeClasspath(getUserClasspath(), configuration);
        dynamicConfig.put("classpath", generateClasspath(configuration, entries));
      }
    } catch (CoreException e) {
      EclipseJPF.logError("Cannot store classpath entries into the dynamic config!", e);
    }
  }

  /**
   * Generate classpath from the classpath entries.
   * 
   * @param configuration
   *          Launch configuration
   * @param entries
   *          Resolved runtime classpath entries (refer to
   *          {@link JavaRuntime#resolveRuntimeClasspath(IRuntimeClasspathEntry[], org.eclipse.debug.core.ILaunchConfiguration)}
   * @return Flattened classpath to be used as JPF CLI run property
   *         <tt>classpath</tt> or null
   */
  public static String generateClasspath(ILaunchConfigurationWorkingCopy configuration, IRuntimeClasspathEntry[] entries) {
    StringBuilder classpathFlattened = new StringBuilder("");

    Set<String> set = new HashSet<String>(entries.length);

    for (int i = 0; i < entries.length; i++) {
      if (entries[i].getClasspathProperty() == IRuntimeClasspathEntry.USER_CLASSES) {
        String location = entries[i].getLocation();
        if (location != null) {
          if (!set.contains(location)) {
            classpathFlattened.append(location).append(",");
            set.add(location);
          }
        }
      }
    }
    if (classpathFlattened.length() > 0) {
      return classpathFlattened.substring(0, classpathFlattened.length() - 1);
    }
    return null;
  }

}
