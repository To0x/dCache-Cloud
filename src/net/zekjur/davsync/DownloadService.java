package net.zekjur.davsync;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.util.ByteArrayBuffer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Notification.Builder;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadService extends IntentService {
		
	ProgressBar pb;
	Dialog dialog;
	int downloadSize = 0;
	int totalSize = 0;
	TextView cur_val;
	//String downloadFilePath = "https://cloud.dcache.org:2880/s0535279/test.pdf";
	String downloadFilePath; // = "https://fbcdn-sphotos-e-a.akamaihd.net/hphotos-ak-frc1/887175_498243270233831_1603221832_o.jpg";
	String fileName;
	
	CountingInputStreamEntity entity = null;
	
	//public void onCreate(Bundle savedInstanceState) {
        //super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_share);
       
        /*Button b = (Button) findViewById(R.id.action_settings);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                 showProgress(downloadFilePath);
                     
                    new Thread(new Runnable() {
                        public void run() {
                             downloadFile();
                        }
                      }).start();
            }
        }); */
    	//Log.i("1. blabal", "ocreate");
    //}
     
	public DownloadService() {
		super("DownloadService");
	}
	
	@Override
	public int onStartCommand (Intent intent, int flags, int startId) {
    	Log.i("2.", "onstartcommand");
	    downloadFilePath = (String) intent.getExtras().get("url"); 
	    fileName = (String) intent.getExtras().get("filename");
		downloadFile();
	    return START_REDELIVER_INTENT;
	}
	
	protected void onHandleIntent(Intent intent) {
    	Log.i("2.", "onhandleintent");
	    /*downloadFilePath = (String) intent.getExtras().get("url"); 
	    fileName = (String) intent.getExtras().get("filename");
		downloadFile();*/
	}
	
    private void downloadFile(){
    	Log.i("Yeah!", "DU BIST IN DOWNLOADFILE GELANDET!");

    	
    	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 100, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Resources res = this.getResources();
    	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////    	
    	
    	/* Setup Notification Manager Begin */ 	
		final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentIntent(contentIntent); //////////////////////////////////////////////////////////////////////////////////////////////
		mBuilder.setContentTitle("Uploading to dCache server")
		.setContentText(fileName)
		.setSmallIcon(android.R.drawable.ic_menu_upload)
        .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.ic_launcher))
        .setTicker(res.getString(R.string.hello_world))
		.setOngoing(true)
		.setProgress(100, 30, false)
        .setWhen(System.currentTimeMillis())
        .setAutoCancel(true)
        .setContentTitle(res.getString(R.string.app_name))
        .setContentText(res.getString(R.string.action_settings));
		final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		/* Setup Notification Manager End */
		
		Notification n = mBuilder.build();

        mNotificationManager.notify(100, n);
		
        mBuilder.setProgress(100, 50, false);

		if (android.os.Build.VERSION.SDK_INT < 16)
			mNotificationManager.notify(downloadFilePath, 0, mBuilder.getNotification());
		else
			mNotificationManager.notify(downloadFilePath, 0, mBuilder.build());
    	
    	
        try {
            final URL url = new URL(downloadFilePath); 
            
            File folder = new File(Environment.getExternalStorageDirectory() + "/dCacheCloud/");
            if (!folder.exists()) {
                folder.mkdir();
            }
            File file = new File(Environment.getExternalStorageDirectory() + "/dCacheCloud/" + fileName);             
                                             
            long startTime = System.currentTimeMillis();
            Log.d("Dateiname:" + fileName, "Download von " + url + " startet");

            /* Open a connection to that URL. */
            URLConnection ucon = url.openConnection();            
            
            /* Get the filesize to display the percentage in the notification bar */
            totalSize = ucon.getContentLength();
            
            /*Define InputStreams to read from the URLConnection. */
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            
            
			mBuilder.setProgress(100, 50, false);
    		
            
            
            
            /* Read bytes to the Buffer until there is nothing more to read(-1). */
            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                downloadSize += current;
                baf.append((byte) current);   
                
                // update the progressbar //
                /*runOnUiThread(new Runnable() {
                    public void run() {
                        pb.setProgress(downloadSize);
                        float per = ((float)downloadSize/totalSize) * 100;
                        cur_val.setText("Downloaded " + downloadSize + "KB / " + totalSize + "KB (" + (int)per + "%)" );
                    }
                });*/
            }
            
            /* Convert the Bytes read to a String. */
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baf.toByteArray());
            fos.close();
            Log.d("Downloadzeit", ((System.currentTimeMillis() - startTime) / 1000) + "s");

        } catch (IOException e) {
        	Log.d("Error", e.toString());
        	showError("Error : Please check your oSEGHILshgl blabla " + e);
        }
           
        
        

        // When the loop is finished, updates the notification
        mBuilder.setContentText("Download complete")
        // Removes the progress bar
                .setProgress(0, 0, false);
        mNotificationManager.notify(1001, mBuilder.build());
		if (android.os.Build.VERSION.SDK_INT < 16)
			mNotificationManager.notify(downloadFilePath, 0, mBuilder.getNotification());
		else
			mNotificationManager.notify(downloadFilePath, 0, mBuilder.build());
    }
     
    private void showError(final String err){
        /*runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(DownloadService.this, err, Toast.LENGTH_LONG).show();
            }
        });*/
    }
     
    private void showProgress(String file_path){
        //dialog = new Dialog(DownloadService.this);
        /*dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.myprogressdialog);
        dialog.setTitle("Download Progress");
 
        TextView text = (TextView) dialog.findViewById(R.id.tv1);
        text.setText("Downloading file from ... " + file_path);
        cur_val = (TextView) dialog.findViewById(R.id.cur_pg_tv);
        cur_val.setText("Starting download...");
        dialog.show();
         
        pb = (ProgressBar)dialog.findViewById(R.id.progress_bar);
        pb.setProgress(0);*/
        //pb.setProgressDrawable(getResources().getDrawable(R.drawable.green_progress)); 
    }
}

