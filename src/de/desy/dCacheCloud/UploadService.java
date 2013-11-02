package de.desy.dCacheCloud;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import de.desy.dCacheCloud.CountingInputStreamEntity.UploadListener;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

/**
 * @author Tom Schubert
 * @version 0.9 BETA
 * @since 21.09.2013
 *
 */
@SuppressLint("NewApi")
public class UploadService extends IntentService {

	// Defines begin //
	private static boolean HTTPLOG = false;
	// Defines End //
	
	private boolean isRedirected = false;
	private DefaultHttpClient httpClient = null;
	private HttpContext context = null;
	private CountingInputStreamEntity entity = null;
	private ContentResolver cr = null;
	private NotificationManager mNotificationManager = null;
	private HttpPut httpPut = null;

	private List<String> target = new ArrayList<String>();
	private String filename = null;
	private Uri fileUri = null;

	public UploadService() {
		super("UploadService");
	}
	
	private boolean uploadFile() {
	
		isRedirected = false;
		
		// the return Value from the doors seems to be like "[Location: http:....?uid=...]"
		String finalTarget = target.get(target.size()-1);
		if (finalTarget != null && !finalTarget.startsWith("http")) {
			finalTarget = finalTarget.substring(finalTarget.indexOf("http"), finalTarget.length()-1);
			target.set(target.size()-1, finalTarget);
		} else if (!finalTarget.endsWith(filename)) {
			finalTarget += filename;
			target.set(target.size()-1, finalTarget);
		}

		httpPut.setURI(URI.create(finalTarget));
		HttpResponse response = null;
		try {
			response = httpClient.execute(httpPut, context);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		if (isRedirected) {
			uploadFile();
			return true;
		}
		
		int status = response.getStatusLine().getStatusCode();
		
		// 201 means the file was created.
		// 200 and 204 mean it was stored but already existed.
		
		if (status == 201 || status == 200 || status == 204) {
			NotificationCancel(fileUri.toString());
		}
		else 
		{
			// Uploading Failed!
			Log.d("davsyncs", "" + response.getStatusLine());
			NotificationNotify(fileUri.toString(),NotificationFailurePrepare(response.getStatusLine().toString()));
		}
		return true;
	}

	private boolean InitializeComponents(Intent intent)
	{
		cr = getContentResolver();
		context = new BasicHttpContext();
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		
		/* Get Settings Begin */
		SharedPreferences preferences = getSharedPreferences("de.desy.dCacheCloud_preferences", Context.MODE_PRIVATE);
		target.add(preferences.getString("webdav_url", null));
		String user = preferences.getString("webdav_user", null);
		String password = preferences.getString("webdav_password", null);
		/* Get Settings End */
		
		if (target.get(target.size()-1) == null) {
			Log.d("dCache", "No URL set up.");
			return false;
		}
			
		// Get Extras from Intend-Loader
		fileUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
		Log.d("davsyncs", "Uploading " + fileUri.toString());
		filename = getFilePath(fileUri);

		if (filename == null) {
			Log.d("dCache", "fileName returned null");
			return false;
		}
		setFileHandling();
		
		try {
			httpClient = ServerHelper.getClient();
		} catch (GeneralSecurityException e) {
			Log.d("SECURITY", String.format("General Security Error: %s", e.toString()));
			e.printStackTrace();
		} catch (IOException e1) {
			Log.d("Unknown", String.format("Error: %s", e1.toString()));
			e1.printStackTrace();
		}
		
		httpClient.setRedirectHandler(new DefaultRedirectHandler() {	
			@Override
			public URI getLocationURI(HttpResponse response, HttpContext contet) throws org.apache.http.ProtocolException {
				
				Log.d("Rederection!!: ", Arrays.toString(response.getHeaders("Location")));
				System.out.println(Arrays.toString(response.getHeaders("Location")));
				
				target.add(Arrays.toString(response.getHeaders("Location")));
				isRedirected = true;
				return super.getLocationURI(response, context);
			}
			
		});
		
		httpPut = new HttpPut();
		httpPut.setEntity(entity);
		ServerHelper.setCredentials(httpClient, httpPut, user, password);
		
		return true;
	}

	private String getFilePath(Uri uriToFile) {
		
		String filename = null;
		String scheme = uriToFile.getScheme();
		if (scheme.equals("file")) {
		    filename = uriToFile.getLastPathSegment();
		}
		else if (scheme.equals("content")) {

		    String[] proj = { MediaStore.Images.Media.TITLE };
		    Cursor cursor = getContentResolver().query(uriToFile, proj, null, null, null);
		    if (cursor != null && cursor.getCount() != 0) {
		    	int columnIndex = -1;
		    	try {
		    		columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE);
		    	} catch (IllegalArgumentException e) {
		    		Log.d("dCache", e.toString());
		    	}
		        cursor.moveToFirst();
		        filename = cursor.getString(columnIndex);
		    }
		    
		    MimeTypeMap mime = MimeTypeMap.getSingleton();
		    String type = mime.getExtensionFromMimeType(cr.getType(uriToFile));
		    
		    filename += "." + type;
		}

		
		return filename;
	}
	
	private Builder NotificationFailurePrepare(String text) {
		
		Builder mBuilder = new Builder(this);
		
		mBuilder.setContentText(filename + ": " + text);
		mBuilder.setContentTitle("Error uploading to dCache server");
		mBuilder.setProgress(0, 0, false);
		mBuilder.setOngoing(false);
		
		return mBuilder;
	}
	
	private Builder NotificationUploadPrepare(String text){
		
		Builder mBuilder = new Builder(this);
		
		/* Setup Notification Manager Begin */
		mBuilder.setContentTitle("Uploading to dCache server");
		mBuilder.setContentText(text);
		mBuilder.setSmallIcon(android.R.drawable.ic_menu_upload);
		mBuilder.setOngoing(true);
		mBuilder.setProgress(100, 0, false);
		/* Setup Notification Manager End */
		
		return mBuilder;
	}
	
	@SuppressWarnings("deprecation")
	private void NotificationNotify(String tag, Builder mBuilder) {
		if (android.os.Build.VERSION.SDK_INT < 11) {
			// Not supported
		}
		if (android.os.Build.VERSION.SDK_INT < 16)
			mNotificationManager.notify(tag, 0,mBuilder.getNotification());
		else
			mNotificationManager.notify(tag, 0, mBuilder.build());
	}
	
	private void setFileHandling() {
		ParcelFileDescriptor fd;
		InputStream stream;
		try {
			fd = cr.openFileDescriptor(fileUri, "r");
			stream = cr.openInputStream(fileUri);
		} catch (FileNotFoundException e1) {
			Log.d("dCache", "File not Found!");
			e1.printStackTrace();
			return;
		}
		
		final Builder mBuilder = NotificationUploadPrepare(filename);
		NotificationNotify(fileUri.toString(), mBuilder);
		
		entity = new CountingInputStreamEntity(stream, fd.getStatSize());
		
		entity.setUploadListener(new UploadListener() {
			@Override
			public void onChange(int percent) {
				mBuilder.setProgress(100, percent, false);
				NotificationNotify(fileUri.toString(), mBuilder);
			}
		});
	}
	
	private void NotificationCancel(String tag) {
		mNotificationManager.cancel(tag, 0);
		DavSyncOpenHelper helper = new DavSyncOpenHelper(this);
		helper.removeUriFromQueue(fileUri.toString());
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		
		int retryCount = 5;
		while (!InitializeComponents(intent) && retryCount-- != 0);
		
		uploadFile();
	}
}
