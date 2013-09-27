package de.desy.dCacheCloud;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Vector;

import net.zekjur.davsync.R;

import org.apache.http.util.ByteArrayBuffer;
import org.apache.ivy.util.url.ApacheURLLister;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
 
public class ServerViewActivity extends Activity {

	private ListView listView;
	private URL url1;
 
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	 	final Context context = this;
	 	
		try 
		{
			url1 = new URL("http://sunsetsims.mustbedestroyed.org/wp-content/uploads/2013/09/");
			//url1 = new URL("http://maslovd.no-ip.org/public/mp3/Metallica/"); 
			
	        setContentView(R.layout.file_list);
	        
	        listView = (ListView) findViewById(R.id.listView1);
	        List serverDir = null;
	        
	        try 
	        {  
	            ApacheURLLister lister1 = new ApacheURLLister();         
	            serverDir = lister1.listAll(url1);
         
	            Vector<String> urlVector = new Vector<String>();
	            for (int i = 0; i < serverDir.size(); i++)
	            {	
	            	urlVector.add(serverDir.get(i).toString().substring(url1.toString().length())); 
	          	            	
		            /*final Intent intent = new Intent(Intent.ACTION_VIEW);
		            intent.setData((Uri) serverDir.get(i));
		            intent.setType("image/png");         
		            
		            final List<ResolveInfo> matches = getPackageManager().queryIntentActivities(intent, 0);
		            for (ResolveInfo match : matches) {
		                final Drawable icon = match.loadIcon(getPackageManager());
		                //final CharSequence label = match.loadLabel(getPackageManager());
		            }*/
	            	
	            	//Log.i(serverDir.get(i).toString(), "ha");
	            	//Log.i(urlVector.lastElement().toString(), "bla");
	            }
	            listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, urlVector));
	        }        
	        catch (Exception e) {
	            e.printStackTrace();
	        }          	
	        	        
	        listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener()
	        {
	            @Override
	            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	            	// Aufrufen der Datei im Browser?
	    		    /*Intent intent = new Intent(getApplicationContext(), WebViewActivity.class);
	    		    intent.putExtra("url", url1.toString() + parent.getItemAtPosition(position).toString());
	    		    startActivity(intent);*/
	    		    //finish();
	            	Log.i("Hello!", "Clicked! YAY!");
	            	
	            	Intent intent = new Intent();
	            	intent.setAction(android.content.Intent.ACTION_VIEW);
	            	
	            	String urlString = url1.toString() + parent.getItemAtPosition(position);
	            	String extension = "";
	            	int i = urlString.lastIndexOf('.');
	            	if (i > 0) {
	            		extension = urlString.substring(i+1);
	            	}
	            	
	            	intent.setData(Uri.parse((url1.toString() + parent.getItemAtPosition(position))));
	            	startActivity(intent); 
	            }
	        });
	        
	        listView.setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener()
	        {
	            @Override
	            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
	            	Log.i("Hello!", "LONGClicked! YAY!");
	            	
	        		Intent intent = new Intent(context, DownloadService.class);
	        		intent.putExtra("url", url1 + parent.getItemAtPosition(position).toString());
	        		intent.putExtra("filename", parent.getItemAtPosition(position).toString());
	        		startService(intent);
	            	
	            	//DownloadFromUrl(url1 + parent.getItemAtPosition(position).toString(), parent.getItemAtPosition(position).toString());
	                return true;
	            }
	        });		
		} 
		catch (MalformedURLException e1) 
		{
			e1.printStackTrace();
		}
	}
	}