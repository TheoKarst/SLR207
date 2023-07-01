package test;

import net.Client;
import net.CommunicationManager;
import util.Utils;

public class TestClient {
	public static void main(String args[]) {
		CommunicationManager.loadCipherKeys("/home/theo/Documents/Cours/SLR/SLR207/SLR207/bash_scripts/keys");
		
		Client client = new Client("tp-1a226-25.enst.fr", 52301);
		
		client.sendFile("/home/theo/Documents/tmp/test/Client/smallfile", "/tmp/karst-21/smallfile");
		client.sendCommand("mkdir", "/tmp/karst-21/test_directory");
		client.sendCommand("bash", "-c", "echo hello_world > " + "/tmp/karst-21/test_directory/testfile");
		client.sendEOF();
		
		boolean success = client.verboseWaitFor();
		
		System.out.println("Success: " + success);
	}
}
