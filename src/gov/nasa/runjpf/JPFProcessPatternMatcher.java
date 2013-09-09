package gov.nasa.runjpf;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.console.IPatternMatchListenerDelegate;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

public class JPFProcessPatternMatcher implements IPatternMatchListenerDelegate {

  /**
   * The console associated with this line tracker
   */
  private TextConsole fConsole;

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.console.IPatternMatchListenerDelegate#connect(org.eclipse
   * .ui.console.IConsole)
   */
  public void connect(TextConsole console) {
    fConsole = console;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.console.IPatternMatchListenerDelegate#disconnect()
   */
  public void disconnect() {
    fConsole = null;
  }

  // Matches java file paths with optional line numbers
  private static final Pattern JAVA_FILE_PATTERN = Pattern.compile("([\\w\\\\/]+\\.java)(?::(\\d+))?");

  protected TextConsole getConsole() {
    return fConsole;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.console.IPatternMatchListenerDelegate#matchFound(org.eclipse
   * .ui.console.PatternMatchEvent)
   */
  public void matchFound(PatternMatchEvent event) {
    try {
      int offset = event.getOffset();
      int length = event.getLength();
      String text = fConsole.getDocument().get(offset, length);

      Matcher m = JAVA_FILE_PATTERN.matcher(text);
      if (m.find()) {
        String filepath = m.group(1);
        int line = 0;
        if (m.groupCount() == 2 && m.group(2) != null && !m.group(2).isEmpty()) {
          line = new Integer(m.group(2));
        }
        // EclipseJPF.logInfo(new Integer(line).toString());
        IFile file = EclipseJPF.getIFile(filepath);
        if (file != null) {
          fConsole.addHyperlink(new HyperLink(file, line), event.getOffset(), event.getLength());
        }
      }
    } catch (BadLocationException e) {
      EclipseJPF.logError("Bad location", e);
    }
  }

}
