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

@SuppressWarnings("restriction")
public class JPFClasspathTab extends JavaClasspathTab {

  private IRuntimeClasspathEntry[] getUserClasspath() {
    IClasspathEntry[] user = getModel().getEntries(ClasspathModel.USER);
    List<IRuntimeClasspathEntry> entries = new ArrayList<IRuntimeClasspathEntry>(user.length);
    IRuntimeClasspathEntry entry;
    IClasspathEntry userEntry;
    for (int i = 0; i < user.length; i++) {
      userEntry= user[i];
      entry = null;
      if (userEntry instanceof ClasspathEntry) {
        entry = ((ClasspathEntry)userEntry).getDelegate();
      } else if (userEntry instanceof IRuntimeClasspathEntry) {
        entry= (IRuntimeClasspathEntry) user[i];
      }
      if (entry != null) {
        entry.setClasspathProperty(IRuntimeClasspathEntry.USER_CLASSES);
        entries.add(entry);
      }
    }     
    return entries.toArray(new IRuntimeClasspathEntry[entries.size()]);
  }
  
  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    super.performApply(configuration);
    
    // we also want to put classpath entries to the dynamic config
    try {
      @SuppressWarnings("unchecked")
      Map<String, String> dynamicConfig = configuration.getAttribute(JPFSettingsTab.ATTR_JPF_DYNAMICCONFIG, (Map<String, String>)null);
      
      if (dynamicConfig != null) {
        StringBuilder classpathFlattened = new StringBuilder();
        IRuntimeClasspathEntry[] entries = JavaRuntime.resolveRuntimeClasspath(getUserClasspath(), configuration);
        List<String> userEntries = new ArrayList<String>(entries.length);
        Set<String> set = new HashSet<String>(entries.length);
       
        for (int i = 0; i < entries.length; i++) {
          if (entries[i].getClasspathProperty() == IRuntimeClasspathEntry.USER_CLASSES) {
            String location = entries[i].getLocation();
            if (location != null) {
              if (!set.contains(location)) {
                classpathFlattened.append(location).append(",");
                userEntries.add(location);
                set.add(location);
              }
            }
          }
        }
        dynamicConfig.put("classpath", classpathFlattened.toString());
      }
    } catch (CoreException e) {
      EclipseJPF.logError("Cannot store classpath entries into the dynamic config!", e);
    }
  }
  

}
