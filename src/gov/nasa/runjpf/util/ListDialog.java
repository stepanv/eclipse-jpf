package gov.nasa.runjpf.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

public class ListDialog extends Dialog {

  String title;
  String message;
  String[] items = new String[0];

  Shell shell;
  Label infoLabel;
  List list;

  String[] selection;

  public ListDialog(Shell parent) {
    this(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
  }

  public ListDialog(Shell parent, int style) {
    super(parent, style);
  }

  protected void createContents(final Shell shell) {
    int margin = 10;
    FormData fd;

    shell.setLayout(new FormLayout());

    infoLabel = new Label(shell, SWT.NONE);
    fd = new FormData();
    fd.left = new FormAttachment(0, margin);
    fd.top = new FormAttachment(0, margin);
    infoLabel.setLayoutData(fd);

    infoLabel.setText(message);

    Button selectButton = new Button(shell, SWT.PUSH);
    selectButton.setText("Select");
    fd = new FormData();
    fd.right = new FormAttachment(100, -margin);
    fd.bottom = new FormAttachment(100, -margin);
    selectButton.setLayoutData(fd);
    selectButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        selection = list.getSelection();
        if (selection == null) {
          setMessage("no item selected!");
        } else {
          shell.close();
        }
      }
    });

    Button cancelButton = new Button(shell, SWT.PUSH);
    cancelButton.setText("Cancel");
    fd = new FormData();
    fd.right = new FormAttachment(selectButton, -margin);
    fd.bottom = new FormAttachment(100, -margin);
    cancelButton.setLayoutData(fd);
    cancelButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        selection = null;
        shell.close();
      }
    });

    list = new List(shell, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
    fd = new FormData();
    fd.left = new FormAttachment(0, margin);
    fd.right = new FormAttachment(100, -margin);
    fd.top = new FormAttachment(infoLabel, margin);
    fd.bottom = new FormAttachment(cancelButton, -margin);
    list.setLayoutData(fd);
    list.setItems(items);

    shell.setDefaultButton(selectButton);
  }

  public void setTitle(String title) {
    this.title = title;
    if (shell != null) {
      shell.setText(message);
    }
  }

  public void setMessage(String message) {
    this.message = message;
    if (infoLabel != null) {
      infoLabel.setText(message);
    }
  }

  public void setItems(String... items) {
    this.items = items;
    if (list != null) {
      list.setItems(items);
    }
  }

  public void setItems(java.util.List<String> itemList) {
    String[] items = itemList.toArray(new String[itemList.size()]);
    setItems(items);
  }

  public String[] getSelection() {
    return selection;
  }

  public String[] open() {
    shell = new Shell(getParent(), getStyle());
    shell.setText(title);
    createContents(shell);
    shell.pack();

    Point pointerLoc = Display.getDefault().getCursorLocation();
    shell.setLocation(pointerLoc.x, pointerLoc.y);

    shell.open();

    Display display = getParent().getDisplay();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) {
        display.sleep();
      }
    }

    return getSelection();
  }
}
