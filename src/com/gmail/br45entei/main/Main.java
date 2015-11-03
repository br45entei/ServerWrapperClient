package com.gmail.br45entei.main;

import com.gmail.br45entei.io.ServerConnection;
import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.util.StringUtil;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;

/** @author Brian_Entei */
public class Main {
	
	protected static Thread							swtThread;
	protected static volatile boolean				isRunning				= false;
	
	protected static Display						display;
	protected static Shell							shell;
	
	/** The socket that the server will be listening on */
	public static final int							server_listen_port		= 17349;
	protected static Text							serverIP;
	protected static Spinner						serverPort;
	protected static volatile int					lastServerPort			= server_listen_port;
	protected static volatile int					serverPortChangeTo		= server_listen_port;
	protected static volatile boolean				reconnectToServer		= false;
	protected static Text							clientUsername;
	protected static Text							clientPassword;
	protected static StyledText						consoleOutput;
	protected static Text							commandInput;
	
	protected static final HashMap<Integer, String>	consoleLogs				= new HashMap<>();
	protected static final HashMap<Integer, String>	consoleErrs				= new HashMap<>();
	
	protected static volatile String				errorStr				= null;
	
	protected static Button							sendCmd;
	
	protected static volatile ServerConnection		server;
	
	protected static volatile boolean				attemptingConnection	= false;
	protected static Button							btnConnectToServer;
	protected static Button							btnDisconnectFromServer;
	protected static StyledText						stackTraceOutput;
	protected static Button							btnStartServer;
	protected static Button							btnStopServer;
	
	protected static volatile boolean				startRemoteServer		= false;
	protected static volatile boolean				stopRemoteServer		= false;
	
	protected static final void addLogFromServer(String log) {
		if(log.trim().isEmpty() || log.trim().equals("COMMAND SENT")) {
			return;
		}
		if(log.startsWith("LOG:")) {
			log = log.substring(4).trim();
		} else if(log.startsWith("WARN:")) {
			log = log.substring(5).trim();
			Integer key = Integer.valueOf(consoleErrs.size());
			consoleErrs.put(key, log);
			return;
		}
		Integer key = Integer.valueOf(consoleLogs.size());
		consoleLogs.put(key, log);
	}
	
	protected static volatile String	deleteMe	= "";
	
	protected static final boolean handleServerData(String data) throws Throwable {
		if(data != null) {
			if(data.trim().equals("RemAdmin/1.0 -1 CLOSE")) {
				return false;
			} else if(data.trim().startsWith("PORT-CHANGE: ")) {
				String portStr = data.substring("PORT-CHANGE:".length());
				portStr = portStr.startsWith(" ") ? portStr.substring(1) : portStr;
				if(StringUtil.isStrLong(portStr)) {
					serverPortChangeTo = Long.valueOf(portStr).intValue();
					return true;//false;
				}
			}
			if(data.startsWith("PONG: ")) {
				if(server != null) {
					String pongStr = data.substring("COMMAND:".length());
					pongStr = pongStr.startsWith(" ") ? pongStr.substring(1) : pongStr;
					server.pongStr = pongStr;
				}
			} else if(data.equals("RemAdmin/1.0 10 RESET LOGS")) {
				consoleLogs.clear();
				consoleErrs.clear();
			} else if(data.trim().toUpperCase().startsWith("SERVER-STATE: ")) {
				//System.out.println("Test 3: \"" + data + "\"");
				if(server != null) {
					//System.out.println("Test 4");
					data = data.trim();
					String serverState = data.substring("SERVER-STATE:".length());
					serverState = serverState.startsWith(" ") ? serverState.substring(1).trim() : serverState.trim();
					String[] split = serverState.split(Pattern.quote(","));
					if(split.length >= 2) {
						//System.out.println("Test 5");
						String activeState = split[0].trim();
						String selectedState = StringUtil.stringArrayToString(split, ',', 1).trim();
						server.serverActive = activeState.equalsIgnoreCase("ACTIVE");
						server.serverJarSelected = selectedState.equalsIgnoreCase("SELECTED");
					} else {
						System.out.println("Test 6");
						server.serverActive = serverState.equalsIgnoreCase("ACTIVE");
					}
					deleteMe = "Server state: \"" + serverState + "\";\r\nresulting booleans: active: " + server.serverActive + "; jar selected: " + server.serverJarSelected + ";\r\nLast check: " + StringUtil.getTime(lastServerStateQuery, false, false) + ";\r\nLast receive: " + (server != null ? StringUtil.getTime(server.lastServerStateSet, false, false) : "null");
					server.lastServerStateSet = System.currentTimeMillis();
				}
			} else {
				addLogFromServer(data);
			}
		}
		return true;
	}
	
	/** @return True if the current thread is the main SWT thread */
	public static final boolean checkThreadAccess() {
		return Thread.currentThread() == swtThread;
	}
	
	/** Launch the application.
	 * 
	 * @param args System command arguments */
	public static void main(String[] args) {
		swtThread = Thread.currentThread();
		Thread consoleLogUpdaterThread = new Thread() {
			@Override
			public final void run() {
				if(!isRunning) {
					while(!isRunning) {
						Functions.sleep(10L);
					}
				}
				while(isRunning) {
					if(server != null && server.isAlive(/*!attemptingConnection*/)) {
						try {
							if(!handleServerData(StringUtil.readLine(server.in))) {
								server.close("RemAdmin/1.0 -1 CLOSE");
							}
						} catch(IOException e) {
							if("Socket closed".equals(e.getMessage())) {
								errorStr = "Connection to server closed.";
							} else if("Connection reset".equals(e.getMessage())) {
								errorStr = "Connection to server reset.";
							} else {
								e.printStackTrace();
								errorStr = "Unable to read server response: " + Functions.throwableToStr(e);
							}
							server.close("RemAdmin/1.0 -1 CLOSE");
						} catch(Throwable e) {
							e.printStackTrace();
						}
					}
					Functions.sleep(10L);
				}
			}
		};
		consoleLogUpdaterThread.setDaemon(true);
		consoleLogUpdaterThread.start();
		Thread serverStateQuerier = new Thread() {
			@Override
			public final void run() {
				if(!isRunning) {
					while(!isRunning) {
						Functions.sleep(10L);
					}
				}
				while(isRunning) {
					serverStateQuery();
					if(startRemoteServer && !server.serverActive) {
						sendCommand("START-SERVER");
						startRemoteServer = false;
					} else if(stopRemoteServer && server.serverActive) {
						sendCommand("STOP-SERVER");
						stopRemoteServer = false;
					}
					Functions.sleep(10L);
				}
			}
		};
		serverStateQuerier.setDaemon(true);
		serverStateQuerier.start();
		display = Display.getDefault();
		shell = new Shell(display, SWT.TITLE | SWT.CLOSE | SWT.MIN);
		shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				e.doit = false;
				shutdown();
			}
		});
		shell.setSize(700, 460);
		shell.setText("Minecraft Server Wrapper Client - Made by Brian_Entei");
		shell.setImages(new Image[] {SWTResourceManager.getImage(Functions.class, "/assets/textures/title/Entei-16x16.png"), SWTResourceManager.getImage(Functions.class, "/assets/textures/title/Entei-32x32.png"), SWTResourceManager.getImage(Functions.class, "/assets/textures/title/Entei-64x64.png"), SWTResourceManager.getImage(Functions.class, "/assets/textures/title/Entei-128x128.png")});
		Functions.centerShellOnPrimaryMonitor(shell);
		
		createContents();
		
		isRunning = true;
		openShell();
		while(isRunning && !shell.isDisposed()) {
			mainLoop();
		}
		if(!shell.isDisposed()) {
			shell.dispose();
		}
	}
	
	public static final void shutdown() {
		isRunning = false;
	}
	
	protected static final void mainLoop() {
		if(!checkThreadAccess()) {
			Functions.sleep(10L);
			return;
		}
		if(display.isDisposed() || shell.isDisposed()) {
			return;
		}
		runClock();
		updateUI();
		runClock();
	}
	
	protected static final void runClock() {
		if(!isRunning || display.isDisposed() || shell.isDisposed()) {
			return;
		}
		if(!display.readAndDispatch()) {
			shell.update();
			Functions.sleep(10L);//display.sleep();
		}
	}
	
	private static volatile long	lastServerStateQuery	= 0L;
	private static Label			statusLabel;
	
	protected static final void serverStateQuery() {
		if(server != null && !attemptingConnection && server.isAlive(/*!attemptingConnection*/)) {
			final long now = System.currentTimeMillis();
			long elapsedTime = now - lastServerStateQuery;
			if(elapsedTime >= 100L) {
				lastServerStateQuery = now;
				sendCommand("GET: SERVER-STATE");
			}
		}
		if(server == null || !server.isAlive()) {
			if(serverPortChangeTo != lastServerPort) {
				reconnectToServer = true;
			}
		}
	}
	
	protected static final void updateUI() {
		if(!isRunning || shell.isDisposed()) {
			return;
		}
		if(reconnectToServer) {
			serverPort.setSelection(serverPortChangeTo);
			lastServerPort = serverPortChangeTo;
			reconnectToServer = false;
			connectToServer();
		}
		final boolean connected = server != null && server.isAlive(/*!attemptingConnection*/);
		btnConnectToServer.setEnabled(attemptingConnection ? false : !connected);
		btnDisconnectFromServer.setEnabled(attemptingConnection ? false : connected);
		String connectText = attemptingConnection ? "Attempting conenction..." : (connected ? "Connected to server" : "Connect to server");
		if(!btnConnectToServer.getText().equals(connectText)) {
			btnConnectToServer.setText(connectText);
		}
		btnStartServer.setEnabled(connected && !server.serverActive && server.serverJarSelected && !attemptingConnection);
		btnStopServer.setEnabled(connected && server.serverActive && !attemptingConnection);
		serverIP.setEnabled(!connected && !attemptingConnection);
		serverPort.setEnabled(!connected && !attemptingConnection);
		clientUsername.setEnabled(!connected && !attemptingConnection);
		clientPassword.setEnabled(!connected && !attemptingConnection);
		sendCmd.setEnabled(connected && !attemptingConnection);
		commandInput.setEnabled(connected && !attemptingConnection);
		String status = "Status: " + (attemptingConnection ? "Connecting to server" : (server != null && server.isAlive() ? (server.serverActive ? "Server active" : server.serverJarSelected ? "Server inactive" : "No jar selected") : "Not connected"));
		if(!statusLabel.getText().equals(status)) {
			statusLabel.setText(status);
		}
		setTextFor(consoleOutput);
		setTextFor(stackTraceOutput);
	}
	
	private static final void createContents() {
		
		Label lblServerIp = new Label(shell, SWT.NONE);
		lblServerIp.setBounds(10, 10, 55, 21);
		lblServerIp.setText("Server ip:");
		
		serverIP = new Text(shell, SWT.BORDER);
		serverIP.setBounds(71, 10, 125, 21);
		
		Label lblPort = new Label(shell, SWT.NONE);
		lblPort.setBounds(202, 10, 35, 21);
		lblPort.setText("Port:");
		
		serverPort = new Spinner(shell, SWT.BORDER);
		serverPort.setMaximum(65535);
		serverPort.setSelection(server_listen_port);
		serverPort.setBounds(243, 10, 69, 22);
		
		Label label = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setBounds(10, 37, 302, 2);
		
		Label lblUsername = new Label(shell, SWT.NONE);
		lblUsername.setBounds(10, 45, 62, 21);
		lblUsername.setText("Username:");
		
		clientUsername = new Text(shell, SWT.BORDER);
		clientUsername.setBounds(78, 45, 76, 21);
		
		Label lblPassword = new Label(shell, SWT.NONE);
		lblPassword.setBounds(160, 45, 69, 21);
		lblPassword.setText("Password:");
		
		clientPassword = new Text(shell, SWT.BORDER | SWT.PASSWORD);
		clientPassword.setBounds(235, 45, 76, 21);
		
		Menu menu = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menu);
		
		MenuItem mntmfile = new MenuItem(menu, SWT.CASCADE);
		mntmfile.setText("&File");
		mntmfile.setAccelerator(SWT.ALT | 'F');
		
		Menu menu_1 = new Menu(mntmfile);
		mntmfile.setMenu(menu_1);
		
		MenuItem mntmExitaltF = new MenuItem(menu_1, SWT.NONE);
		mntmExitaltF.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shutdown();
			}
		});
		mntmExitaltF.setText("E&xit\t(Alt + F4)");
		mntmExitaltF.setAccelerator(SWT.ALT | SWT.F4);
		
		Label label_1 = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		label_1.setBounds(10, 72, 302, 2);
		
		Label label_2 = new Label(shell, SWT.SEPARATOR | SWT.VERTICAL);
		label_2.setBounds(318, 10, 2, 391);
		
		consoleOutput = new StyledText(shell, SWT.BORDER | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
		consoleOutput.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_FOREGROUND));
		consoleOutput.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		consoleOutput.setEditable(false);
		consoleOutput.setBounds(326, 31, 358, 339);
		
		Label lblConsoleOutput = new Label(shell, SWT.NONE);
		lblConsoleOutput.setBounds(326, 10, 91, 15);
		lblConsoleOutput.setText("Console output:");
		
		commandInput = new Text(shell, SWT.BORDER);
		commandInput.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.character == SWT.CR) {
					e.doit = false;
					sendInput(commandInput.getText());
					commandInput.setText("");
				}
			}
		});
		commandInput.setBounds(326, 376, 277, 25);
		
		sendCmd = new Button(shell, SWT.NONE);
		sendCmd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sendInput(commandInput.getText());
				commandInput.setText("");
			}
		});
		sendCmd.setBounds(609, 376, 75, 25);
		sendCmd.setText("Send Cmd");
		
		btnConnectToServer = new Button(shell, SWT.NONE);
		btnConnectToServer.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				connectToServer();
			}
		});
		btnConnectToServer.setBounds(10, 80, 302, 25);
		btnConnectToServer.setText("Connect to server");
		
		btnDisconnectFromServer = new Button(shell, SWT.NONE);
		btnDisconnectFromServer.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				closeServer();
			}
		});
		btnDisconnectFromServer.setBounds(10, 111, 302, 25);
		btnDisconnectFromServer.setText("Disconnect from server");
		
		Label label_3 = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		label_3.setBounds(10, 142, 302, 2);
		
		Label lblStackTraceserrors = new Label(shell, SWT.NONE);
		lblStackTraceserrors.setBounds(10, 150, 116, 15);
		lblStackTraceserrors.setText("Stack Traces(Errors):");
		
		stackTraceOutput = new StyledText(shell, SWT.BORDER | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
		stackTraceOutput.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		stackTraceOutput.setEditable(false);
		stackTraceOutput.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
		stackTraceOutput.setBounds(10, 171, 302, 199);
		
		btnStartServer = new Button(shell, SWT.NONE);
		btnStartServer.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				startRemoteServer = true;
			}
		});
		btnStartServer.setBounds(10, 376, 75, 25);
		btnStartServer.setText("Start Server");
		
		btnStopServer = new Button(shell, SWT.NONE);
		btnStopServer.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				stopRemoteServer = true;
			}
		});
		btnStopServer.setBounds(91, 376, 75, 25);
		btnStopServer.setText("Stop Server");
		
		statusLabel = new Label(shell, SWT.NONE);
		statusLabel.setBounds(172, 381, 140, 15);
		statusLabel.setText("Status:");
	}
	
	protected static final void closeServer() {
		if(server != null) {
			server.close("RemAdmin/1.0 -1 CLOSE");
		}
	}
	
	protected static final void connectToServer() {
		final String serverIP = Main.serverIP.getText();
		final int serverPort = Main.serverPort.getSelection();
		final String clientUsername = Main.clientUsername.getText();
		final String clientPassword = Main.clientPassword.getText();
		Thread thread = new Thread(Thread.currentThread().getName() + "_ConnectToServerThread") {
			@Override
			public final void run() {
				if(server == null || !server.isAlive(/*true*/)) {
					attemptingConnection = true;
					errorStr = null;
					System.out.println("Attempting connection to \"" + serverIP + ":" + serverPort + "\":");
					//closeServer();
					mainLoop();
					try {
						server = ServerConnection.connectTo(serverIP, serverPort);
						if(server == null) {
							errorStr = "Unresolved hostname: \"" + serverIP + "\"!";
							attemptingConnection = false;
							return;
						}
						if(server.isAlive()) {
							lastServerPort = serverPort;
							serverPortChangeTo = serverPort;
							errorStr = null;
							consoleLogs.clear();
							consoleErrs.clear();
							String authLine = "CONNECT " + Base64.getEncoder().encodeToString((clientUsername + ":" + clientPassword).getBytes()).trim() + " RemAdmin/1.0";
							System.out.println("Connection to server \"" + serverIP + ":" + serverPort + "\" successful. Authenticating...");// Sending authentication line: \"" + authLine + "\":");
							server.out.println(authLine);
							server.out.flush();
						}
					} catch(ConnectException e) {
						errorStr = "Unable to connect to server \"" + serverIP + ":" + serverPort + "\": " + e.getMessage();
					} catch(IOException e) {
						System.err.print("Unable to connect to server\"" + serverIP + ":" + serverPort + "\":");
						e.printStackTrace();
						errorStr = "Failed to connect to server \"" + serverIP + ":" + serverPort + "\": " + e.getMessage();
					} catch(Throwable e) {
						e.printStackTrace();
						errorStr = "An unhandled exception occurred: " + Functions.throwableToStr(e);
					}
					attemptingConnection = false;
				}
			}
		};
		thread.setDaemon(true);
		thread.start();
	}
	
	protected static final void sendCommands(String[] cmds) {
		boolean printed = false;
		for(String cmd : cmds) {
			if(cmd != null) {
				server.out.print(cmd + "\r\n");
				printed = true;
			}
		}
		if(printed) {
			server.out.print("\r\n");
			server.out.flush();
		}
	}
	
	protected static final void sendCommand(String cmd) {
		if(cmd != null) {
			server.out.print(cmd + "\r\n");
			server.out.print("\r\n");
			server.out.flush();
		}
	}
	
	protected static final void sendInput(String input) {
		server.out.print("COMMAND: " + input + "\r\n");
		server.out.print("\r\n");
		server.out.flush();
	}
	
	protected static final void setTextFor(StyledText styledText) {
		String text = "";
		if(styledText == consoleOutput) {//if(errorStr == null) {
			try {
				ArrayList<Integer> keySet = new ArrayList<>(consoleLogs.keySet());
				Collections.sort(keySet);
				for(Integer key : keySet) {
					String log = consoleLogs.get(key);
					if(log != null && !log.isEmpty()) {
						text += log + "\n";
					}
				}
				if(server != null && server.isAlive()) {
					text += ">";//XXX Console caret addition
				}
			} catch(ConcurrentModificationException ignored) {
			}
		} else {
			if(errorStr == null) {
				ArrayList<Integer> keySet = new ArrayList<>(consoleErrs.keySet());
				Collections.sort(keySet);
				for(Integer key : keySet) {
					String log = consoleErrs.get(key);
					if(log != null && !log.isEmpty()) {
						text += log + "\n";
					}
				}
			} else {
				text = errorStr;
			}
			//XXX DEBUG: text += "\r\n" + deleteMe;
		}
		if(!styledText.getText().equals(text)) {
			final int numOfVisibleLines = Math.floorDiv(styledText.getSize().y, styledText.getLineHeight());
			final int originalIndex = styledText.getTopIndex();
			int index = originalIndex;
			final int lineCount = styledText.getLineCount();
			/*if(HTTPClientRequest.debug) {
				this.txtInputfield.setText("index: \"" + index + "\"; line count: \"" + lineCount + "\"; visible lines: \"" + numOfVisibleLines + "\";");
			}*/
			runClock();
			if(lineCount - index == numOfVisibleLines) {
				index = -1;
			}
			final Point selection = styledText.getSelection();
			final int caretOffset = styledText.getCaretOffset();
			//==
			//styledText.setText(text);
			styledText.getContent().setText(text);
			//==
			try {
				if(caretOffset == selection.x) {//Right to left text selection
					styledText.setCaretOffset(caretOffset);
					styledText.setSelection(selection.y, selection.x);
				} else {//Left to right text selection
					styledText.setSelection(selection);
					styledText.setCaretOffset(caretOffset);
				}
			} catch(IllegalArgumentException ignored) {
			}
			final int newLineCount = styledText.getLineCount();
			if(index == -1) {
				index = newLineCount - 1;
			} else {
				if(newLineCount >= lineCount) {
					index = newLineCount - (lineCount - index);
				} else {
					index = newLineCount - (newLineCount - index);
				}
			}
			styledText.setTopIndex(index);//originalIndex);//this.isScrollLocked ? originalIndex : index);
			runClock();
		}
	}
	
	protected static final void openShell() {
		if(!shell.isVisible()) {
			shell.setVisible(true);
			shell.open();
			shell.layout();
		}
	}
	
	protected static final void hideShell() {
		if(shell.isVisible()) {
			shell.setVisible(false);
		}
	}
}
