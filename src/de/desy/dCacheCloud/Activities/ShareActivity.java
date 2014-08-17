package de.desy.dCacheCloud.Activities;

import java.util.ArrayList;

import javax.crypto.SecretKey;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import de.desy.dCacheCloud.CryptoHelper;
import de.desy.dCacheCloud.KeyStoreHelper;
import de.desy.dCacheCloud.R;
import de.desy.dCacheCloud.UploadService;

public class ShareActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_share);
		finish();

		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();

		if (type == null)
			return;

		if (!intent.hasExtra(Intent.EXTRA_STREAM))
			return;

		if (Intent.ACTION_SEND.equals(action)) {
			Uri fileUri = getFilePath((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
			encryptFile(fileUri);
			uploadFile(fileUri);
			deleteLocalFile(fileUri);
		} else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
			ArrayList<Parcelable> list = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			for (Parcelable p : list) {
				Uri fileUri = getFilePath((Uri) p);
				encryptFile(fileUri);
				uploadFile(fileUri);
				deleteLocalFile(fileUri);
			}
		}
	}

	private void encryptFile(Uri file)
	{
		SecretKey key = CryptoHelper.generateBlockCipherKey(256);
		String fileHash = CryptoHelper.hash(file.getLastPathSegment());
		
		if (CryptoHelper.encryptBlockCipherWithIV(file, key))
		{
				KeyStoreHelper.storeKey(KeyStoreHelper.getKeyStore(this), fileHash, key);
		}
	}
	
	private void uploadFile(Uri uri) {
		Log.d("davsync", "Sharing " + uri.toString());

		Intent ulIntent = new Intent(this, UploadService.class);
		ulIntent.putExtra(Intent.EXTRA_STREAM, uri);
		startService(ulIntent);
	}
	
	private void deleteLocalFile(Uri uri)
	{
		//TODO:
		// check if upload is completed!
	}
	
	private Uri getFilePath(Uri uriToFile) {
		
		ContentResolver cr = getContentResolver();
		String filename = null;
		String scheme = uriToFile.getScheme();
		Uri uri = uriToFile;
		
		if (scheme.equals("content")) {

		    String[] proj = { MediaStore.Images.Media.DATA };
		    Cursor cursor = cr.query(uriToFile, proj, null, null, null);
		    if (cursor != null && cursor.getCount() != 0) {
		    	int columnIndex = -1;
		    	try {
		    		columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		    	} catch (IllegalArgumentException e) {
		    		Log.d("dCache", e.toString());
		    	}
		    	
		        cursor.moveToFirst();
		        filename = cursor.getString(columnIndex);
		    }
		    
		    uri = Uri.parse("file://"  + filename);
		}
		
		return uri;
	}
}
