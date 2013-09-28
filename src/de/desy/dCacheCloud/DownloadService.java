package de.desy.dCacheCloud;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.protocol.HttpContext;

import android.app.Dialog;
import android.app.IntentService;
import android.support.v4.app.NotificationCompat;
//import android.app.Notification.Builder;
import android.support.v4.app.NotificationCompat.Builder;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DownloadService extends IntentService {
		
	ProgressBar pb;
	Dialog dialog;
	int downloadSize = 0;
	int totalSize = 0;
	TextView cur_val;
	String downloadFilePath;
	String fileName = null;
	private NotificationManager mNotificationManager = null;
	private HttpGet httpGet = null;
	private DefaultHttpClient httpClient = null;
	private boolean isRedirected = false;
	
	private List<String> target = new ArrayList<String>();
	
	private Builder NotificationFailurePrepare(String text) {
		
		Builder mBuilder = new Builder(this);
		
		mBuilder.setContentText(fileName + ": " + text);
		mBuilder.setContentTitle("Error uploading to dCache server");
		mBuilder.setProgress(0, 0, false);
		mBuilder.setOngoing(false);
		
		return mBuilder;
	}
	
	private NotificationCompat.Builder NotificationUploadPrepare(String text){
		
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
		
		/* Setup Notification Manager Begin */
		mBuilder.setContentTitle("Download from dCache server");
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
		
	private void NotificationCancel(String tag) {
		mNotificationManager.cancel(tag, 0);
		DavSyncOpenHelper helper = new DavSyncOpenHelper(this);
	}
		
	public DownloadService() {
		super("DownloadService");
	}
	
	@Override
	public int onStartCommand (Intent intent, int flags, int startId) {
    	Log.i("2.", "onstartcommand");
	    downloadFilePath = (String) intent.getExtras().get("url"); 
	    fileName = (String) intent.getExtras().get("filename");
		InitializeComponents();
	    return START_REDELIVER_INTENT;
	}
	
	protected void onHandleIntent(Intent intent) {
    	Log.i("2.", "onhandleintent");
	}
	
	private boolean downloadFile() throws IllegalStateException, IOException {
		
		isRedirected = false;
		
		// the return Value from the doors seems to be like "[Location: http:....?uid=...]"
		String finalTarget = target.get(target.size()-1);
		if (finalTarget != null && !finalTarget.startsWith("http")) {
			finalTarget = finalTarget.substring(finalTarget.indexOf("http"), finalTarget.length()-1);
			target.set(target.size()-1, finalTarget);
		}

		httpGet.setURI(URI.create(finalTarget));
		
        HttpResponse response = null;
		try {
			response = httpClient.execute(httpGet);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}    
        
		if (isRedirected) {
			downloadFile();
			return true;
		}
		
        final Builder mBuilder = NotificationUploadPrepare(fileName);
		
        InputStream in = response.getEntity().getContent();
        Long fileSize = response.getEntity().getContentLength();
        FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStorageDirectory() + "/dCacheCloud/" + fileName));
        
        byte[] buffer = new byte[4096];
        int length;
        long bytesWritten = 0;
        int lastPercent = 0;
        
        while((length = in.read(buffer)) > 0) {
            fos.write(buffer, 0, length);
            bytesWritten += length;
            
			int percent = (int) ((bytesWritten * 100) / fileSize);
			if (lastPercent != percent) {
				mBuilder.setProgress(100, percent, false);
				NotificationNotify(fileName, mBuilder);
				lastPercent = percent;
			}            
        }
        
        mBuilder.setOngoing(false);
        NotificationNotify(fileName, mBuilder);
        /* end Download */
        
        return false;
	}
	
    private void InitializeComponents(){    	
        try {
    		SharedPreferences preferences = getSharedPreferences("net.zekjur.davsync_preferences", Context.MODE_PRIVATE);
    		String user = preferences.getString("webdav_user", null);
    		String password = preferences.getString("webdav_password", null);
            
    		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    		
            File folder = new File(Environment.getExternalStorageDirectory() + "/dCacheCloud/");
            if (!folder.exists()) {
                folder.mkdir();
            }
            File file = new File(Environment.getExternalStorageDirectory() + "/dCacheCloud/" + fileName);             
            
            httpGet = new HttpGet(downloadFilePath);
            
            try {
				httpClient = ServerHelper.getClient();
			} catch (KeyManagementException e) {
				e.printStackTrace();
			} catch (UnrecoverableKeyException e) {
				e.printStackTrace();
			} catch (KeyStoreException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (CertificateException e) {
				e.printStackTrace();
			}
            
    		httpClient.setRedirectHandler(new DefaultRedirectHandler() {	
    			@Override
    			public URI getLocationURI(HttpResponse response, HttpContext contet) throws org.apache.http.ProtocolException {
    				
    				Log.d("Rederection!!: ", Arrays.toString(response.getHeaders("Location")));
    				System.out.println(Arrays.toString(response.getHeaders("Location")));
    				
    				target.add(Arrays.toString(response.getHeaders("Location")));
    				isRedirected = true;
    				return super.getLocationURI(response, contet);
    			}
    			
    		});
            
    		ServerHelper.setCredentials(httpClient, httpGet, user, password);
    		target.add(downloadFilePath);
    		downloadFile();
        }
        catch (Exception e) {
        	e.printStackTrace();
        }
    }
}

