/**
 * 
 */
package com.gmail.br45entei.io;

import com.gmail.br45entei.data.Property;
import com.gmail.br45entei.main.Main;
import com.gmail.br45entei.main.PopupDialog;
import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.swt.Response;
import com.gmail.br45entei.util.AddressUtil;
import com.gmail.br45entei.util.FileTransfer;
import com.gmail.br45entei.util.FileTransfer.FileData;
import com.gmail.br45entei.util.StringUtil;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Pattern;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.wb.swt.SWTResourceManager;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class FTClient {
	
	protected volatile Response			result	= Response.NO_RESPONSE;
	protected final Display				display;
	protected final Shell				parent;
	protected Shell						shell;
	protected volatile ServerConnection	server;
	protected final Thread				serverFTHandler;
	protected final Thread				serverFileUploader;
	private Label						lblDownloadText;
	protected Tree						tree;
	protected TreeColumn				trclmnName;
	protected TreeColumn				trclmnSize;
	protected TreeColumn				trclmnDate;
	private Text						currentFTPath;
	private Button						btnUpload;
	
	//======
	
	protected static enum TreeSortType {
		DOWNLOAD,
		FILENAME,
		SIZE,
		DATE;
	}
	
	protected volatile TreeSortType						treeSortType				= TreeSortType.DOWNLOAD;
	
	//======
	
	protected volatile boolean							isClosed					= false;
	protected final ConcurrentLinkedDeque<String>		cmdsToSendToServer			= new ConcurrentLinkedDeque<>();
	
	//=======================================================
	
	protected volatile File								downloadPath;
	protected volatile boolean							openDownloadFolder			= false;
	protected volatile File								lastDownloadedFile			= null;
	protected volatile boolean							openDownloadedFile			= false;
	protected volatile boolean							batchDownload				= false;
	protected final Property<Double>					batchDownloadPercentage		= new Property<>("BatchDownloadPercentComplete");
	protected final Property<String>					batchDownloadFileName		= new Property<>("BatchDownloadFileName");
	protected volatile UploadProgress					downloadProgressDialog		= null;
	protected volatile int								numOfBatchFilesDownloaded	= 0;
	protected volatile int								numOfBatchFoldersCreated	= 0;
	
	protected volatile int								numOfFilesDeletedLocally	= 0;
	
	public volatile String								currentFTpath				= null;
	
	protected final ConcurrentLinkedDeque<File>			filesToUpload				= new ConcurrentLinkedDeque<>();
	protected volatile boolean							batchUpload					= false;
	protected volatile String							batchUploadDesiredPath		= null;
	protected final Property<Double>					uploadProgress				= new Property<>("UploadProgress", Double.valueOf(0.0D));
	protected volatile IOException						uploadError					= null;
	
	protected final ConcurrentLinkedDeque<PopupMessage>	popupMessagesToDisplay		= new ConcurrentLinkedDeque<>();
	
	protected static final class PopupMessage {
		public final String	title;
		public final String	message;
		
		public PopupMessage(String title, String message) {
			this.title = title;
			this.message = message;
		}
		
	}
	
	protected volatile String[]		fileListPaths		= new String[0];
	protected volatile boolean		listDirty			= false;
	protected volatile boolean		listTypeDirty		= false;
	
	protected volatile String		selectedFilePath	= null;
	
	//=======================================================
	
	private static final Image[]	icons				= new Image[20];
	private Button					btnRefresh;
	private Button					btnNewFolder;
	private Button					btnSetDownloadDirectory;
	
	static {
		icons[0] = SWTResourceManager.getImage(Main.class, "/assets/textures/icons/folder.png");
		icons[1] = SWTResourceManager.getImage(Main.class, "/assets/textures/icons/page_white.png");
		icons[2] = SWTResourceManager.getImage(Main.class, "/assets/textures/icons/page_white_text.png");
		icons[3] = SWTResourceManager.getImage(Main.class, "/assets/textures/icons/script.png");
		icons[4] = SWTResourceManager.getImage(Main.class, "/assets/textures/icons/report.png");
		icons[5] = SWTResourceManager.getImage(Main.class, "/assets/textures/icons/database_table.png");
		icons[6] = SWTResourceManager.getImage(Main.class, "/assets/textures/icons/lock.png");
		icons[7] = SWTResourceManager.getImage(Main.class, "/assets/textures/icons/NBTExplorer.png");
		icons[8] = SWTResourceManager.getImage(Main.class, "/assets/textures/icons/arrow_refresh_small.png");
		icons[9] = SWTResourceManager.getImage(Main.class, "/assets/textures/icons/Java.exe.png");
		icons[10] = SWTResourceManager.getImage(Main.class, "/assets/textures/icons/7zip.exe.png");
		icons[11] = SWTResourceManager.getImage(Main.class, "/assets/textures/icons/arrow_up.png");
		icons[12] = SWTResourceManager.getImage(Main.class, "/assets/textures/icons/picture.png");
		icons[13] = SWTResourceManager.getImage(Main.class, "/assets/textures/icons/music.png");
		icons[14] = SWTResourceManager.getImage(Main.class, "/assets/textures/icons/control_play_video.png");
		icons[15] = SWTResourceManager.getImage(Main.class, "/assets/textures/icons/audacity.exe.png");
		icons[16] = SWTResourceManager.getImage(Main.class, "/assets/textures/icons/folder_add.png");
		icons[17] = SWTResourceManager.getImage(Main.class, "/assets/textures/icons/report_key.png");
		icons[18] = SWTResourceManager.getImage(Main.class, "/assets/textures/icons/table_key.png");
		icons[19] = SWTResourceManager.getImage(Main.class, "/assets/textures/icons/page_white_data_loss.png");
	}
	
	/** Create the dialog.
	 * 
	 * @param display */
	public FTClient(Shell parent) {
		this.display = Display.getDefault();
		this.parent = parent;
		createContents();
		this.serverFTHandler = new Thread(new Runnable() {//Used for input purposes
			@Override
			public final void run() {
				FTClient.this.server.println("DIR" + (FTClient.this.currentFTpath != null && !FTClient.this.currentFTpath.isEmpty() ? ": " + FTClient.this.currentFTpath : ""));
				//FTClient.this.server.println("LIST");
				while(!FTClient.this.isClosed && FTClient.this.server != null && FTClient.this.server.isAlive()) {
					if(!FTClient.this.handleServerData()) {
						break;
					}
				}
				if(FTClient.this.server.isAlive()) {
					FTClient.this.server.closeNicely();
				}
			}
		});
		this.serverFTHandler.setDaemon(true);
		this.serverFileUploader = new Thread(new Runnable() {//Used for output purposes
			@Override
			public final void run() {
				while(!FTClient.this.isClosed && FTClient.this.server != null && FTClient.this.server.isAlive()) {
					long loopStart = System.currentTimeMillis();
					if(FTClient.this.uploadError != null) {
						FTClient.this.filesToUpload.clear();
						showPopupMessage("Error uploading files", "An error occurred while uploading files:\r\n" + Functions.throwableToStr(FTClient.this.uploadError));
						FTClient.this.uploadError = null;
					}
					if(FTClient.this.cmdsToSendToServer.size() > 0) {
						String x;
						while((x = FTClient.this.cmdsToSendToServer.poll()) != null) {
							FTClient.this.server.println(x);
						}
					}
					if(FTClient.this.filesToUpload != null && FTClient.this.filesToUpload.size() > 0) {
						if(FTClient.this.batchUpload) {
							FTClient.this.server.println("NOPOPUPDIALOGS: true");
						}
						for(File fileToUpload : FTClient.this.filesToUpload) {
							if(fileToUpload == null || !fileToUpload.isFile()) {
								continue;
							}
							try {
								URLConnection url = fileToUpload.toURI().toURL().openConnection();
								long size = url.getContentLengthLong();
								if(size < Integer.MAX_VALUE) {
									if(FTClient.this.batchUploadDesiredPath != null) {
										FTClient.this.server.println("MKDIRGODIR: " + FTClient.this.batchUploadDesiredPath);//currentFTpath);
									}
									FTClient.this.server.println("FILE");
									FileTransfer.sendFile(fileToUpload, FTClient.this.server.outStream, FTClient.this.uploadProgress);
								} else {
									FTClient.this.showPopupMessage("Failed to upload file - File size too large", "Due to the limitations of a Java Integer,\r\nfile sizes larger than " + Functions.humanReadableByteCount(Integer.MAX_VALUE, false, 2) + " cannot be uploaded using this software.\r\nSorry!");
								}
								FTClient.this.filesToUpload.remove(fileToUpload);
								try {
									url.getInputStream().close();
									url.getOutputStream().close();
								} catch(IOException ignored) {
								}
							} catch(IOException e) {
								FTClient.this.uploadError = e;
							}
							FTClient.this.server.println("LIST");
						}
						if(FTClient.this.batchUpload) {
							FTClient.this.server.println("NOPOPUPDIALOGS: false");
							FTClient.this.batchUpload = false;
							FTClient.this.batchUploadDesiredPath = null;
						}
					}
					if(System.currentTimeMillis() - loopStart > 10L) {
						Functions.sleep(10L);
					}
				}
				FTClient.this.result = FTClient.this.result != Response.CLOSE ? Response.DISCONNECT : FTClient.this.result;
			}
		});
		this.serverFileUploader.setDaemon(true);
		this.batchDownloadPercentage.setValue(Double.valueOf(0.0D));
		this.batchDownloadFileName.setValue("");
	}
	
	public final void showPopupMessage(String title, String message) {
		this.popupMessagesToDisplay.add(new PopupMessage(title, message));
	}
	
	protected final boolean handleServerData() {
		boolean continueData = true;
		try {
			String line = StringUtil.readLine(this.server.in);
			if(line == null) {
				return false;
			}
			if(line.isEmpty()) {//probably a ping check
				return true;
			}
			if(line.equals("GETALLFILES: END")) {
				this.showPopupMessage("Server files downloaded", "Successfully downloaded " + this.numOfBatchFilesDownloaded + (this.numOfBatchFilesDownloaded == 1 ? " file" : " files") + " and " + this.numOfBatchFoldersCreated + (this.numOfBatchFoldersCreated == 1 ? " folder" : " folders") + " from the server.");
				this.resetBatchDownload();
				this.openDownloadFolder = true;
			} else if(line.startsWith("GETALLFILES: ")) {
				String percentCompleteFileName = line.substring("GETALLFILES: ".length());
				String[] split = percentCompleteFileName.split(Pattern.quote(" "));
				if(split.length >= 2) {
					if(StringUtil.isStrDouble(split[0])) {
						this.batchDownloadPercentage.setValue(Double.valueOf(split[0]));
					} else {
						System.err.println("Malformed percentage returned from server: " + split[0]);
					}
					this.batchDownloadFileName.setValue(StringUtil.stringArrayToString(split, ' ', 1));
				}
			} else if(line.startsWith("MKDIR: ")) {
				String path = line.substring("MKDIR: ".length());
				File folder = path.equals("/") ? this.downloadPath : new File(this.downloadPath, (path.startsWith("/") ? path.substring(1) : path).replace("/", File.separator));
				if(!folder.exists()) {
					folder.mkdirs();
					if(this.batchDownload) {
						this.numOfBatchFoldersCreated++;
					}
				}
			} else if(line.equals("FILE")) {
				FileData data = FileTransfer.readFile(this.server.in);
				File folder = this.currentFTpath.equals("/") ? this.downloadPath : new File(this.downloadPath, (this.currentFTpath.startsWith("/") ? this.currentFTpath.substring(1) : this.currentFTpath).replace("/", File.separator));
				if(!folder.exists()) {
					folder.mkdirs();
				}
				File file = new File(folder, data.name);
				try(FileOutputStream out = new FileOutputStream(file)) {
					out.write(data.data, 0, data.getSize());
					out.flush();
				} catch(IOException e) {
					this.showPopupMessage("Failed to download file", "The file \"" + data + "\" failed to save to disk!\r\nPlease try again.\r\n\r\nError details: " + e.getMessage());
				}
				Files.setLastModifiedTime(Paths.get(file.toURI()), FileTime.fromMillis(data.lastModified));//BasicFileAttributes attributes = Files.readAttributes(Paths.get(file.toURI()), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
				if(!this.batchDownload) {
					if(this.openDownloadedFile) {
						this.openDownloadedFile = false;
						try {
							Desktop.getDesktop().edit(file);
						} catch(IOException e) {
							if(!Program.launch(file.getAbsolutePath(), folder.getAbsolutePath())) {
								Desktop.getDesktop().open(file);
							}
						}
					} else {
						Main.showFileToUser(file);
					}
					this.lastDownloadedFile = file;
				} else {
					this.numOfBatchFilesDownloaded++;
				}
			} else if(line.startsWith("DIR: ")) {
				String check = line.substring("DIR: ".length());
				if(check.equals("null")) {
					FTClient.this.server.println("DIR: /");
				} else {
					this.currentFTpath = check;
				}
			} else if(line.startsWith("LIST: ")) {
				String numOfListStr = line.substring("LIST: ".length());
				if(StringUtil.isStrInt(numOfListStr)) {
					int numOfList = Integer.valueOf(numOfListStr).intValue();
					this.fileListPaths = new String[numOfList];
					for(int i = 0; i < numOfList; i++) {
						this.fileListPaths[i] = StringUtil.readLine(this.server.in);
					}
					this.listDirty = true;
				}
			} else if(line.startsWith(Main.PROTOCOL)) {
				continueData = handleProtocolMessage(line) && continueData;
			}
		} catch(IOException e) {
			e.printStackTrace();
			continueData = false;
		}
		return continueData;
	}
	
	protected final void resetBatchDownload() {
		this.batchDownload = false;
		this.numOfBatchFilesDownloaded = 0;
		this.numOfBatchFoldersCreated = 0;
		this.batchDownloadPercentage.setValue(Double.valueOf(0.0D));
		this.batchDownloadFileName.setValue("");
		if(!this.batchDownload) {
			if(this.downloadProgressDialog != null) {
				this.downloadProgressDialog.close();
				this.downloadProgressDialog = null;
			}
		}
	}
	
	private final boolean handleProtocolMessage(String line) {
		if(line == null) {
			return false;
		}
		if(line.isEmpty()) {
			return true;
		}
		if(line.equals(Main.PROTOCOL + " -1 CLOSE")) {
			return false;
		}
		if(line.equals(Main.PROTOCOL + " 47 FILESYSTEM OBJECT ALREADY EXISTS")) {
			this.showPopupMessage("Error creating file/folder", "That file or folder already exists.\r\nPlease choose another name.");
		} else if(line.startsWith(Main.PROTOCOL + " 47 FILESYSTEM OBJECT NAME CONFLICT: ")) {
			String conflictingName = line.substring((Main.PROTOCOL + " 47 FILESYSTEM OBJECT NAME CONFLICT: ").length());
			this.showPopupMessage("Error renaming file/folder", "There is already a file or folder with the name: " + conflictingName + "\r\nPlease choose another name.");
		} else if(line.startsWith(Main.PROTOCOL + " 28 USER ALREADY CONNECTED: ")) {
			this.showPopupMessage("Connection Error", "The server reports you are already logged into the File Transfer window.\r\nTry disconnecting and reconnecting to fix the problem.");
		} else {
			System.err.println("Unknown protocol encountered: " + line);
		}
		return true;
	}
	
	public final Shell getShell() {
		return this.shell;
	}
	
	public final void setFocus() {
		if(this.shell.getMinimized()) {
			this.shell.setMinimized(false);
		}
		this.shell.setFocus();
		this.shell.forceActive();
	}
	
	public final void close() {
		this.result = Response.CLOSE;
		if(Main.checkThreadAccess()) {
			for(Shell shell : this.shell.getShells()) {
				shell.dispose();
			}
		}
	}
	
	public final File getDefaultDownloadPath() {
		return new File(Main.rootDir, "ServerFiles" + File.separator + AddressUtil.getClientAddressNoPort(this.server.getIpAddress()));
	}
	
	/** Open the dialog.
	 * 
	 * @return the result */
	public Response open(String ip, int port, String username, String password) {
		String errorStr = null;
		try {
			this.server = ServerConnection.connectTo(ip, port);
			if(this.server != null) {
				if(this.server.isAlive()) {
					String authLine = "HelloIAm " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes()).trim() + " " + Main.PROTOCOL + " FILETRANSFER";
					System.out.println("File transfer connection to server \"" + ip + ":" + port + "\" successful. Authenticating...");// Sending authentication line: \"" + authLine + "\":");
					this.server.println(authLine);
					this.server.out.flush();
				}
				final String response = StringUtil.readLine(this.server.in);
				if(!response.startsWith(Main.PROTOCOL_NAME + Main.PROTOCOL_DELIMITER)) {
					errorStr = "Failed to establish file transfer connection with server: \"" + ip + ":" + port + "\":\r\nServer sent invalid response: \"" + response + "\"...";
				} else if(response.contains("Version Mismatch: ")) {
					errorStr = "Failed to establish file transfer connection with server: \"" + ip + ":" + port + "\":\r\nVersion mismatch: " + response.split(Pattern.quote(":"))[1];
					this.server.close();
					this.server = null;
				} else if(response.equals(Main.PROTOCOL + " 1 Authentication Failure")) {
					errorStr = "Failed to establish file transfer connection with server: \"" + ip + ":" + port + "\":\r\nUnknown username or bad password.";
					this.server.close();
					this.server = null;
				} else {
					if(response.equals(Main.PROTOCOL + " 43 FILETRANSFER CONNECTION ESTABLISHED")) {
						Main.addLogFromServer("File transfer connection established!");
						this.downloadPath = this.getDefaultDownloadPath();
						if(!this.downloadPath.exists()) {
							this.downloadPath.mkdirs();
						}
						this.serverFTHandler.start();
						this.serverFileUploader.start();
					} else {
						String instaClose = Main.PROTOCOL + " -1 CLOSE";
						String notConnected = Main.PROTOCOL + " 27 USER NOT CONNECTED: ";
						String alreadyConnected = Main.PROTOCOL + " 28 USER ALREADY CONNECTED: ";
						if(response.startsWith(notConnected)) {
							errorStr = "Failed to establish file transfer connection with server: User \"" + response.substring(notConnected.length()) + "\" not logged in!";
						} else if(response.startsWith(alreadyConnected)) {
							errorStr = "Failed to establish file transfer connection with server: User \"" + response.substring(alreadyConnected.length()) + "\" already logged in!";
						} else if(response.equals(instaClose)) {
							errorStr = "NO_PERMS";
						} else {
							errorStr = "Failed to establish file transfer connection with server: \"" + ip + ":" + port + "\":\r\n" + response;
						}
						this.server.close();
						this.server = null;
					}
				}
			}
		} catch(ConnectException e) {
			errorStr = "Unable to connect to server \"" + ip + ":" + port + "\": " + e.getMessage();
			this.server = null;
		} catch(IOException e) {
			System.err.print("Unable to connect to server\"" + ip + ":" + port + "\": ");
			e.printStackTrace();
			errorStr = "Failed to connect to server \"" + ip + ":" + port + "\": " + e.getMessage();
			this.server = null;
		} catch(Throwable e) {
			e.printStackTrace();
			errorStr = "An unhandled exception occurred: " + Functions.throwableToStr(e);
			this.server = null;
		}
		if(this.server == null) {
			this.shell.dispose();
			if("NO_PERMS".equals(errorStr)) {
				return Response.NO_PERMS;
			}
			new PopupDialog(this.shell, "Error opening file transfer connection", errorStr != null ? errorStr : "An unknown error occurred when attempting to open a secondary connection to the server wrapper.\r\nPlease try again.").open();
			return Response.DISCONNECT;
		}
		this.shell.open();
		this.shell.layout();
		while(!this.shell.isDisposed()) {
			Main.mainLoop();
			this.updateUI();
			if(!this.server.isAlive()) {
				break;
			}
			if(this.result != Response.NO_RESPONSE) {
				break;
			}
		}
		this.isClosed = true;
		if(this.server.isAlive()) {
			new Thread(new Runnable() {
				@Override
				public final void run() {
					FTClient.this.server.closeNicely();
				}
			}).start();
		}
		if(!this.shell.isDisposed()) {
			this.shell.dispose();
		}
		if(this.result == Response.DISCONNECT) {
			new PopupDialog(this.parent, "Connection Error", "The file transfer connection was interrupted unexpectedly.\r\nPlease try connecting again.").open();
		}
		return this.result;
	}
	
	private final TreeItem setItemInTree(String path) {
		TreeItem item = new TreeItem(this.tree, SWT.NONE);
		item.setText(path.split(Pattern.quote("?")));
		String filePath = item.getText(0);
		if(filePath.endsWith("/")) {
			item.setImage(icons[0]);
		} else {
			String name = FilenameUtils.getName(filePath);
			String ext = FilenameUtils.getExtension(filePath);
			if(name.equalsIgnoreCase("settings.txt")) {
				item.setImage(icons[4]);
			} else if(ext.equalsIgnoreCase("txt") || ext.equalsIgnoreCase("log")) {
				item.setImage(icons[2]);
			} else if(ext.equalsIgnoreCase("yml") || ext.equalsIgnoreCase("yaml") || ext.equalsIgnoreCase("json")) {
				item.setImage(icons[3]);
			} else if(ext.equalsIgnoreCase("properties")) {
				item.setImage(icons[4]);
			} else if(ext.equalsIgnoreCase("dat") || ext.equalsIgnoreCase("mca") || ext.equalsIgnoreCase("mcr")) {
				item.setImage(icons[7]);
			} else if(ext.equalsIgnoreCase("lock")) {
				item.setImage(icons[6]);
			} else if(ext.equalsIgnoreCase("jar")) {
				item.setImage(icons[9]);
			} else if(ext.equalsIgnoreCase("zip") || ext.equalsIgnoreCase("7z") || ext.equalsIgnoreCase("gz")) {
				item.setImage(icons[10]);
			} else if(isImageFile(ext)) {
				item.setImage(icons[12]);
			} else if(isMusicFile(ext)) {
				item.setImage(icons[ext.equalsIgnoreCase("aup") ? 15 : 13]);
			} else if(isVideoFile(ext)) {
				item.setImage(icons[14]);
			} else if(ext.equalsIgnoreCase("cer") || ext.equalsIgnoreCase("der") || ext.equalsIgnoreCase("crt") || ext.equalsIgnoreCase("p7b") || ext.equalsIgnoreCase("p7r") || ext.equalsIgnoreCase("spc") || ext.equalsIgnoreCase("pfx") || ext.equalsIgnoreCase("p12")) {
				item.setImage(icons[17]);
			} else if(ext.equalsIgnoreCase("csr") || ext.equalsIgnoreCase("jks")) {
				item.setImage(icons[18]);
			} else if(ext.equalsIgnoreCase("old")) {
				item.setImage(icons[19]);
			} else {
				item.setImage(icons[1]);
			}
		}
		item.setText(0, filePath.length() <= this.currentFTpath.length() ? filePath : filePath.substring(this.currentFTpath.length()));
		return item;
	}
	
	private final void updateTree() {
		if(this.batchUpload) {
			return;
		}
		this.tree.clearAll(true);
		for(TreeItem item : this.tree.getItems()) {
			item.dispose();
		}
		for(String path : this.fileListPaths) {
			if(path == null || path.split(Pattern.quote("?")).length != 3) {
				continue;
			}
			setItemInTree(path);
		}
	}
	
	private final void updateTreeOrder() {
		if(this.treeSortType == TreeSortType.DOWNLOAD) {
			this.updateTree();
			return;
		}
		if(this.batchUpload) {
			return;
		}
		this.tree.clearAll(true);
		for(TreeItem item : this.tree.getItems()) {
			item.dispose();
		}
		ArrayList<Integer> directories = new ArrayList<>();
		//XXX Sort folders:
		ArrayList<String> orderedDirs = new ArrayList<>();
		int i = 0;
		for(String path : this.fileListPaths) {
			if(path == null || path.split(Pattern.quote("?")).length != 3) {
				i++;
				continue;
			}
			String[] split = path.split(Pattern.quote("?"));
			if(split[0].endsWith("/")) {
				//directories.add(Integer.valueOf(i));
				String add = split[this.treeSortType == TreeSortType.DATE ? 2 : 0];
				if(this.treeSortType == TreeSortType.DATE) {
					String oldAdd = add;
					try {
						add = StringUtil.getCacheValidatorTimeFormat().parse(add).getTime() + "";
					} catch(ParseException e) {
						e.printStackTrace();
						add = oldAdd;
						this.treeSortType = TreeSortType.FILENAME;
					}
				}
				orderedDirs.add(add + "-" + i);
			}
			i++;
		}
		orderedDirs.sort(this.treeSortType == TreeSortType.DATE ? CASE_INSENSITIVE_ORDER : String.CASE_INSENSITIVE_ORDER);
		if(this.treeSortType == TreeSortType.FILENAME) {
			Collections.reverse(orderedDirs);
		}
		for(String ordered : orderedDirs) {
			directories.add(Integer.valueOf(ordered.substring(ordered.lastIndexOf("-") + 1)));
		}
		//XXX Sort files:
		ArrayList<String> orderedList = new ArrayList<>();
		i = 0;
		for(String path : this.fileListPaths) {
			if(path == null || path.split(Pattern.quote("?")).length != 3) {
				i++;
				continue;
			}
			String[] split = path.split(Pattern.quote("?"));
			if(split[0].endsWith("/")) {
				i++;
				continue;
			}
			String add = split[this.treeSortType == TreeSortType.FILENAME ? 0 : this.treeSortType == TreeSortType.SIZE ? 1 : 2];
			if(this.treeSortType == TreeSortType.SIZE) {
				add = new BigDecimal(Functions.fromHumanReadableByteCount(add, true)).toPlainString();
			} else if(this.treeSortType == TreeSortType.DATE) {
				String oldAdd = add;
				try {
					add = StringUtil.getCacheValidatorTimeFormat().parse(add).getTime() + "";
				} catch(ParseException e) {
					e.printStackTrace();
					add = oldAdd;
					this.treeSortType = TreeSortType.FILENAME;
				}
			}
			orderedList.add(add + "-" + i);
			i++;
		}
		orderedList.sort(this.treeSortType == TreeSortType.SIZE || this.treeSortType == TreeSortType.DATE ? CASE_INSENSITIVE_ORDER : String.CASE_INSENSITIVE_ORDER);
		if(this.treeSortType == TreeSortType.FILENAME) {
			Collections.reverse(orderedList);
		}
		for(String ordered : orderedList) {
			directories.add(Integer.valueOf(ordered.substring(ordered.lastIndexOf("-") + 1)));
		}
		//XXX Put it all together:
		for(Integer index : directories) {
			if(index == null || index.intValue() < 0 || index.intValue() >= this.fileListPaths.length) {
				System.err.println("Warning: invalid index: " + index);
				continue;
			}
			String path = this.fileListPaths[index.intValue()];
			if(path == null || path.split(Pattern.quote("?")).length != 3) {
				continue;
			}
			setItemInTree(path);
		}
	}
	
	private final void updateUI() {
		Functions.setTextFor(this.lblDownloadText, this.downloadPath.getAbsolutePath());
		Functions.setTextFor(this.currentFTPath, this.batchDownload ? "[Downloading from: " + this.currentFTpath + " ...]" : this.currentFTpath);
		if(!this.popupMessagesToDisplay.isEmpty()) {
			PopupMessage message = this.popupMessagesToDisplay.poll();
			if(message != null) {
				new PopupDialog(this.shell, message.title, message.message).open();
				if(this.openDownloadFolder) {
					this.openDownloadFolder = false;
					try {
						Desktop.getDesktop().browse(FTClient.this.downloadPath.toURI());
					} catch(IOException e1) {
						Program.launch(FTClient.this.downloadPath.getAbsolutePath());
					}
				}
			}
		}
		if(this.uploadError != null) {
			new PopupDialog(this.shell, "Error uploading file to server", "" + this.uploadError.getMessage()).open();
			this.uploadError = null;
		}
		if(this.listDirty) {
			this.updateTree();
			this.listDirty = false;
			this.updateTreeOrder();
			this.listTypeDirty = false;
		}
		if(this.listTypeDirty) {
			this.updateTreeOrder();
			this.listTypeDirty = false;
		}
		Functions.setTextFor(this.shell, "Server Files - " + Main.getShellTitle());
		Functions.setShellImages(this.shell, Main.getShellImages());
		
		Point btnSetDownloadDirLoc = new Point(this.shell.getSize().x - 162, 10);
		Point treeSize = new Point(this.shell.getSize().x - 26, this.shell.getSize().y - 151);
		Point lblDownloadTextSize = new Point(this.shell.getSize().x - 299, 20);
		Point currentFTPathSize = new Point(this.shell.getSize().x - 116, 24);
		
		Functions.setLocationFor(this.btnSetDownloadDirectory, btnSetDownloadDirLoc);
		Functions.setSizeFor(this.tree, treeSize);
		Functions.setSizeFor(this.lblDownloadText, lblDownloadTextSize);
		Functions.setSizeFor(this.currentFTPath, currentFTPathSize);
		
		final int treeColumnNameWidth = this.shell.getSize().x - 295;
		if(this.trclmnName.getWidth() != treeColumnNameWidth) {
			this.trclmnName.setWidth(treeColumnNameWidth);
		}
		if(!this.batchDownload) {
			if(this.downloadProgressDialog != null) {
				this.downloadProgressDialog.close();
				this.downloadProgressDialog = null;
			}
		} else {
			if(this.downloadProgressDialog == null) {
				this.downloadProgressDialog = new UploadProgress(this.shell, false, this.batchDownloadPercentage, this.batchDownloadFileName);
				this.downloadProgressDialog.open();
			}
		}
	}
	
	protected static final boolean isFileViewable(String ext) {
		return isTextFile(ext) || isImageFile(ext) || isMusicFile(ext) || isVideoFile(ext);
	}
	
	protected static final boolean isTextFile(String ext) {
		return ext.equalsIgnoreCase("txt") || ext.equalsIgnoreCase("log") || ext.equalsIgnoreCase("yml") || ext.equalsIgnoreCase("yaml") || ext.equalsIgnoreCase("json") || ext.equalsIgnoreCase("properties");
	}
	
	protected static final boolean isImageFile(String ext) {
		return ext.equalsIgnoreCase("ico") || ext.equalsIgnoreCase("png") || ext.equalsIgnoreCase("bmp") || ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("jiff") || ext.equalsIgnoreCase("jfif") || ext.equalsIgnoreCase("jp2") || ext.equalsIgnoreCase("jpx") || ext.equalsIgnoreCase("j2k") || ext.equalsIgnoreCase("j2c") || ext.equalsIgnoreCase("fpx") || ext.equalsIgnoreCase("pcd") || ext.equalsIgnoreCase("tif") || ext.equalsIgnoreCase("tiff") || ext.equalsIgnoreCase("gif") || ext.equalsIgnoreCase("tga") || ext.equalsIgnoreCase("dds");
	}
	
	protected static final boolean isMusicFile(String ext) {
		return ext.equalsIgnoreCase("aup") || ext.equalsIgnoreCase("mp3") || ext.equalsIgnoreCase("wav") || ext.equalsIgnoreCase("ogg") || ext.equalsIgnoreCase("mid") || ext.equalsIgnoreCase("midi") || ext.equalsIgnoreCase("flac") || ext.equalsIgnoreCase("m4a");
	}
	
	protected static final boolean isVideoFile(String ext) {
		return ext.equalsIgnoreCase("mp4") || ext.equalsIgnoreCase("avi") || ext.equalsIgnoreCase("wmv") || ext.equalsIgnoreCase("vp8") || ext.equalsIgnoreCase("vp9") || ext.equalsIgnoreCase("webm") || ext.equalsIgnoreCase("flv") || ext.equalsIgnoreCase("mov") || ext.equalsIgnoreCase("qt") || ext.equalsIgnoreCase("rm") || ext.equalsIgnoreCase("rmvb") || ext.equalsIgnoreCase("m4v") || ext.equalsIgnoreCase("mpg") || ext.equalsIgnoreCase("mpeg") || ext.equalsIgnoreCase("m2v") || ext.equalsIgnoreCase("nsv");
	}
	
	/** Create contents of the dialog. */
	@SuppressWarnings("unused")
	private void createContents() {
		this.shell = new Shell(this.display, SWT.SHELL_TRIM | SWT.PRIMARY_MODAL);// | SWT.APPLICATION_MODAL);
		this.shell.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if(e.character == SWT.ESC) {
					e.doit = false;
				}
			}
		});
		this.shell.setText("Server Files - " + Main.getShellTitle());
		this.shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				e.doit = false;
				FTClient.this.result = Response.CLOSE;
			}
		});
		this.shell.setSize(640, 489);
		this.shell.setMinimumSize(this.shell.getSize());
		Functions.centerShell2OnShell1(this.parent, this.shell);
		this.shell.setImages(Main.getShellImages());
		
		Menu menu = new Menu(this.shell, SWT.BAR);
		this.shell.setMenuBar(menu);
		
		MenuItem mntmfile = new MenuItem(menu, SWT.CASCADE);
		mntmfile.setText("&File");
		
		Menu menu_1 = new Menu(mntmfile);
		mntmfile.setMenu(menu_1);
		
		MenuItem mntmUploadFolderTo = new MenuItem(menu_1, SWT.NONE);
		mntmUploadFolderTo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(FTClient.this.shell);
				dialog.setFilterPath(FTClient.this.downloadPath.getAbsolutePath());
				dialog.setText("Choose folder to upload");
				dialog.setMessage("Choose the folder whose contents you would like uploaded to the server.");
				String filePath = dialog.open();
				if(filePath != null && !filePath.isEmpty()) {
					File file = new File(filePath);
					if(file.isDirectory()) {
						//FTClient.this.cmdsToSendToServer.add("MKDIRGODIR: " + file.getName());
						FTClient.this.batchUpload = true;
						FTClient.this.batchUploadDesiredPath = FTClient.this.currentFTpath + file.getName() + "/";
						for(File send : file.listFiles()) {
							if(!Files.isReadable(Paths.get(send.toURI()))) {
								continue;
							}
							FTClient.this.filesToUpload.add(send);
						}
					}
				}
			}
		});
		mntmUploadFolderTo.setText("&Upload folder to current directory...");
		
		MenuItem mntmDownloadAllServer = new MenuItem(menu_1, SWT.NONE);
		mntmDownloadAllServer.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FTClient.this.openDownloadedFile = false;
				FTClient.this.resetBatchDownload();
				FTClient.this.batchDownload = true;
				FTClient.this.cmdsToSendToServer.add("GETALLFILES");
			}
		});
		mntmDownloadAllServer.setText("&Download all server files...");
		
		new MenuItem(menu_1, SWT.SEPARATOR);
		
		MenuItem mntmdeleteLocalServer = new MenuItem(menu_1, SWT.NONE);
		mntmdeleteLocalServer.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Response deleteFiles = new ConfirmDeleteLocalFilesDialog(FTClient.this.shell).open(FTClient.this.downloadPath.getAbsolutePath());
				if(deleteFiles == Response.YES) {
					for(File file : getDefaultDownloadPath().listFiles()) {
						if(!FileDeleteStrategy.FORCE.deleteQuietly(file)) {
							file.deleteOnExit();
						} else {
							FTClient.this.numOfFilesDeletedLocally++;
						}
					}
					if(FTClient.this.numOfFilesDeletedLocally > 0) {
						showPopupMessage("Local file deletion", "Successfully deleted " + FTClient.this.numOfFilesDeletedLocally + " local " + (FTClient.this.numOfFilesDeletedLocally == 1 ? "file" : "folders and files") + " from disk.");
					} else {
						showPopupMessage("Local file deletion", "There were no local files found to delete.");
					}
					FTClient.this.numOfFilesDeletedLocally = 0;
				}
			}
		});
		mntmdeleteLocalServer.setText("&Delete local server files");
		
		new MenuItem(menu_1, SWT.SEPARATOR);
		
		MenuItem mntmcloseServerFiles = new MenuItem(menu_1, SWT.NONE);
		mntmcloseServerFiles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FTClient.this.result = Response.CLOSE;
			}
		});
		mntmcloseServerFiles.setText("&Close server files view");
		
		Label lblDownloadDestination = new Label(this.shell, SWT.NONE);
		lblDownloadDestination.setBounds(10, 10, 115, 20);
		lblDownloadDestination.setText("Download destination:");
		
		this.lblDownloadText = new Label(this.shell, SWT.BORDER);
		this.lblDownloadText.setBounds(131, 10, this.shell.getSize().x - 299, 20);
		
		//this.shell.setSize(640, 489);
		
		this.tree = new Tree(this.shell, SWT.BORDER | SWT.FULL_SELECTION);
		this.tree.setLinesVisible(true);
		this.tree.setHeaderVisible(true);
		this.tree.setBounds(10, 95, this.shell.getSize().x - 26, this.shell.getSize().y - 151);
		this.tree.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				final TreeItem[] selection = FTClient.this.tree.getSelection();
				if(selection != null && selection.length != 0) {
					TreeItem item = selection[0];
					FTClient.this.selectedFilePath = item.getText(0).equals("../") ? item.getText(0) : FTClient.this.currentFTpath + item.getText(0);
				} else {
					FTClient.this.selectedFilePath = null;
					FTClient.this.tree.deselectAll();
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				final TreeItem[] selection = FTClient.this.tree.getSelection();
				if(selection != null && selection.length != 0) {
					TreeItem item = selection[0];
					FTClient.this.selectedFilePath = item.getText(0).equals("../") ? item.getText(0) : FTClient.this.currentFTpath + item.getText(0);
					if(FTClient.this.selectedFilePath.endsWith("/")) {
						FTClient.this.cmdsToSendToServer.add("DIR: " + FTClient.this.selectedFilePath);
					} else {
						FTClient.this.openDownloadedFile = isFileViewable(FilenameUtils.getExtension(item.getText(0)));
						FTClient.this.cmdsToSendToServer.add("GETFILE: " + FTClient.this.selectedFilePath);
					}
				} else {
					FTClient.this.selectedFilePath = null;
					FTClient.this.tree.deselectAll();
				}
			}
		});
		Menu menu1 = new Menu(this.tree);
		this.tree.setMenu(menu1);
		this.tree.addMenuDetectListener(new MenuDetectListener() {
			@Override
			public void menuDetected(MenuDetectEvent e) {
				//System.out.println(e.getSource().getClass().getSimpleName());
				TreeItem[] items = FTClient.this.tree.getSelection();
				if(items == null || items.length == 0 || items[0] == null) {
					e.doit = false;
				}
			}
		});
		this.tree.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				TreeItem[] items = FTClient.this.tree.getSelection();
				if(items.length == 0) {
					return;
				}
				final TreeItem item = items[0];
				if(e.keyCode == (SWT.ALT | '\r')) {
					e.doit = false;
				}
				String fileName = item.getText(0);
				final String filePath = FTClient.this.currentFTpath + fileName;
				File folder = FTClient.this.currentFTpath.equals("/") ? FTClient.this.downloadPath : new File(FTClient.this.downloadPath, (FTClient.this.currentFTpath.startsWith("/") ? FTClient.this.currentFTpath.substring(1) : FTClient.this.currentFTpath).replace("/", File.separator));
				if(!folder.exists()) {
					folder.mkdirs();
				}
				final File local = new File(folder, fileName);
				final String ext = FilenameUtils.getExtension(fileName);
				final boolean viewable = isFileViewable(ext);
				
			}
		});
		menu1.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {
				MenuItem[] items = menu1.getItems();
				for(int i = 0; i < items.length; i++) {
					items[i].dispose();
				}
				TreeItem item = FTClient.this.tree.getSelection()[0];
				String fileName = item.getText(0);
				final String filePath = FTClient.this.currentFTpath + fileName;
				File folder = FTClient.this.currentFTpath.equals("/") ? FTClient.this.downloadPath : new File(FTClient.this.downloadPath, (FTClient.this.currentFTpath.startsWith("/") ? FTClient.this.currentFTpath.substring(1) : FTClient.this.currentFTpath).replace("/", File.separator));
				if(!folder.exists()) {
					folder.mkdirs();
				}
				final File local = new File(folder, fileName);
				final String ext = FilenameUtils.getExtension(fileName);
				final boolean viewable = isFileViewable(ext);
				if(fileName.endsWith("/")) {
					MenuItem newItem = new MenuItem(menu1, SWT.NONE);
					menu1.setDefaultItem(newItem);
					newItem.setText("Browse");
					newItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public final void widgetSelected(SelectionEvent event) {
							FTClient.this.cmdsToSendToServer.add("DIR: " + filePath);
						}
					});
					//TODO add the ability to download an entire folder!
				} else {
					MenuItem newItem = new MenuItem(menu1, SWT.NONE);
					menu1.setDefaultItem(newItem);
					if(viewable) {
						newItem.setText("Download and view file");
					} else {
						newItem.setText("Download file");
					}
					newItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public final void widgetSelected(SelectionEvent event) {
							FTClient.this.openDownloadedFile = viewable;
							FTClient.this.cmdsToSendToServer.add("GETFILE: " + filePath);
						}
					});
					if(viewable) {
						MenuItem newItem1 = new MenuItem(menu1, SWT.NONE);
						newItem1.setText("Download file");
						newItem1.addSelectionListener(new SelectionAdapter() {
							@Override
							public final void widgetSelected(SelectionEvent event) {
								FTClient.this.openDownloadedFile = false;
								FTClient.this.cmdsToSendToServer.add("GETFILE: " + filePath);
							}
						});
					}
				}
				if(local.exists()) {
					if(local.isFile() && viewable) {
						MenuItem newItem1 = new MenuItem(menu1, SWT.NONE);
						newItem1.setText("Edit locally");
						newItem1.addSelectionListener(new SelectionAdapter() {
							@Override
							public final void widgetSelected(SelectionEvent event) {
								try {
									Desktop.getDesktop().edit(local);
								} catch(IOException e) {
									if(!Program.launch(local.getAbsolutePath(), folder.getAbsolutePath())) {
										try {
											Desktop.getDesktop().open(local);
										} catch(IOException e1) {
											Main.showFileToUser(local);
										}
									}
								}
							}
						});
					}
					MenuItem newItem1 = new MenuItem(menu1, SWT.NONE);
					newItem1.setText(local.isDirectory() ? "Browse locally" : "Show in folder");
					newItem1.addSelectionListener(new SelectionAdapter() {
						@Override
						public final void widgetSelected(SelectionEvent event) {
							Main.showFileToUser(local);
						}
					});
				}
				
				new MenuItem(menu1, SWT.SEPARATOR);
				
				MenuItem newItem2 = new MenuItem(menu1, SWT.NONE);
				newItem2.setText("Rename " + (fileName.endsWith("/") ? "folder" : "file") + "...");
				newItem2.addSelectionListener(new SelectionAdapter() {
					
					@Override
					public final void widgetSelected(SelectionEvent event) {
						FTClient.this.cmdsToSendToServer.add("RENAME: " + filePath + "\r\n" + new RenameFileDialog(FTClient.this.shell).open(fileName.replace("/", "")));
					}
				});
				MenuItem newItem3 = new MenuItem(menu1, SWT.NONE);
				newItem3.setText("Delete " + (fileName.endsWith("/") ? "folder" : "file"));
				newItem3.addSelectionListener(new SelectionAdapter() {
					@Override
					public final void widgetSelected(SelectionEvent event) {
						Response delete = new ConfirmDeleteServerFileDialog(FTClient.this.shell).open(fileName);
						if(delete == Response.YES) {
							FTClient.this.cmdsToSendToServer.add("DELETE: " + filePath);
						}
					}
				});
			}
		});
		
		this.trclmnName = new TreeColumn(this.tree, SWT.NONE);
		this.trclmnName.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(FTClient.this.treeSortType != TreeSortType.FILENAME) {
					FTClient.this.treeSortType = TreeSortType.FILENAME;
					FTClient.this.listTypeDirty = true;
					FTClient.this.trclmnName.setText("Name \u25B2");
					FTClient.this.trclmnSize.setText("Size");
					FTClient.this.trclmnDate.setText("Date Modified");
				} else {
					final TreeSortType oldType = FTClient.this.treeSortType;
					FTClient.this.treeSortType = TreeSortType.DOWNLOAD;
					FTClient.this.listTypeDirty = oldType != TreeSortType.DOWNLOAD;
					FTClient.this.trclmnName.setText("Name \u25BC");
					FTClient.this.trclmnSize.setText("Size");
					FTClient.this.trclmnDate.setText("Date Modified");
				}
			}
		});
		this.trclmnName.setResizable(false);
		this.trclmnName.setWidth(this.shell.getSize().x - 295);
		this.trclmnName.setText("Name \u25BC");
		
		this.trclmnSize = new TreeColumn(this.tree, SWT.NONE);
		this.trclmnSize.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(FTClient.this.treeSortType != TreeSortType.SIZE) {
					FTClient.this.treeSortType = TreeSortType.SIZE;
					FTClient.this.listTypeDirty = true;
					FTClient.this.trclmnName.setText("Name");
					FTClient.this.trclmnSize.setText("Size \u25BC");
					FTClient.this.trclmnDate.setText("Date Modified");
				} else {
					final TreeSortType oldType = FTClient.this.treeSortType;
					FTClient.this.treeSortType = TreeSortType.DOWNLOAD;
					FTClient.this.listTypeDirty = oldType != TreeSortType.DOWNLOAD;
					FTClient.this.trclmnName.setText("Name \u25BC");
					FTClient.this.trclmnSize.setText("Size");
					FTClient.this.trclmnDate.setText("Date Modified");
				}
			}
		});
		this.trclmnSize.setResizable(false);
		this.trclmnSize.setWidth(85);
		this.trclmnSize.setText("Size");
		
		this.trclmnDate = new TreeColumn(this.tree, SWT.NONE);
		this.trclmnDate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(FTClient.this.treeSortType != TreeSortType.DATE) {
					FTClient.this.treeSortType = TreeSortType.DATE;
					FTClient.this.listTypeDirty = true;
					FTClient.this.trclmnName.setText("Name");
					FTClient.this.trclmnSize.setText("Size");
					FTClient.this.trclmnDate.setText("Date Modified \u25BC");
				} else {
					final TreeSortType oldType = FTClient.this.treeSortType;
					FTClient.this.treeSortType = TreeSortType.DOWNLOAD;
					FTClient.this.listTypeDirty = oldType != TreeSortType.DOWNLOAD;
					FTClient.this.trclmnName.setText("Name \u25BC");
					FTClient.this.trclmnSize.setText("Size");
					FTClient.this.trclmnDate.setText("Date Modified");
				}
			}
		});
		this.trclmnDate.setResizable(false);
		this.trclmnDate.setWidth(180);
		this.trclmnDate.setText("Date Modified");
		
		Button btnUp = new Button(this.shell, SWT.NONE);
		btnUp.setToolTipText("Go up one level");
		btnUp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FTClient.this.cmdsToSendToServer.add("DIR: ../");
			}
		});
		btnUp.setBounds(10, 36, 24, 24);
		//btnUp.setText("../");
		btnUp.setImage(icons[11]);
		
		//this.shell.setSize(640, 489);
		
		this.currentFTPath = new Text(this.shell, SWT.BORDER | SWT.READ_ONLY);
		this.currentFTPath.setToolTipText("Current server directory");
		this.currentFTPath.setEditable(false);
		this.currentFTPath.setBounds(100, 36, this.shell.getSize().x - 116, 24);
		
		this.btnUpload = new Button(this.shell, SWT.NONE);
		this.btnUpload.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(FTClient.this.shell);
				dialog.setFilterPath(FTClient.this.downloadPath.getAbsolutePath());
				String filePath = dialog.open();
				if(filePath != null) {
					File check = new File(filePath);
					if(check.isFile()) {
						FTClient.this.filesToUpload.add(check);
						new UploadProgress(FTClient.this.shell, true, FTClient.this.uploadProgress, new Property<>("FileName", check.getName())).open();
					}
				}
			}
		});
		this.btnUpload.setBounds(10, 66, 197, 23);
		this.btnUpload.setText("Upload file...");
		
		this.btnRefresh = new Button(this.shell, SWT.NONE);
		this.btnRefresh.setToolTipText("Refresh the current directory");
		this.btnRefresh.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FTClient.this.cmdsToSendToServer.add("LIST");
			}
		});
		this.btnRefresh.setImage(icons[8]);
		this.btnRefresh.setBounds(40, 36, 24, 24);
		
		this.btnNewFolder = new Button(this.shell, SWT.NONE);
		this.btnNewFolder.setToolTipText("Create new folder");
		this.btnNewFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FTClient.this.cmdsToSendToServer.add("MKDIR: " + new CreateFolderDialog(FTClient.this.shell).open());
			}
		});
		this.btnNewFolder.setImage(icons[16]);
		this.btnNewFolder.setBounds(70, 36, 24, 24);
		
		Button btnUploadLocallyChanged = new Button(this.shell, SWT.NONE);
		btnUploadLocallyChanged.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int numOfFilesUploaded = 0;
				for(TreeItem item : FTClient.this.tree.getItems()) {
					String fileName = item.getText(0);
					File local = new File(getDefaultDownloadPath(), fileName);
					if(!local.isFile()) {
						continue;
					}
					Date date;
					try {
						date = StringUtil.getCacheValidatorTimeFormat().parse(item.getText(2));
					} catch(ParseException ignored) {
						date = null;//this should never happen
					}
					if(date == null) {
						continue;
					}
					long serverTime = date.getTime();
					long localTime = -1L;
					try {
						URLConnection url = local.toURI().toURL().openConnection();
						localTime = url.getLastModified() - 3000L;
						url.getInputStream().close();
						url.getOutputStream().close();
					} catch(Throwable ignored) {
						if(localTime == -1L) {
							localTime = local.lastModified();
						}
					}
					if(localTime > serverTime) {
						FTClient.this.filesToUpload.add(local);
						numOfFilesUploaded++;
					}
				}
			}
		});
		btnUploadLocallyChanged.setToolTipText("Uploads local files that are found to be newer than their\r\nserver counterparts in the current directory.");
		btnUploadLocallyChanged.setBounds(417, 66, 197, 23);
		btnUploadLocallyChanged.setText("Upload locally changed files");
		
		//this.shell.setSize(640, 489);
		
		this.btnSetDownloadDirectory = new Button(this.shell, SWT.NONE);
		this.btnSetDownloadDirectory.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(FTClient.this.shell);
				dialog.setText("Choose a download destination");
				dialog.setMessage("Choose the folder that files from the server will be downloaded to.");
				dialog.setFilterPath(FTClient.this.downloadPath.getAbsolutePath());
				String filePath = dialog.open();
				if(filePath != null) {
					File check = new File(filePath);
					if(check.isDirectory()) {
						FTClient.this.downloadPath = check;
					}
				}
			}
		});
		this.btnSetDownloadDirectory.setBounds(this.shell.getSize().x - 162, 10, 146, 20);
		this.btnSetDownloadDirectory.setText("Set download directory...");
		
		Button btnOpenDownloadFolder = new Button(this.shell, SWT.NONE);
		btnOpenDownloadFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					Desktop.getDesktop().browse(FTClient.this.downloadPath.toURI());
				} catch(IOException e1) {
					Program.launch(FTClient.this.downloadPath.getAbsolutePath());
				}
			}
		});
		btnOpenDownloadFolder.setBounds(213, 66, 198, 23);
		btnOpenDownloadFolder.setText("Open download folder");
		
		Label label = new Label(this.shell, SWT.SEPARATOR | SWT.VERTICAL);
		label.setBounds(620, 66, 2, 23);
		
	}
	
	/** Use ONLY for Number strings! */
	public static final Comparator<String> CASE_INSENSITIVE_ORDER = new CaseInsensitiveComparator();
	
	protected static class CaseInsensitiveComparator implements Comparator<String> {
		
		@Override
		public int compare(String s1, String s2) {
			if(s1.contains("-")) {
				s1 = s1.substring(0, s1.lastIndexOf("-"));
			}
			if(s2.contains("-")) {
				s2 = s2.substring(0, s2.lastIndexOf("-"));
			}
			double n1 = Double.valueOf(s1).doubleValue();
			double n2 = Double.valueOf(s2).doubleValue();
			return n1 < n2 ? -1 : (n1 == n2 ? 0 : 1);
			/*int n1 = s1.length();
			int n2 = s2.length();
			int min = Math.min(n1, n2);
			for(int i = 0; i < min; i++) {
				char c1 = s1.charAt(i);
				char c2 = s2.charAt(i);
				if(c1 != c2) {
					c1 = Character.toUpperCase(c1);
					c2 = Character.toUpperCase(c2);
					if(c1 != c2) {
						c1 = Character.toLowerCase(c1);
						c2 = Character.toLowerCase(c2);
						if(c1 != c2) {
							// No overflow because of numeric promotion
							return c1 - c2;
						}
					}
				}
			}
			return n1 - n2;*/
		}
		
	}
}
