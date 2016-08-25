package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);

	private Socket socket;
	private BufferedReader in; //
	private PrintWriter out; //
	static Map<String, UserTracker> userList = new HashMap<>(); // - created a HashMap to pull all the user names and their sockets from the UserTracker Class I created
	static Map<String, BufferedReader> readers = new HashMap<>(); // Buffered readers for each user connected to the Server
	static Map<String, PrintWriter> writers = new HashMap<>(); // PrintWriter for each user
	static Map<InetAddress, Integer> ipUserCount = new HashMap<>(); //InetAddress user count
	String timeStamp = new SimpleDateFormat("EEE yyyy MMM dd hh:mm:ss a z").format(new Date());  //String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
	
	//Constructs a handler thread, squirreling away the socket. //
	public ClientHandler(Socket socket) {
		super();
		this.socket = socket;
	}
	
	
	/**
	 * Handles the client Request as per the command typed by the user
	 *
	 */
	public void run() {
		try {
			ObjectMapper mapper = new ObjectMapper(); // map request and response to an object model
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));  //creates a buffered reader using the socket input stream
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));			//creates a print writer using the socket output stream
			while (!socket.isClosed()) {
				
				String rawMessage = reader.readLine();
				Message message = mapper.readValue(rawMessage, Message.class);
				timeStamp = new SimpleDateFormat("EEE yyyy MMM dd hh:mm:ss a z ").format(new Date());
				
				if (message.getCommand().charAt(0) == '@')
				{
					log.info("user <{}> direct message <{}>", message.getUsername(), message.getContents()); //
					String userToSend = message.getCommand().substring(1);
					message.setContents(
							timeStamp + "<" + message.getUsername() + "> " + "(whisper): " + message.getContents()); // message.getContents()
					String directResponse = mapper.writeValueAsString(message); //
					writers.get(userToSend).write(directResponse); //
					writers.get(userToSend).flush();
				} else
				{
					switch (message.getCommand())
					{
					case "connect":
						if(ipUserCount.get(socket.getInetAddress()) == null) {
							Integer ipCount = 1;
							ipUserCount.put(socket.getInetAddress(), ipCount);
							log.info("user <{}> connected", message.getUsername());
							message.setContents(timeStamp + " " + socket.getLocalAddress() + " <" + message.getUsername()
									+ ">" + " has connected");
							String connectionResponse = mapper.writeValueAsString(message); //
							for (UserTracker userTracker : userList.values()){
								writers.get(userTracker.getUsername()).write(connectionResponse); //
								writers.get(userTracker.getUsername()).flush(); // .getSocket().getOutputStream().write(mapper.writeValueAsString(message));
							}
							
							ClientHandler.userList.put(message.getUsername(),
									new UserTracker(message.getUsername(), this.socket)); //
							ClientHandler.readers.put(message.getUsername(), reader); //
							ClientHandler.writers.put(message.getUsername(), writer); //
						} else {
							if(ipUserCount.get(socket.getInetAddress()) >= 3) {
								
								socket.close();
								
							} else {
								int count = ipUserCount.get(socket.getInetAddress());
								count++;
								ipUserCount.put(socket.getInetAddress(), count);
								log.info("user <{}> connected", message.getUsername());
								message.setContents(timeStamp + " " + socket.getLocalAddress() + " <" + message.getUsername()
										+ ">" + " has connected");
								String connectionResponse = mapper.writeValueAsString(message); //
								for (UserTracker userTracker : userList.values()){
									writers.get(userTracker.getUsername()).write(connectionResponse); //
									writers.get(userTracker.getUsername()).flush(); // .getSocket().getOutputStream().write(mapper.writeValueAsString(message));
								}
								
								ClientHandler.userList.put(message.getUsername(),
										new UserTracker(message.getUsername(), this.socket)); //
								ClientHandler.readers.put(message.getUsername(), reader); //
								ClientHandler.writers.put(message.getUsername(), writer); //
							}
						}
						
						
						break;
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						ClientHandler.readers.remove(message.getUsername());
						ClientHandler.writers.remove(message.getUsername());
						ClientHandler.userList.remove(message.getUsername());
						this.socket.close();
						message.setContents(timeStamp + "<" + message.getUsername() + ">" + " has disconnected");
						String disconnectionMessage = mapper.writeValueAsString(message); //
						for (UserTracker userTracker : userList.values())
						{
							writers.get(userTracker.getUsername()).write(disconnectionMessage); //
							writers.get(userTracker.getUsername()).flush(); // .getSocket().getOutputStream().write(mapper.writeValueAsString(message));
						}
						break;
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						message.setContents(timeStamp + "<" + message.getUsername() + "> " + "(" + message.getCommand()
								+ "): " + message.getContents());
						String response = mapper.writeValueAsString(message);
						writer.write(response);
						writer.flush();
						break;
					case "broadcast": //
						log.info("user <{}> broadcasted message <{}>", message.getUsername(), message.getContents()); //
						message.setContents(
								timeStamp + "<" + message.getUsername() + "> " + "(all): " + message.getContents());
						for (UserTracker userTracker : userList.values())
						{ // used a for loop to pull all the values from the
							// userList HashMap
							String broadcastResponse = mapper.writeValueAsString(message); //
							writers.get(userTracker.getUsername()).write(broadcastResponse); //
							writers.get(userTracker.getUsername()).flush(); // .getSocket().getOutputStream().write(mapper.writeValueAsString(message));
						}
						break;
					case "users": //
						StringBuilder connectedUsers = new StringBuilder();
						log.info("user <{}> broadcasted message <{}>", message.getUsername(), message.getContents()); //
						for (UserTracker userTracker : userList.values())
						{ // used a for loop to pull all the values from the
							// userList HashMap
							connectedUsers.append("\n")
									.append(socket.getLocalAddress() + " <" + userTracker.getUsername() + ">"); // userTracker.getUsername();
						}
						message.setContents(timeStamp + "Currently connected users: " + connectedUsers.toString());
						String usersResponse = mapper.writeValueAsString(message); //
						writer.write(usersResponse); //
						writer.flush(); // .getSocket().getOutputStream().write(mapper.writeValueAsString(message));
						break;
					default:

					}
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
