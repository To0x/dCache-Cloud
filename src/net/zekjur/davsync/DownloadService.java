package net.zekjur.davsync;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadService extends Intent {
	
	
	ProgressBar pb;
	Dialog dialog;
	int downloadSize = 0;
	int totalSize = 0;
	TextView cur_val;
	//String downloadFilePath = "https://cloud.dcache.org:2880/s0535279/test.pdf";
	String downloadFilePath = "https://fbcdn-sphotos-e-a.akamaihd.net/hphotos-ak-frc1/887175_498243270233831_1603221832_o.jpg";
	
	/*@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
       
        Button b = (Button) findViewById(R.id.action_settings);
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
        }); 
    }*/
     
	public DownloadService() {
		super();
	}
	
	protected void onHandleIntent(Intent intent) {
		downloadFile();
	}
	
    private void downloadFile(){
         
        try {
            URL url = new URL(downloadFilePath);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
 
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
 
            //connect
            urlConnection.connect();
 
            //set the path where we want to save the file          
            File SDCardRoot = Environment.getExternalStorageDirectory();
            //File SystemRoot = Environment.getDataDirectory();
            //create a new file, to save the downloaded file
            File file = new File(SDCardRoot,"downloaded_file.png");
  
            FileOutputStream fileOutput = new FileOutputStream(file);
 
            //Stream used for reading the data from the internet
            InputStream inputStream = urlConnection.getInputStream();
 
            //this is the total size of the file which we are downloading
            totalSize = urlConnection.getContentLength();
 
            /*runOnUiThread(new Runnable() {
                public void run() {
                    pb.setMax(totalSize);
                }              
            });*/
             
            //create a buffer...
            byte[] buffer = new byte[1024];
            int bufferLength = 0;
 
            while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
                fileOutput.write(buffer, 0, bufferLength);
                downloadSize += bufferLength;
                // update the progressbar //
                /*runOnUiThread(new Runnable() {
                    public void run() {
                        pb.setProgress(downloadSize);
                        float per = ((float)downloadSize/totalSize) * 100;
                        cur_val.setText("Downloaded " + downloadSize + "KB / " + totalSize + "KB (" + (int)per + "%)" );
                    }
                });*/
            }
            //close the output stream when complete //
            fileOutput.close();
            /*runOnUiThread(new Runnable() {
                public void run() {
                    // pb.dismiss(); // if you want close it..
                }
            });*/        
         
        } catch (final MalformedURLException e) {
            showError("Error : MalformedURLException " + e);       
            e.printStackTrace();
        } catch (final IOException e) {
            showError("Error : IOException " + e);         
            e.printStackTrace();
        }
        catch (final Exception e) {
            showError("Error : Please check your internet connection " + e);
        }      
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
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.myprogressdialog);
        dialog.setTitle("Download Progress");
 
        TextView text = (TextView) dialog.findViewById(R.id.tv1);
        text.setText("Downloading file from ... " + file_path);
        cur_val = (TextView) dialog.findViewById(R.id.cur_pg_tv);
        cur_val.setText("Starting download...");
        dialog.show();
         
        pb = (ProgressBar)dialog.findViewById(R.id.progress_bar);
        pb.setProgress(0);
        //pb.setProgressDrawable(getResources().getDrawable(R.drawable.green_progress)); 
    }
}