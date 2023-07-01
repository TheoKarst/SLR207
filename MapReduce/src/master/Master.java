package master;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import util.Utils;

public class Master {

	public static void main(String[] args) {
		
//		ProcessBuilder pb = new ProcessBuilder("/tmp/test_script.sh");
//		
//		try {
//			Process process = pb.start();
//			
//			StreamReader outStreamReader = new StreamReader(process.getInputStream(), 5100);
//			StreamReader errStreamReader = new StreamReader(process.getErrorStream(), 5100);
//			
//			outStreamReader.start();
//			errStreamReader.start();
//			
//			String output = outStreamReader.poll();
//			System.out.println("Output:\n" + output);
//			
//			System.out.println();
//			
//			String error = errStreamReader.poll();
//			System.out.println("Error:\n" + error);
//		
//			process.destroy();
//			
//		} catch (IOException | InterruptedException e) {
//			e.printStackTrace();
//		}
		
		if(args.length != 1) {
			System.err.println("usage: java Master [list_computer]");
			System.exit(1);
		}
		
		// List the names of the split files:
		ArrayList<String> filenames = Utils.listFiles(Utils.SPLITS_FOLDER);
				
		// List enough computers to perform our splits, among the computers listed in the given filename:
		ArrayList<String> splitComputers = Utils.loadLines(args[0], filenames.size());
		ArrayList<String> allComputers = Utils.loadLines(args[0]);
		
		
		
		// Split the files to the remote computers:
		splitFiles(filenames, splitComputers);
		
		// Run the map and shuffle procedures on the remote computers:
		try {
			mapProcedure(filenames, splitComputers);			
			sendComputerList(splitComputers, args[0]);
			shuffleProcedure(filenames, splitComputers);
			reduceProcedure(allComputers);
		}
		catch (InterruptedException | IOException e) {
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
			final String filename = Utils.SPLITS_FOLDER + filenames.get(i);
			
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						// Create a folder on the remote computer to store the splits files:
						System.out.println("ssh " + host + " mkdir -p " + Utils.SPLITS_FOLDER);
						ProcessBuilder pb = new ProcessBuilder("ssh", host, "mkdir", "-p", Utils.SPLITS_FOLDER);
						
						pb.redirectError(); pb.inheritIO();
						Process process = pb.start();
						
						// Wait for the folder to be created:
						process.waitFor();
						
						// Send the split file to the remote computer:
						System.out.println("scp " + filename + " " + host + ":" + Utils.SPLITS_FOLDER);
						pb = new ProcessBuilder("scp", filename, host + ":" + Utils.SPLITS_FOLDER);
						pb.redirectError(); pb.inheritIO();
						process = pb.start();
						
						// Wait for the copy to complete:
						process.waitFor();
					}
					catch(IOException | InterruptedException e) {
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
	
	// Asserting that computers.get(i) has the split file filenames.get(i), we run the mapping procedure on each computer
	// with the corresponding file, using the slave.jar:
	public static void mapProcedure(ArrayList<String> filenames, ArrayList<String> computers) throws InterruptedException, IOException {
		assert filenames.size() == computers.size();
		
		Process processes[] = new Process[filenames.size()];
		for(int i = 0; i < filenames.size(); i++) {
			String host = Utils.getFullName(computers.get(i));
			String splitFile = Utils.SPLITS_FOLDER + filenames.get(i);
			
			System.out.println("ssh " + host + " java -jar " + Utils.SLAVE_FILE + " 0 " + splitFile);
			ProcessBuilder pb = new ProcessBuilder("ssh", host, "java", "-jar", Utils.SLAVE_FILE, "0", splitFile);
			
			pb.redirectError(); pb.inheritIO();
			processes[i] = pb.start();
		}
		
		// Wait for all processes to complete:
		for(Process process : processes)
			process.waitFor();
		
		System.out.println("MAP FINISHED\n");
	}
	
	// Send the given file to all the computers listed, and save it as "machines.txt":
	public static void sendComputerList(ArrayList<String> computers, String computersFile) throws InterruptedException, IOException {

		Process processes[] = new Process[computers.size()];
		for(int i = 0; i < computers.size(); i++) {
			String host = Utils.getFullName(computers.get(i));
			
			System.out.println("scp " + computersFile + " " + host + ":" + Utils.COMPUTERS_FILE);
			ProcessBuilder pb = new ProcessBuilder("scp", computersFile, host + ":" + Utils.COMPUTERS_FILE);
			
			pb.redirectError(); pb.inheritIO();
			processes[i] = pb.start();
		}
		
		// Wait for all processes to complete:
		for(Process process : processes)
			process.waitFor();
	}
	
	// Asserting that computers.get(i) has the split file filenames.get(i), we run the shuffle procedure on each computer
	// with the corresponding file, using the slave.jar:
	public static void shuffleProcedure(ArrayList<String> filenames, ArrayList<String> computers) throws InterruptedException, IOException {
		
		Process processes[] = new Process[computers.size()];
		for(int i = 0; i < computers.size(); i++) {
			String host = Utils.getFullName(computers.get(i));
			String mapFile = Utils.MAPS_FOLDER + Utils.splitNameToMapName(filenames.get(i));
			
			System.out.println("ssh " + host + " java -jar " + Utils.SLAVE_FILE + " 1 " + mapFile);
			ProcessBuilder pb = new ProcessBuilder("ssh", host, "java", "-jar", Utils.SLAVE_FILE, "1", mapFile);
			
			pb.redirectError(); pb.inheritIO();
			processes[i] = pb.start();	
		}
		
		// Wait for all processes to complete:
		for(Process process : processes)
			process.waitFor();
		
		System.out.println("SHUFFLE FINISHED\n");
	}
	
	public static void reduceProcedure(ArrayList<String> computers) throws InterruptedException, IOException {
		
		Process processes[] = new Process[computers.size()];
		for(int i = 0; i < computers.size(); i++) {
			String host = Utils.getFullName(computers.get(i));
			
			System.out.println("ssh " + host + " java -jar " + Utils.SLAVE_FILE + " 2");
			ProcessBuilder pb = new ProcessBuilder("ssh", host, "java", "-jar", Utils.SLAVE_FILE, "2");
			processes[i] = pb.start();
		}
		
		// Wait for all processes to complete:
		for(Process process : processes)
			process.waitFor();
		
		System.out.println("REDUCE FINISHED\n");
	}
}
