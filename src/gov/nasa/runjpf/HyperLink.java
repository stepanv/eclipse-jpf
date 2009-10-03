package gov.nasa.runjpf;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.console.IHyperlink;

public class HyperLink implements IHyperlink{  
  IFile file;
  int line;
  
  public HyperLink(IFile file, int line){
    this.file = file;
    this.line = line;
  }

  @Override
  public void linkActivated() {
    EclipseJPF.openLink(file, line);
  }

  @Override
  public void linkEntered() {}

  @Override
  public void linkExited() {}
  
}
