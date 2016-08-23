package com.cooksys.assessment.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class Server implements Runnable {
	private Logger log = LoggerFactory.getLogger(Server.class);
	
	private int port;
	private ExecutorService executor;
	//private ServerSocket serverSocket; //added by AJ
	
	public Server(int port, ExecutorService executor) {
		super();
		this.port = port;
		//serverSocket = new ServerSocket(port); //added by AJ
		this.executor = executor;
	}
	
	

    
    
    /**
     * The set of all the print writers for all the clients.  This
     * set is kept so we can easily broadcast messages.
     */
//    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>(); //added by AJ
	

//	Thread accept = new Thread() { //added by AJ
	public void run() {
		log.info("server started");
//		Server server = new Server(); //added by AJ
//		Thread serverThread = new Thread(server); //added by AJ
		ServerSocket ss;
		try {
			ss = new ServerSocket(this.port);
			while (true) {
				Socket socket = ss.accept();
				ClientHandler handler = new ClientHandler(socket);
				executor.execute(handler);
			}
		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
//	;} //added by AJ




	
