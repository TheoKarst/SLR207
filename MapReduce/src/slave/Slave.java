package slave;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.Client;
import net.ClientManager;
import net.CommunicationManager;
import net.Server;
import util.Utils;

public class Slave {

	public static void main(String[] args) {
		// Load the keys to setup the communication manager (this will allow us to cipher all the communications with a
		// random symmetric key, shared with ssh to all trusted computers). All the exchanges between trusted computers will
		// be using these keys and AES to have secured communications:
		CommunicationManager.loadCipherKeys(Utils.KEYS_FILE);
		
		// Start the server on the given port, and execute commands that are received:
		new Server(Utils.PORT);
	}
	
	public static void createMapFromSplit(String fullpathSplit) {
		Utils.printToLogFile("createMapFromSplit(" + fullpathSplit + ")");
		
		// Create a directory to save the output file:
		File directory = new File(Utils.MAPS_FOLDER);
		if (!directory.exists())
		    directory.mkdirs();
		
		// Create a map file with a name depending on the name of the split file:
		String mapFilename = Utils.splitNameToMapName(fullpathSplit);
		
		try(BufferedReader reader = new BufferedReader(new FileReader(fullpathSplit));
			BufferedWriter writer = new BufferedWriter(new FileWriter(mapFilename))){
			
			String line = null;
			while((line = reader.readLine()) != null) {
				for(String word : line.split(" ")) {
					if(word != "")
						writer.write(word + " 1\n");
				}
			}
			
		} catch (IOException e) {
			Utils.printToLogFile(e);
		}
	}
	
	public static void createShufflesFromMap(String fullpathMap) {
		Utils.printToLogFile("createShufflesFromMap(" + fullpathMap + ")");
		
		String hostname = Utils.getHostName();
		Pattern pattern = Pattern.compile("UM([0-9]+)\\.txt");
		Matcher matcher = pattern.matcher(Utils.getBasename(fullpathMap));
		
		if(!matcher.matches()) {
			Utils.printToLogFile("Unexpected filename: should be like UM[number].txt");
			System.exit(1);
		}
		
		// Create a directory to save the output files:
		File directory = new File(Utils.SHUFFLES_FOLDER);
		if (!directory.exists())
		    directory.mkdirs();
		
		try(BufferedReader reader = new BufferedReader(new FileReader(fullpathMap))){
			
			String line = null;
			while((line = reader.readLine()) != null)
				createShuffleFile(line, hostname);
			
		} catch (IOException e) {
			Utils.printToLogFile(e);
		}
	}
	
	public static void sendShuffles(ArrayList<String> computers) {
		Utils.printToLogFile("sendShuffles(computers)");
		
		ArrayList<String> shuffles = Utils.listFiles(Utils.SHUFFLES_FOLDER, false);
		
		// Pattern of all files in the SHUFFLES_FOLDER:
		Pattern pattern = Pattern.compile("([0-9]+)-(.+)\\.txt");
		
		
		Thread threads[] = new Thread[shuffles.size()];
		for(int i = 0; i < shuffles.size(); i++) {
			final String shuffleFile = shuffles.get(i);
			
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					Matcher matcher = pattern.matcher(shuffleFile);
					
					if(!matcher.matches()) {
						Utils.printToLogFile("Unexpected filename in the shuffles folder: " + shuffleFile);
						return;
					}
					
					int hash = Integer.parseInt(matcher.group(1));
					
					// The computer where to send the file:
					String host = Utils.getFullName(computers.get(hash % computers.size()));
					
					Client client = new Client(host, Utils.PORT);
					
					// Create a folder on the remote computer to store the shuffled files:
					Utils.printToLogFile("mkdir -p " + Utils.SHUFFLES_RECV_FOLDER);
					client.sendCommand("mkdir", "-p", Utils.SHUFFLES_RECV_FOLDER);
					
					// Send the file to the remote computer:
					Utils.printToLogFile("Send " + Utils.SHUFFLES_FOLDER + shuffleFile + " to host " + host + 
							" at " + Utils.SHUFFLES_RECV_FOLDER + shuffleFile);
					client.sendFile(Utils.SHUFFLES_FOLDER + shuffleFile, Utils.SHUFFLES_RECV_FOLDER + shuffleFile);
					
					// End of communication:
					client.sendEOF();

					// Wait for the client to complete:
					client.waitFor();
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
			Utils.printToLogFile(e);
		}
	}
	
	public static void createShuffleFile(String line, String hostname) {
		Utils.printToLogFile("createShuffleFile(" + line + ", " + hostname + ")");
		
		String key = line.split(" ")[0];
		String shuffleFilename = Utils.SHUFFLES_FOLDER + key.hashCode() + "-" + hostname + ".txt";
		
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(shuffleFilename, true))){
			writer.write(line + "\n");
		}
		catch (IOException e) {
			Utils.printToLogFile(e);
		}
	}
	
	public static void reduceFromShuffleReceived() {
		Utils.printToLogFile("reduceFromShufflereceived()");
		
		// Get the files in schufflesreceived folder, and sort them by name (this way, file with the same hash are side by side:
		ArrayList<String> filenames = Utils.listFiles(Utils.SHUFFLES_RECV_FOLDER, false);
		Collections.sort(filenames);
		
		// Create a directory to save the output files:
		File directory = new File(Utils.REDUCES_FOLDER);
		if (!directory.exists())
		    directory.mkdirs();
		
		// Pattern of all files in the SHUFFLES_RECV_FOLDER:
		Pattern pattern = Pattern.compile("([0-9]+)-(.+)\\.txt");
		
		int fileIndex = 0;
		while(fileIndex < filenames.size()) {
			String previousHash = null;
			String currentHash = null;
			String currentKey = null;
			boolean sameHash = true;
			int occurences = 0;
			
			// While we have files with the same hash, we count the occurences of the key in all files:
			do {
				String filename = filenames.get(fileIndex);
				previousHash = currentHash;
				
				// Get the hash from the filename:
				Matcher matcher = pattern.matcher(filename);
				
				if(!matcher.matches()) {
					Utils.printToLogFile("Unexpected filename in folder shufflesreceived: " + filename);
					fileIndex++;
					continue;
				}
				
				currentHash = matcher.group(1);
				
				sameHash = previousHash == null || currentHash.equals(previousHash);
				if(sameHash) {
					// Count occurences:
					try(BufferedReader reader = new BufferedReader(new FileReader(Utils.SHUFFLES_RECV_FOLDER + filename))){
						String line = reader.readLine();
						
						currentKey = line.split(" ")[0];
						occurences += Integer.parseInt(line.split(" ")[1]);
						
						while((line = reader.readLine()) != null)
							occurences += Integer.parseInt(line.split(" ")[1]);
							
					} catch (IOException e) {
						Utils.printToLogFile(e);
					}
					
					fileIndex++;
				}
			}
			while(sameHash && fileIndex < filenames.size());
			
			// Save the result in a new file:
			String reduceFile = (sameHash ? currentHash : previousHash) + ".txt";
			try(FileWriter writer = new FileWriter(Utils.REDUCES_FOLDER + reduceFile)){
				writer.write(currentKey + " " + occurences + "\n");
			} catch (IOException e) {
				Utils.printToLogFile(e);
			}
		}
	}
	
	// Send back all the files from the reduces folder to the client:
	public static void collectReduces(ClientManager manager, String clientFolder) {
		ArrayList<String> filenames = Utils.listFiles(Utils.REDUCES_FOLDER, true);
		
		for(String filename : filenames) {
			manager.sendFile(filename, clientFolder + Utils.getBasename(filename));
		}
	}
}
