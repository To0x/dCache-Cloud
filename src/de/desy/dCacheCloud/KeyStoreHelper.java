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
import android.content.SharedPreferences;

public class KeyStoreHelper {

	private static final String KEYSTOREFILEPATH = "mystore";
	private static ProtectionParameter pp = null;
	private static KeyStore ks = null;
	private static FileOutputStream fos;
	private static String password = "";
	private static Context context = null;
	
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
		context = c;
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
			closeStore();
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
	
	public static PrivateKey getOwnPriv()
	{
		if (ks == null)
			return null;
		
		KeyStore.PrivateKeyEntry privEntry;
		try {
			privEntry = (PrivateKeyEntry) ks.getEntry("ownPrivate", pp);
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
	
	public static PrivateKey getOwnPriv(Context c)
	{
		KeyStore.PrivateKeyEntry priv;
		try {
			priv = (PrivateKeyEntry) getKeyStore(c).getEntry("ownPrivate", pp);
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
	
	public static PublicKey getOwnPub(Context c)
	{
		java.security.cert.Certificate cert;
		try {
			cert = getKeyStore(c).getCertificate("ownCert");
			return cert.getPublicKey();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static PublicKey getOwnPub()
	{
		if (ks == null)
			return null;
		
		java.security.cert.Certificate cert;
		try {
			cert = ks.getCertificate("ownCert");
			return cert.getPublicKey();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static boolean storeOwnAsymmetric(Context c, KeyPair pair)
	{
		X509Certificate cert = generateSelfSignedCertificate(pair);
		KeyStore.PrivateKeyEntry privEntry = new PrivateKeyEntry(pair.getPrivate(), new java.security.cert.Certificate[] {cert});
		
		try {
			KeyStore ks = getKeyStore(c);
			ks.setEntry("ownPrivate", privEntry, pp);
			ks.setCertificateEntry("ownCert", cert);
			closeStore();
			return true;
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
		
		return false;
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
	
	public static void closeStore()
	{
		try {
			fos = context.openFileOutput(KEYSTOREFILEPATH, Context.MODE_PRIVATE);
			ks.store(fos, password.toCharArray());
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
	
	public static void closeStore(Context c)
	{
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
