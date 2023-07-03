package master;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import net.Client;
import net.CommunicationManager;
import util.Utils;

public class Master {

	public static void main(String[] args) {
		if(args.length != 3 && args.length != 4) {
			System.err.println("usage:\tjava Master [keys_file] [computers_file] [splits_folder]");
			System.err.println("or:\tjava Master [keys_file] [computers_file] [input_file] [n_splits]");
			System.exit(1);
		}
		
		final String WORKING_DIR = System.getProperty("user.dir");	// Folder from which the master was started
		
		// Where the different files are stored on the local computer:
		final String KEYS_FILE = args[0];
		final String COMPUTERS_FILE = args[1];
		final String SPLITS_FOLDER = args.length == 3 ? args[2] : WORKING_DIR + "/splits/";
		
		
		// Load the keys to setup the communication manager (this will allow us to cipher all the communications with a
		// random symmetric key, shared with ssh to all trusted computers). All the exchanges between trusted computers will
		// be using these keys and AES to have secured communications:
		CommunicationManager.loadCipherKeys(args[0]);
		
		if(args.length == 4) {
			createSplits(SPLITS_FOLDER, args[2], Integer.parseInt(args[3]));
		}
		
		// List the full name (path + filename) of the split files:
		ArrayList<String> filenames = Utils.listFiles(SPLITS_FOLDER, true);
				
		// List enough computers to perform our splits, among the computers listed in the given filename:
		ArrayList<String> splitComputers = Utils.loadLines(COMPUTERS_FILE, filenames.size());
		ArrayList<String> allComputers = Utils.loadLines(COMPUTERS_FILE);
		
		
		// Run all the steps of MapReduce algorithm, and measure the duration of each step:
		
		long t0 = System.currentTimeMillis();
		
		splitFiles(filenames, splitComputers);
		long t1 = System.currentTimeMillis();
		
		mapProcedure(filenames, splitComputers);
		long t2 = System.currentTimeMillis();
		
		sendComputerList(splitComputers, COMPUTERS_FILE);
		long t3 = System.currentTimeMillis();
		
		shuffleProcedure(filenames, splitComputers);
		long t4 = System.currentTimeMillis();
		
		reduceProcedure(allComputers);
		long t5 = System.currentTimeMillis();
		
		collectReducesProcedure(allComputers, WORKING_DIR + "/reduces/");
		long t6 = System.currentTimeMillis();
		
		System.out.println("Durations:");
		System.out.println("SPLIT;MAP;SHUFFLE;REDUCE;COLLECT");
		System.out.println((t1-t0) + ";" + (t2-t1) + ";" + (t4-t3) + ";" + (t5-t4) + ";" + (t6-t5));
	}
	
	// Create split files from the given inputFile, and save these files in the given folder:
	public static void createSplits(String splitsFolder, String inputFile, int nSplits) {
		// Create the folder if it didn't exists:
		File folder = new File(splitsFolder);
		if (!folder.exists())
			folder.mkdirs();
		
		long fileSize = new File(inputFile).length();		
		
		Thread threads[] = new Thread[nSplits];
		for(int i = 0; i < nSplits; i++) {
			
			// If i > 0, start reading from the first space character found (to avoid splitting a word between two files):
			final boolean startFromSpace = i > 0;
			final String filename = splitsFolder + "S" + i + ".txt";
			final long offset = i * fileSize / nSplits;
			
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					try (RandomAccessFile reader = new RandomAccessFile(inputFile, "r");
						FileOutputStream fileOutputStream = new FileOutputStream(filename)) {
						
						// Start reading file from offset:
						reader.seek(offset);
						
				        // Number of bytes we still have to read:
				        long bytesToRead = fileSize / nSplits;
				        
				        int bytes = 0;
				        byte[] buffer = new byte[4*1024];
				        
				        if(startFromSpace) {
					        while (bytesToRead > 0 && (bytes = reader.read(buffer, 0, (int)Math.min(bytesToRead, buffer.length))) != -1) {
					        	bytesToRead -= bytes;
					        	int index = new String(buffer, 0, bytes, StandardCharsets.UTF_8).indexOf(' ');
				        		
				        		if(index != -1) {
				        			fileOutputStream.write(buffer, index, bytes - index);
				        			break;
				        		}
					        }
				        }
				        
				        // Continue reading the file to the first space after bytesToRead = 0:
				        while ((bytes = reader.read(buffer, 0, bytesToRead <= 0 ? buffer.length : (int)Math.min(bytesToRead, buffer.length))) != -1) {
				        	
				        	// If we have read enough bytes, we search the first space to stop reading:
				        	if(bytesToRead <= 0) {
				        		int index = new String(buffer, 0, bytes, StandardCharsets.UTF_8).indexOf(' ');
				        		
				        		if(index != -1) {
				        			fileOutputStream.write(buffer, 0, index);
				        			break;
				        		}
				        	}
				        	else {
				        		fileOutputStream.write(buffer, 0, bytes);
				        		bytesToRead -= bytes;
				        	}
				        }
				        
					} catch (IOException e) {
						e.printStackTrace();
					}
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
	
	// Send the given files (the split files) to the given remote computers, in their splits folder.
	// Exactly one file is sent to each computer:
	public static void splitFiles(ArrayList<String> filenames, ArrayList<String> computers) {
		assert filenames.size() == computers.size();
		
		Thread threads[] = new Thread[filenames.size()];
		for(int i = 0; i < filenames.size(); i++) {
			final String host = Utils.getFullName(computers.get(i));
			final String local_filename = filenames.get(i);
			final String dest_filename = Utils.SPLITS_FOLDER + Utils.getBasename(local_filename);
			
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					Client client = new Client(host, Utils.PORT);
					
					// Create a folder on the remote computer to store the splits files:
					System.out.println("Host " + host + ": mkdir -p " + Utils.SPLITS_FOLDER);
					client.sendCommand("mkdir", "-p", Utils.SPLITS_FOLDER);
					
					// Send the split file to the remote computer:
					System.out.println("Send " + local_filename + " to " + host + " as " + dest_filename);
					client.sendFile(local_filename, dest_filename);
					
					// End of communication:
					client.sendEOF();
					
					// Wait for the commands to complete:
					System.out.println("Success: " + client.verboseWaitFor());
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
			final String splitFile = Utils.SPLITS_FOLDER + Utils.getBasename(filenames.get(i));
			
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					System.out.println("Host " + host + ": MapProcedure (0) with " + splitFile);
					
					Client client = new Client(host, Utils.PORT);
					client.sendMapReduceCommand(0, splitFile);
					
					// End of communication:
					client.sendEOF();
					
					// Wait for the client to complete:
					System.out.println("Success: " + client.verboseWaitFor());
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
					System.out.println("Send " + computersFile + " to " + host + " as " + Utils.COMPUTERS_FILE);
					
					Client client = new Client(host, Utils.PORT);
					client.sendFile(computersFile, Utils.COMPUTERS_FILE);
					
					// End of communication:
					client.sendEOF();
					
					// Wait for the client to complete:
					System.out.println("Success: " + client.verboseWaitFor());
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
			final String mapFile = Utils.splitNameToMapName(filenames.get(i));
			
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					System.out.println("Host " + host + ": ShuffleProcedure (1) with " + mapFile);
					
					Client client = new Client(host, Utils.PORT);
					client.sendMapReduceCommand(1, mapFile);
					
					// End of communication:
					client.sendEOF();

					// Wait for the client to complete:
					System.out.println("Success: " + client.verboseWaitFor());
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
					System.out.println("Host " + host + ": ReduceProcedure (2)");
					
					Client client = new Client(host, Utils.PORT);
					client.sendMapReduceCommand(2);
					
					// End of communication:
					client.sendEOF();
					
					// Wait for the client to complete:
					System.out.println("Success: " + client.verboseWaitFor());
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
	
	public static void collectReducesProcedure(ArrayList<String> computers, String reducesFolder) {
		
		// Create the folder to save the reduces:
		File folder = new File(reducesFolder);
		if (!folder.exists())
		    folder.mkdirs();
		
		Thread threads[] = new Thread[computers.size()];
		for(int i = 0; i < computers.size(); i++) {
			final String host = Utils.getFullName(computers.get(i));
			
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					System.out.println("Host " + host + ": CollectReducesProcedure (3)");
					
					Client client = new Client(host, Utils.PORT);
					client.sendMapReduceCommand(3, reducesFolder);
					
					// End of communication:
					client.sendEOF();
					
					// Wait for the client to complete:
					System.out.println("Success: " + client.waitForFiles());
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
		
		System.out.println("REDUCE COLLECT FINISHED. Files saved in: " + reducesFolder);
	}
}
