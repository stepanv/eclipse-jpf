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
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * Class search engine for lookup of all subtypes for the given supertype.<br/>
 * Inspired and reused from
 * <tt>org.eclipse.jdt.internal.debug.ui.launcher.MainMethodSearchEngine</tt>.
 * 
 * @author stepan
 * 
 */
public class ClassSearchEngine {

  private class TypeCollector extends SearchRequestor {

    public TypeCollector() {
      typesList = new LinkedList<IType>();
    }

    public List<IType> getTypeList() {
      return typesList;
    }

    private List<IType> typesList;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jdt.core.search.SearchRequestor#acceptSearchMatch(org.eclipse
     * .jdt.core.search.SearchMatch)
     */
    @Override
    public void acceptSearchMatch(SearchMatch match) throws CoreException {
      Object enclosingElement = match.getElement();
      if (enclosingElement instanceof IType) {
        IType curr = (IType) enclosingElement;
        typesList.add(curr);
      }
    }
  }

  /**
   * Searches for all subtypes in the given scope. Valid styles are
   * IJavaElementSearchConstants.CONSIDER_BINARIES and
   * IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS
   * 
   * @param pm
   *          progress monitor
   * @param scope
   *          search scope
   * @param includeSubtypes
   *          whether to consider types that inherit a main method
   */
  public IType[] searchMainMethods(IProgressMonitor pm, IJavaSearchScope scope, boolean includeSubtypes, String supertype) {
    pm.beginTask("Searching for all direct subtypes of " + supertype + " ...", 100);
    int searchTicks = 100;
    if (includeSubtypes) {
      searchTicks = 25;
    }
    SearchPattern pattern = SearchPattern.createPattern(supertype, IJavaSearchConstants.TYPE, IJavaSearchConstants.IMPLEMENTORS, 1);
    SearchParticipant[] participants = new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
    TypeCollector collector = new TypeCollector();
    IProgressMonitor searchMonitor = new SubProgressMonitor(pm, searchTicks);
    try {
      new SearchEngine().search(pattern, participants, scope, collector, searchMonitor);
    } catch (CoreException ce) {
      EclipseJPF.logError("Problem occurred while searching for direct subtypes of " + supertype, ce);
    }

    List<IType> result = collector.getTypeList();
    if (includeSubtypes) {

      IProgressMonitor subtypesMonitor = new SubProgressMonitor(pm, 75);
      subtypesMonitor.beginTask("Searching for subtypes...", result.size());
      Set<IType> set = addSubtypes(result, subtypesMonitor, scope);

      IProgressMonitor removeInterfacesMonitor = new SubProgressMonitor(pm, 95);
      subtypesMonitor.beginTask("Removing interfaces...", set.size());
      List<IType> classes = removeInterfaces(set, removeInterfacesMonitor);

      return classes.toArray(new IType[classes.size()]);
    }
    return result.toArray(new IType[result.size()]);
  }

  /**
   * Remove all interfaces from the set of found types.
   * 
   * @param set
   *          The set of all types
   * @param removeInterfacesMonitor
   *          progress monitor
   * @return list of classes
   */
  private List<IType> removeInterfaces(Set<IType> set, IProgressMonitor removeInterfacesMonitor) {
    List<IType> classesSet = new ArrayList<>(set.size());
    for (IType type : set) {
      try {
        if (type.isClass()) {
          classesSet.add(type);
        }
      } catch (JavaModelException e) {
        EclipseJPF.logError("Problem occurred when accessing type: " + type, e);
      }
      removeInterfacesMonitor.worked(1);
    }
    return classesSet;
  }

  /**
   * Adds subtypes and enclosed types to the listing of 'found' types
   * 
   * @param types
   *          the list of found types thus far
   * @param monitor
   *          progress monitor
   * @param scope
   *          the scope of elements
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
        } catch (JavaModelException e) {
          EclipseJPF.logError("Error occurred while searching for subtypes.", e);
        }
      }
      monitor.worked(1);
    }
    return result;
  }

  /**
   * Searches for all main methods in the given scope. Valid styles are
   * IJavaElementSearchConstants.CONSIDER_BINARIES and
   * IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS
   * 
   * @param includeSubtypes
   *          whether to consider types that inherit a main method
   * @param supertype
   */
  public IType[] searchClasses(IRunnableContext context, final IJavaSearchScope scope, final boolean includeSubtypes, final String supertype) throws InvocationTargetException, InterruptedException {
    final IType[][] res = new IType[1][];

    IRunnableWithProgress runnable = new IRunnableWithProgress() {
      public void run(IProgressMonitor pm) throws InvocationTargetException {
        res[0] = searchMainMethods(pm, scope, includeSubtypes, supertype);
      }
    };
    context.run(true, true, runnable);

    return res[0];
  }

}