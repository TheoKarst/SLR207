package test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public class TestSplit {
	public static void main(String args[]) {
		if(args.length != 3) {
			System.err.println("usage: java TestSplit [splitsFolder] [inputFile] [nSplits]");
			System.exit(1);
		}
		
		createSplits(args[0], args[1], Integer.parseInt(args[2]));
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
}
