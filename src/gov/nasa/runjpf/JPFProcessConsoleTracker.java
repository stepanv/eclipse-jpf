package gov.nasa.runjpf;

import gov.nasa.runjpf.internal.launching.JPFDebugger;
import gov.nasa.runjpf.internal.launching.JPFRunner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.console.IPatternMatchListenerDelegate;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

/**
 * The tracker (referenced from <tt>/plugin.xml</tt>) for process type
 * <tt>gov.nasa.jpf.ui.jpfProcess</tt> that is injected by {@link JPFRunner} on
 * top of original <tt>StandardVMRunner</tt> configuration implementation. The same for {@link JPFDebugger}.<br/>
 * Refer to {@link JPFRunner#jpfProcessDefaultMap()}.
 * 
 * @author stepan
 * 
 */
public class JPFProcessConsoleTracker implements IPatternMatchListenerDelegate {

  /**
   * The console associated with this line tracker
   */
  private TextConsole console;

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.console.IPatternMatchListenerDelegate#connect(org.eclipse
   * .ui.console.IConsole)
   */
  public void connect(TextConsole console) {
    this.console = console;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.console.IPatternMatchListenerDelegate#disconnect()
   */
  public void disconnect() {
    console = null;
  }

  // Matches java file paths with optional line numbers
  private static final Pattern JAVA_FILE_PATTERN = Pattern.compile("([\\w\\\\/]+\\.java)(?::(\\d+))?");

  protected TextConsole getConsole() {
    return console;
  }

  /**
   * Match found event.<br/>
   * Reuses the original implementation from this plug-in
   * <tt>gov.nasa.runjpf.PatternMatcher</tt> class.
   */
  @Override
  public void matchFound(PatternMatchEvent event) {
    try {
      int offset = event.getOffset();
      int length = event.getLength();
      String text = console.getDocument().get(offset, length);

      Matcher m = JAVA_FILE_PATTERN.matcher(text);
      if (m.find()) {
        String filepath = m.group(1);
        int line = 0;
        if (m.groupCount() == 2 && m.group(2) != null && !m.group(2).isEmpty()) {
          line = new Integer(m.group(2));
        }
        IFile file = EclipseJPF.getIFile(filepath);
        if (file != null) {
          console.addHyperlink(new HyperLink(file, line), event.getOffset(), event.getLength());
        }
      }
    } catch (BadLocationException e) {
      EclipseJPF.logError("Bad location", e);
    }
  }

}
