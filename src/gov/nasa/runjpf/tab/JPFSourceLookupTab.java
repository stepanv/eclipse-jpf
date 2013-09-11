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

@SuppressWarnings("restriction")
public class JPFSourceLookupTab extends SourceLookupTab {

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    super.performApply(configuration);
    
    try {
      @SuppressWarnings("unchecked")
      Map<String, String> dynamicConfig = configuration.getAttribute(JPFSettingsTab.ATTR_JPF_DYNAMICCONFIG, (Map<String, String>)null);
      
      if (dynamicConfig != null) {
        
        // we cannot get this directly from this objects because it's too much encapsulated
        String memento = configuration.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, (String)null);
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
  
  public static String generateSourcepath(ISourceContainer[] sourceContainers) throws CoreException {
    StringBuilder sourceLookupPathsFlattened = new StringBuilder();
    Set<IPath> sourceLookupPaths = new HashSet<>();
    
    generateSourcepathRecursively(sourceContainers, sourceLookupPaths, sourceLookupPathsFlattened);
    
    if (sourceLookupPathsFlattened.length() > 0) {
      return sourceLookupPathsFlattened.substring(0, sourceLookupPathsFlattened.length() - 1);
    }
    return null;
  }

  private static void generateSourcepathRecursively(ISourceContainer[] sourceContainers, Set<IPath> sourceLookupPaths, StringBuilder sourceLookupPathsFlattened) throws CoreException {
    for (ISourceContainer sourceContainer : sourceContainers) {
      if (sourceContainer instanceof AbstractSourceContainer) {
        generateSourcepathRecursively(((AbstractSourceContainer)sourceContainer).getSourceContainers(), sourceLookupPaths, sourceLookupPathsFlattened);
      }
      if (sourceContainer instanceof ContainerSourceContainer) {
        IContainer container = ((ContainerSourceContainer)sourceContainer).getContainer();
        IPath path = container.getLocation();
        if (path != null && !sourceLookupPaths.contains(path)) {
          sourceLookupPaths.add(path);
          sourceLookupPathsFlattened.append(path.toOSString()).append(",");
        }
      }
    }
  }

}
