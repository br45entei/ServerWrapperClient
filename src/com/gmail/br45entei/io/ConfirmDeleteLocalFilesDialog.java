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

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class ConfirmDeleteLocalFilesDialog extends Dialog {
	private static volatile boolean	runFromMain	= true;
	
	protected Response				result		= Response.NO_RESPONSE;
	protected Shell					shell;
	
	/** Create the dialog.
	 * 
	 * @param parent
	 * @param style */
	public ConfirmDeleteLocalFilesDialog(Shell parent) {
		super(parent, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
		setText("SWT Dialog");
	}
	
	/** Open the dialog.
	 * 
	 * @return the result */
	public Response open(String downloadedFilesPath) {
		createContents(downloadedFilesPath);
		this.shell.open();
		this.shell.layout();
		Display display = getParent().getDisplay();
		while(!this.shell.isDisposed()) {
			if(runFromMain) {
				Main.mainLoop();
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
		return this.result;
	}
	
	/** Create contents of the dialog. */
	private void createContents(String downloadedFilesPath) {
		this.shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
		this.shell.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				e.doit = e.character != SWT.ESC;
			}
		});
		this.shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				e.doit = false;
				ConfirmDeleteLocalFilesDialog.this.result = Response.CLOSE;
			}
		});
		this.shell.setSize(387, 145);
		this.shell.setText("Confirm delete local files - " + Main.getShellTitle());
		this.shell.setImages(Main.getShellImages());
		Functions.centerShell2OnShell1(this.getParent(), this.shell);
		
		Label lblAreYouSure = new Label(this.shell, SWT.WRAP);
		lblAreYouSure.setBounds(10, 10, 360, 71);
		lblAreYouSure.setText("Are you sure you wish to delete the downloaded server files located in the following directory? This cannot be undone.\r\n" + downloadedFilesPath);
		
		Button btnYes = new Button(this.shell, SWT.NONE);
		btnYes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ConfirmDeleteLocalFilesDialog.this.result = Response.YES;
			}
		});
		btnYes.setBounds(10, 87, 177, 23);
		btnYes.setText("Yes");
		
		Button btnNo = new Button(this.shell, SWT.NONE);
		btnNo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ConfirmDeleteLocalFilesDialog.this.result = Response.NO;
			}
		});
		btnNo.setBounds(193, 87, 177, 23);
		btnNo.setText("No");
	}
	
}
