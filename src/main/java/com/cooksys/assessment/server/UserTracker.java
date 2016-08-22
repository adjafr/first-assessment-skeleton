package com.cooksys.assessment.server;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;

import javax.xml.crypto.Data;




public class UserTracker
{

//added by AJ starts here
	String username;
	Socket socket;
	
	public UserTracker()
	{
		// TODO Auto-generated constructor stub
	}
	
	
	public UserTracker(String username, Socket socket)
	{
		super();
		this.username = username;
		this.socket = socket;
	}
	public String getUsername()
	{
		return username;
	}
	public void setUsername(String username)
	{
		this.username = username;
	}
	public Socket getSocket()
	{
		return socket;
	}
	public void setSocket(Socket socket)
	{
		this.socket = socket;
	}
//added by AJ ends here
	
}
	
//    private static HashSet<String> users = new HashSet<String>(); //added by AJ
	
//	Object[] obj = new Object[2]{
//	
//	obj[0] = new User();
//	obj[1] = new Socket();
//	}
	


