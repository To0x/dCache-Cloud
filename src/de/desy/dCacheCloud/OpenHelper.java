package de.desy.dCacheCloud;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class OpenHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "dCacheCloud";
	private static final int DATABASE_VERSION = 1;

	public OpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// quee
		db.execSQL("CREATE TABLE IF NOT EXISTS sync_queue (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uri STRING NOT NULL, not_before DATETIME, uploading BOOLEAN DEFAULT 0);");
		// user-keys: id, name, public-key
		db.execSQL("CREATE TABLE IF NOT EXISTS user_keys(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name STRING NOT NULL, public_key STRING NOT NULL, public_hash STRING NOT NULL, private_key STRING DEFAULT NULL);");
		// file-keys: id, name, file-hash, aes + salt
		db.execSQL("CREATE TABLE IF NOT EXISTS file_keys(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name STRING NOT NULL, hashBase64 STRING NOT NULL, aes STRING NOT NULL);");
		
		createPublicKey(db);
	}

	private void createPublicKey(SQLiteDatabase db)
	{
		KeyPairGenerator kgen;
		PrivateKey priv;
		PublicKey pub;
		db.beginTransaction();
		try {
			kgen = KeyPairGenerator.getInstance("RSA");
			kgen.initialize(2048);
			KeyPair kpair = kgen.generateKeyPair();
			priv = kpair.getPrivate();
			pub = kpair.getPublic();
			
			ContentValues values = new ContentValues();
			values.put("name", "myOwn");
			values.put("public_key", pub.toString());
			values.put("private_key", priv.toString());
			db.insertOrThrow("user_keys", null, values);
			
			db.setTransactionSuccessful();
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		finally
		{
			db.endTransaction();
//			db.close();
		}
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	/*
	 * Returns an ArrayList of Uris which are queued and not already being
	 * uploaded. Also marks all of them as being uploaded so that duplicate
	 * network change events don’t upload the same Uris a lot of times.
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

	public boolean setPersonPublicKey(String name, String public_key)
	{
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		try
		{
			ContentValues values = new ContentValues();
			values.put("name", name);
			values.put("public_key", public_key);
			db.insertOrThrow("user_keys", null, values);
			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}
		return false;
	}
	
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
	
	public boolean writeFileAESKey(String base64FileHash, String name, byte[] aesKey)
	{
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		try
		{
			System.out.println("queueUri");
			SQLiteDatabase database = getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put("name", name);
			values.put("hashBase64", base64FileHash);
			values.put("aes", aesKey);
			database.insertOrThrow("file_keys", null, values);
			db.setTransactionSuccessful();
			return true;
		}
		finally
		{
			db.endTransaction();
//			db.close();
		}
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
}
