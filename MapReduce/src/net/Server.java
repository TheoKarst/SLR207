package net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import util.Utils;

// https://heptadecane.medium.com/file-transfer-via-java-sockets-e8d4f30703a5

public class Server {
		
	public Server(int serverPort) {
		try(ServerSocket serverSocket = new ServerSocket(serverPort)){
			
			while(true) {
				Socket clientSocket = serverSocket.accept();
				
				new Thread(new Runnable() {
					@Override
					public void run() {
						// While there are commands to parse from the client, parse them:
						new ClientManager(clientSocket).parseCommands();
					}
				}).start();
			}
		}
		catch (IOException e) {
			Utils.printToLogFile(e);
		}
	}
}
