package de.desy.dCacheCloud;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.crypto.SecretKey;

import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import android.content.Context;

public class KeyStoreHelper {

	private static final String KEYSTOREFILEPATH = "mystore";
	private static ProtectionParameter pp = null;
	private static KeyStore ks = null;
	private static FileOutputStream fos;
	private static String password = "";
	private static boolean passwordSet = false;
//	private static Context context = null;
	
	public static void passwordWasCorrect() {
		passwordSet = true;
	}
	
	private static boolean keyStoreExists(Context c)
	{
		for (String s : c.fileList())
		{
			if (s.equals(KEYSTOREFILEPATH))
				return true;
		}
		return false;
	}
	
	
	public static KeyStore getKeyStore ()
	{
		return ks;
	}
	
	public static KeyStore load(Context c)
	{
		if (!passwordSet)
			return null;
		
		ks = null;
		try {
			ks = KeyStore.getInstance(KeyStore.getDefaultType());
			FileInputStream fis = c.openFileInput(KEYSTOREFILEPATH);
			ks.load (fis, password.toCharArray());
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return ks;
	}
		
	public static KeyStore init(Context c, String passwd)
	{
//		context = c;
		password = passwd;
		
		try {
			
			ks = KeyStore.getInstance(KeyStore.getDefaultType());
			pp = new PasswordProtection(password.toCharArray());
						
			if (!keyStoreExists(c))
			{
				ks.load(null,password.toCharArray());
				
				/* TODO GENERATE RSA-KEY-PAIR //
				KeyPair pair = CryptoHelper.generateAsymmetricKeyPair(1024);
				PrivateKey priv = pair.getPrivate();
				KeyStore.PrivateKeyEntry privEntry = new PrivateKeyEntry(priv, null);
				ks.setEntry("ownPrivate", privEntry, pp);
				*/
				fos = c.openFileOutput(KEYSTOREFILEPATH, Context.MODE_PRIVATE);
				ks.store(fos, password.toCharArray());
				return ks;
			}
			else
			{
				FileInputStream fis = c.openFileInput(KEYSTOREFILEPATH);
				ks.load(fis, password.toCharArray());
					
				return ks;
			}
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
//			closeStore();
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
//			FileInputStream fis = context.openFileInput(KEYSTOREFILEPATH);
//			ks.load(fis, password.toCharArray());
			
			KeyStore.SecretKeyEntry entry = (SecretKeyEntry) ks.getEntry(name, pp);
//			closeStore();
			
			return entry.getSecretKey();
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnrecoverableEntryException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
//			catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (CertificateException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		return null;
	}
	
	public static PrivateKey getOwnPriv()
	{
		if (ks == null)
			return null;
		
		KeyStore.PrivateKeyEntry privEntry;
		try {
//			FileInputStream fis = context.openFileInput(KEYSTOREFILEPATH);
//			ks.load(fis, password.toCharArray());
			
			privEntry = (PrivateKeyEntry) ks.getEntry("ownPrivate", pp);
			
			if (privEntry == null)
				return null;
			
			return privEntry.getPrivateKey();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnrecoverableEntryException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	
	public static PrivateKey getOwnPriv(Context c)
	{
		KeyStore.PrivateKeyEntry priv;
		try {
			priv = (PrivateKeyEntry) load(c).getEntry("ownPrivate", pp);
//			closeStore();
			return priv.getPrivateKey();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnrecoverableEntryException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (NullPointerException e)
		{
			return null;
		}
		return null;
	}
	*/
	
	/*
	public static PublicKey getOwnPub(Context c)
	{
		java.security.cert.Certificate cert;
		try {
			cert = load(c).getCertificate("ownCert");
//			closeStore();
			return cert.getPublicKey();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	*/
	
	public static PublicKey getOwnPub()
	{
		if (ks == null)
			return null;
		
		java.security.cert.Certificate cert;
		try {
//			FileInputStream fis = context.openFileInput(KEYSTOREFILEPATH);
//			ks.load(fis, password.toCharArray());
			
			cert = ks.getCertificate("ownCert");
//			closeStore();
			return cert.getPublicKey();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} 
//		catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		} catch (CertificateException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		return null;
	}
	
	public static boolean storeOwnAsymmetric(KeyPair pair)
	{
		X509Certificate cert = generateSelfSignedCertificate(pair);
		KeyStore.PrivateKeyEntry privEntry = new PrivateKeyEntry(pair.getPrivate(), new java.security.cert.Certificate[] {cert});
		
		try {
//			KeyStore ks = load(c);
			ks.setEntry("ownPrivate", privEntry, pp);
			ks.setCertificateEntry("ownCert", cert);
//			closeStore();
			return true;
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
		
		return false;
	}
		
	/*
	public static SecretKey getKey(Context c, String name)
	{
		try {
			KeyStore.SecretKeyEntry key = (SecretKeyEntry) load(c).getEntry(name, pp);
//			closeStore();
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
	*/
	
	/*
	public static void closeStore()
	{
		try {
			fos = context.openFileOutput(KEYSTOREFILEPATH, Context.MODE_PRIVATE);
			ks.store(fos, password.toCharArray());
			ks = null;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	*/
	
	public static void close(Context c)
	{
		if (!passwordSet)
			return;
		
		try {
			fos = c.openFileOutput(KEYSTOREFILEPATH, Context.MODE_PRIVATE);
			ks.store(fos, password.toCharArray());
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

    public static X509Certificate generateSelfSignedCertificate(KeyPair pair) {
    try {
    		final long MIN = 1000 * 60L;
    		final long HALFHOUR = MIN * 30L;
    		final long ONEHOUR = HALFHOUR * 2;
    		final long ONEDAY = ONEHOUR * 24L;
    		final long ONEYEAR = ONEDAY * 365L;
	    	
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

            // Generate self-signed certificate
            X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
            builder.addRDN(BCStyle.OU, "DESY");
            builder.addRDN(BCStyle.O, "HTW");
            builder.addRDN(BCStyle.CN, "own");

            Date notBefore = new Date(System.currentTimeMillis() - ONEDAY);
            Date notAfter = new Date(System.currentTimeMillis() + 10 * ONEYEAR);
            BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

            X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(builder.build(), serial, notBefore, notAfter, builder.build(), pair.getPublic());
            ContentSigner sigGen = new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider(org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME).build(pair.getPrivate());
            X509Certificate cert = new JcaX509CertificateConverter().setProvider(org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME).getCertificate(certGen.build(sigGen));
            
            cert.checkValidity(new Date());
            cert.verify(cert.getPublicKey());

            return cert;
            
            /*
            // Save to keystore
            KeyStore store = KeyStore.getInstance("JKS");
            if (keystore.exists()) {
                    FileInputStream fis = new FileInputStream(keystore);
                    store.load(fis, keystorePassword.toCharArray());
                    fis.close();
            } else {
                    store.load(null);
            }
            store.setKeyEntry(hostname, pair.getPrivate(), keystorePassword.toCharArray(),
                            new java.security.cert.Certificate[] { cert });
            FileOutputStream fos = new FileOutputStream(keystore);
            store.store(fos, keystorePassword.toCharArray());
            fos.close();
            */
    } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException("Failed to generate self-signed certificate!", t);
    }
}

}
