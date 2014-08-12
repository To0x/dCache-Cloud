package de.desy.dCacheCloud;
 
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Vector;

import External.IntentIntegrator;
import External.IntentResult;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import de.desy.dCacheCloud.Activities.FriendFoundActivity;
import de.desy.dCacheCloud.Activities.ProfileActivity;
import de.desy.dCacheCloud.Activities.ServerViewActivity;
import de.desy.dCacheCloud.Activities.SettingsActivity;

public class MainActivity extends Activity {
 
	private ListView listView1;
 	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId())
		{
		case R.id.action_settings:
			break;
		case R.id.action_addUser:
			IntentIntegrator id = new IntentIntegrator(this);
			id.initiateScan(IntentIntegrator.QR_CODE_TYPES);
//	        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
//	        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
//	        startActivityForResult(intent, 0);
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater infl = getMenuInflater();
		infl.inflate(R.menu.main_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		IntentResult res  = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (res != null)
		{
			try
			{
				OpenHelper oh = new OpenHelper(this);
				String name = oh.getFriendName(res.getContents());
				
				if (name != null)
				{
					Toast.makeText(this, String.format("Friend already exist with name : %s",name), Toast.LENGTH_LONG).show();
				}
				else
				{
					Bundle b = new Bundle();
					b.putString("KEY", res.getContents());
					Intent friendFoundIntent = new Intent(this, FriendFoundActivity.class);
					MessageDigest digest = MessageDigest.getInstance("SHA-256");
					byte[] data = digest.digest(res.getContents().getBytes("UTF-8"));
					String hash = String.format("%0" + (data.length*2) + "X", new BigInteger(1, data));
					b.putString("HASH",hash);
					friendFoundIntent.putExtras(b);
					startActivity(friendFoundIntent);
				}
			}
			catch (Exception e)
			{}

		}
	}

	public void onCreate(Bundle savedInstanceState) {
		final Context context = this;
 
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
 
        listView1 = (ListView) findViewById(R.id.listView1);
        Vector<String> listView1Vector = new Vector<String>();
        listView1Vector.add("Server"); 
        listView1Vector.add("Einstellungen");
        listView1Vector.add("Profil");
        listView1.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listView1Vector));
 
		listView1.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	Log.i("Hello!", "listView1 Clicked! YAY!");
            	if (position == 0)
            	{
            		
            		SharedPreferences preferences = getSharedPreferences("de.desy.dCacheCloud_preferences", Context.MODE_PRIVATE);
            		String user = preferences.getString("webdav_user", null);
            		String password = preferences.getString("webdav_password", null);
            		
            		if (isNetworkConnected()) {
            			if (user != null && user != "" && password != null && password != "") {
			
		            		if (android.os.Build.VERSION.SDK_INT > 10) {
		            			StrictMode.ThreadPolicy policy = 
		            			        new StrictMode.ThreadPolicy.Builder().permitAll().build();
		            			StrictMode.setThreadPolicy(policy);
		            		}
		            		
		        		    Intent intent = new Intent(context, ServerViewActivity.class);
			        		try {
								intent.putExtra("url", new URL(preferences.getString("webdav_url", null)));
							} catch (MalformedURLException e) {
								e.printStackTrace();
							}
		        		    startActivity(intent);
            			}
            			else {
                			Toast.makeText(getApplicationContext(), "Please fill in your user data before trying to use the dCache Cloud!", Toast.LENGTH_LONG).show();            				
            			}
            		}
            		else {
            			Toast.makeText(getApplicationContext(), "You are not connected to the internet!", Toast.LENGTH_LONG).show();
            		}
            	}
            	else if (position == 1)
            	{
        		    Intent intent = new Intent(context, SettingsActivity.class);
        		    startActivity(intent);
            	}
            	else
            	{
            		Intent intent = new Intent(context, ProfileActivity.class);
            		startActivity(intent);
            		// call profile!
            	}
            	
            }
        });		
	} 
	
    public boolean isNetworkConnected() {
        final ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED;
   }
}