package test;

import net.Client;
import net.CommunicationManager;
import util.Utils;

public class TestClient {
	public static void main(String args[]) {
		CommunicationManager.loadCipherKeys(Utils.LOCAL_WD + Utils.KEYS_FILE);
		
		Client client = new Client("127.0.0.1", 12345);
		
		client.sendFile("/home/theo/Documents/tmp/test/Client/smallfile", "/home/theo/Documents/tmp/test/Server/outfile");
		client.sendCommand("mkdir", "/home/theo/Documents/tmp/test/test_dir");
		client.sendCommand("bash", "-c", "echo hello_world > /home/theo/Documents/tmp/test/test_dir/test_file");
		client.sendEOF();
		
		boolean success = client.verboseWaitFor();
		
		System.out.println("Success: " + success);
	}
}
