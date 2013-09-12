package gov.nasa.runjpf.tab;

import gov.nasa.runjpf.EclipseJPF;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ContainerSourceContainer;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.internal.launching.JavaSourceLookupDirector;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;

/**
 * <p>
 * This is class just adds automatic update of Dynamic Config
 * <tt>sourcepath</tt> property when anything is changed here.
 * </p>
 * <p>
 * Even though th super class is marked as noextendable it seems it works well.
 * </p>
 * 
 * @author stepan
 * 
 */
@SuppressWarnings("restriction")
public class JPFSourceLookupTab extends SourceLookupTab {

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    super.performApply(configuration);

    try {
      @SuppressWarnings("unchecked")
      Map<String, String> dynamicConfig = configuration.getAttribute(JPFOverviewTab.ATTR_JPF_DYNAMICCONFIG, (Map<String, String>) null);

      if (dynamicConfig != null) {

        // we cannot get this directly from this objects because it's too much
        // encapsulated
        String memento = configuration.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, (String) null);
        if (memento != null) {
          JavaSourceLookupDirector director = new JavaSourceLookupDirector();
          director.initializeFromMemento(memento, configuration);

          dynamicConfig.put("sourcepath", generateSourcepath(director.getSourceContainers()));
        } // if memento is null we cannot generate the sourcepath
      }
    } catch (CoreException e) {
      EclipseJPF.logError("Cannot get sources!", e);
      // That's fine .. we don't need to propagate this error further
    }

  }

  /**
   * Generates sourcepath to be used by JPF <tt>sourcepath</tt> property based
   * on the source containers the launch configuration is associated with (refer
   * to the <i>Sources</i> tab).
   * 
   * @param sourceContainers
   *          The source containers used for generation of the
   *          <tt>sourcepath</tt>.
   * @return flattened <tt>sourcepath</tt>
   */
  public static String generateSourcepath(ISourceContainer[] sourceContainers) {
    StringBuilder sourceLookupPathsFlattened = new StringBuilder();
    Set<IPath> sourceLookupPaths = new HashSet<>();

    generateSourcepathRecursively(sourceContainers, sourceLookupPaths, sourceLookupPathsFlattened);

    if (sourceLookupPathsFlattened.length() > 0) {
      return sourceLookupPathsFlattened.substring(0, sourceLookupPathsFlattened.length() - 1);
    }
    return null;
  }

  /**
   * Recursively fetches sourcepath for all associated sources.
   * 
   * @param sourceContainers
   *          The input for the generation.
   * @param sourceLookupPaths
   *          A set that doesn't allow cycles in the recursion.
   * @param sourceLookupPathsFlattened
   *          The result.
   */
  private static void generateSourcepathRecursively(ISourceContainer[] sourceContainers, Set<IPath> sourceLookupPaths,
                                                    StringBuilder sourceLookupPathsFlattened) {
    for (ISourceContainer sourceContainer : sourceContainers) {
      if (sourceContainer instanceof JavaProjectSourceContainer) {
        try {
          generateSourcepathRecursively(((AbstractSourceContainer) sourceContainer).getSourceContainers(), sourceLookupPaths,
                                        sourceLookupPathsFlattened);
        } catch (CoreException e) {
          EclipseJPF.logError("Unable to generate sourcepath for: " + sourceContainer, e);
          // we're fine, let's try to continue
        }
      }
      if (sourceContainer instanceof ContainerSourceContainer) {
        IContainer container = ((ContainerSourceContainer) sourceContainer).getContainer();
        IPath path = container.getLocation();
        if (path != null && !sourceLookupPaths.contains(path)) {
          sourceLookupPaths.add(path);
          sourceLookupPathsFlattened.append(path.toOSString()).append(",");
        }
      }
    }
  }

}
