package master;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamReader extends Thread {
		
		private BufferedReader reader;
		private StringBuilder stringBuilder;
		private long stopTime;
		private long timeout;
		
		
		public StreamReader(InputStream inputStream, long timeout) {
			this.reader = new BufferedReader(new InputStreamReader(inputStream));
			this.stringBuilder = new StringBuilder();
			this.timeout = timeout;
		}
		
		@Override
		public void run() {
			String line = null;
			try {
				while((line = reader.readLine()) != null && System.currentTimeMillis() < stopTime)
					stringBuilder.append(line + "\n");
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void start() {
			this.stopTime = System.currentTimeMillis() + timeout;
			super.start();
		}
		
		public String poll() throws InterruptedException {
			this.join();
			
			return this.stringBuilder.toString();
		}
	}