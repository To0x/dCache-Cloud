package de.desy.dCacheCloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import android.net.Uri;
import android.os.Environment;

public final class CryptoHelper {

	// DEFINES
	private static final String HASH_ALGORITHM =  			"SHA-256";
	private static final String SYMMETRIC_ALGORITHM = 		"AES";
	private static final String ASYMMETRIC_ALGORITHM = 		"RSA";
	private static final String SECURERANDOM_ALGORITHM = 	"SHA1PRNG";
	private static final String SYMMETRIC_MODE = 			"CBC";
	private static final String SYMMETRIC_PADDING = 		"PKCS7Padding";
		
	public static String hash(String value)
	{

		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(HASH_ALGORITHM);
			byte[] data = digest.digest(value.getBytes("UTF-8"));
			String hash = String.format("%0" + (data.length*2) + "X", new BigInteger(1, data));
			return hash;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static SecretKey generateBlockCipherKey(int keySize)
	{
		KeyGenerator kgen;
		try {
			kgen = KeyGenerator.getInstance(SYMMETRIC_ALGORITHM);
			kgen.init(keySize);
			return kgen.generateKey();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static KeyPair generateAsymmetricKeyPair(int keySize)
	{
		KeyPairGenerator kgen;
		try {
			kgen = KeyPairGenerator.getInstance(ASYMMETRIC_ALGORITHM);
			kgen.initialize(keySize);
			return kgen.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Deprecated
	public static byte[] encryptAsymmetric(String message, boolean encryptWithPrivateKey, PrivateKey privkey, PublicKey pubKey) {
		
		/*
		 * TODO: implement!
		
		
		PrivateKey priv = null;
		PublicKey pub = null;
		if (!encryptWithPrivateKey)
		{
			pub = (PublicKey) key;
		}
		else
		{
			priv = (PrivateKey) key;
		}
		*/
		try {

			
			Cipher cip = Cipher.getInstance("RSA/None/PKCS1Padding");
			Cipher cip2 = Cipher.getInstance("RSA/None/PKCS1Padding");
			
			/*
			 * should be given!
			 */
			//if (encryptWithPrivateKey)
			//{
				cip.init(Cipher.ENCRYPT_MODE, pubKey);
				cip2.init(Cipher.DECRYPT_MODE, privkey);
			//}
			//else
			//{
				//cip.init(Cipher.ENCRYPT_MODE, pub);
				//cip2.init(Cipher.DECRYPT_MODE, priv);
			//}
			
			byte[] input = message.getBytes("UTF-8");
			
			byte[] encrypted = cip.doFinal(input);
			byte[] decrypted = cip2.doFinal(encrypted);
			
			return decrypted;
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		
		
		return null;
	}
	
	private static boolean encryptBlockCipher(Uri fileName, SecretKey key, boolean withIV) {
		FileInputStream fis;
		FileOutputStream fos;
		CipherOutputStream cos;
		
		File sdCard = Environment.getExternalStorageDirectory();
		File fileInput = new File(fileName.getPath());
		File fileOutputDirectory = new File(sdCard, "dCacheCloud/.enc");
		fileOutputDirectory.mkdirs();
		
		File fileOutput = new File(sdCard, String.format("dCacheCloud/.enc/%s", fileName.getLastPathSegment()));

		// generate Cipher
		try {
			Cipher cipEncrypt = Cipher.getInstance(String.format("%s/%s/%s", SYMMETRIC_ALGORITHM,SYMMETRIC_MODE,SYMMETRIC_PADDING));		
			final byte[] ivData = new byte[cipEncrypt.getBlockSize()];
			SecureRandom rnd = SecureRandom.getInstance(SECURERANDOM_ALGORITHM);
			rnd.nextBytes(ivData);
			final IvParameterSpec iv = new IvParameterSpec(ivData);
			
			fis = new FileInputStream(fileInput);
			fos = new FileOutputStream(fileOutput);
			
			fos.write(iv.getIV(), 0, iv.getIV().length);
			
			if (withIV)
				cipEncrypt.init(Cipher.ENCRYPT_MODE, key, iv);
			else
				cipEncrypt.init(Cipher.ENCRYPT_MODE, key);
			
			if (!fileInput.exists())
				fileInput.createNewFile();
			
			if (!fileOutput.exists())
				fileOutput.createNewFile();
			

			cos = new CipherOutputStream(fos, cipEncrypt);
					
			/// ENCRYPT ///
			int read;
			byte[] buffer = new byte[cipEncrypt.getBlockSize()];
			
			// put iv at the start!
			while ((read = fis.read(buffer)) != -1) 
			{
				cos.write(buffer, 0 , read);
			}
			
			cos.flush();
			cos.close();
			fos.close();
			fis.close();
			
			return true;
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private static boolean decryptBlockCipher(Uri fileName, SecretKey key, boolean withIV)
	{
		CipherInputStream cis;
		FileInputStream fis;
		FileOutputStream fos;

		// generate cipher
		try {
			File sdCard = Environment.getExternalStorageDirectory();
			Cipher cipDecrypt = Cipher.getInstance(String.format("%s/%s/%s", SYMMETRIC_ALGORITHM,SYMMETRIC_MODE,SYMMETRIC_PADDING));
			byte[] ivData = new byte[cipDecrypt.getBlockSize()];
			
			File fileInput = new File(sdCard, String.format("dCacheCloud/.enc/%s", fileName));
			
			File fileOutputDirectory = new File(sdCard, "dCacheCloud/");
			fileOutputDirectory.mkdirs();
			
			File fileOutput = new File(sdCard, String.format("dCacheCloud/%s",fileName));

			fis = new FileInputStream(fileInput);
			fos = new FileOutputStream(fileOutput);

			
			fis.read(ivData, 0, ivData.length);
			IvParameterSpec iv = new IvParameterSpec(ivData);
			
			if (withIV)
				cipDecrypt.init(Cipher.DECRYPT_MODE, key, iv);
			else
				cipDecrypt.init(Cipher.DECRYPT_MODE, key);
			

			cis = new CipherInputStream(fis, cipDecrypt);
			
			/// DECRYPT ///
			int read;
			byte[] buffer = new byte[cipDecrypt.getBlockSize()];
			while ((read = cis.read(buffer)) != -1)
			{
				fos.write(buffer,0,read);
			}
			
			fos.flush();
			fos.close();
			cis.close();
			fis.close();
			
			return true;
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean decryptBlockCipherWithIV(Uri fileName, SecretKey key){
		return decryptBlockCipher(fileName, key, true);
	}
	
	public static boolean decryptBlockCipherWihtoutIV(Uri fileName, SecretKey key){
		return decryptBlockCipher(fileName, key, false);
	}
	
	public static boolean encryptBlockCipherWithIV(Uri fileName, SecretKey key)
	{
		return encryptBlockCipher(fileName, key, true);
	}
	
	public static void encryptBlockCipherWihtoutIV(Uri fileName, SecretKey key){
		encryptBlockCipher(fileName, key, false);
	}
}
