package test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import net.CommunicationManager;

public class TestDecipher {

	public static void main(String[] args) {
		if(args.length != 3) {
			System.err.println("usage: java TestDecipher keyFile cipheredFile decipheredFile");
			System.exit(1);
		}
		
		try (FileInputStream keysInputStream = new FileInputStream(args[0])) {
			byte[] key_buf = new byte[16];
			byte[] iv_buf =  new byte[16];
			
			keysInputStream.read(key_buf);
			keysInputStream.read(iv_buf);
			
			SecretKeySpec key = new SecretKeySpec(key_buf, "AES");
			IvParameterSpec iv = new IvParameterSpec(iv_buf);
			
			try(CipherInputStream inputStream = getCipherInputStream(new FileInputStream(args[1]), key, iv);
			    FileOutputStream outputStream = new FileOutputStream(args[2])) {
				
				byte[] deciphered = inputStream.readAllBytes();
				outputStream.write(deciphered);
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static CipherInputStream getCipherInputStream(InputStream stream, SecretKeySpec key, IvParameterSpec iv) {
		try {
			Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
		    cipher.init(Cipher.DECRYPT_MODE, key, iv);
		    
		    return new CipherInputStream(stream, cipher);
		}
		catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		
		return null;
	}

}
