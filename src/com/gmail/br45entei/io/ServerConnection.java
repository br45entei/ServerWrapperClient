package com.gmail.br45entei.io;

import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** @author Brian_Entei */
public final class ServerConnection {
	
	public final String			ip;
	public final int			port;
	public final Socket			socket;
	public final InputStream	in;
	public final OutputStream	outStream;
	public final PrintWriter	out;
	
	public volatile String		pongStr				= null;
	public volatile boolean		serverActive		= false;
	public volatile boolean		serverJarSelected	= false;
	public volatile long		lastServerStateSet	= 0L;
	
	@SuppressWarnings("resource")
	public static final ServerConnection connectTo(String ip, int port) throws IOException {
		if(!IOUtils.isIPReachable(ip)) {
			return null;
		}
		Socket socket = new Socket();
		socket.setKeepAlive(true);
		//socket.setTcpNoDelay(true);
		InetSocketAddress addr = new InetSocketAddress(ip, port);
		socket.connect(addr);
		return new ServerConnection(socket, addr);
	}
	
	private ServerConnection(Socket socket, InetSocketAddress addr) throws IOException {
		this.ip = addr.getHostName();
		this.port = addr.getPort();
		this.socket = socket;
		this.in = this.socket.getInputStream();
		this.outStream = this.socket.getOutputStream();
		this.out = new PrintWriter(new OutputStreamWriter(this.outStream, StandardCharsets.UTF_8), true);
	}
	
	/** @return True if this connection is alive */
	public final boolean isAlive(boolean pingCheck) {
		pingCheck = false;
		if(pingCheck) {
			this.out.println("PING: " + Functions.nextSessionId());
			this.out.flush();
			if(this.out.checkError()) {
				return false;
			}
			return true;
		}
		return !this.socket.isClosed() && this.socket.isConnected();
	}
	
	public final boolean close(String partingMessage) {
		if(this.isAlive(false)) {
			try {
				this.out.println(partingMessage);
				this.out.flush();
			} catch(Throwable ignored) {
			}
		}
		return this.close();
	}
	
	/** @return True if this connection was successfully closed */
	public final boolean close() {
		try {
			this.in.close();
			this.outStream.close();
			this.out.close();
			this.socket.close();
		} catch(Throwable ignored) {
		}
		return !this.isAlive(false);
	}
	
}