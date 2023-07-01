package test;

import net.CommunicationManager;
import net.Server;
import util.Utils;

public class TestServer {

	public static void main(String[] args) {
		CommunicationManager.loadCipherKeys(Utils.LOCAL_WD + Utils.KEYS_FILE);
		
		new Server(12345);
	}

}
