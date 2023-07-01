package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
	public final static String LOGIN = "karst-21";
	public final static String DOMAIN = "enst.fr";
	
	public final static String LOCAL_WD = System.getProperty("user.dir") + "/";
	public final static String REMOTE_WD = "/tmp/" + LOGIN + "/";
	
	private final static String LOG_FILE = "log.txt";
	public final static String KEYS_FILE = "keys.txt";
	public final static String COMPUTERS_FILE = "machines.txt";
	public final static String SLAVE_FILE = "slave.jar";
	
	public final static String SPLITS_FOLDER = "splits/";
	public final static String MAPS_FOLDER = "maps/";
	public final static String SHUFFLES_FOLDER = "shuffles/";
	public final static String SHUFFLES_RECV_FOLDER = "shufflesreceived/";
	public final static String REDUCES_FOLDER = "reduces/";
	
	
	public static String getFullName(String computer) {
		return LOGIN + "@" + computer + "." + DOMAIN;
	}
	
	public static String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static ArrayList<String> listFiles(String directory){
		ArrayList<String> filenames = new ArrayList<String>();

		File[] files = new File(directory).listFiles();

		for (File file : files)
		    if (file.isFile())
		        filenames.add(file.getName());
		
		return filenames;
	}
	
	public static ArrayList<String> loadLines(String filename){
		ArrayList<String> lines = new ArrayList<String>();
		
		try(BufferedReader reader = new BufferedReader(new FileReader(filename))){
			String line = null;
			while((line = reader.readLine()) != null)
				lines.add(line);
				
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return lines;
	}
	
	public static ArrayList<String> loadLines(String filename, int nLines){
		ArrayList<String> lines = new ArrayList<String>();
		
		try(BufferedReader reader = new BufferedReader(new FileReader(filename))){
			String line = null;
			while((line = reader.readLine()) != null && lines.size() < nLines)
				lines.add(line);
				
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(lines.size() < nLines) {
			System.err.println("Not enough lines in the file: " + filename + " (required: " + nLines + ")");
			System.exit(1);
		}
		
		return lines;
	}
	
	// From a split filename, return  the corresponding map filename (ex: S0.txt -> UM0.txt):
	public static String splitNameToMapName(String splitName) {
		Pattern pattern = Pattern.compile("S([0-9]+)\\.txt");
		Matcher matcher = pattern.matcher(splitName);
		
		if(!matcher.matches()) {
			System.err.println("Unexpected filename: should be like S[number].txt");
			System.exit(1);
		}
		
		return "UM" + matcher.group(1) + ".txt";
	}
	
	public static String getFilename(String fullpath) {
		return fullpath.substring(fullpath.lastIndexOf(File.separator) + 1);
	}
	
	public static void printToLogfile(String data) {
		try {
			FileWriter writer = new FileWriter(LOG_FILE, true);
			writer.write(data);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void printToLogFile(Exception e) {
		try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
			e.printStackTrace(writer);
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
		
	}
}
