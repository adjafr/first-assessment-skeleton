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
			ObjectMapper mapper = new ObjectMapper(); // map JSON requests from the client (JavaScript) to the message model
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));  //creates a buffered reader using the socket input stream
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));			//creates a print writer using the socket output stream
			while (!socket.isClosed()) {
				
				String rawMessage = reader.readLine();// reads line from the bufferedReader
				Message message = mapper.readValue(rawMessage, Message.class);// maps json to the Message model
				timeStamp = new SimpleDateFormat("EEE yyyy MMM dd hh:mm:ss a z ").format(new Date());// timestamp
				
				if (message.getCommand().charAt(0) == '@') // checks if this is a direct message
				{
					log.info("user <{}> direct message <{}>", message.getUsername(), message.getContents()); //
					String userToSend = message.getCommand().substring(1);
					message.setContents(
							timeStamp + "<" + message.getUsername() + "> " + "(whisper): " + message.getContents()); // message.getContents()
					String directResponse = mapper.writeValueAsString(message); //gets content from the Message model
					writers.get(userToSend).write(directResponse); //write it to the output stream
					writers.get(userToSend).flush(); // clean up of the output stream.
				
				} else 
				{
					switch (message.getCommand())
					{
					case "connect":
						
						if(ipUserCount.get(socket.getInetAddress()) == null) { // checks if the at least one user is connected from the ip address
							Integer ipCount = 1;
							ipUserCount.put(socket.getInetAddress(), ipCount); // to userCount from ip first
							log.info("user <{}> connected", message.getUsername()); 
							message.setContents(timeStamp + " " + socket.getLocalAddress() + " <" + message.getUsername()
									+ ">" + " has connected");// user connected content
							String connectionResponse = mapper.writeValueAsString(message); //get response from mapper
							
							/**
							 * notifies all users connected to the server of the userConnection by writing to each user's print writer
							 * 
							 */
							for (UserTracker userTracker : userList.values()){
								writers.get(userTracker.getUsername()).write(connectionResponse); //
								writers.get(userTracker.getUsername()).flush(); // .getSocket().getOutputStream().write(mapper.writeValueAsString(message));
							}
							
							ClientHandler.userList.put(message.getUsername(), new UserTracker(message.getUsername(), this.socket)); //add UserTracker to userListMap
							ClientHandler.readers.put(message.getUsername(), reader); // adds reader of user to readers Map by username
							ClientHandler.writers.put(message.getUsername(), writer); // adds writers of user to writers Map by username
						} else {
							
							/*
							 * 
							 * close socket if the 3 or more users have connected from the same IP address
							 */
							if(ipUserCount.get(socket.getInetAddress()) >= 3) {
								
								socket.close();
								
							} else {
								int ipCount = ipUserCount.get(socket.getInetAddress());
								ipCount++;// increase the count of user connected from same ip address
								ipUserCount.put(socket.getInetAddress(), ipCount); // set the updated count
								log.info("user <{}> connected", message.getUsername());
								message.setContents(timeStamp + " " + socket.getLocalAddress() + " <" + message.getUsername() // sets the content of the message
										+ ">" + " has connected");
								String connectionResponse = mapper.writeValueAsString(message); //get content as string from message model using mapper
								/**
								 * notifies all users connected to the server of the userConnection by writing to each user's print writer
								 * 
								 */
								for (UserTracker userTracker : userList.values()){
									writers.get(userTracker.getUsername()).write(connectionResponse); //
									writers.get(userTracker.getUsername()).flush(); // .getSocket().getOutputStream().write(mapper.writeValueAsString(message));
								}
								
								ClientHandler.userList.put(message.getUsername(), new UserTracker(message.getUsername(), this.socket)); //add UserTracker to userListMap
								ClientHandler.readers.put(message.getUsername(), reader); // adds reader of user to readers Map by username
								ClientHandler.writers.put(message.getUsername(), writer); // adds writers of user to writers Map by username
							}
						}
						
						
						break;
						
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						ClientHandler.readers.remove(message.getUsername()); //remove the user buffered reader from the readers map
						ClientHandler.writers.remove(message.getUsername()); // remove the printwriter from the witers map
						ClientHandler.userList.remove(message.getUsername()); // remove the user from the userTracker map
						this.socket.close(); // finally closing the socket connection
						
						int ipCount = ipUserCount.get(socket.getInetAddress());
						ipCount--;// increase the count of user connected from same ip address
						ipUserCount.put(socket.getInetAddress(), ipCount); // set the updated count
						
						message.setContents(timeStamp + "<" + message.getUsername() + ">" + " has disconnected");
						String disconnectionMessage = mapper.writeValueAsString(message); // get disconnection message from the message model using mapper
						/*
						 * notify all the users in the user map to alert them the disconnection message and decrease the ipCount
						 * of that computer by 1
						 */
						for (UserTracker userTracker : userList.values())
						{
							writers.get(userTracker.getUsername()).write(disconnectionMessage); //
							writers.get(userTracker.getUsername()).flush(); // .getSocket().getOutputStream().write(mapper.writeValueAsString(message));
							
						}
						break;
						
//					case "exit":
//						log.info("user <{}> disconnected", message.getUsername());
//						ClientHandler.readers.remove(message.getUsername()); //remove the user buffered reader from the readers map
//						ClientHandler.writers.remove(message.getUsername()); // remove the printwriter from the witers map
//						ClientHandler.userList.remove(message.getUsername()); // remove the user from the userTracker map
//						this.socket.close(); // finally closing the socket connection
//						
//						int ipCountExit = ipUserCount.get(socket.getInetAddress());
//						ipCountExit--;// increase the count of user connected from same ip address
//						ipUserCount.put(socket.getInetAddress(), ipCountExit); // set the updated count
//						
//						message.setContents(timeStamp + "<" + message.getUsername() + ">" + " has exited");
//						String exitMessage = mapper.writeValueAsString(message); // get disconnection message from the message model using mapper
//						/*
//						 * notify all the users in the user map to alert them the disconnection message and decrease the ipCount
//						 * of that computer by 1
//						 */
//						for (UserTracker userTracker : userList.values())
//						{
//							writers.get(userTracker.getUsername()).write(exitMessage); //
//							writers.get(userTracker.getUsername()).flush(); // .getSocket().getOutputStream().write(mapper.writeValueAsString(message));
//							
//						}
//						break;
						
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						message.setContents(timeStamp + "<" + message.getUsername() + "> " + "(" + message.getCommand()
								+ "): " + message.getContents());
						String response = mapper.writeValueAsString(message); // get message content as string from mapper
						writer.write(response); //echo message to the client PrintWrier 
						writer.flush(); // final cleanup
						break;
					case "broadcast": //
						log.info("user <{}> broadcasted message <{}>", message.getUsername(), message.getContents()); //
						message.setContents(
								timeStamp + "<" + message.getUsername() + "> " + "(all): " + message.getContents()); // set content of the broadcast message will display
						/*
						 * 
						 * broadcast each user the message that needs to be broadcasted 
						 */
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
						
						/*
						 * Get all the users form the userListMap and concatinate the ip address and the username from the user Tracker 
						 * write the constructed user -ip string to the printWiter of the currently connected user
						 * 
						 */
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
