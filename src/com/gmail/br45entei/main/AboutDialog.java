/**
 * 
 */
package com.gmail.br45entei.main;

import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.swt.Response;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** @author Brian_Entei */
public class AboutDialog extends Dialog {
	
	protected Response	result	= Response.NO_RESPONSE;
	protected Shell		shell;
	
	/** Create the dialog.
	 * 
	 * @param parent The parent Shell */
	public AboutDialog(Shell parent) {
		super(parent, SWT.DIALOG_TRIM);
		setText("SWT Dialog");
	}
	
	/** Open the dialog.
	 * 
	 * @return the result */
	public Response open() {
		this.createContents();
		this.shell.open();
		this.shell.layout();
		//Display display = getParent().getDisplay();
		while(!this.shell.isDisposed()) {
			if(this.result != Response.NO_RESPONSE) {
				break;
			}
			Main.mainLoop();
			/*if(!display.readAndDispatch()) {
				display.sleep();
			}*/
		}
		if(!this.shell.isDisposed()) {
			this.shell.close();
		}
		return this.result;
	}
	
	/** Create contents of the dialog. */
	private void createContents() {
		this.shell = new Shell(this.getParent(), this.getStyle());
		this.shell.setSize(410, 310);
		this.shell.setText("About " + Main.getDefaultShellTitle());
		this.shell.setImages(Main.getDefaultShellImages());
		Functions.centerShell2OnShell1(this.getParent(), this.shell);
		
		Text lblMessage = new Text(this.shell, SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		lblMessage.setText("Server Wrapper Client is a program written in Java by Brian Entei.\r\nIt is used to connect to a ServerWrapper of the same version over\r\nthe network or internet to make administrating Minecraft servers\r\neasier.\r\n\r\nTo get started, type the ip address and port of an existing\r\nServerWrapper server into the appropriate text fields, enter your\r\ncredentials for logging in, and click on \"Connect to server\".\r\nIf successful, the \"Connection to server\" button will be greyed out,\r\nand you should immediately receive the minecraft server's status\r\nin the bottom left of the window, and the minecraft server's\r\nconsole log if the server is started. Errors from the Minecraft server\r\nalso appear next to the log to the left in red.\r\nYou can even stop and start the Minecraft server with the buttons\r\nin the lower left.\r\n\r\nUsernames and passwords are not saved or sent to anything or\r\nanyone, except for the ServerWrapper itself. Source code for this\r\napplication is available on GitHub at: https://github.com/br45entei/ServerWrapperClient\r\n\r\nQuestions/comments?\r\nLeave me a message at br45entei@gmail.com and I will be sure to\r\nget back with you! Thank you for using Server Wrapper Client.");
		lblMessage.setBounds(10, 10, 384, 230);
		
		Button btnDone = new Button(this.shell, SWT.NONE);
		btnDone.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				AboutDialog.this.result = Response.DONE;
			}
		});
		btnDone.setBounds(164, 246, 75, 25);
		btnDone.setText("Done");
		
	}
	
}
