package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

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
	static HashMap<String, UserTracker> userList = new HashMap<>(); //added by AJ - created a HashMap to pull all the user names and their sockets from the UserTracker Class I created
	static HashMap<String, BufferedReader> readers = new HashMap<>();
	static HashMap<String, PrintWriter> writers = new HashMap<>();
	String connectedUsers = "";
	String timeStamp = new SimpleDateFormat("EEE yyyy MMM dd hh:mm:ss a z").format(new Date());  //String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
	
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
//			PrintWriter broadcastedWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);
//				Message connectedUsers = mapper.readValue(raw, Message.class);
				timeStamp = new SimpleDateFormat("EEE yyyy MMM dd hh:mm:ss a z ").format(new Date());
				switch (message.getCommand()) {
				
					case "connect":
						
						log.info("user <{}> connected", message.getUsername());
						ClientHandler.userList.put(message.getUsername(), new UserTracker(message.getUsername(), this.socket));  //added by AJ
						ClientHandler.readers.put(message.getUsername(), reader);  //added by AJ
						ClientHandler.writers.put(message.getUsername(), writer);  //added by AJ
						
						
						break;
						
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						ClientHandler.readers.remove(message.getUsername());
						ClientHandler.writers.remove(message.getUsername());
						ClientHandler.userList.remove(message.getUsername());
						this.socket.close();
						break;
						
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
//						timeStamp = new SimpleDateFormat("EEE yyyy MMM dd hh:mm:ss a z").format(new Date());
						message.setContents(timeStamp + message.getContents());
						String response = mapper.writeValueAsString(message);
						writer.write(response);
						writer.flush();
						break;
						
					case "broadcast": //added by AJ
						log.info("user <{}> broadcasted message <{}>", message.getUsername(), message.getContents());  //added by AJ
						message.setContents(timeStamp + message.getContents());
						for(UserTracker userTracker: userList.values()) { //used a for loop to pull all the values from the userList HashMap
							String broadcastResponse = mapper.writeValueAsString(message);  //added by AJ
							writers.get(userTracker.getUsername()).write(broadcastResponse);  //added by AJ
							writers.get(userTracker.getUsername()).flush();  //added by AJ.getSocket().getOutputStream().write(mapper.writeValueAsString(message));	
						}
						break;
			
						
					case "users": //added by AJ
						log.info("user <{}> broadcasted message <{}>", message.getUsername(), message.getContents());  //added by AJ
						message.setContents(timeStamp + message.getContents());
						for(UserTracker userTracker: userList.values()) { //used a for loop to pull all the values from the userList HashMap
							connectedUsers += "\n" + userTracker.getUsername(); //userTracker.getUsername();
							String broadcastResponse = mapper.writeValueAsString(message);  //added by AJ
							writers.get(userTracker.getUsername()).write(connectedUsers);  //added by AJ
							writers.get(userTracker.getUsername()).flush();  //added by AJ.getSocket().getOutputStream().write(mapper.writeValueAsString(message));	
						}
						break;
						
						
//						case "users": //added by AJ
//						//log.info("user <{}> connected users <{}>", message.getUsername(), message.getContents());  //added by AJ
//						for(UserTracker userTracker: userList.values()) { //used a for loop to pull all the values from the userList HashMap
////							log.info("this loop has executed");
//							connectedUsers += "\n" + userTracker.getUsername();
//						}	
//						PrintWriter userWriters = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
//						message.setContents(timeStamp + message.getContents());
//							String messageWriter = mapper.writeValueAsString(message);  //added by AJ
//							userWriters.write(messageWriter);  //added by AJ
//							userWriters.flush();  //added by AJ.getSocket().getOutputStream().write(mapper.writeValueAsString(message));		
//							break;
							
						case "@user": //added by AJ
							log.info("user <{}> direct message <{}>", message.getUsername(), message.getContents());  //added by AJ
							for(UserTracker userTracker: userList.values()) { //used a for loop to pull all the values from the userList HashMap
								message.setContents(timeStamp + message.getContents());
								String broadcastResponse = mapper.writeValueAsString(message);  //added by AJ
								writers.get(userTracker.getUsername()).write(broadcastResponse);  //added by AJ
								writers.get(userTracker.getUsername()).flush();  //added by AJ.getSocket().getOutputStream().write(mapper.writeValueAsString(message));
								
							}
							break;
						
						
//					case "@user": //added by AJ
//						log.info("user <{}> broadcasted message <{}>", message.getUsername(), message.getContents());  //added by AJ
//						for(UserTracker userTracker: userList.values()) { //used a for loop to pull all the values from the userList HashMap
//							String broadcastResponse = mapper.writeValueAsString(message);  //added by AJ
//							writers.get(userTracker.getUsername()).write(broadcastResponse);  //added by AJ
//							writers.get(userTracker.getUsername()).flush();  //added by AJ.getSocket().getOutputStream().write(mapper.writeValueAsString(message));
//							break;
//						}
//						

				
//						break;  //added by AJ
					default:
				
					
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
