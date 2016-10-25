package com.gmail.br45entei.main;

import com.gmail.br45entei.data.DisposableByteArrayOutputStream;
import com.gmail.br45entei.io.FTClient;
import com.gmail.br45entei.io.ServerConnection;
import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.swt.Response;
import com.gmail.br45entei.util.StringUtil;
import com.gmail.br45entei.util.StringUtil.EnumOS;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
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
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public final class Main {
	
	public static final boolean								debug					= false;
	
	protected static Thread									swtThread;
	protected static volatile boolean						isRunning				= false;
	
	public static final String								PROTOCOL_NAME			= "RemAdmin";
	public static final String								PROTOCOL_DELIMITER		= "/";
	public static final String								PROTOCOL_VERSION		= "1.02";
	
	/** This application's networking protocol */
	public static final String								PROTOCOL				= PROTOCOL_NAME + PROTOCOL_DELIMITER + PROTOCOL_VERSION;
	
	protected static Display								display;
	protected static Shell									shell;
	
	/** The socket that the server will be listening on */
	public static final int									server_listen_port		= 17349;
	protected static Text									serverIP;
	protected static Spinner								serverPort;
	protected static volatile int							lastServerPort			= server_listen_port;
	protected static volatile int							serverPortChangeTo		= server_listen_port;
	protected static volatile boolean						reconnectToServer		= false;
	protected static Text									clientUsername;
	protected static Text									clientPassword;
	protected static StyledText								consoleOutput;
	protected static Text									commandInput;
	
	protected static MenuItem								mntmExitaltF;
	protected static MenuItem								mntmFileTransfer;
	
	protected static final ConcurrentLinkedQueue<String>	consoleLogs				= new ConcurrentLinkedQueue<>();
	protected static final ConcurrentLinkedQueue<String>	consoleErrs				= new ConcurrentLinkedQueue<>();
	
	private static volatile long							lastLogTime				= 0L;
	private static volatile long							lastErrLogTime			= 0L;
	private static volatile long							lastConstructLogTime	= 0L;
	private static volatile long							lastConstructErrLogTime	= 0L;
	
	//====
	
	public static final File								rootDir					= new File(System.getProperty("user.dir"));
	
	static {
		if(!rootDir.exists()) {
			rootDir.mkdirs();
		}
	}
	
	public static final String		settingsFileName		= "settings.txt";
	private static volatile File	settingsFile;
	private static volatile boolean	failedToCreateSettings	= false;
	
	//====
	
	protected static synchronized void setLastLogTime(long time) {
		lastLogTime = time;
	}
	
	protected static synchronized void setLastErrLogTime(long time) {
		lastErrLogTime = time;
	}
	
	protected static volatile String			errorStr				= null;
	
	protected static Button						sendCmd;
	
	protected static volatile ServerConnection	server;
	protected static volatile FTClient			ftClient				= null;
	
	protected static volatile boolean			attemptingConnection	= false;
	protected static Button						btnConnectToServer;
	protected static Button						btnDisconnectFromServer;
	protected static StyledText					stackTraceOutput;
	protected static Button						btnStartServer;
	protected static Button						btnStopServer;
	protected static Button						btnRestartServer;
	protected static Button						btnKillServer;
	protected static Label						statusLabel;
	protected static Label						verticalSeparator;
	
	protected static volatile boolean			startRemoteServer		= false;
	protected static volatile boolean			stopRemoteServer		= false;
	protected static volatile boolean			restartRemoteServer		= false;
	protected static volatile boolean			killRemoteServer		= false;
	
	protected static volatile boolean			isAboutDialogOpen		= false;
	
	private static volatile long				lastServerStateQuery	= 0L;
	private static volatile long				lastServerResourceQuery	= 0L;
	
	private static volatile boolean				setCustomIconFromServer	= false;
	
	protected static volatile String			lastFTServerPath		= "/";
	
	public static final String getDefaultShellTitle() {
		return "Server Wrapper Client - Version " + PROTOCOL_VERSION;
	}
	
	public static final Image[] getDefaultShellImages() {
		return new Image[] {SWTResourceManager.getImage(Functions.class, "/assets/textures/title/Entei-16x16.png"), SWTResourceManager.getImage(Functions.class, "/assets/textures/title/Entei-32x32.png"), SWTResourceManager.getImage(Functions.class, "/assets/textures/title/Entei-64x64.png"), SWTResourceManager.getImage(Functions.class, "/assets/textures/title/Entei-128x128.png")};
	}
	
	public static final Image[] getShellImages() {
		return shell.getImages();
	}
	
	protected static final void clearLogsFromServer() {
		errorStr = null;
		consoleLogs.clear();
		consoleErrs.clear();
		final long now = System.currentTimeMillis();
		setLastLogTime(now);
		setLastErrLogTime(now);
	}
	
	public static final void addLogFromServer(String log) {
		if(log.trim().isEmpty() || log.trim().equals("COMMAND SENT")) {
			return;
		}
		if(log.startsWith("LOG:")) {
			log = log.substring(4).trim();
		} else if(log.startsWith("WARN:")) {
			log = log.substring(5).trim();
			consoleErrs.add(log);
			if(consoleErrs.size() > 500) {
				while(consoleErrs.size() > 500) {
					consoleErrs.poll();
				}
			}
			setLastErrLogTime(System.currentTimeMillis());
			return;
		}
		consoleLogs.add(log);
		if(consoleLogs.size() > 500) {
			while(consoleLogs.size() > 500) {
				consoleLogs.poll();
			}
		}
		setLastLogTime(System.currentTimeMillis());
	}
	
	protected static volatile String	deleteMe	= "";
	private static MenuItem				mntmSaveToSettings;
	protected static MenuItem			mntmRestartServerAutomatically;
	private static Label				label_2;
	private static TabItem				tbtmResourceUsage;
	private static Composite			consoleTabComposite;
	private static ProgressBar			cpuUsageBar;
	private static Label				cpuUsageTxt;
	private static Label				ramUsageTxt;
	private static ProgressBar			ramUsageBar;
	private static Label				threadCountTxt;
	private static TabFolder			tabFolder;
	private static Composite			resourceTabComposite;
	private static Composite			cpuUsageComposite;
	private static Composite			ramUsageComposite;
	
	protected static final boolean handleServerData(String data) throws Throwable {
		if(data != null) {
			String closeStr = PROTOCOL + " -1 CLOSE";
			if(data.trim().equals(closeStr) || (data.trim().startsWith(PROTOCOL_NAME + PROTOCOL_DELIMITER) && data.trim().endsWith(" -1 CLOSE"))) {
				return false;
			} else if(data.trim().startsWith("PORT-CHANGE: ")) {
				String portStr = data.substring("PORT-CHANGE:".length());
				portStr = portStr.startsWith(" ") ? portStr.substring(1) : portStr;
				if(StringUtil.isStrLong(portStr)) {
					serverPortChangeTo = Long.valueOf(portStr).intValue();
					return true;//false;
				}
			}
			if(data.startsWith("MESSAGE: ")) {
				if(server != null) {
					String msgStr = data.substring("MESSAGE:".length());
					msgStr = msgStr.startsWith(" ") ? msgStr.substring(1) : msgStr;
					if(StringUtil.isStrInt(msgStr)) {
						int length = Integer.valueOf(msgStr).intValue();
						if(debug) {
							addLogFromServer("Message length: " + length);
						}
						if(length > 0) {
							DisposableByteArrayOutputStream baos = new DisposableByteArrayOutputStream();
							int count = 0;
							while(count < length) {
								baos.write(server.in.read());
								count++;
							}
							final String message = new String(baos.toByteArray(), StandardCharsets.UTF_8);
							if(debug) {
								addLogFromServer("Message received: " + message);
							}
							server.popupMessages.add(message);
							baos.dispose();
							baos.close();//Does nothing
							baos = null;
							System.gc();
						}
					}
				}
			} else if(data.startsWith("PONG: ")) {
				if(server != null) {
					String pongStr = data.substring("PONG:".length());
					pongStr = pongStr.startsWith(" ") ? pongStr.substring(1) : pongStr;
					server.pongStr = pongStr;
				}
			} else if(data.equals(PROTOCOL + " 10 RESET LOGS")) {
				clearLogsFromServer();
			} else if(data.trim().toUpperCase().startsWith("SERVER-STATE: ")) {
				if(server != null) {
					data = data.trim();
					String serverState = data.substring("SERVER-STATE:".length());
					serverState = serverState.startsWith(" ") ? serverState.substring(1).trim() : serverState.trim();
					String[] split = serverState.split(Pattern.quote(","));
					if(split.length >= 2) {
						String activeState = split[0].trim();
						String selectedState = StringUtil.stringArrayToString(split, ',', 1).trim();
						server.serverActive = activeState.equalsIgnoreCase("ACTIVE");
						server.serverJarSelected = selectedState.equalsIgnoreCase("SELECTED");
					} else {
						server.serverActive = serverState.equalsIgnoreCase("ACTIVE");
					}
					deleteMe = "Server state: \"" + serverState + "\";\r\nresulting booleans: active: " + server.serverActive + "; jar selected: " + server.serverJarSelected + ";\r\nLast check: " + StringUtil.getTime(lastServerStateQuery, false, false) + ";\r\nLast receive: " + (server != null ? StringUtil.getTime(server.lastServerStateSet, false, false) : "null");
					server.lastServerStateSet = System.currentTimeMillis();
				}
			} else if(data.trim().toUpperCase().startsWith("AUTOMATIC-RESTART: ")) {
				if(server != null) {
					data = data.trim();
					String automaticRestart = data.substring("AUTOMATIC-RESTART:".length());
					automaticRestart = automaticRestart.startsWith(" ") ? automaticRestart.substring(1).trim() : automaticRestart.trim();
					server.automaticRestart = Boolean.valueOf(automaticRestart).booleanValue();
				}
			} else if(data.trim().toUpperCase().startsWith("CPU-USAGE: ")) {
				if(server != null) {
					String usage = data.substring("CPU-USAGE:".length());
					usage = (usage.startsWith(" ") ? usage.substring(1) : usage).trim();
					if(StringUtil.isStrDouble(usage)) {
						server.serverCpuUsage = Double.valueOf(usage).doubleValue();
					}
				}
			} else if(data.trim().toUpperCase().startsWith("RAM-USAGE: ")) {
				if(server != null) {
					String usage = data.substring("RAM-USAGE:".length());
					usage = (usage.startsWith(" ") ? usage.substring(1) : usage).trim();
					String[] split = usage.split(Pattern.quote("-"));
					if(split.length == 2) {
						if(StringUtil.isStrLong(split[0]) && StringUtil.isStrLong(split[1])) {
							server.serverRamUsage = Long.valueOf(split[0]).longValue();
							server.serverRamCommit = Long.valueOf(split[1]).longValue();
						}
					}
				}
			} else if(data.trim().toUpperCase().startsWith("THREAD-COUNT: ")) {
				if(server != null) {
					String count = data.substring("THREAD-COUNT:".length());
					count = (count.startsWith(" ") ? count.substring(1) : count).trim();
					if(StringUtil.isStrInt(count)) {
						server.serverNumOfThreads = Integer.valueOf(count).intValue();
					}
				}
			} else if(data.trim().toUpperCase().startsWith("TITLE: ")) {
				if(server != null) {
					String title = data.substring("TITLE:".length());
					title = title.startsWith(" ") ? title.substring(1) : title;
					if(title.trim().equals("null")) {
						server.serverName = null;
					} else {
						server.serverName = title;
					}
				}
			} else if(data.trim().toUpperCase().startsWith("FAVICON: ")) {
				String favInfo = data.substring("FAVICON:".length()).substring(1);
				long length = -1;
				String[] pSplit = favInfo.split(Pattern.quote("="));
				if(pSplit.length > 1) {
					String pname = pSplit[0];
					String pvalue = StringUtil.stringArrayToString(pSplit, '=', 1).trim();
					if(pname.equals("length")) {
						if(StringUtil.isStrLong(pvalue)) {
							length = Long.valueOf(pvalue).longValue();
						}
					}
				}
				//System.out.println("Favicon length: " + length + "; data: \"" + data + "\"; favInfo: \"" + favInfo + "\";");
				if(server != null && length > 0) {
					DisposableByteArrayOutputStream baos = new DisposableByteArrayOutputStream();
					int count = 0;
					while(count < length) {
						baos.write(server.in.read());
						count++;
					}
					server.serverFavicon = baos.toByteArray();
					baos.dispose();
					baos.close();//Does nothing
					baos = null;
					System.gc();
				} else if(length == 0) {
					server.serverFavicon = null;
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
	
	public static final File getSettingsFile() {
		if(settingsFile == null || !settingsFile.isFile()) {
			try {
				settingsFile = new File(rootDir, settingsFileName);
				settingsFile.createNewFile();
				failedToCreateSettings = false;
			} catch(Throwable e) {
				if(!failedToCreateSettings) {
					System.err.print("Unable to create \"" + settingsFileName + "\" file: ");
					e.printStackTrace();
				}
				failedToCreateSettings = true;
			}
		}
		return settingsFile;
	}
	
	public static final boolean loadSettings() {
		File file = getSettingsFile();
		if(file != null && file.isFile()) {
			try(BufferedReader br = new BufferedReader(new FileReader(file))) {
				while(br.ready()) {
					final String line = br.readLine();
					final String[] split = line.split(Pattern.quote("="));
					final String pname = split[0];
					final String pvalue = StringUtil.stringArrayToString(split, '=', 1);
					if(pname.equalsIgnoreCase("ipAddress")) {
						serverIP.setText(pvalue);
					} else if(pname.equalsIgnoreCase("port")) {
						if(StringUtil.isStrInt(pvalue)) {
							int port = Integer.valueOf(pvalue).intValue();
							serverPort.setSelection(port);
							lastServerPort = port;
							serverPortChangeTo = port;
						}
					} else if(pname.equalsIgnoreCase("username")) {
						clientUsername.setText(pvalue.equals("null") ? "" : pvalue);
					} else if(pname.equalsIgnoreCase("password")) {
						clientPassword.setText(pvalue.equals("null") ? "" : pvalue);
					} else if(pname.equalsIgnoreCase("lastFTServerPath")) {
						lastFTServerPath = pvalue;
					}
				}
				return true;
			} catch(IOException e) {
				System.err.print("Failed to load from file \"" + file.getName() + "\": ");
				e.printStackTrace();
			}
		} else {
			System.out.println("Unable to load from settings file because the file does not exist or could not be accessed.");
		}
		return false;
	}
	
	public static final boolean saveSettings() {
		File file = getSettingsFile();
		if(file != null && file.isFile()) {
			try(PrintWriter pr = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), true)) {
				pr.println("ipAddress=" + serverIP.getText());
				pr.println("port=" + serverPort.getSelection());
				pr.println("username=" + clientUsername.getText());
				pr.println("password=" + clientPassword.getText());
				pr.println();
				pr.println("lastFTServerPath=" + lastFTServerPath);
				pr.flush();
				return true;
			} catch(Throwable e) {
				System.err.print("Failed to save to file \"" + file.getName() + "\": ");
				e.printStackTrace();
			}
		} else {
			System.out.println("Unable to load from settings file because the file does not exist or could not be accessed.");
		}
		return false;
	}
	
	/** Launch the application.
	 * 
	 * @param args System command arguments */
	public static void main(String[] args) {
		swtThread = Thread.currentThread();
		try {
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
							if(!attemptingConnection) {
								try {
									if(!handleServerData(StringUtil.readLine(server.in))) {
										if(server.close(PROTOCOL + " -1 CLOSE")) {
											server = null;
										}
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
									server.close(PROTOCOL + " -1 CLOSE");
								} catch(Throwable e) {
									e.printStackTrace();
									errorStr = "Unable to handle incoming server data: " + Functions.throwableToStr(e);
								}
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
						if(server != null && server.isAlive()) {
							if(startRemoteServer && !server.serverActive) {
								sendCommand("START-SERVER");
								startRemoteServer = false;
							} else if(killRemoteServer && server.serverActive) {
								sendCommand("KILL-SERVER");
								killRemoteServer = false;
							} else if(stopRemoteServer && server.serverActive) {
								sendCommand("STOP-SERVER");
								stopRemoteServer = false;
							} else if(restartRemoteServer && server.serverActive) {
								sendCommand("RESTART-SERVER");
								restartRemoteServer = false;
							}
						}
						Functions.sleep(10L);
					}
				}
			};
			serverStateQuerier.setDaemon(true);
			serverStateQuerier.start();
			display = Display.getDefault();
			shell = new Shell(display, SWT.SHELL_TRIM);
			shell.addListener(SWT.Close, new Listener() {
				@Override
				public void handleEvent(Event event) {
					event.doit = false;
					if(ftClient != null) {
						ftClient.setFocus();
						ftClient.close();
						ftClient = null;
					}
					if(!isAboutDialogOpen) {
						shutdown();
					}
				}
			});
			shell.addShellListener(new ShellAdapter() {
				@Override
				public void shellClosed(ShellEvent e) {
					e.doit = false;
					if(ftClient != null) {
						ftClient.setFocus();
						ftClient.close();
						ftClient = null;
					}
					if(!isAboutDialogOpen) {
						shutdown();
					}
				}
			});
			shell.setSize(700, 460);
			shell.setMinimumSize(shell.getSize());
			shell.setText(getDefaultShellTitle());
			shell.setImages(getDefaultShellImages());
			Functions.centerShellOnPrimaryMonitor(shell);
			
			createContents();
			loadSettings();
			
			isRunning = true;
			openShell();
			while(isRunning && !shell.isDisposed()) {
				mainLoop();
			}
			saveSettings();
			if(!shell.isDisposed()) {
				shell.dispose();
			}
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
	
	public static final void shutdown() {
		isRunning = false;
	}
	
	public static final void mainLoop() {
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
	
	protected static final void serverStateQuery() {
		if(server != null && !attemptingConnection && server.isAlive(/*!attemptingConnection*/)) {
			final long now = System.currentTimeMillis();
			long elapsedTime = now - lastServerStateQuery;
			if(elapsedTime >= 500L) {// 1/2 of a second//100) {// 1/10th of a second
				lastServerStateQuery = now;
				sendCommand("GET: SERVER-STATE");
				sendCommand("GET: AUTOMATIC-RESTART");
			}
			elapsedTime = now - lastServerResourceQuery;
			if(elapsedTime >= 125L) {// 1/8th of a second
				lastServerResourceQuery = now;
				sendCommand("GET: CPU-USAGE");
				sendCommand("GET: RAM-USAGE");
				sendCommand("GET: THREAD-COUNT");
			}
		}
		if(server == null || !server.isAlive()) {
			if(serverPortChangeTo != lastServerPort) {
				reconnectToServer = true;
			}
		}
	}
	
	private static final void setShellIcon() {
		Image[] images;
		if(server == null || !server.isAlive()) {
			if(server != null) {
				server.serverFavicon = null;
			}
			images = getDefaultShellImages();
		} else {
			if(server.serverFavicon != null) {
				if(!setCustomIconFromServer) {
					try {
						Image image = new Image(display, new ByteArrayInputStream(server.serverFavicon));
						images = new Image[] {image};
						setCustomIconFromServer = true;
					} catch(Throwable e) {
						System.err.print("Failed to change shell icon: " + Functions.throwableToStr(e));
						server.serverFavicon = null;
						images = getDefaultShellImages();
					}
				} else {
					images = shell.getImages();//Don't change the icon, it has already been set
				}
			} else {
				images = getDefaultShellImages();
				setCustomIconFromServer = false;
			}
		}
		Functions.setShellImages(shell, images);
		runClock();
	}
	
	private static final void setShellTitle() {
		String shellTitle = getDefaultShellTitle();
		if(server == null || !server.isAlive()) {
			if(server != null) {
				server.serverName = null;
			}
		} else {
			String serverName = server.serverName;
			if(serverName != null) {
				shellTitle = serverName + " - Server Wrapper Client";
			}
		}
		Functions.setTextFor(shell, shellTitle);
	}
	
	public static final String getShellTitle() {
		return shell.getText();
	}
	
	private static final void updateShellAppearance() {
		setShellTitle();
		setShellIcon();
	}
	
	private static final void resizeContentsToShell() {
		final Point shellSize = shell.getSize();//700 460
		final int sizeXOffset = 5;
		final int sizeYOffset = 5;
		
		Functions.setSizeFor(tabFolder, new Point(shellSize.x - 342 - sizeXOffset, shellSize.y - 69 - sizeYOffset));
		final Point comTabSize = consoleTabComposite.getSize();
		final Point resourceComTabSize = resourceTabComposite.getSize();
		
		Point consoleSize = new Point(comTabSize.x, comTabSize.y - 30);
		Point commandInputSize = new Point(comTabSize.x - (sendCmd.getSize().x + 5), 25);
		Point commandInputLocation = new Point(0, comTabSize.y - 25);
		Point sendCmdLocation = new Point(comTabSize.x - sendCmd.getSize().x, comTabSize.y - sendCmd.getSize().y);
		Point vertSepSize = new Point(2, shellSize.y - 69 - sizeYOffset);
		Point btnStartLocation = new Point(btnStartServer.getLocation().x, shellSize.y - 147 - sizeYOffset);
		Point btnStopLocation = new Point(btnStopServer.getLocation().x, shellSize.y - 147 - sizeYOffset);
		Point btnRestartLocation = new Point(btnRestartServer.getLocation().x, shellSize.y - 116 - sizeYOffset);
		Point btnKillLocation = new Point(btnKillServer.getLocation().x, shellSize.y - 116 - sizeYOffset);
		Point statusLabelLocation = new Point(10, shellSize.y - 80 - sizeYOffset);
		Point stackTraceOutSize = new Point(302, shellSize.y - 324 - sizeYOffset);
		Point label2Location = new Point(10, shellSize.y - 84 - sizeYOffset);
		
		Functions.setSizeFor(cpuUsageComposite, new Point(resourceComTabSize.x - 20, cpuUsageComposite.getSize().y));
		Point cpuUsageBarSize = new Point(cpuUsageComposite.getSize().x - (cpuUsageBar.getLocation().x + 10), cpuUsageBar.getSize().y);
		Functions.setSizeFor(ramUsageComposite, new Point(resourceComTabSize.x - 20, ramUsageComposite.getSize().y));
		Point ramUsageBarSize = new Point(ramUsageComposite.getSize().x - (ramUsageBar.getLocation().x + 10), ramUsageBar.getSize().y);
		
		Functions.setSizeFor(consoleOutput, consoleSize);
		Functions.setSizeFor(commandInput, commandInputSize);
		Functions.setLocationFor(commandInput, commandInputLocation);
		Functions.setLocationFor(sendCmd, sendCmdLocation);
		Functions.setSizeFor(verticalSeparator, vertSepSize);
		Functions.setLocationFor(btnStartServer, btnStartLocation);
		Functions.setLocationFor(btnStopServer, btnStopLocation);
		Functions.setLocationFor(btnRestartServer, btnRestartLocation);
		Functions.setLocationFor(btnKillServer, btnKillLocation);
		Functions.setLocationFor(statusLabel, statusLabelLocation);
		Functions.setSizeFor(stackTraceOutput, stackTraceOutSize);
		Functions.setLocationFor(label_2, label2Location);
		
		Functions.setSizeFor(cpuUsageBar, cpuUsageBarSize);
		Functions.setSizeFor(ramUsageBar, ramUsageBarSize);
	}
	
	public static final Shell getDialogShell() {
		return (ftClient == null || ftClient.getShell().isDisposed()) ? shell : ftClient.getShell();
	}
	
	public static final boolean isConnectedToServer() {
		return server != null && server.isAlive(/*!attemptingConnection*/);
	}
	
	protected static final void updateUI() {
		if(!isRunning || shell.isDisposed()) {
			return;
		}
		updateShellAppearance();
		if(reconnectToServer) {
			serverPort.setSelection(serverPortChangeTo);
			lastServerPort = serverPortChangeTo;
			reconnectToServer = false;
			connectToServer();
		}
		btnConnectToServer.setEnabled(attemptingConnection ? false : !isConnectedToServer());
		btnDisconnectFromServer.setEnabled(attemptingConnection ? false : isConnectedToServer());
		String connectText = attemptingConnection ? "Attempting conenction..." : (isConnectedToServer() ? "Connected to server" : "Connect to server");
		if(!btnConnectToServer.getText().equals(connectText)) {
			btnConnectToServer.setText(connectText);
		}
		if(mntmRestartServerAutomatically.getEnabled() != isConnectedToServer()) {
			mntmRestartServerAutomatically.setEnabled(isConnectedToServer());
		}
		if(isConnectedToServer()) {
			if(mntmRestartServerAutomatically.getSelection() != server.automaticRestart) {
				mntmRestartServerAutomatically.setSelection(server.automaticRestart);
			}
		} else {
			if(mntmRestartServerAutomatically.getSelection()) {
				mntmRestartServerAutomatically.setSelection(false);
			}
		}
		
		//=====
		
		final String connMsg = isConnectedToServer() ? "[No data available.]" : "[Not connected]";
		if(isConnectedToServer()) {
			if(!server.popupMessages.isEmpty()) {
				final String message = server.popupMessages.poll();
				System.out.println("New message: " + message);
				PopupDialog dialog = new PopupDialog(getDialogShell(), "Server Message", message);
				dialog.open();
			}
		}
		if(isConnectedToServer() && server.serverActive) {
			if(server.serverCpuUsage != -1L) {
				int progress = Double.valueOf(Math.round(server.serverCpuUsage)).intValue();
				if(cpuUsageBar.getSelection() != progress) {
					cpuUsageBar.setSelection(progress);
				}
				final String cpuStr = Functions.roundToStr(server.serverCpuUsage) + "%";
				if(!cpuUsageTxt.getText().equals(cpuStr)) {
					cpuUsageTxt.setText(cpuStr);
				}
			} else {
				if(!cpuUsageTxt.getText().equals(connMsg)) {
					cpuUsageTxt.setText(connMsg);
				}
				if(cpuUsageBar.getSelection() != 0) {
					cpuUsageBar.setSelection(0);
				}
			}
			if(server.serverRamUsage != -1L && server.serverRamCommit != -1L) {
				int progress = Math.round(((server.serverRamUsage + 0.00F) / (server.serverRamCommit + 0.00F)) * 100.00F);
				if(ramUsageBar.getSelection() != progress) {
					ramUsageBar.setSelection(progress);
				}
				final String ramStr = Functions.humanReadableByteCount(server.serverRamUsage, true, 2) + " / " + Functions.humanReadableByteCount(server.serverRamCommit, true, 2);
				if(!ramUsageTxt.getText().equals(ramStr)) {
					ramUsageTxt.setText(ramStr);
				}
			} else {
				if(ramUsageBar.getSelection() != 0) {
					ramUsageBar.setSelection(0);
				}
				if(!ramUsageTxt.getText().equals(connMsg)) {
					ramUsageTxt.setText(connMsg);
				}
			}
			if(server.serverNumOfThreads != -1) {
				if(!threadCountTxt.getText().equals("" + server.serverNumOfThreads)) {
					threadCountTxt.setText("" + server.serverNumOfThreads);
				}
			} else {
				if(!threadCountTxt.getText().equals(connMsg)) {
					threadCountTxt.setText(connMsg);
				}
			}
		} else {
			if(!cpuUsageTxt.getText().equals(connMsg)) {
				cpuUsageTxt.setText(connMsg);
			}
			if(cpuUsageBar.getSelection() != 0) {
				cpuUsageBar.setSelection(0);
			}
			if(!ramUsageTxt.getText().equals(connMsg)) {
				ramUsageTxt.setText(connMsg);
			}
			if(ramUsageBar.getSelection() != 0) {
				ramUsageBar.setSelection(0);
			}
			if(!threadCountTxt.getText().equals(connMsg)) {
				threadCountTxt.setText(connMsg);
			}
		}
		
		//=====
		
		if(mntmExitaltF.isEnabled() == isAboutDialogOpen) {
			mntmExitaltF.setEnabled(!isAboutDialogOpen);
		}
		btnStartServer.setEnabled(isConnectedToServer() && server != null && !server.serverActive && server.serverJarSelected && !attemptingConnection);
		btnStopServer.setEnabled(isConnectedToServer() && server != null && server.serverActive && !attemptingConnection);
		btnRestartServer.setEnabled(isConnectedToServer() && server != null && server.serverActive && !attemptingConnection);
		btnKillServer.setEnabled(isConnectedToServer() && server != null && server.serverActive && !attemptingConnection);
		serverIP.setEnabled(!isConnectedToServer() && !attemptingConnection);
		serverPort.setEnabled(!isConnectedToServer() && !attemptingConnection);
		clientUsername.setEnabled(!isConnectedToServer() && !attemptingConnection);
		clientPassword.setEnabled(!isConnectedToServer() && !attemptingConnection);
		sendCmd.setEnabled(isConnectedToServer() && !attemptingConnection);
		commandInput.setEnabled(isConnectedToServer() && !attemptingConnection);
		String status = "Status: " + (attemptingConnection ? "Connecting to server" : (server != null && server.isAlive() ? (server.serverActive ? "Server active" : server.serverJarSelected ? "Server inactive" : "No jar selected") : "Not connected"));
		if(!statusLabel.getText().equals(status)) {
			statusLabel.setText(status);
		}
		resizeContentsToShell();
		setTextFor(consoleOutput);
		setTextFor(stackTraceOutput);
	}
	
	@SuppressWarnings("unused")
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
		
		mntmRestartServerAutomatically = new MenuItem(menu_1, SWT.CHECK);
		mntmRestartServerAutomatically.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(server != null && server.isAlive()) {
					sendCommand("SET: AUTOMATIC-RESTART=" + mntmRestartServerAutomatically.getSelection());
				}
			}
		});
		mntmRestartServerAutomatically.setEnabled(server != null && server.isAlive());
		mntmRestartServerAutomatically.setText("Restart server automatically upon server exit");
		
		MenuItem mntmLoadFromSettings = new MenuItem(menu_1, SWT.NONE);
		mntmLoadFromSettings.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addLogFromServer(loadSettings() ? "Wrapper settings successfully loaded from file." : "Unable to load from settings file! Is the file accessible?");
			}
		});
		mntmLoadFromSettings.setText("L&oad from settings file\t(Ctrl+O)");
		mntmLoadFromSettings.setAccelerator(SWT.CTRL | 'O');
		
		mntmSaveToSettings = new MenuItem(menu_1, SWT.NONE);
		mntmSaveToSettings.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addLogFromServer(saveSettings() ? "Wrapper settings successfully saved to file." : "Unable to save settings to file! Is the file accessible?");
			}
		});
		mntmSaveToSettings.setText("&Save to settings file\t(Ctrl+S)");
		mntmSaveToSettings.setAccelerator(SWT.CTRL | 'S');
		
		new MenuItem(menu_1, SWT.SEPARATOR);
		
		mntmExitaltF = new MenuItem(menu_1, SWT.NONE);
		mntmExitaltF.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shutdown();
			}
		});
		mntmExitaltF.setText("E&xit\t(Alt+F4)");
		mntmExitaltF.setAccelerator(SWT.ALT | SWT.F4);
		
		mntmFileTransfer = new MenuItem(menu, SWT.NONE);
		mntmFileTransfer.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(server == null) {
					new PopupDialog(getDialogShell(), "Not connected to server", "Please connect to the server to use file transfer.").open();
					return;
				}
				if(ftClient != null) {
					ftClient.setFocus();
				} else {
					final FTClient client = ftClient = new FTClient(shell);
					ftClient.currentFTpath = (lastFTServerPath == null || lastFTServerPath.isEmpty()) ? ftClient.currentFTpath : lastFTServerPath;
					Response result = ftClient.open(server.ip, server.port, clientUsername.getText(), clientPassword.getText());
					lastFTServerPath = client.currentFTpath;
					ftClient = null;
				}
			}
		});
		mntmFileTransfer.setText("File Transfer...");
		
		MenuItem mntmAbout = new MenuItem(menu, SWT.NONE);
		mntmAbout.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(!isAboutDialogOpen) {
					isAboutDialogOpen = true;
					new AboutDialog(shell).open();
					isAboutDialogOpen = false;
				}
			}
		});
		mntmAbout.setText("&About...");
		
		//XXX Shell size: 700, 460
		Point shellSize = shell.getSize();
		
		Label label_1 = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		label_1.setBounds(10, 72, 302, 2);
		
		verticalSeparator = new Label(shell, SWT.SEPARATOR | SWT.VERTICAL);
		verticalSeparator.setBounds(318, 10, 2, shellSize.y - 69);
		
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
		stackTraceOutput.setBounds(10, 171, 302, shell.getSize().y - 324);
		
		//XXX Shell size: 700, 460
		btnStartServer = new Button(shell, SWT.NONE);
		btnStartServer.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				startRemoteServer = true;
			}
		});
		btnStartServer.setBounds(10, shell.getSize().y - 147, 148, 25);
		
		btnStartServer.setText("Start Server");
		
		btnStopServer = new Button(shell, SWT.NONE);
		btnStopServer.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				stopRemoteServer = true;
			}
		});
		btnStopServer.setBounds(164, shell.getSize().y - 147, 148, 25);
		btnStopServer.setText("Stop Server");
		
		statusLabel = new Label(shell, SWT.NONE);
		statusLabel.setBounds(10, 380, 302, 21);
		statusLabel.setText("Status:");
		
		btnRestartServer = new Button(shell, SWT.NONE);
		btnRestartServer.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				restartRemoteServer = true;
			}
		});
		btnRestartServer.setBounds(10, shell.getSize().y - 116, 148, 25);
		btnRestartServer.setText("Restart Server");
		
		btnKillServer = new Button(shell, SWT.NONE);
		btnKillServer.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				killRemoteServer = true;
			}
		});
		btnKillServer.setBounds(164, shell.getSize().y - 116, 148, 25);
		btnKillServer.setText("Kill Server");
		
		label_2 = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		label_2.setBounds(10, 376, 302, 2);
		
		tabFolder = new TabFolder(shell, SWT.NONE);
		tabFolder.setBounds(326, 10, 358, 381);
		
		TabItem tbtmConsoleOutput = new TabItem(tabFolder, SWT.NONE);
		tbtmConsoleOutput.setToolTipText("View the server console");
		tbtmConsoleOutput.setText("Console Output");
		
		consoleTabComposite = new Composite(tabFolder, SWT.NONE);
		consoleTabComposite.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		tbtmConsoleOutput.setControl(consoleTabComposite);
		
		sendCmd = new Button(consoleTabComposite, SWT.NONE);
		sendCmd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sendInput(commandInput.getText());
				commandInput.setText("");
			}
		});
		sendCmd.setBounds(275, 328, 75, 25);
		sendCmd.setText("Send Cmd");
		
		consoleOutput = new StyledText(consoleTabComposite, SWT.BORDER | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
		consoleOutput.setBounds(0, 0, 350, 322);
		consoleOutput.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_FOREGROUND));
		consoleOutput.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		consoleOutput.setEditable(false);
		
		commandInput = new Text(consoleTabComposite, SWT.BORDER);
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
		commandInput.setBounds(0, 328, 269, 25);
		
		tbtmResourceUsage = new TabItem(tabFolder, SWT.NONE);
		tbtmResourceUsage.setToolTipText("Monitor system resources used by the server");
		tbtmResourceUsage.setText("Resource Usage");
		
		resourceTabComposite = new Composite(tabFolder, SWT.NONE);
		tbtmResourceUsage.setControl(resourceTabComposite);
		resourceTabComposite.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		
		cpuUsageComposite = new Composite(resourceTabComposite, SWT.NONE);
		cpuUsageComposite.setBackground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		cpuUsageComposite.setBounds(10, 10, 330, 68);
		
		Label lblCpuUsage = new Label(cpuUsageComposite, SWT.NONE);
		lblCpuUsage.setBackground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		lblCpuUsage.setBounds(10, 16, 75, 15);
		lblCpuUsage.setText("CPU Usage:");
		
		cpuUsageBar = new ProgressBar(cpuUsageComposite, SWT.NONE);
		cpuUsageBar.setBounds(124, 10, 196, 48);
		
		cpuUsageTxt = new Label(cpuUsageComposite, SWT.NONE);
		cpuUsageTxt.setBackground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		cpuUsageTxt.setBounds(10, 43, 108, 15);
		
		ramUsageComposite = new Composite(resourceTabComposite, SWT.NONE);
		ramUsageComposite.setBackground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		ramUsageComposite.setBounds(10, 84, 330, 68);
		
		Label lblResourceUsage = new Label(ramUsageComposite, SWT.NONE);
		lblResourceUsage.setBackground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		lblResourceUsage.setBounds(10, 10, 108, 15);
		lblResourceUsage.setText("Resource Usage:");
		
		ramUsageTxt = new Label(ramUsageComposite, SWT.WRAP);
		ramUsageTxt.setBackground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		ramUsageTxt.setBounds(10, 31, 108, 27);
		
		ramUsageBar = new ProgressBar(ramUsageComposite, SWT.NONE);
		ramUsageBar.setBounds(124, 31, 196, 27);
		
		Label lblThreadCount = new Label(ramUsageComposite, SWT.NONE);
		lblThreadCount.setBackground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		lblThreadCount.setBounds(124, 10, 85, 15);
		lblThreadCount.setText("Thread Count:");
		
		threadCountTxt = new Label(ramUsageComposite, SWT.NONE);
		threadCountTxt.setBackground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		threadCountTxt.setBounds(215, 10, 105, 15);
		
	}
	
	protected static final void closeServer() {
		if(server != null) {
			server.close(PROTOCOL + " -1 CLOSE");
		}
	}
	
	protected static final void connectToServer() {
		if(attemptingConnection) {
			return;
		}
		final String serverIP = Main.serverIP.getText();
		final int serverPort = Main.serverPort.getSelection();
		final String clientUsername = Main.clientUsername.getText();
		final String clientPassword = Main.clientPassword.getText();
		Thread thread = new Thread(Thread.currentThread().getName() + "_ConnectToServerThread") {
			@Override
			public final void run() {
				attemptingConnection = true;
				errorStr = null;
				if(server != null) {
					server.close();
					server = null;
					mainLoop();
				}
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
						clearLogsFromServer();
						String authLine = "HelloIAm " + Base64.getEncoder().encodeToString((clientUsername + ":" + clientPassword).getBytes()).trim() + " " + PROTOCOL;
						System.out.println("Connection to server \"" + serverIP + ":" + serverPort + "\" successful. Authenticating...");// Sending authentication line: \"" + authLine + "\":");
						server.out.println(authLine);
						server.out.flush();
					}
					final String response = StringUtil.readLine(server.in);
					if(!response.startsWith(PROTOCOL_NAME + PROTOCOL_DELIMITER)) {
						errorStr = "Failed to log into server: \"" + serverIP + ":" + serverPort + "\":\r\nServer sent invalid response: \"" + response + "\"...";
					} else if(response.contains("Version Mismatch: ")) {
						errorStr = "Failed to log into server: \"" + serverIP + ":" + serverPort + "\":\r\nVersion mismatch: " + response.split(Pattern.quote(":"))[1];
						addLogFromServer(response);
						//server.close();
						//server = null;
					} else if(response.equals(PROTOCOL + " 1 Authentication Failure")) {
						errorStr = "Failed to log into server: \"" + serverIP + ":" + serverPort + "\":\r\nUnknown username or bad password.";
						addLogFromServer(response);
					} else {
						addLogFromServer(response);
					}
				} catch(ConnectException e) {
					errorStr = "Unable to connect to server \"" + serverIP + ":" + serverPort + "\": " + e.getMessage();
					server = null;
				} catch(IOException e) {
					System.err.print("Unable to connect to server\"" + serverIP + ":" + serverPort + "\": ");
					e.printStackTrace();
					errorStr = "Failed to connect to server \"" + serverIP + ":" + serverPort + "\": " + e.getMessage();
					server = null;
				} catch(Throwable e) {
					e.printStackTrace();
					errorStr = "An unhandled exception occurred: " + Functions.throwableToStr(e);
					server = null;
				}
				attemptingConnection = false;
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
		if(!isConnectedToServer()) {
			return;
		}
		if(cmd != null) {
			final PrintWriter out = server.out;
			if(out != null) {//Stupid NPE's ...
				out.print(cmd + "\r\n");
				out.print("\r\n");
				out.flush();
			}
		}
	}
	
	protected static final void sendInput(String input) {
		if(!isConnectedToServer()) {
			return;
		}
		final PrintWriter out = server.out;
		if(out != null) {//Stupid NPE's ...
			out.print("COMMAND: " + input + "\r\n");
			out.print("\r\n");
			out.flush();
		}
	}
	
	protected static final void setTextFor(StyledText styledText) {
		String text = "";
		if(styledText == consoleOutput) {//if(errorStr == null) {
			if(lastConstructLogTime >= lastLogTime) {
				return;
			}
			try {
				Iterator<String> iterator = consoleLogs.iterator();
				while(iterator.hasNext()) {
					String log = iterator.next();
					if(log != null && !log.isEmpty()) {
						text += log + "\n";
					}
				}
				if(server != null && server.isAlive()) {
					text += ">";//XXX Console caret addition
				}
				lastConstructLogTime = System.currentTimeMillis();
			} catch(ConcurrentModificationException ignored) {
			}
		} else {
			if(errorStr == null) {
				if(lastConstructErrLogTime >= lastErrLogTime) {
					return;
				}
				Iterator<String> iterator = consoleErrs.iterator();
				while(iterator.hasNext()) {
					String log = iterator.next();
					if(log != null && !log.isEmpty()) {
						text += log + "\n";
					}
				}
				lastConstructErrLogTime = System.currentTimeMillis();
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
	
	public static void showFileToUser(File file) {
		if(file.isDirectory()) {
			if(StringUtil.getOSType() == EnumOS.WINDOWS) {
				try {
					Runtime.getRuntime().exec("explorer.exe /select," + file.getAbsolutePath());
				} catch(IOException e) {
					try {
						Desktop.getDesktop().browse(file.toURI());
					} catch(IOException ignored) {
						Program.launch(file.getAbsolutePath());
					}
				}
			} else {
				try {
					Desktop.getDesktop().browse(file.getParentFile().toURI());
				} catch(IOException ignored) {
					Program.launch(file.getParentFile().getAbsolutePath());
				}
			}
		} else if(file.isFile()) {
			if(StringUtil.getOSType() == EnumOS.WINDOWS) {
				try {
					Runtime.getRuntime().exec("explorer.exe /select," + file.getAbsolutePath());
				} catch(IOException e) {
					try {
						Desktop.getDesktop().browse(file.getParentFile().toURI());
					} catch(IOException ignored) {
						Program.launch(file.getParentFile().getAbsolutePath());
					}
				}
			} else {
				try {
					Desktop.getDesktop().browse(file.getParentFile().toURI());
				} catch(IOException ignored) {
					Program.launch(file.getParentFile().getAbsolutePath());
				}
			}
		}
	}
	
}
