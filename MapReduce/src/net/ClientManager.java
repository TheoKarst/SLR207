package net;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/*
 * The client manager is used to manage a connection with a client, from the server. Servers are run on remote hosts, and execute commands
 * that are sent by clients. The server can also send back messages to the client.
 */
public class ClientManager extends CommunicationManager {
	
	public ClientManager(Socket clientSocket) {
		super(clientSocket);
	}
	
	public void parseCommands() {
		try {
			while(true) {
				int commandId = inputStream.readInt();
				
				switch(commandId) {
					case SEND_FILE_COMMAND:
						receiveFile();
						break;
					
					case SEND_BASH_COMMAND:
						receiveCommand();
						break;
						
					case EOF_MESSAGE:
						outputStream.writeInt(ACK_MESSAGE);
						outputStream.flush();
						return;
				}
			}
		}
		catch(IOException e) {
			printStackTrace(e);
		}
	}
	
	// Message format:
	// filePathSize:		int
	// filepath:			byte[]
	// fileSize:			long
	// fileData:			byte[]
	public void receiveFile() {
		
		try {
			// Read the filepath:
			int filepathSize = inputStream.readInt();
			byte[] buffer = new byte[filepathSize];
			inputStream.read(buffer);
			String filepath = new String(buffer, StandardCharsets.UTF_8);
			
	        FileOutputStream fileOutputStream = new FileOutputStream(filepath);
	        
	        // Read the file size:
	        long size = inputStream.readLong();
	        
	        int bytes = 0;
	        buffer = new byte[4*1024];
	        while (size > 0 && (bytes = inputStream.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
	            fileOutputStream.write(buffer,0,bytes);
	            size -= bytes;      // read upto file size
	        }
	        
	        fileOutputStream.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	// Message format:
	// nargs:				int
	// size_arg1:			int
	// arg1:				byte[]
	// size_arg2:			int
	// arg2:				byte[]
	// ...
	public void receiveCommand() {
		try {
			// Receive the arguments of the command to execute:
			int nargs = inputStream.readInt();
			String args[] = new String[nargs];
			
			for(int i = 0; i < nargs; i++) {
				byte buffer[] = new byte[inputStream.readInt()];
				
				inputStream.read(buffer);
				args[i] = new String(buffer, StandardCharsets.UTF_8);
			}
			
			// Execute the command:
			ProcessBuilder pb = new ProcessBuilder(args);
			Process process = pb.start();
			process.waitFor();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	// Message format:
	// commandId:		int
	// messageSize:		int
	// message:			byte[]
	public void sendOutMessage(String message) {
		try {
			// Send command id:
			outputStream.writeInt(SEND_OUT_MESSAGE);
			
			// Send the message size:
			outputStream.writeInt(message.length());
			
			// Send the message:
			outputStream.write(message.getBytes());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// Message format:
	// commandId:		int
	// messageSize:		int
	// message:			byte[]
	public void sendErrMessage(String message) {
		try {
			// Send command id:
			outputStream.writeInt(SEND_ERR_MESSAGE);
			
			// Send the message size:
			outputStream.writeInt(message.length());
			
			// Send the message:
			outputStream.write(message.getBytes());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void printStackTrace(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter writer = new PrintWriter(sw);
		e.printStackTrace(writer);
		
		sendErrMessage(sw.toString());
	}
}
