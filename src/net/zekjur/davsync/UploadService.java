package net.zekjur.davsync;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import net.zekjur.davsync.CountingInputStreamEntity.UploadListener;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.app.Notification;
import android.app.Notification.Builder;
import android.util.Log;
import android.webkit.MimeTypeMap;


public class UploadService extends IntentService {
	public UploadService() {
		super("UploadService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		final Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
		Log.d("davsyncs", "Uploading " + uri.toString());

		SharedPreferences preferences = getSharedPreferences("net.zekjur.davsync_preferences", Context.MODE_PRIVATE);

		String webdavUrl = preferences.getString("webdav_url", null);
		String webdavUser = preferences.getString("webdav_user", null);
		String webdavPassword = preferences.getString("webdav_password", null);
		if (webdavUrl == null) {
			Log.d("davsyncs", "No WebDAV URL set up.");
			return;
		}

		ContentResolver cr = getContentResolver();

		
		////
		String filename = null;
		String scheme = uri.getScheme();
		if (scheme.equals("file")) {
		    filename = uri.getLastPathSegment();
		}
		else if (scheme.equals("content")) {

		    String[] proj = { MediaStore.Images.Media.TITLE };
		    Cursor cursor = getContentResolver().query(uri, proj, null, null, null);
		    if (cursor != null && cursor.getCount() != 0) {
		        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE);
		        cursor.moveToFirst();
		        filename = cursor.getString(columnIndex);
		    }
		    
		    MimeTypeMap mime = MimeTypeMap.getSingleton();
		    String type = mime.getExtensionFromMimeType(cr.getType(uri));
		    
		    filename += "." + type;
		}
		
		/////

		if (filename == null) {
			Log.d("davsyncs", "filenameFromUri returned null");
			return;
		}

		
		
		final Builder mBuilder = new Notification.Builder(this);
		mBuilder.setContentTitle("Uploading to dCache server");
		mBuilder.setContentText(filename);
		mBuilder.setSmallIcon(android.R.drawable.ic_menu_upload);
		mBuilder.setOngoing(true);
		mBuilder.setProgress(100, 30, false);
		final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		try {
			if (android.os.Build.VERSION.SDK_INT < 16)
				mNotificationManager.notify(uri.toString(), 0,mBuilder.getNotification());
			else
				mNotificationManager.notify(uri.toString(), 0, mBuilder.build());
		}
		catch (Exception e) {
			int bla = 3;
			bla = 4;
		}

		HttpPut httpPut = new HttpPut(webdavUrl + filename);

		ParcelFileDescriptor fd;
		InputStream stream;
		try {
			fd = cr.openFileDescriptor(uri, "r");
			stream = cr.openInputStream(uri);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
		}

		CountingInputStreamEntity entity = new CountingInputStreamEntity(stream, fd.getStatSize());
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

		int timeoutConnection = 100000;
		HttpParams myParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(myParams, timeoutConnection);
		HttpConnectionParams.setSoTimeout(myParams, timeoutConnection);
		
		
		DefaultHttpClient httpClient = null;
			try {
				httpClient = getClient();
			} catch (KeyManagementException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (UnrecoverableKeyException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (KeyStoreException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (NoSuchAlgorithmException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (CertificateException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}


		if (webdavUser != null && webdavPassword != null) {
			AuthScope authScope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);
			UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(webdavUser, webdavPassword);
						
			httpClient.getCredentialsProvider().setCredentials(authScope, credentials);

			try {
				httpPut.addHeader(new BasicScheme().authenticate(credentials, httpPut));
			} catch (AuthenticationException e1) {
				e1.printStackTrace();
				return;
			}
		}

		try {
			ClientConnectionManager manag = httpClient.getConnectionManager();
			HttpResponse response = httpClient.execute(httpPut);
			int status = response.getStatusLine().getStatusCode();
			// 201 means the file was created.
			// 200 and 204 mean it was stored but already existed.
			if (status == 201 || status == 200 || status == 204) {
				// The file was uploaded, so we remove the ongoing notification,
				// remove it from the queue and thats it.
				mNotificationManager.cancel(uri.toString(), 0);
				DavSyncOpenHelper helper = new DavSyncOpenHelper(this);
				helper.removeUriFromQueue(uri.toString());
				return;
			}
			Log.d("davsyncs", "" + response.getStatusLine());
			mBuilder.setContentText(filename + ": " + response.getStatusLine());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			mBuilder.setContentText(filename + ": " + e.getLocalizedMessage());
		} catch (IOException e) {
			e.printStackTrace();
			mBuilder.setContentText(filename + ": " + e.getLocalizedMessage());
		}

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
	
	public DefaultHttpClient getClient() throws KeyStoreException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, CertificateException, IOException 
	{ 
		DefaultHttpClient ret = null;

		//sets up parameters
	    HttpParams params = new BasicHttpParams();
	    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
	    HttpProtocolParams.setContentCharset(params, "utf-8");
	    //params.setBooleanParameter("http.protocol.expect-continue", false);
	    params.setBooleanParameter("http.protocol.expect-continue", true);
	
	    
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        
		//SSLSocketFactory sslf = new SSLSocketFactory(trustStore);
		//sslf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		
		MySSLSocketFactory ssl = new MySSLSocketFactory(trustStore);
		ssl.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		
	    SchemeRegistry registry = new SchemeRegistry();
	    registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
	    registry.register(new Scheme("https", ssl, 443));
	    
	    
	    
	    //registers schemes for both http and https
	    //final SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
	
	    ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(params, registry);
	    ret = new DefaultHttpClient(manager, params);
	    return ret;
	}
}
