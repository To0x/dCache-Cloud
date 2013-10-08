package de.desy.dCacheCloud;
 
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import de.desy.dCacheCloud.R;
 
public class MainActivity extends Activity {
 
	private ListView listView1;
 
	public void onCreate(Bundle savedInstanceState) {
		final Context context = this;
 
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
 
        listView1 = (ListView) findViewById(R.id.listView1);
        Vector<String> listView1Vector = new Vector<String>();
        listView1Vector.add("Server"); 
        listView1Vector.add("Einstellungen");
        listView1.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listView1Vector));
 
		listView1.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	Log.i("Hello!", "listView1 Clicked! YAY!");
            	if (position == 0)
            	{
            		if (android.os.Build.VERSION.SDK_INT > 10) {
            			StrictMode.ThreadPolicy policy = 
            			        new StrictMode.ThreadPolicy.Builder().permitAll().build();
            			StrictMode.setThreadPolicy(policy);
            		}
            		
        		    Intent intent = new Intent(context, ServerViewActivity.class);
            		SharedPreferences preferences = getSharedPreferences("net.zekjur.davsync_preferences", Context.MODE_PRIVATE);
	        		try {
						intent.putExtra("url", new URL(preferences.getString("webdav_url", null)));
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
        		    startActivity(intent);

            	}
            	else
            	{
        		    Intent intent = new Intent(context, SettingsActivity.class);
        		    startActivity(intent);
            	}
            }
        });		
	} 
}