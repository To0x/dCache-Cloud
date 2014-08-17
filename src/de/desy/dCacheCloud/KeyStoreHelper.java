package de.desy.dCacheCloud;

import java.io.File;
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
	private static ProtectionParameter pp;
	
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
		try {
			
			KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			SharedPreferences preferences = c.getSharedPreferences("de.desy.dCacheCloud_preferences", Context.MODE_PRIVATE);
			String password = preferences.getString("webdav_password", null);
			pp = new PasswordProtection(password.toCharArray());
			
			if (!keyStoreExists(c))
			{
				ks.load(null,password.toCharArray());
				
				// TODO GENERATE RSA-KEY-PAIR //
				FileOutputStream fos = c.openFileOutput(KEYSTOREFILEPATH, Context.MODE_PRIVATE);
				ks.store(fos, password.toCharArray());
				fos.close();
			}
			
			FileInputStream fis = c.openFileInput(KEYSTOREFILEPATH);
			ks.load(fis,password.toCharArray());
				
			return ks;
			//SecretKeyEntry ksentry = new SecretKeyEntry(key);
			//PasswordProtection pp = new PasswordProtection(password.toCharArray());
			
			//SecretKeyEntry ksentry2 = (SecretKeyEntry) ks.getEntry(fileHash, pp);
			
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static boolean storeKey(KeyStore store, String name, SecretKey key)
	{
		try {
			KeyStore.Entry entry = new SecretKeyEntry(key);
			store.setEntry(name, entry, pp);
			return true;
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static SecretKey getKey(KeyStore store, String name)
	{
		try {
			KeyStore.SecretKeyEntry entry = (SecretKeyEntry) store.getEntry(name, pp);
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

}
