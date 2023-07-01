package net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CommunicationManager {
	public final static int EOF_MESSAGE = 0;
	public final static int ACK_MESSAGE = 1;
	
	public final static int SEND_ERR_MESSAGE = 2;
	public final static int SEND_OUT_MESSAGE = 3;	
	
	public final static int SEND_FILE_COMMAND = 10;		// Send a file to the server
	public final static int SEND_BASH_COMMAND = 11;		// Send a bash command to the server
	
	public final static int SEND_MAP_REDUCE_COMMAND = 12;	// Send a command related to MapReduce algorithm (split, map, reduce, etc...)
	
	private Socket socket;
	protected DataInputStream inputStream;
	protected DataOutputStream outputStream;
	
	// Secret key and IV to cipher the communication:
	private static SecretKey secretKey = null;
	private static IvParameterSpec iv = null;
	
	public CommunicationManager(Socket socket) {
		this.socket = socket;
		try {
			this.inputStream = new DataInputStream(getCipherInputStream(socket.getInputStream()));
			this.outputStream = new DataOutputStream(getCipherOutputStream(socket.getOutputStream()));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public CommunicationManager(String serverAddr, int serverPort) {
		try {
			this.socket = new Socket(serverAddr, serverPort);
			this.inputStream = new DataInputStream(getCipherInputStream(socket.getInputStream()));
			this.outputStream = new DataOutputStream(getCipherOutputStream(socket.getOutputStream()));
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private CipherInputStream getCipherInputStream(InputStream stream) {
		try {
			Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
		    cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
		    
		    return new CipherInputStream(stream, cipher);
		}
		catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private CipherOutputStream getCipherOutputStream(OutputStream stream) {
		try {
			Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
		    cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
		    
		    return new CipherOutputStream(stream, cipher);
		}
		catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public final static void loadCipherKeys(String filename) {
		try (FileInputStream inputStream = new FileInputStream(filename)) {
			byte[] key = new byte[16];
			byte[] iv =  new byte[16];
			
			inputStream.read(key);
			inputStream.read(iv);
			
			CommunicationManager.secretKey = new SecretKeySpec(key, "AES");
			CommunicationManager.iv = new IvParameterSpec(iv);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
}
