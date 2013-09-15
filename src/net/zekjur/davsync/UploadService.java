package net.zekjur.davsync;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream.PutField;
import java.net.ProtocolException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;

import net.zekjur.davsync.CountingInputStreamEntity.UploadListener;

import org.apache.http.HttpConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
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
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
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



@SuppressLint("NewApi")
public class UploadService extends IntentService {

	// Defines begin //
	private static boolean HTTPLOG = false;
	// Defines End //
	
	private boolean isRedirected = false;
	private String finalTarget = null;
	private DefaultHttpClient httpClient = null;
	private HttpContext context = null;
	CountingInputStreamEntity entity = null;
	private ContentResolver cr = null;
	private Builder mBuilder = null;
	private NotificationManager mNotificationManager = null;
	
	// Credentials
	private String url = null;
	private String user = null;
	private String password = null;
	
	String filename = null;
	Uri uri = null;
	
	
	
	
	public UploadService() {
		super("UploadService");
	}
	
	private boolean uploadFile() {
				
		// the return Value from the doors seems to be like "[Location: http:....?uid=...]"
		if (finalTarget != null && !finalTarget.startsWith("http")) {
			finalTarget = finalTarget.substring(finalTarget.indexOf("http"), finalTarget.length()-1);
		} else
			return false;
		
		HttpPut httpPut = new HttpPut(finalTarget);
		httpPut.setEntity(entity);
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
		
		int status = response.getStatusLine().getStatusCode();
		// 201 means the file was created.
		// 200 and 204 mean it was stored but already existed.
		
		if (status == 201 || status == 200 || status == 204) {

			return true;
		}
		else {

			if (isRedirected) {
				// TODO in case of next step?
			}
			return false;	
		}
	}

	private boolean InitializeComponents()
	{
		cr = getContentResolver();
		context = new BasicHttpContext();
		mBuilder = new Notification.Builder(this);
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		
		/* Get Settings Begin */
		SharedPreferences preferences = getSharedPreferences("net.zekjur.davsync_preferences", Context.MODE_PRIVATE);
		url = preferences.getString("webdav_url", null);
		user = preferences.getString("webdav_user", null);
		password = preferences.getString("webdav_password", null);
		/* Get Settings End */

		if (url == null) {
			Log.d("dCache", "No URL set up.");
			return false;
		}

		String filename = getFilePath(uri);

		if (filename == null) {
			Log.d("dCache", "fileName returned null");
			return false;
		}

		
		try {
			httpClient = getClient();
		} catch (GeneralSecurityException e) {
			Log.d("SECURITY", String.format("General Security Error: %s", e.toString()));
			e.printStackTrace();
		} catch (IOException e1) {
			Log.d("Unknown", String.format("Error: %s", e1.toString()));
			e1.printStackTrace();
		}
		
		return true;
	}
	
	@Override
	public void onCreate() {
		
		super.onCreate();
		if (!InitializeComponents()) {
			// Something went wrong!
			
		}
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
	
	private void NotificationCancel(String tag) {
		mNotificationManager.cancel(tag, 0);
		DavSyncOpenHelper helper = new DavSyncOpenHelper(this);
		helper.removeUriFromQueue(uri.toString());
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onHandleIntent(Intent intent) {
		
		// Get Extras from Intend-Loader
		uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
		Log.d("davsyncs", "Uploading " + uri.toString());

		/* Setup Notification Manager Begin */
		mBuilder.setContentTitle("Uploading to dCache server");
		mBuilder.setContentText(filename);
		mBuilder.setSmallIcon(android.R.drawable.ic_menu_upload);
		mBuilder.setOngoing(true);
		mBuilder.setProgress(100, 30, false);
		/* Setup Notification Manager End */
		
		if (android.os.Build.VERSION.SDK_INT < 11) {
		}
		if (android.os.Build.VERSION.SDK_INT < 16)
			mNotificationManager.notify(uri.toString(), 0,mBuilder.getNotification());
		else
			mNotificationManager.notify(uri.toString(), 0, mBuilder.build());

		HttpPut httpPut = new HttpPut(url + filename);

		ParcelFileDescriptor fd;
		InputStream stream;
		try {
			fd = cr.openFileDescriptor(uri, "r");
			stream = cr.openInputStream(uri);
		} catch (FileNotFoundException e1) {
			Log.d("dCache", "File not Found!");
			e1.printStackTrace();
			return;
		}
		
		entity = new CountingInputStreamEntity(stream, fd.getStatSize());
		
		entity.setUploadListener(new UploadListener() {
			@Override
			public void onChange(int percent) {
				mBuilder.setProgress(100, percent, false);
				if (android.os.Build.VERSION.SDK_INT < 16)
					mNotificationManager.notify(uri.toString(), 0,mBuilder.getNotification());
				else
					mNotificationManager.notify(uri.toString(), 0, mBuilder.build());
			}
		});
		
		httpPut.setEntity(entity);

		if (user != null && password != null) {
			AuthScope authScope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);
			UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user, password);
						
			httpClient.getCredentialsProvider().setCredentials(authScope, credentials);

			try {
				httpPut.addHeader(new BasicScheme().authenticate(credentials, httpPut));
			} catch (AuthenticationException e1) {
				e1.printStackTrace();
				return;
			}
		}
	
		httpClient.setRedirectHandler(new DefaultRedirectHandler() {
			
			@Override
			public URI getLocationURI(HttpResponse response, HttpContext contet) throws org.apache.http.ProtocolException {
				
				Log.d("Rederection!!: ", Arrays.toString(response.getHeaders("Location")));
				System.out.println(Arrays.toString(response.getHeaders("Location")));
				
				finalTarget = Arrays.toString(response.getHeaders("Location"));
				isRedirected = true;
				return super.getLocationURI(response, context);
			}
			
		});
		
		HttpResponse response = null;
		try {	
			response = httpClient.execute(httpPut, context);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			mBuilder.setContentText(filename + ": " + e.getLocalizedMessage());
		} catch (IOException e) {
			e.printStackTrace();
			mBuilder.setContentText(filename + ": " + e.getLocalizedMessage());
		}
			
		// Code will interrupt if there is a redirection!
		
		if (isRedirected) 
		{
			if (uploadFile())
			{
				
			} else {
				// Uploading Failed
			}
			return;
		}
		
		if (response == null)
			return;
			
		int status = response.getStatusLine().getStatusCode();

		if (status == 201 || status == 200 || status == 204) {
			NotificationCancel(uri.toString());
			return;
		}
		else 
		{
			// Uploading Failed!
			Log.d("davsyncs", "" + response.getStatusLine());
			mBuilder.setContentText(filename + ": " + response.getStatusLine());
	
			// XXX: It would be good to provide an option to try again.
			// (or try it again automatically?)
			// XXX: possibly we should re-queue the images in the database
			mBuilder.setContentTitle("Error uploading to dCache server");
			mBuilder.setProgress(0, 0, false);
			mBuilder.setOngoing(false);
			if (android.os.Build.VERSION.SDK_INT < 16)
				mNotificationManager.notify(uri.toString(), 0,mBuilder.getNotification());
			else
				mNotificationManager.notify(uri.toString(), 0, mBuilder.build());
		}
	}
	
	public DefaultHttpClient getClient() throws KeyStoreException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, CertificateException, IOException 
	{ 
		DefaultHttpClient ret = null;

		if (HTTPLOG) 
		{
			/* LOGGING begin 
			java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
			java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);
	
			System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
			System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
			System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
			System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
			System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "debug");
			/* LOGGING end */
		}
		
		
		//sets up parameters //
	    HttpParams params = new BasicHttpParams();
	    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
	    HttpProtocolParams.setContentCharset(params, "utf-8");
	    //params.setBooleanParameter("http.protocol.expect-continue", false);
	    params.setBooleanParameter("http.protocol.expect-continue", true);
	
	    // set up TrustStore for Certificates //
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);

		MySSLSocketFactory ssl = new MySSLSocketFactory(trustStore);
		ssl.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		
	    SchemeRegistry registry = new SchemeRegistry();
	    registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
	    registry.register(new Scheme("https", ssl, 443));
	    
	    ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(params, registry);
	    ret = new DefaultHttpClient(manager, params);
	    return ret;
	}
}
