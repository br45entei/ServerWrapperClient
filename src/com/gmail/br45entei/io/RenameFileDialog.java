/**
 * 
 */
package com.gmail.br45entei.io;

import com.gmail.br45entei.main.Main;
import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.swt.Response;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class RenameFileDialog extends Dialog {
	private static volatile boolean	runFromMain		= true;
	
	protected Response				result			= Response.NO_RESPONSE;
	protected Shell					shell;
	protected Text					text;
	
	protected String				renameTo		= "";
	protected String				originalName	= "";
	
	/** Create the dialog.
	 * 
	 * @param parent
	 * @param style */
	public RenameFileDialog(Shell parent) {
		super(parent, SWT.DIALOG_TRIM);
		setText("SWT Dialog");
	}
	
	/** Open the dialog.
	 * 
	 * @return the result */
	public String open(String originalName) {
		this.originalName = originalName;
		createContents();
		this.shell.open();
		this.shell.layout();
		Display display = getParent().getDisplay();
		while(!this.shell.isDisposed()) {
			if(runFromMain) {
				Main.mainLoop();
				this.updateUI();
			} else {
				if(!display.readAndDispatch()) {
					this.shell.update();
					Functions.sleep(10);//display.sleep();
				}
			}
			if(this.result != Response.NO_RESPONSE) {
				break;
			}
		}
		if(!this.shell.isDisposed()) {
			this.shell.dispose();
		}
		return this.renameTo;
	}
	
	private final void updateUI() {
		Functions.setTextFor(this.shell, "Rename file - " + this.getParent().getText());
		Functions.setShellImages(this.shell, Main.getShellImages());
	}
	
	/** Create contents of the dialog. */
	private final void createContents() {
		this.shell = new Shell(this.getParent(), getStyle());
		this.shell.setSize(450, 140);
		this.shell.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if(e.character == SWT.ESC) {
					e.doit = false;
				}
			}
		});
		this.shell.setText("Rename file - " + this.getParent().getText());
		this.shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				e.doit = false;
				RenameFileDialog.this.result = Response.CLOSE;
			}
		});
		Functions.centerShell2OnShell1(getParent(), this.shell);
		this.shell.setImages(Main.getShellImages());
		
		Label lblPleaseChooseA = new Label(this.shell, SWT.BORDER | SWT.WRAP);
		lblPleaseChooseA.setBounds(10, 10, 424, 40);
		lblPleaseChooseA.setText("Please choose a new name for the file \"" + this.originalName + "\":");
		
		Button btnDone = new Button(this.shell, SWT.NONE);
		btnDone.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				RenameFileDialog.this.renameTo = RenameFileDialog.this.text.getText();
				RenameFileDialog.this.result = Response.DONE;
			}
		});
		btnDone.setBounds(10, 81, 209, 23);
		btnDone.setText("Done");
		
		Button btnCancel = new Button(this.shell, SWT.NONE);
		btnCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				RenameFileDialog.this.renameTo = RenameFileDialog.this.originalName;
				RenameFileDialog.this.result = Response.CANCEL;
			}
		});
		btnCancel.setBounds(225, 81, 209, 23);
		btnCancel.setText("Cancel");
		
		this.text = new Text(this.shell, SWT.BORDER);
		this.text.setBounds(10, 56, 424, 19);
		
	}
}
