/**
 * 
 */
package com.gmail.br45entei.io;

import com.gmail.br45entei.data.Property;
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
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class UploadProgress extends Dialog {
	private static volatile boolean	runFromMain	= true;
	
	protected Response				result		= Response.NO_RESPONSE;
	protected Shell					shell;
	private final Property<Double>	progress;
	private final Property<String>	fileName;
	private ProgressBar				progressBar;
	private Label					lblUploadingFile;
	private final boolean			upload;
	
	/** Create the dialog. */
	public UploadProgress(Shell parent, boolean uploadOrDownload, Property<Double> progress, Property<String> fileName) {
		super(parent, SWT.DIALOG_TRIM);
		setText("SWT Dialog");
		this.progress = progress;
		this.fileName = fileName == null ? new Property<>("") : fileName;
		this.upload = uploadOrDownload;
		this.createContents();
	}
	
	/** Open the dialog.
	 * 
	 * @return the result */
	public Response open() {
		this.shell.open();
		this.shell.layout();
		while(!this.shell.isDisposed()) {
			if(runFromMain) {
				Main.mainLoop();
				this.updateUI();
			} else {
				if(!this.shell.getDisplay().readAndDispatch()) {
					this.shell.update();
					Functions.sleep(10);//display.sleep();
				}
			}
			if(this.result != Response.NO_RESPONSE || this.progressBar.getSelection() == 100) {
				break;
			}
		}
		if(!this.shell.isDisposed()) {
			this.shell.dispose();
		}
		return this.result;
	}
	
	private final void updateUI() {
		int selection = (int) Math.round(this.progress.getValue().doubleValue() * 100.00D);
		if(this.progressBar.getSelection() != selection) {
			this.progressBar.setSelection(selection);
		}
		Functions.setTextFor(this.lblUploadingFile, (this.upload ? "Upload" : "Download") + "ing file \"" + this.fileName.getValue() + "\":");
		Functions.setTextFor(this.shell, "File " + (this.upload ? "upload" : "download") + " progress - " + this.getParent().getText());
		Functions.setShellImages(this.shell, Main.getShellImages());
	}
	
	/** Create contents of the dialog. */
	private void createContents() {
		this.shell = new Shell(this.getParent(), SWT.DIALOG_TRIM);
		this.shell.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if(e.character == SWT.ESC) {
					e.doit = false;
				}
			}
		});
		this.shell.setText("File " + (this.upload ? "upload" : "download") + " progress - " + this.getParent().getText());
		this.shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				e.doit = false;
				UploadProgress.this.result = Response.CLOSE;
			}
		});
		this.shell.setSize(420, 120);
		Functions.centerShell2OnShell1(this.getParent(), this.shell);
		this.shell.setImages(Main.getShellImages());
		
		this.lblUploadingFile = new Label(this.shell, SWT.BORDER | SWT.WRAP);
		this.lblUploadingFile.setBounds(10, 10, 394, 49);
		this.lblUploadingFile.setText((this.upload ? "Upload" : "Download") + "ing file \"" + this.fileName.getValue() + "\":");
		
		this.progressBar = new ProgressBar(this.shell, SWT.NONE);
		this.progressBar.setBounds(10, 65, 394, 18);
	}
	
	public final void close() {
		this.result = Response.CLOSE;
	}
	
}
