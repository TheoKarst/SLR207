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
	// size_arg1:			int
	// arg1:				byte[]
	// size_arg2:			int
	// arg2:				byte[]
	// ...
	
	// List of availavle commands:
	// 0: createMapFromSplit(splitFile)
	// 1: createShuffleFromMap(mapFile)
	// 2: reduceProcedure
	// 3: collectReduces(masterAddr, masterFolder)
	public void sendMapReduceCommand(int mode, String... args) {
		try {
			// Send command id:
			outputStream.writeInt(SEND_MAP_REDUCE_COMMAND);
			
			// Send mode:
			outputStream.writeInt(mode);
			
			// Send related strings, if any:			
			for(String arg : args) {
				outputStream.writeInt(arg.length());
				outputStream.write(arg.getBytes());
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
			inputStream.readFully(buffer);
			
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
	
	public boolean waitForFiles() {
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
						
					case SEND_FILE_COMMAND:
						receiveFile();
						break;
						
					case SEND_BASH_COMMAND:
						System.err.println("[ERROR] Receiving a bash command from the server but it wasn't expected");
						return false;
						
					case ACK_MESSAGE:
						return true;
						
					default:
						System.err.println("Unexpected message from the server");
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
						
					case SEND_FILE_COMMAND:
						System.err.println("[ERROR] Receiving a file from the server but it wasn't expected");
						return false;
						
					case SEND_BASH_COMMAND:
						System.err.println("[ERROR] Receiving a bash command from the server but it wasn't expected");
						return false;
						
					case ACK_MESSAGE:
						return true;
						
					default:
						System.err.println("Unexpected message from the server");
						return false;
				}
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	@Override
	public void sendFile(String filepath, String remoteFilepath) {
		try {
			super.sendFile(filepath, remoteFilepath);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void receiveFile() {
		try {
			super.receiveFile();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
}
