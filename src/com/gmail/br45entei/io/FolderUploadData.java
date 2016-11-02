package com.gmail.br45entei.io;

import java.io.File;

/** @author Brian_Entei */
public class FolderUploadData {
	
	/** The folder that will be uploaded */
	public final File	folder;
	/** The serverPath that will represent this folder on the server side */
	public final String	serverPath;
	
	/** @param folder The folder that will be uploaded
	 * @param serverPath The server path that the folder will go into */
	public FolderUploadData(File folder, String serverPath) {
		if(!folder.isDirectory()) {
			throw new IllegalArgumentException("folder argument must be a folder, not a file: " + folder.getAbsolutePath());
		}
		this.folder = folder;
		serverPath = serverPath.replace("\\", "/");
		serverPath = serverPath.startsWith("/") ? serverPath : "/" + serverPath;
		serverPath = serverPath.endsWith("/") ? serverPath : serverPath + "/";
		this.serverPath = serverPath + this.folder.getName() + "/";
	}
	
}
