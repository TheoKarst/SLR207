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
	
	public final static int PORT = 52301;
	
	private final static String LOG_FILE = "/tmp/" + LOGIN + "/log.txt";
	public final static String KEYS_FILE = "/tmp/" + LOGIN + "/keys";
	public final static String COMPUTERS_FILE = "/tmp/" + LOGIN + "/machines.txt";
	public final static String SLAVE_FILE = "/tmp/" + LOGIN + "/slave.jar";
	
	public final static String SPLITS_FOLDER = "/tmp/" + LOGIN + "/splits/";
	public final static String MAPS_FOLDER = "/tmp/" + LOGIN + "/maps/";
	public final static String SHUFFLES_FOLDER = "/tmp/" + LOGIN + "/shuffles/";
	public final static String SHUFFLES_RECV_FOLDER = "/tmp/" + LOGIN + "/shufflesreceived/";
	public final static String REDUCES_FOLDER = "/tmp/" + LOGIN + "/reduces/";
	
	
	public static String getFullName(String computer) {
		return computer + "." + DOMAIN;
	}
	
	public static String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static ArrayList<String> listFiles(String folder, boolean fullname){
		ArrayList<String> filenames = new ArrayList<String>();

		File[] files = new File(folder).listFiles();

		for (File file : files)
		    if (file.isFile())
		        filenames.add(fullname ? folder + file.getName() : file.getName());
		
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
	public static String splitNameToMapName(String fullpath) {
		String filename = getBasename(fullpath);
		Pattern pattern = Pattern.compile("S([0-9]+)\\.txt");
		Matcher matcher = pattern.matcher(filename);
		
		if(!matcher.matches()) {
			System.err.println("Unexpected filename: should be like S[number].txt");
			System.exit(1);
		}
		
		return MAPS_FOLDER + "UM" + matcher.group(1) + ".txt";
	}
	
	// Return the name of the file, without the full path:
	public static String getBasename(String fullpath) {
		return fullpath.substring(fullpath.lastIndexOf(File.separator) + 1);
	}
	
	public static void printToLogFile(String data) {
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
