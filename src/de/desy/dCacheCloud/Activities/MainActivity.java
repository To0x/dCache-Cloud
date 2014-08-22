package de.desy.dCacheCloud.Activities;
 
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyPair;
import java.util.Vector;

import External.IntentIntegrator;
import External.IntentResult;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import de.desy.dCacheCloud.CryptoHelper;
import de.desy.dCacheCloud.DatabaseHelper;
import de.desy.dCacheCloud.KeyStoreHelper;
import de.desy.dCacheCloud.R;
import de.desy.dCacheCloud.UploadService;

public class MainActivity extends Activity implements OnItemClickListener {
 
	private ListView lvMainMenu;
	private Context context;
	private DatabaseHelper oh;
	
	private static int UPLOAD_REQUEST = 1;
 		
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId())
		{
		case R.id.action_addUser:
			IntentIntegrator id = new IntentIntegrator(this);
			id.initiateScan(IntentIntegrator.QR_CODE_TYPES);
			break;
		case R.id.action_upload:
	        Intent fileintent = new Intent(Intent.ACTION_GET_CONTENT);
	        fileintent.setType("*/*");
	        try {
	            startActivityForResult(fileintent, UPLOAD_REQUEST);
	        } catch (ActivityNotFoundException e) {
	        	e.printStackTrace();
	        }
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
		KeyStoreHelper.close(context);
		super.onDestroy();
	}

	@SuppressWarnings("unused")
	private static boolean isUserDataSet(Context c)
	{
		SharedPreferences preferences = c.getSharedPreferences("de.desy.dCacheCloud_preferences", Context.MODE_PRIVATE);
		String user = preferences.getString("webdav_user", null);
		String password = preferences.getString("webdav_password", null);
		
		if (user != null && password != null)
			return true;
		
		return false;
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		IntentResult res  = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		
		if (requestCode == UPLOAD_REQUEST)
		{
			Toast.makeText(getApplicationContext(), "File found", Toast.LENGTH_LONG).show();
			Uri fileUri = intent.getData();
			Intent ulIntent = new Intent(context, UploadActivity.class);
			ulIntent.setType(fileUri.getScheme());
			ulIntent.setAction(Intent.ACTION_SEND);
			ulIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
			startActivity(ulIntent);
		}
		
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
	
	/*
	 * if finish will initialize other components!
	 */
	public void waitForUserPassword()
	{
	    LayoutInflater li = LayoutInflater.from(this);
	    View promptsView = li.inflate(R.layout.searchprompt, null);
	    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
	    alertDialogBuilder.setView(promptsView);

	    final EditText userInput = (EditText) promptsView.findViewById(R.id.user_input);


	    // set dialog message
	    alertDialogBuilder.setCancelable(false).setNegativeButton("Go",new DialogInterface.OnClickListener() {
	            
	    	public void onClick(DialogInterface dialog,int id) {
	                String user_text = (userInput.getText()).toString();

	                if (KeyStoreHelper.init(getApplicationContext(), user_text) == null)
	                {
	                    String message = "The password you have entered is incorrect." + " \n \n" + "Please try again!";
	                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
	                    builder.setTitle("Error");
	                    builder.setMessage(message);
	                    builder.setPositiveButton("Cancel", null);
	                    
	                    builder.setNegativeButton("Retry", new DialogInterface.OnClickListener() {
	                        @Override
	                       public void onClick(DialogInterface dialog, int id) {
	                            waitForUserPassword();
	                       }
	                   });
	                    builder.create().show();

	                }
	                else
	                {
	            		setContentView(R.layout.activity_main);
	                    initialize();
	                    KeyStoreHelper.passwordWasCorrect();
	                }
	                }
	          }).setPositiveButton("Cancel",new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog,int id) {
	            dialog.dismiss();
	            }
	          }
	        );
	    AlertDialog alertDialog = alertDialogBuilder.create();
	    alertDialog.show();
	}

	/**
	 * Initialize the global variables and instantiates the needed classes
	 * will call this method after the user input the correct password in method waitForserPassword
	 * 
	 */
	private void initialize()
	{
		oh = new DatabaseHelper(context);
	
		KeyStoreHelper.load(context);
		
		if (KeyStoreHelper.getOwnPriv() == null)
		{
			// Key´s have to initialize
			KeyPair pair = CryptoHelper.generateAsymmetricKeyPair(1024);
			KeyStoreHelper.storeOwnAsymmetric(pair);
		}
		
		lvMainMenu = (ListView) findViewById(R.id.listView1);
		
        Vector<String> lvMenuItems = new Vector<String>();
        lvMenuItems.add("Server"); 
        lvMenuItems.add("Einstellungen");
        lvMenuItems.add("Profil");
        lvMenuItems.add("Import");
        lvMainMenu.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, lvMenuItems));
        lvMainMenu.setOnItemClickListener(this);
        
        // for GCM
       //GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
       //String regid = getRegistrationId(this);
	}
		
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this.getApplicationContext();
		
		waitForUserPassword();
	} 


	@Override
	protected void onStop() {
//		KeyStoreHelper.close(context);
		super.onStop();
	}

	@Override
	protected void onStart() {
//		KeyStoreHelper.load(context);
		super.onStart();
	}

	public boolean isNetworkConnected() {
        final ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.getState() == NetworkInfo.State.CONNECTED;
   }

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
    	else if (position == 2)
    	{
    		Intent intent = new Intent(context, ProfileActivity.class);
    		startActivity(intent);
    		// call profile!
    	}
    	else if (position == 3)
    	{
    		Intent intent = new Intent(context, ImportDataActivity.class);
    		startActivity(intent);
    	}
	}
}