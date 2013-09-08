package gov.nasa.runjpf.internal.ui;

import gov.nasa.runjpf.EclipseJPF;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

public class ClassSearchEngine {
    
    private class MethodCollector extends SearchRequestor {
      private List<IType> fResult;

      public MethodCollector() {
        fResult = new ArrayList<IType>(200);
        typesList = new LinkedList<IType>();
      }

      public List<IType> getResult() {
        return fResult;
      }
      
      private List<IType> typesList;
      
      public boolean hasNonLookedUpSubtypes() {
        return typesList.size() != 0;
      }
      public IType popNonLookedUpSubtype() {
        return typesList.remove(0);
      }

      /* (non-Javadoc)
       * @see org.eclipse.jdt.core.search.SearchRequestor#acceptSearchMatch(org.eclipse.jdt.core.search.SearchMatch)
       */
      @Override
      public void acceptSearchMatch(SearchMatch match) throws CoreException {
        Object enclosingElement = match.getElement();
        if (enclosingElement instanceof IType) {
          try {
            IType curr= (IType) enclosingElement;
            if (curr.isClass()) {
              fResult.add(curr);
            }
            typesList.add(curr);
          } catch (JavaModelException e) {
            EclipseJPF.logError("Error while matching with: " + match, e);
          }
        }
      }
    }

    /**
     * Searches for all main methods in the given scope.
     * Valid styles are IJavaElementSearchConstants.CONSIDER_BINARIES and
     * IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS
     * 
     * @param pm progress monitor
     * @param scope search scope
     * @param includeSubtypes whether to consider types that inherit a main method
     * @param supertype 
     */ 
    public IType[] searchMainMethods(IProgressMonitor pm, IJavaSearchScope scope, boolean includeSubtypes, String supertype) {
      pm.beginTask(LauncherMessages.MainMethodSearchEngine_1, 100); 
      int searchTicks = 100;
      if (includeSubtypes) {
        searchTicks = 25;
      }
      
      SearchPattern pattern = SearchPattern.createPattern(supertype, IJavaSearchConstants.TYPE, IJavaSearchConstants.IMPLEMENTORS, 1); 
      SearchParticipant[] participants = new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()};
      MethodCollector collector = new MethodCollector();
      IProgressMonitor searchMonitor = new SubProgressMonitor(pm, searchTicks);
      SearchEngine searchEngine = new SearchEngine();
      try {
        searchEngine.search(pattern, participants, scope, collector, searchMonitor);
        while (collector.hasNonLookedUpSubtypes()) {
          String newTypeToSearch = collector.popNonLookedUpSubtype().getFullyQualifiedName();
          pattern = SearchPattern.createPattern(newTypeToSearch, IJavaSearchConstants.TYPE, IJavaSearchConstants.IMPLEMENTORS, 1);
          searchEngine.search(pattern, participants, scope, collector, searchMonitor);
        }
      } catch (CoreException ce) {
        JDIDebugUIPlugin.log(ce);
      }

      List<IType> result = collector.getResult();
      if (includeSubtypes) {
        IProgressMonitor subtypesMonitor = new SubProgressMonitor(pm, 75);
        subtypesMonitor.beginTask(LauncherMessages.MainMethodSearchEngine_2, result.size()); 
        Set<IType> set = addSubtypes(result, subtypesMonitor, scope);
        return set.toArray(new IType[set.size()]);
      }
      return result.toArray(new IType[result.size()]);
    }

    /**
     * Adds subtypes and enclosed types to the listing of 'found' types 
     * @param types the list of found types thus far
     * @param monitor progress monitor
     * @param scope the scope of elements
     * @return as set of all types to consider
     */
    private Set<IType> addSubtypes(List<IType> types, IProgressMonitor monitor, IJavaSearchScope scope) {
      Iterator<IType> iterator = types.iterator();
      Set<IType> result = new HashSet<IType>(types.size());
      IType type = null;
      ITypeHierarchy hierarchy = null;
      IType[] subtypes = null;
      while (iterator.hasNext()) {
        type = iterator.next();
        if (result.add(type)) {
          try {
            hierarchy = type.newTypeHierarchy(monitor);
            subtypes = hierarchy.getAllSubtypes(type);
            for (int i = 0; i < subtypes.length; i++) {
              if (scope.encloses(subtypes[i])) {
                result.add(subtypes[i]);
              }
            }       
          } 
          catch (JavaModelException e) {JDIDebugUIPlugin.log(e);}
        }
        monitor.worked(1);
      }
      return result;
    }
    
    
    /**
     * Returns the package fragment root of <code>IJavaElement</code>. If the given
     * element is already a package fragment root, the element itself is returned.
     */
    public static IPackageFragmentRoot getPackageFragmentRoot(IJavaElement element) {
      return (IPackageFragmentRoot) element.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
    } 
    
    /**
     * Searches for all main methods in the given scope.
     * Valid styles are IJavaElementSearchConstants.CONSIDER_BINARIES and
     * IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS
     * 
     * @param includeSubtypes whether to consider types that inherit a main method
     * @param supertype 
     */
    public IType[] searchClasses(IRunnableContext context, final IJavaSearchScope scope, final boolean includeSubtypes, final String supertype) throws InvocationTargetException, InterruptedException  {   
      final IType[][] res= new IType[1][];
      
      IRunnableWithProgress runnable= new IRunnableWithProgress() {
        public void run(IProgressMonitor pm) throws InvocationTargetException {
          res[0]= searchMainMethods(pm, scope, includeSubtypes, supertype);
        }
      };
      context.run(true, true, runnable);
      
      return res[0];
    }
        
  }