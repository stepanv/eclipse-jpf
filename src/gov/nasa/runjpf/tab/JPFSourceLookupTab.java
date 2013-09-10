package gov.nasa.runjpf.tab;

import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import gov.nasa.runjpf.EclipseJPF;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.CompositeSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ContainerSourceContainer;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.internal.launching.JavaSourceLookupDirector;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;

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
        
        JavaSourceLookupDirector director = new JavaSourceLookupDirector();
        director.initializeFromMemento(memento, configuration);
        
        StringBuilder sourceLookupPathsFlattened = new StringBuilder();
        Set<IPath> sourceLookupPaths = new HashSet<>();
        lookupDirectories(director.getSourceContainers(), sourceLookupPaths, sourceLookupPathsFlattened);
      
        dynamicConfig.put("sourcepath", sourceLookupPathsFlattened.toString());
      }
    } catch (CoreException e) {
      EclipseJPF.logError("Cannot get sources!", e);
      // That's fine .. we don't need to propagate this error further
    }
    
  }

  private void lookupDirectories(ISourceContainer[] sourceContainers, Set<IPath> sourceLookupPaths, StringBuilder sourceLookupPathsFlattened) throws CoreException {
    for (ISourceContainer container : sourceContainers) {
      System.out.println(container);
      if (container instanceof JavaProjectSourceContainer) {
        JavaProjectSourceContainer javaContainer = (JavaProjectSourceContainer)container;
        lookupDirectories(javaContainer.getSourceContainers(), sourceLookupPaths, sourceLookupPathsFlattened);
      }
      if (container instanceof ContainerSourceContainer) {
        IContainer container2 = ((ContainerSourceContainer)container).getContainer();
        IPath path = container2.getLocation();
        if (path != null && !sourceLookupPaths.contains(path)) {
          sourceLookupPaths.add(path);
          sourceLookupPathsFlattened.append(path.toOSString()).append(",");
        }
      }
    }
  }
  

}
