package de.desy.dCacheCloud.Activities;
 
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyPair;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import de.desy.dCacheCloud.CryptoHelper;
import de.desy.dCacheCloud.DatabaseHelper;
import de.desy.dCacheCloud.KeyStoreHelper;
import de.desy.dCacheCloud.R;

public class MainActivity extends Activity {
 
	private ListView lvMainMenu;
	private Context context;
	private DatabaseHelper oh;
 		
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId())
		{
		case R.id.action_addUser:
			IntentIntegrator id = new IntentIntegrator(this);
			id.initiateScan(IntentIntegrator.QR_CODE_TYPES);
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

	
	@Override
	protected void onDestroy() {
		KeyStoreHelper.closeStore(context);
		super.onDestroy();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		IntentResult res  = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (res != null)
		{
			try
			{
				String name = oh.getFriendName(res.getContents());
				
				if (name != null)
				{
					Toast.makeText(this, String.format("Friend already exist with name : %s",name), Toast.LENGTH_LONG).show();
				}
				else
				{
					Bundle b = new Bundle();
					b.putString("KEY", res.getContents());
					b.putString("HASH",CryptoHelper.hash(res.getContents()));
					Intent friendFoundIntent = new Intent(this, FriendFoundActivity.class);
					friendFoundIntent.putExtras(b);
					startActivity(friendFoundIntent);
				}
			}
			catch (Exception e)
			{}

		}
	}

	private void initialize()
	{
		context = this.getApplicationContext();
		// TODO: App Crash if this is the first Start - cause no password is set!
		KeyStoreHelper.getKeyStore(context);
		oh = new DatabaseHelper(context);
		
	
		if (KeyStoreHelper.getOwnPriv(context) == null)
		{
			// Key´s have to initialize
			KeyPair pair = CryptoHelper.generateAsymmetricKeyPair(1024);
			KeyStoreHelper.storeOwnAsymmetric(context, pair);
		}
		
		
		lvMainMenu = (ListView) findViewById(R.id.listView1);
		
        Vector<String> lvMenuItems = new Vector<String>();
        lvMenuItems.add("Server"); 
        lvMenuItems.add("Einstellungen");
        lvMenuItems.add("Profil");
        lvMainMenu.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, lvMenuItems));
	}

	public void onCreate(Bundle savedInstanceState) {
 
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
 
        initialize();
 
		lvMainMenu.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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