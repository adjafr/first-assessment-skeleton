package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import javax.xml.crypto.Data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);

	private Socket socket;
	private BufferedReader in; //added by AJ
	private PrintWriter out; //added by AJ
	static ArrayList<UserTracker> userList = new ArrayList<>(); //added by AJ

	//Constructs a handler thread, squirreling away the socket. //added by AJ
	public ClientHandler(Socket socket) {
		super();
		this.socket = socket;
		
		
			
			
			
	}

	public void run() {
		try {
			
			ObjectMapper mapper = new ObjectMapper();
			
			//create character streams for the socket
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);

				switch (message.getCommand()) {
				
					case "connect":
						log.info("user <{}> connected", message.getUsername());
						ClientHandler.userList.add(new UserTracker(message.getUsername(), this.socket));  //added by AJ
						break;
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						this.socket.close();
						break;
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						String response = mapper.writeValueAsString(message);
						writer.write(response);
						writer.flush();
						break;
					case "broadcast": //added by AJ
						log.info("user <{}> broadcasted message <{}>", message.getUsername(), message.getContents());  //added by AJ
						String broadcastResponse = mapper.writeValueAsString(message);  //added by AJ
						writer.write(broadcastResponse);  //added by AJ
						writer.flush();  //added by AJ
						break;  //added by AJ
						
					
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
