package de.desy.dCacheCloud;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Base64;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "dCacheCloud";
	private static final int DATABASE_VERSION = 1;

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// quee
		db.execSQL("CREATE TABLE IF NOT EXISTS sync_queue (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uri STRING NOT NULL, not_before DATETIME, uploading BOOLEAN DEFAULT 0);");
		// user-keys: id, name, public-key
		db.execSQL("CREATE TABLE IF NOT EXISTS user_keys(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name STRING NOT NULL, public_key STRING NOT NULL, public_hash STRING NOT NULL);");
		// file-keys: id, name, file-hash
//		db.execSQL("CREATE TABLE IF NOT EXISTS file_keys(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name STRING NOT NULL, hash STRING NOT NULL;");
		
		//createPublicKey(db);
	}

/*
	private void createPublicKey(SQLiteDatabase db)
	{
		KeyPair pair;
		db.beginTransaction();
		try {
			pair = CryptoHelper.generateAsymmetricKeyPair(2048);
	
			// write to DB!
			ContentValues values = new ContentValues();
			values.put("name", "myOwn");
			values.put("public_key", pair.getPublic().toString());
			values.put("public_hash", CryptoHelper.hash(pair.getPublic().toString()));
			// TODO: Store private Key in KeyStore?!?
//			values.put("private_key", pair.getPrivate().toString());
			db.insertOrThrow("user_keys", null, values);
			
			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
//			db.close();
		}
	}
	*/
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	/*
	 * Returns an ArrayList of Uris which are queued and not already being
	 * uploaded. Also marks all of them as being uploaded so that duplicate
	 * network change events donâ€™t upload the same Uris a lot of times.
	 */
	public ArrayList<String> getQueuedUris() {
		System.out.println("getQueuedUris");
		ArrayList<String> result = new ArrayList<String>();
		SQLiteDatabase database = getWritableDatabase();
		database.beginTransaction();
		try {
			Cursor cursor = database.rawQuery("SELECT uri FROM sync_queue WHERE NOT uploading", null);
			if (cursor.moveToFirst()) {
				do {
					result.add(cursor.getString(0));
				} while (cursor.moveToNext());
			}

			if (cursor != null && !cursor.isClosed())
				cursor.close();

			database.execSQL("UPDATE sync_queue SET uploading = 1");

			database.setTransactionSuccessful();
		} finally {
			database.endTransaction();
		}
//		database.close();
		return result;
	}

	/*
	public String getOwnPublicKey()
	{
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		try
		{
			// own key is always id = 1 - first dataset created in the database - directly after creating the tables!
			Cursor cur = db.rawQuery("SELECT public_key FROM user_keys where id = ?", new String[] {"1"});
			if (cur.moveToFirst())
			{
				db.setTransactionSuccessful();
				return cur.getString(0);
			}
		}
		finally {
			db.endTransaction();
//			db.close();
		}
		return null;
	}
	*/

	public String getOwnHashKey()
	{
		SQLiteDatabase db = getReadableDatabase();
		db.beginTransaction();
		try
		{
			Cursor cur = db.rawQuery("SELECT public_hash FROM user_keys where id = ?", new String[] {"1"});
			if (cur.moveToFirst())
			{
				db.setTransactionSuccessful();
				return cur.getString(0);
			}
		}
		finally
		{
			db.endTransaction();
		}
		return "";
	}
	
	public boolean setPersonPublicKey(String name, String public_key, String public_hash)
	{
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		try
		{
			ContentValues values = new ContentValues();
			values.put("name", name);
			values.put("public_key", public_key);
			values.put("public_hash", public_hash);
			db.insertOrThrow("user_keys", null, values);
			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}
		return false;
	}
	
	/*
	public String getFileAESKey(String name)
	{
		//TODO: Check Hash!
		SQLiteDatabase db = getReadableDatabase();
		db.beginTransaction();
		try
		{
			Cursor cur = db.rawQuery("SELECT aes FROM file_keys WHERE name = ?", new String[] {name});
			if (cur.moveToFirst())
			{
				db.setTransactionSuccessful();
				return cur.getString(0);
			}
		}
		finally
		{
			db.endTransaction();
//			db.close();
		}
		return null;
	}
	
	public boolean writeFileAESKey(String base64FileHash, String name, String aesKey, String iv)
	{
		/// TODO Prüfen, wie man Daten richtig in eine Datenbank speicher - mit Salt und so?!?
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		try
		{
			SQLiteDatabase database = getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put("name", name);
			values.put("hashBase64", base64FileHash);
			values.put("aes", String.format("%s;%s", aesKey, iv));
			database.insertOrThrow("file_keys", null, values);
			db.setTransactionSuccessful();
			return true;
		}
		finally
		{
			db.endTransaction();
		}
	}
	*/
	public boolean isAlreadyAFriend(String public_key)
	{
		if (getFriendHashKey(public_key) != null)
			return true;
		
		return false;
	}
	
	public String getFriendHashKey(String public_key)
	{
		SQLiteDatabase db = getReadableDatabase();
		db.beginTransaction();
		try
		{
			Cursor cur = db.rawQuery("SELECT public_hash FROM user_keys WHERE name = ?", new String[] {public_key});
			if (cur.moveToFirst())
			{
				db.setTransactionSuccessful();
				return cur.getString(0);
			}
		}
		finally
		{
			db.endTransaction();
//			db.close();
		}
		return null;
	}
	
	public String getFriendName(String public_key)
	{
		SQLiteDatabase db = getReadableDatabase();
		db.beginTransaction();
		try
		{
			Cursor cur = db.rawQuery("SELECT name FROM user_keys WHERE public_key = ?", new String[] {public_key});
			if (cur.moveToFirst())
			{
				db.setTransactionSuccessful();
				return cur.getString(0);
			}
		}
		finally
		{
			db.endTransaction();
//			db.close();
		}
		return null;
	}
	
	public List<String> getAllFriends()
	{
		List<String> friends = new ArrayList<String>();
		SQLiteDatabase db = getReadableDatabase();
		db.beginTransaction();
		try
		{
			Cursor cur = db.rawQuery("SELECT name FROM user_keys ORDER BY id", null);
			if (cur.moveToFirst())
			{
				friends.add(cur.getString(0));
				while (cur.moveToNext())
				{
					friends.add(cur.getString(0));
				}
			}
			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}
		return friends;
	}
	
	public String getPersonPublicKey(String name)
	{
		SQLiteDatabase db = getReadableDatabase();
		db.beginTransaction();
		try
		{
			Cursor cur = db.rawQuery("SELECT public_key FROM user_keys WHERE name = ?", new String[] {name});
			if (cur.moveToFirst())
			{
				db.setTransactionSuccessful();
				return cur.getString(0);
			}
		}
		finally
		{
			db.endTransaction();
//			db.close();
		}
		return null;
	}
	
	public void queueUri(Uri uri) {
		System.out.println("queueUri");
		SQLiteDatabase database = getWritableDatabase();
		database.beginTransaction();
		ContentValues values = new ContentValues();
		values.put("uri", uri.toString());
		database.insertOrThrow("sync_queue", null, values);
		database.setTransactionSuccessful();
		database.endTransaction();
//		database.close();
	}

	public void removeUriFromQueue(String uri) {
		System.out.println("removeUriFromQueue");
		SQLiteDatabase database = getWritableDatabase();
		database.beginTransaction();
		database.delete("sync_queue", "uri = ?", new String[] { uri });
		database.setTransactionSuccessful();
		database.endTransaction();
//		database.close();
	}
	
	public void storeOwnPublic(PublicKey pub)
	{
		setPersonPublicKey("ownPublic", new String(Base64.encode(pub.getEncoded(), Base64.DEFAULT)) , CryptoHelper.hash(new String(Base64.encode(pub.getEncoded(), Base64.DEFAULT))));
	}
	
	public PublicKey getOwnPublic()
	{
	       String pubKeyStr = getPersonPublicKey("ownPublic");       
	        byte[] sigBytes = Base64.decode(pubKeyStr, Base64.DEFAULT);
	        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(sigBytes);
	        KeyFactory keyFact = null;
	        try {
	            keyFact = KeyFactory.getInstance("RSA", "BC");
	        } catch (NoSuchAlgorithmException e) {
	            e.printStackTrace();
	        } catch (NoSuchProviderException e) {
	            e.printStackTrace();
	        }
	        try {
	            return  keyFact.generatePublic(x509KeySpec);
	        } catch (InvalidKeySpecException e) {
	            e.printStackTrace();
	        }
	        return null;
	}
}
