package de.desy.dCacheCloud;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.SecretKey;

import android.content.Context;
import android.content.SharedPreferences;

public class KeyStoreHelper {

	private static final String KEYSTOREFILEPATH = "mystore";
	private static ProtectionParameter pp = null;
	private static KeyStore ks = null;
	private static FileOutputStream fos;
	private static String password = "";
	
	private static boolean keyStoreExists(Context c)
	{
		for (String s : c.fileList())
		{
			if (s.equals(KEYSTOREFILEPATH))
				return true;
		}
		return false;
	}
	
	public static KeyStore getKeyStore(Context c)
	{
		if (ks != null)
			return ks;
		
		try {
			
			ks = KeyStore.getInstance(KeyStore.getDefaultType());
			SharedPreferences preferences = c.getSharedPreferences("de.desy.dCacheCloud_preferences", Context.MODE_PRIVATE);
			password = preferences.getString("webdav_password", null);
			pp = new PasswordProtection(password.toCharArray());
						
			if (!keyStoreExists(c))
			{
				ks.load(null,password.toCharArray());
				
				// TODO GENERATE RSA-KEY-PAIR //
//				KeyPair pair = CryptoHelper.generateAsymmetricKeyPair(1024);
				fos = c.openFileOutput(KEYSTOREFILEPATH, Context.MODE_PRIVATE);
				ks.store(fos, password.toCharArray());
				return ks;
			}
			
			FileInputStream fis = c.openFileInput(KEYSTOREFILEPATH);
			ks.load(fis, password.toCharArray());
				
			return ks;
			//SecretKeyEntry ksentry = new SecretKeyEntry(key);
			//PasswordProtection pp = new PasswordProtection(password.toCharArray());
			
			//SecretKeyEntry ksentry2 = (SecretKeyEntry) ks.getEntry(fileHash, pp);
			
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static boolean storeKey(String name, SecretKey key) {
		if (ks == null)
			return false;

		try {
			KeyStore.Entry entry = new SecretKeyEntry(key);
			ks.setEntry(name, entry, pp);
			return true;
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}

		return false;
	}
	
	public static SecretKey getKey(String name)
	{
		if (ks == null)
			return null;
		
		try {
			KeyStore.SecretKeyEntry entry = (SecretKeyEntry) ks.getEntry(name, pp);
			return entry.getSecretKey();
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnrecoverableEntryException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static SecretKey getKey(Context c, String name)
	{
		try {
			KeyStore.SecretKeyEntry key = (SecretKeyEntry) getKeyStore(c).getEntry(name, pp);
			return key.getSecretKey();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnrecoverableEntryException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void closeStore(Context c)
	{
		try {
			fos = c.openFileOutput(KEYSTOREFILEPATH, Context.MODE_PRIVATE);
			ks.store(fos, password.toCharArray());
			password = "";
			pp = null;
			ks = null;
			fos = null;
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		}
	}

}
