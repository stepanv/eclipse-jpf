package gov.nasa.runjpf;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

class PatternMatchListener implements IPatternMatchListener{
  //Matches java file paths with optional line numbers
  Pattern javaFile = Pattern.compile("([\\w\\\\/]+\\.java)(?::(\\d+))?");

  @Override
  public int getCompilerFlags() {
    return 0;
  }

  @Override
  public String getLineQualifier() {
    return null;
  }

  @Override
  public String getPattern() {
    return "([\\w\\\\/]+\\.java)(?::(\\d+))?";
  }

  @Override
  public void connect(TextConsole console) {
     
  }

  @Override
  public void disconnect() {
    
  }

  @Override
  public void matchFound(PatternMatchEvent event) {
    TextConsole source = (TextConsole)(event.getSource());
    try {
      String text = source.getDocument().get(event.getOffset(), event.getLength());
      
      Matcher m = javaFile.matcher(text);
      if (m.find()){
        String filepath = m.group(1);
        int line = 0;
        if (m.groupCount() == 2 && m.group(2) != null && !m.group(2).isEmpty()){
          line = new Integer(m.group(2));
        }
        //EclipseJPF.logInfo(new Integer(line).toString());
        IFile file = EclipseJPF.getIFile(filepath);
        if (file != null)
          source.addHyperlink(new HyperLink(file, line), event.getOffset(), event.getLength());
      }
    } catch (BadLocationException e) {
      EclipseJPF.logError("Bad location", e);
    }
    
  }
  
}