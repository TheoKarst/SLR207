package net;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/*
 * The client is used to send commands to the server. These commands will be executed by the server, who can send back messages
 * to this client.
 */
public class Client extends CommunicationManager {
	
	public Client(String serverAddr, int serverPort) {		
		super(serverAddr, serverPort);
	}
	
	// Message format:
	// commandId:			int
	// filePathSize:		int
	// filepath:			byte[]
	// fileSize:			long
	// fileData:			byte[]
	public void sendFile(String filepath, String remoteFilepath) {
		File file = new File(filepath);
	       
	    try (FileInputStream fileInputStream = new FileInputStream(file)){
	    	// Send command id:
	    	outputStream.writeInt(SEND_FILE_COMMAND);
	    	
	    	// Send filepath:
	    	outputStream.writeInt(remoteFilepath.length());
	    	outputStream.write(remoteFilepath.getBytes());
	    	outputStream.flush();
	    	
	    	// Send File size:
	    	outputStream.writeLong(file.length());
	    	
	    	// Break file into chunks
	        int bytes = 0;
	        byte[] buffer = new byte[4*1024];
	        while ((bytes=fileInputStream.read(buffer))!=-1){
	        	outputStream.write(buffer,0,bytes);
	            outputStream.flush();
	        }
	    }
	    catch(IOException e) {
	     	e.printStackTrace();
	    }
	}
	
	// Message format:
	// commandId:			int
	// nargs:				int
	// size_arg1:			int
	// arg1:				byte[]
	// size_arg2:			int
	// arg2:				byte[]
	// ...
	public void sendCommand(String... command) {
		try {
			// Send command id:
			outputStream.writeInt(SEND_BASH_COMMAND);
			
			// Send number of arguments:
			outputStream.writeInt(command.length);
			
			for(String param : command) {
				outputStream.writeInt(param.length());
				outputStream.write(param.getBytes());
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Message format:
	// commandId:			int
	// mode:				int
	// file_size:			int (if any)
	// file:				byte[] (if any)
	public void sendMapReduceCommand(int mode, String relatedFile) {
		try {
			// Send command id:
			outputStream.writeInt(SEND_MAP_REDUCE_COMMAND);
			
			// Send mode:
			outputStream.writeInt(mode);
			
			// Send related file, if any:
			if(relatedFile != null) {
				outputStream.writeInt(relatedFile.length());
				outputStream.write(relatedFile.getBytes());
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendEOF() {
		try {
			outputStream.writeInt(EOF_MESSAGE);
			outputStream.flush();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	// Message format:
	// messageSize:		int
	// message:			byte[]
	public String receiveMessage() {
		try {
			byte buffer[] = new byte[inputStream.readInt()];
			inputStream.read(buffer);
			
			return new String(buffer, StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		return "";
	}

	// Wait for an acknowledgement from the server and ignore messages from the server.
	// Return true if everything happened as expected:
	public boolean waitFor() {
		try {
			while(true) {
				int commandId = inputStream.readInt();
				
				switch(commandId) {
					case SEND_OUT_MESSAGE:
					case SEND_ERR_MESSAGE:
						receiveMessage();		// Read the message from inputStream, but don't print it
						break;
						
					case ACK_MESSAGE:
						return true;
						
					default:
						return false;					
				}
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	// Wait for an acknowledgement from the server and print the messages from the server.
	// Return true if everything happened as expected:
	public boolean verboseWaitFor() {
		try {
			while(true) {
				int commandId = inputStream.readInt();
				
				switch(commandId) {
					case SEND_OUT_MESSAGE:
						System.out.println(receiveMessage());
						break;
					
					case SEND_ERR_MESSAGE:
						System.err.println(receiveMessage());
						break;
						
					case ACK_MESSAGE:
						return true;
						
					default:
						return false;
				}
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}
}
