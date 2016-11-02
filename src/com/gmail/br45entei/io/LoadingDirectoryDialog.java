package com.gmail.br45entei.io;

import com.gmail.br45entei.main.Main;
import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.swt.Response;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class LoadingDirectoryDialog extends Dialog {
	
	private final FTClient	parent;
	
	protected Response		result	= Response.NO_RESPONSE;
	protected Shell			shell;
	private Label			lblUploadingFile;
	
	/** Create the dialog. */
	public LoadingDirectoryDialog(FTClient parent) {
		super(parent.getShell(), SWT.DIALOG_TRIM);
		setText("SWT Dialog");
		this.parent = parent;
		this.createContents();
	}
	
	/** Open the dialog.
	 * 
	 * @return the result */
	public Response open() {
		this.shell.open();
		this.shell.layout();
		while(!this.shell.isDisposed()) {
			this.parent.mainLoop();
			this.updateUI();
			if(this.result != Response.NO_RESPONSE) {
				break;
			}
		}
		if(!this.shell.isDisposed()) {
			this.shell.dispose();
		}
		return this.result;
	}
	
	private final void updateUI() {
		Functions.setShellImages(this.shell, Main.getShellImages());
		Functions.centerShell2OnShell1(this.getParent(), this.shell);
	}
	
	/** Create contents of the dialog. */
	private void createContents() {
		this.shell = new Shell(this.getParent(), SWT.NONE);
		this.shell.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if(e.character == SWT.ESC) {
					e.doit = false;
				}
			}
		});
		this.shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				e.doit = false;
				LoadingDirectoryDialog.this.result = Response.CLOSE;
			}
		});
		this.shell.setSize(162, 36);
		Functions.centerShell2OnShell1(this.getParent(), this.shell);
		this.shell.setImages(Main.getShellImages());
		
		this.lblUploadingFile = new Label(this.shell, SWT.CENTER);
		this.lblUploadingFile.setBounds(10, 10, 142, 13);
		this.lblUploadingFile.setText("Fetching directory list...");
	}
	
	public final void close() {
		this.result = Response.CLOSE;
	}
}
