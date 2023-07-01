package net;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import slave.Slave;
import util.Utils;

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
						
					case SEND_MAP_REDUCE_COMMAND:
						receiveMapReduceCommand();
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
			printStackTrace(e);
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
		catch(IOException | InterruptedException e) {
			printStackTrace(e);
		}
	}
	
	// Message format:
	// mode:				int
	// file_size:			int (if any)
	// file:				byte[] (if any)
	public void receiveMapReduceCommand() {
		try {
			int mode = inputStream.readInt();
			
			if(mode == 0 || mode == 1) {
				byte[] buffer = new byte[inputStream.readInt()];
				inputStream.read(buffer);
				String relatedFile = new String(buffer, StandardCharsets.UTF_8);
				
				if(mode == 0) {
					Slave.createMapFromSplit(relatedFile);
				}
				else if(mode == 1) {
					Slave.createShufflesFromMap(relatedFile);
					Slave.sendShuffles(Utils.loadLines(Utils.COMPUTERS_FILE));
				}
			}
			else if(mode == 2) {
				Slave.reduceFromShuffleReceived();
			}
		}
		catch(IOException e) {
			printStackTrace(e);
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
			printStackTrace(e);
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
			printStackTrace(e);
		}
	}
	
	public void printStackTrace(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter writer = new PrintWriter(sw);
		e.printStackTrace(writer);
		
		sendErrMessage(sw.toString());
	}
}
