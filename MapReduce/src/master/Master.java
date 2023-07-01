package master;

import java.util.ArrayList;

import net.Client;
import net.CommunicationManager;
import util.Utils;

public class Master {

	public static void main(String[] args) {
		if(args.length != 2) {
			System.err.println("usage: java Master [keys_file] [computers_file] [splits_folder]");
			System.exit(1);
		}
		
		// Load the keys to setup the communication manager (this will allow us to cipher all the communications with a
		// random symmetric key, shared with ssh to all trusted computers). All the exchanges between trusted computers will
		// be using these keys and AES to have secured communications:
		CommunicationManager.loadCipherKeys(args[0]);
		
		// List the names of the split files:
		ArrayList<String> filenames = Utils.listFiles(args[2]);
				
		// List enough computers to perform our splits, among the computers listed in the given filename:
		ArrayList<String> splitComputers = Utils.loadLines(args[1], filenames.size());
		ArrayList<String> allComputers = Utils.loadLines(args[1]);
		
		
		// Split the files to the remote computers:
		splitFiles(filenames, splitComputers);
		
		// Run the map and shuffle procedures on the remote computers:
		mapProcedure(filenames, splitComputers);			
		sendComputerList(splitComputers, args[1]);
		shuffleProcedure(filenames, splitComputers);
		reduceProcedure(allComputers);
	}
	
	// Send the given files (the split files) to the given remote computers, in their splits folder.
	// Exactly one file is sent to each computer:
	public static void splitFiles(ArrayList<String> filenames, ArrayList<String> computers) {
		assert filenames.size() == computers.size();
		
		Thread threads[] = new Thread[filenames.size()];
		for(int i = 0; i < filenames.size(); i++) {
			final String host = Utils.getFullName(computers.get(i));
			final String filename = Utils.SPLITS_FOLDER + filenames.get(i);
			
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					Client client = new Client(host, Utils.PORT);
					
					// Create a folder on the remote computer to store the splits files:
					System.out.println("ssh " + host + " mkdir -p " + Utils.SPLITS_FOLDER);
					client.sendCommand("mkdir", "-p", Utils.SPLITS_FOLDER);
					
					// Send the split file to the remote computer:
					System.out.println("scp " + filename + " " + host + ":" + Utils.SPLITS_FOLDER);
					client.sendFile(filename, Utils.SPLITS_FOLDER);
					
					// Wait for the commands to complete:
					client.verboseWaitFor();
				}
			});
			
			threads[i].start();
		}
		
		// Wait for all threads to complete:
		try {
			for(Thread thread : threads)
				thread.join();
		}
		catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	// Asserting that computers.get(i) has the split file filenames.get(i), we run the mapping procedure on each computer
	// with the corresponding file, using the slave.jar:
	public static void mapProcedure(ArrayList<String> filenames, ArrayList<String> computers) {
		assert filenames.size() == computers.size();
		
		Thread threads[] = new Thread[filenames.size()];
		for(int i = 0; i < filenames.size(); i++) {
			final String host = Utils.getFullName(computers.get(i));
			final String splitFile = Utils.SPLITS_FOLDER + filenames.get(i);
			
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					System.out.println("ssh " + host + " java -jar " + Utils.SLAVE_FILE + " 0 " + splitFile);
					
					Client client = new Client(host, Utils.PORT);
					client.sendMapReduceCommand(0, splitFile);
					
					// Wait for the client to complete:
					client.verboseWaitFor();
				}
			});
			
			threads[i].start();
		}
		
		// Wait for all threads to complete:
		try {
			for(Thread thread : threads)
				thread.join();
		}
		catch(InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("MAP FINISHED\n");
	}
	
	// Send the given file to all the computers listed, and save it as "machines.txt":
	public static void sendComputerList(ArrayList<String> computers, String computersFile) {

		Thread threads[] = new Thread[computers.size()];
		for(int i = 0; i < computers.size(); i++) {
			final String host = Utils.getFullName(computers.get(i));
			
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					System.out.println("scp " + computersFile + " " + host + ":" + Utils.COMPUTERS_FILE);
					
					Client client = new Client(host, Utils.PORT);
					client.sendFile(computersFile, Utils.COMPUTERS_FILE);
					
					// Wait for the client to complete:
					client.verboseWaitFor();
				}
			});
			
			threads[i].start();
		}
		
		// Wait for all threads to complete:
		try {
			for(Thread thread : threads)
				thread.join();
		}
		catch(InterruptedException e) {
			e.printStackTrace();
		}		
	}
	
	// Asserting that computers.get(i) has the split file filenames.get(i), we run the shuffle procedure on each computer
	// with the corresponding file, using the slave.jar:
	public static void shuffleProcedure(ArrayList<String> filenames, ArrayList<String> computers) {
		
		Thread threads[] = new Thread[computers.size()];
		for(int i = 0; i < computers.size(); i++) {
			final String host = Utils.getFullName(computers.get(i));
			final String mapFile = Utils.MAPS_FOLDER + Utils.splitNameToMapName(filenames.get(i));
			
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					System.out.println("ssh " + host + " java -jar " + Utils.SLAVE_FILE + " 1 " + mapFile);
					
					Client client = new Client(host, Utils.PORT);
					client.sendMapReduceCommand(1, mapFile);

					// Wait for the client to complete:
					client.verboseWaitFor();
				}
			});
			
			threads[i].start();
		}
		
		// Wait for all threads to complete:
		try {
			for(Thread thread : threads)
				thread.join();
		}
		catch(InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("SHUFFLE FINISHED\n");
	}
	
	public static void reduceProcedure(ArrayList<String> computers) {
		
		Thread threads[] = new Thread[computers.size()];
		for(int i = 0; i < computers.size(); i++) {
			final String host = Utils.getFullName(computers.get(i));
			
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					System.out.println("ssh " + host + " java -jar " + Utils.SLAVE_FILE + " 2");
					
					Client client = new Client(host, Utils.PORT);
					client.sendMapReduceCommand(2, null);
					
					// Wait for the client to complete:
					client.verboseWaitFor();
				}
			});
			
			threads[i].start();
		}
		
		// Wait for all threads to complete:
		try {
			for(Thread thread : threads)
				thread.join();
		}
		catch(InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("REDUCE FINISHED\n");
	}
}
